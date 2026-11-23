package com.sakinah.tasbih.wird

import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.data.dailyResetDay
import com.sakinah.tasbih.prayer.Prayer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** A prayer's time on a date, or null where none is known, as before a place is chosen. */
fun interface PrayerClock {
    fun time(date: LocalDate, prayer: Prayer): ZonedDateTime?
}

/**
 * What a task's days and times are read against: the Hijri calendar with the user's
 * corrections, the prayer times of the user's place, and the hour at which the app's day turns,
 * the same one at which the day's adhkar start afresh.
 */
class WirdCalendar(
    val hijri: HijriCalendar,
    val prayers: PrayerClock?,
    val zone: ZoneId = ZoneId.systemDefault(),
    val reset: DailyResetSettings = DailyResetSettings(),
) {
    /** The app's day at [now]: before the turning hour it is still the day before. */
    fun today(now: Instant): LocalDate = dailyResetDay(now.atZone(zone), reset)

    /** When [day] begins and ends, by the turning hour. */
    fun window(day: LocalDate): Pair<Instant, Instant> =
        day.atTime(reset.time).atZone(zone).toInstant() to day.plusDays(1).atTime(reset.time).atZone(zone).toInstant()
}

/** A reminder to raise: [task] on its [day], at [at]. */
data class WirdReminder(val task: WirdTask, val day: LocalDate, val at: ZonedDateTime)

object WirdPlanner {
    /** How far ahead a reminder is looked for: a Hijri month and more. */
    const val HORIZON_DAYS = 62L

    /** A task whose reminder came due this long ago is not announced any more. */
    val STALE: java.time.Duration = java.time.Duration.ofHours(2)

    fun occursOn(task: WirdTask, date: LocalDate, hijri: HijriCalendar): Boolean = when (task.repeat) {
        WirdRepeat.ONCE -> task.date == date
        WirdRepeat.DAILY -> true
        WirdRepeat.WEEKLY -> date.dayOfWeek in task.weekdays
        WirdRepeat.HIJRI_MONTHLY -> hijri.fromGregorian(date)?.let { day ->
            // The thirtieth falls on the month's last day in a month of twenty-nine.
            day.day in task.hijriDays ||
                (WirdTask.LAST_HIJRI_DAY in task.hijriDays && day.day == 29 && hijri.lengthOf(day.yearMonth) == 29)
        } ?: false
    }

    /** The task's time on [date], or null for a task with no time or no prayer times to go by. */
    fun timeOn(task: WirdTask, date: LocalDate, calendar: WirdCalendar): ZonedDateTime? = when (task.timeKind) {
        WirdTimeKind.NONE -> null
        WirdTimeKind.CLOCK -> date.atStartOfDay(calendar.zone).plusMinutes(task.minuteOfDay.coerceIn(0, 1439).toLong())
        WirdTimeKind.PRAYER -> task.prayer?.let { prayer ->
            calendar.prayers?.time(date, prayer)?.plusMinutes(task.offsetMinutes.toLong())?.withZoneSameInstant(calendar.zone)
        }
    }

    /** The first day after [after] on which the task falls, within a year and a little more. */
    fun nextDay(task: WirdTask, after: LocalDate, hijri: HijriCalendar): LocalDate? {
        if (task.repeat == WirdRepeat.ONCE) return task.date?.takeIf { it.isAfter(after) }
        return (1L..400L).asSequence().map(after::plusDays).firstOrNull { occursOn(task, it, hijri) }
    }

    /** Every reminder that falls after [from] and no later than [to], earliest first. */
    fun remindersBetween(tasks: List<WirdTask>, from: Instant, to: Instant, calendar: WirdCalendar): List<WirdReminder> {
        if (!to.isAfter(from)) return emptyList()
        val first = from.atZone(calendar.zone).toLocalDate().minusDays(1)
        val last = to.atZone(calendar.zone).toLocalDate()
        val days = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.toList()
        return tasks.filter(WirdTask::reminds).flatMap { task ->
            days.filter { occursOn(task, it, calendar.hijri) }.mapNotNull { day ->
                timeOn(task, day, calendar)?.takeIf { it.toInstant().isAfter(from) && !it.toInstant().isAfter(to) }
                    ?.let { WirdReminder(task, day, it) }
            }
        }.sortedBy { it.at.toInstant() }
    }

    /** The first reminder after [now], or null when none is on. */
    fun nextReminder(tasks: List<WirdTask>, now: Instant, calendar: WirdCalendar): WirdReminder? {
        val start = now.atZone(calendar.zone).toLocalDate().minusDays(1)
        return tasks.filter(WirdTask::reminds).mapNotNull { task ->
            (0L..HORIZON_DAYS).asSequence()
                .map(start::plusDays)
                .filter { occursOn(task, it, calendar.hijri) }
                .mapNotNull { day -> timeOn(task, day, calendar)?.let { WirdReminder(task, day, it) } }
                .firstOrNull { it.at.toInstant().isAfter(now) }
        }.minByOrNull { it.at.toInstant() }
    }
}
