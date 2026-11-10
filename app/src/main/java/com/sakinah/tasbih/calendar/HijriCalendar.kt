package com.sakinah.tasbih.calendar

import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** A day in the Hijri calendar. [month] runs from 1 (Muharram) to 12 (Dhu al-Hijjah). */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val yearMonth: HijriMonth get() = HijriMonth(year, month)
}

/** One Hijri month. Stored by [key], which stays readable in the saved settings. */
data class HijriMonth(val year: Int, val month: Int) : Comparable<HijriMonth> {
    init {
        require(month in 1..12) { "Hijri month out of range: $month" }
    }

    /** Months counted from the start of year 0, so two months can be compared or subtracted. */
    val index: Int get() = year * 12 + month - 1

    val key: String get() = "$year-$month"

    fun plus(months: Int): HijriMonth = fromIndex(index + months)

    fun next(): HijriMonth = plus(1)

    fun previous(): HijriMonth = plus(-1)

    override fun compareTo(other: HijriMonth): Int = index.compareTo(other.index)

    companion object {
        fun fromIndex(index: Int): HijriMonth = HijriMonth(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1)

        fun fromKey(key: String): HijriMonth? {
            val parts = key.split('-')
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
            return HijriMonth(year, month)
        }
    }
}

/**
 * The Umm al-Qura calendar that Android ships, moved to match what people actually follow.
 *
 * The official calendar is computed in advance, but religious months begin when the new moon is
 * seen, and countries announce that on their own, so the real date can be a day apart. Two
 * corrections cover this: [offsetDays] moves every day, and [corrections] moves the first day of
 * one month only, in days, with a positive value meaning it began later than computed. Moving a
 * month's first day changes the length of the month before it, never of any other month.
 */
class HijriCalendar(
    private val offsetDays: Int = 0,
    private val corrections: Map<HijriMonth, Int> = emptyMap(),
) {
    /** The months that have a known start, both previous and next, so every lookup can step once. */
    val firstMonth: HijriMonth = HijriMonth(FIRST_YEAR + 1, 1)
    val lastMonth: HijriMonth = HijriMonth(LAST_YEAR - 1, 12)

    /** The first Gregorian day this calendar can name, and the last. */
    val firstDay: LocalDate by lazy { requireNotNull(monthStart(firstMonth)) }
    val lastDay: LocalDate by lazy { requireNotNull(monthStart(lastMonth.next())).minusDays(1) }

    fun isSupported(month: HijriMonth): Boolean = month in firstMonth..lastMonth

    fun isSupported(date: LocalDate): Boolean = date in firstDay..lastDay

    fun fromGregorian(date: LocalDate): HijriDate? {
        if (!isSupported(date)) return null
        val epochDay = date.toEpochDay()
        val computed = try {
            HijrahDate.from(LocalDate.ofEpochDay(epochDay + offsetDays))
        } catch (_: DateTimeException) {
            return null
        }
        var month = HijriMonth(computed.get(ChronoField.YEAR), computed.get(ChronoField.MONTH_OF_YEAR))
        // A correction moves a boundary by a day or two, so the answer is at most a month away.
        repeat(MAX_STEPS) {
            val start = startEpochDay(month) ?: return null
            if (epochDay < start) {
                month = month.previous()
                return@repeat
            }
            val nextStart = startEpochDay(month.next()) ?: return null
            if (epochDay >= nextStart) {
                month = month.next()
                return@repeat
            }
            return HijriDate(month.year, month.month, (epochDay - start + 1).toInt())
        }
        return null
    }

    /** Null for a day the month does not have, such as the 30th of a 29-day month. */
    fun toGregorian(date: HijriDate): LocalDate? {
        val month = date.yearMonth
        val length = lengthOf(month) ?: return null
        if (date.day !in 1..length) return null
        val start = monthStart(month) ?: return null
        return start.plusDays((date.day - 1).toLong())
    }

    fun monthStart(month: HijriMonth): LocalDate? = startEpochDay(month)?.let(LocalDate::ofEpochDay)

    fun lengthOf(month: HijriMonth): Int? {
        val start = startEpochDay(month) ?: return null
        val nextStart = startEpochDay(month.next()) ?: return null
        return (nextStart - start).toInt()
    }

    fun correctionOf(month: HijriMonth): Int = corrections[month] ?: 0

    /**
     * Whether [month] may begin [days] later than computed. A month always has 29 or 30 days, and
     * moving one start changes both this month and the one before it, so both must stay valid.
     */
    fun canCorrect(month: HijriMonth, days: Int): Boolean {
        if (days !in -MAX_CORRECTION..MAX_CORRECTION || !isSupported(month)) return false
        val corrected = HijriCalendar(offsetDays, corrections + (month to days))
        return corrected.lengthOf(month.previous()) in VALID_LENGTHS &&
            corrected.lengthOf(month) in VALID_LENGTHS
    }

    private fun startEpochDay(month: HijriMonth): Long? {
        val computed = try {
            HijrahChronology.INSTANCE.date(month.year, month.month, 1).toEpochDay()
        } catch (_: DateTimeException) {
            return null
        }
        return computed - offsetDays + correctionOf(month)
    }

    companion object {
        const val MAX_OFFSET = 2
        const val MAX_CORRECTION = 1
        private const val MAX_STEPS = 4
        private val VALID_LENGTHS = 29..30

        private val YEARS = HijrahChronology.INSTANCE.range(ChronoField.YEAR)

        /** The years Android's Umm al-Qura data covers. Each edge year is kept as a margin. */
        val FIRST_YEAR: Int = YEARS.minimum.toInt()
        val LAST_YEAR: Int = YEARS.maximum.toInt()

        fun from(settings: CalendarSettings): HijriCalendar = HijriCalendar(
            offsetDays = settings.hijriOffsetDays.coerceIn(-MAX_OFFSET, MAX_OFFSET),
            corrections = settings.monthCorrections.mapNotNull { (key, days) ->
                HijriMonth.fromKey(key)?.let { it to days.coerceIn(-MAX_CORRECTION, MAX_CORRECTION) }
            }.toMap(),
        )
    }
}
