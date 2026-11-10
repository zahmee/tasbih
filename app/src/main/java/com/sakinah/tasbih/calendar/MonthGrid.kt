package com.sakinah.tasbih.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** A month of the leading calendar, which fills a grid. */
sealed interface CalendarMonth {
    /** The days in order. Empty when the calendar cannot name this month. */
    fun days(calendar: HijriCalendar): List<LocalDate>

    fun plus(months: Int): CalendarMonth

    /** Months from [other] to this one, so a pager can keep a page number for each. */
    fun minus(other: CalendarMonth): Int

    data class Hijri(val month: HijriMonth) : CalendarMonth {
        override fun days(calendar: HijriCalendar): List<LocalDate> {
            val start = calendar.monthStart(month) ?: return emptyList()
            val length = calendar.lengthOf(month) ?: return emptyList()
            return List(length) { start.plusDays(it.toLong()) }
        }

        override fun plus(months: Int): CalendarMonth = Hijri(month.plus(months))

        override fun minus(other: CalendarMonth): Int = month.index - (other as Hijri).month.index
    }

    data class Gregorian(val month: YearMonth) : CalendarMonth {
        override fun days(calendar: HijriCalendar): List<LocalDate> =
            List(month.lengthOfMonth()) { month.atDay(it + 1) }

        override fun plus(months: Int): CalendarMonth = Gregorian(month.plusMonths(months.toLong()))

        override fun minus(other: CalendarMonth): Int {
            val that = (other as Gregorian).month
            return (month.year - that.year) * 12 + (month.monthValue - that.monthValue)
        }
    }

    companion object {
        /** The month of [primary] that holds [date]. */
        fun containing(date: LocalDate, primary: PrimaryCalendar, calendar: HijriCalendar): CalendarMonth? =
            when (primary) {
                PrimaryCalendar.GREGORIAN -> Gregorian(YearMonth.from(date))
                PrimaryCalendar.HIJRI -> calendar.fromGregorian(date)?.let { Hijri(it.yearMonth) }
            }

        /** The first and last months of [primary] that the Hijri data covers in full. */
        fun range(primary: PrimaryCalendar, calendar: HijriCalendar): Pair<CalendarMonth, CalendarMonth> =
            when (primary) {
                PrimaryCalendar.HIJRI -> Hijri(calendar.firstMonth) to Hijri(calendar.lastMonth)
                PrimaryCalendar.GREGORIAN -> {
                    val first = YearMonth.from(calendar.firstDay).let {
                        if (calendar.firstDay.dayOfMonth == 1) it else it.plusMonths(1)
                    }
                    val last = YearMonth.from(calendar.lastDay).let {
                        if (calendar.lastDay == it.atEndOfMonth()) it else it.minusMonths(1)
                    }
                    Gregorian(first) to Gregorian(last)
                }
            }
    }
}

/** One square of a month grid. [hijri] is null only outside the Hijri data. */
data class DayCell(val date: LocalDate, val hijri: HijriDate?)

/**
 * A month laid out in weeks of seven, starting on [firstDayOfWeek]. Days of other months are null,
 * and there are always six weeks, so the grid keeps its size from one month to the next.
 */
fun buildMonthGrid(month: CalendarMonth, calendar: HijriCalendar, firstDayOfWeek: DayOfWeek): List<List<DayCell?>> {
    val days = month.days(calendar)
    if (days.isEmpty()) return List(WEEKS) { List(DAYS_IN_WEEK) { null } }
    val leading = Math.floorMod(days.first().dayOfWeek.value - firstDayOfWeek.value, DAYS_IN_WEEK)
    val cells = List<DayCell?>(leading) { null } +
        days.map { DayCell(it, calendar.fromGregorian(it)) }
    val padded = cells + List(WEEKS * DAYS_IN_WEEK - cells.size) { null }
    return padded.chunked(DAYS_IN_WEEK)
}

/** The seven days of a week in the order a grid shows them. */
fun weekDays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = List(DAYS_IN_WEEK) { firstDayOfWeek.plus(it.toLong()) }

const val WEEKS = 6
const val DAYS_IN_WEEK = 7
