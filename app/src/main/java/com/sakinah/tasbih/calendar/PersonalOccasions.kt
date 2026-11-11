package com.sakinah.tasbih.calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** An occasion of the user's own on a Hijri day, kept from year to year, such as a birthday. */
data class PersonalOccasion(val id: String, val name: String, val month: Int, val day: Int)

/** A personal occasion on the day it next falls. [daysAway] is 0 for today. */
data class UpcomingPersonal(val occasion: PersonalOccasion, val date: LocalDate, val daysAway: Long)

object PersonalOccasions {
    const val MAX_NAME = 40
    private const val SEPARATOR = '|'

    /** Saved as «month|day|id|name», the name last so that it may hold any character. */
    fun encode(occasions: List<PersonalOccasion>): Set<String> =
        occasions.map { "${it.month}$SEPARATOR${it.day}$SEPARATOR${it.id}$SEPARATOR${it.name}" }.toSet()

    fun decode(saved: Set<String>): List<PersonalOccasion> = saved.mapNotNull { line ->
        val parts = line.split(SEPARATOR, limit = 4)
        val month = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..12 } ?: return@mapNotNull null
        val day = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..30 } ?: return@mapNotNull null
        val id = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val name = parts.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        PersonalOccasion(id, name, month, day)
    }.sortedWith(compareBy({ it.month }, { it.day }, { it.name }))

    /**
     * The day [occasion] falls in Hijri [year]. A 30th in a month of 29 days that year is kept
     * on the 29th, its last day, so it is never skipped.
     */
    fun dateIn(calendar: HijriCalendar, year: Int, occasion: PersonalOccasion): LocalDate? {
        val month = HijriMonth(year, occasion.month)
        val length = calendar.lengthOf(month) ?: return null
        return calendar.toGregorian(HijriDate(year, occasion.month, occasion.day.coerceAtMost(length)))
    }

    fun on(calendar: HijriCalendar, date: LocalDate, occasions: List<PersonalOccasion>): List<PersonalOccasion> {
        if (occasions.isEmpty()) return emptyList()
        val year = calendar.fromGregorian(date)?.year ?: return emptyList()
        return occasions.filter { dateIn(calendar, year, it) == date }
    }

    /** Each personal occasion on the day it next falls, from [today], soonest first. */
    fun upcoming(calendar: HijriCalendar, today: LocalDate, occasions: List<PersonalOccasion>): List<UpcomingPersonal> {
        val year = calendar.fromGregorian(today)?.year ?: return emptyList()
        return occasions.mapNotNull { occasion ->
            (year..year + 1).firstNotNullOfOrNull { y -> dateIn(calendar, y, occasion)?.takeIf { it >= today } }
                ?.let { UpcomingPersonal(occasion, it, ChronoUnit.DAYS.between(today, it)) }
        }.sortedWith(compareBy({ it.date }, { it.occasion.name }))
    }
}
