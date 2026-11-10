package com.sakinah.tasbih.calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How widely an occasion is marked. [CONTESTED] ones are shown only when the user asks for them. */
enum class OccasionKind { MAJOR, MINOR, CONTESTED }

/** Occasions on a fixed Hijri day. Their names are in the app's strings. */
enum class Occasion(val month: Int, val day: Int, val kind: OccasionKind) {
    NEW_YEAR(1, 1, OccasionKind.MAJOR),
    TASUA(1, 9, OccasionKind.MINOR),
    ASHURA(1, 10, OccasionKind.MAJOR),
    MAWLID(3, 12, OccasionKind.CONTESTED),
    ISRA_MIRAJ(7, 27, OccasionKind.CONTESTED),
    MID_SHABAN(8, 15, OccasionKind.CONTESTED),
    RAMADAN(9, 1, OccasionKind.MAJOR),
    LAST_TEN_NIGHTS(9, 21, OccasionKind.MINOR),
    EID_AL_FITR(10, 1, OccasionKind.MAJOR),
    SIX_OF_SHAWWAL(10, 2, OccasionKind.MINOR),
    FIRST_TEN_OF_DHUL_HIJJAH(12, 1, OccasionKind.MINOR),
    ARAFAH(12, 9, OccasionKind.MAJOR),
    EID_AL_ADHA(12, 10, OccasionKind.MAJOR),
    TASHREEQ(12, 11, OccasionKind.MINOR),
    ;

    fun isShown(showOther: Boolean): Boolean = kind != OccasionKind.CONTESTED || showOther
}

/** An occasion on the day it next falls. [daysAway] is 0 for today. */
data class UpcomingOccasion(val occasion: Occasion, val date: LocalDate, val daysAway: Long)

object Occasions {
    fun on(date: HijriDate, showOther: Boolean): List<Occasion> =
        Occasion.entries.filter { it.month == date.month && it.day == date.day && it.isShown(showOther) }

    /**
     * The 13th, 14th and 15th of each month. Not in Ramadan, which is fasted already, nor on the
     * 13th of Dhu al-Hijjah, which is one of the days of Tashreeq when fasting is not allowed.
     */
    fun isWhiteDay(date: HijriDate): Boolean =
        date.day in 13..15 && date.month != 9 && !(date.month == 12 && date.day == 13)

    /** Every shown occasion on the day it next falls, from [today], soonest first. */
    fun upcoming(
        calendar: HijriCalendar,
        today: LocalDate,
        showOther: Boolean,
        kinds: Set<OccasionKind> = OccasionKind.entries.toSet(),
    ): List<UpcomingOccasion> {
        val year = calendar.fromGregorian(today)?.year ?: return emptyList()
        return Occasion.entries
            .filter { it.kind in kinds && it.isShown(showOther) }
            .mapNotNull { occasion ->
                (year..year + 1).firstNotNullOfOrNull { y ->
                    calendar.toGregorian(HijriDate(y, occasion.month, occasion.day))?.takeIf { it >= today }
                }?.let { UpcomingOccasion(occasion, it, ChronoUnit.DAYS.between(today, it)) }
            }
            .sortedWith(compareBy({ it.date }, { it.occasion.ordinal }))
    }

    /** The next of the big occasions, for the widget that counts down to it. */
    fun nextMajor(calendar: HijriCalendar, today: LocalDate): UpcomingOccasion? =
        upcoming(calendar, today, showOther = false, kinds = setOf(OccasionKind.MAJOR)).firstOrNull()
}
