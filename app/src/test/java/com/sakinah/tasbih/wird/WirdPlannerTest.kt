package com.sakinah.tasbih.wird

import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.calendar.HijriDate
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.prayer.Prayer
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal val Zone: ZoneId = ZoneId.of("Asia/Riyadh")

/** Prayer times that are the same every day, so a test can say when each falls. */
internal val FixedPrayers = PrayerClock { date, prayer ->
    val time = when (prayer) {
        Prayer.FAJR -> LocalTime.of(5, 0)
        Prayer.SUNRISE -> LocalTime.of(6, 20)
        Prayer.DHUHR -> LocalTime.of(12, 0)
        Prayer.ASR -> LocalTime.of(15, 20)
        Prayer.MAGHRIB -> LocalTime.of(17, 40)
        Prayer.ISHA -> LocalTime.of(19, 0)
    }
    date.atTime(time).atZone(Zone)
}

internal fun calendar(reset: DailyResetSettings = DailyResetSettings(), prayers: PrayerClock? = FixedPrayers) =
    WirdCalendar(HijriCalendar(), prayers, Zone, reset)

internal fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant = date.atTime(hour, minute).atZone(Zone).toInstant()

class WirdPlannerTest {
    private val hijri = HijriCalendar()
    private val monday = LocalDate.of(2026, 11, 23)

    @Test
    fun `a weekly task falls on its days only`() {
        val task = WirdTask(title = "صيام", repeat = WirdRepeat.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertTrue(WirdPlanner.occursOn(task, monday, hijri))
        assertFalse(WirdPlanner.occursOn(task, monday.plusDays(1), hijri))
        assertTrue(WirdPlanner.occursOn(task, monday.plusDays(3), hijri))
        assertEquals(monday.plusDays(3), WirdPlanner.nextDay(task, monday, hijri))
    }

    @Test
    fun `the white days fall on the thirteenth to the fifteenth of the Hijri month`() {
        val task = WirdPreset.WHITE_DAYS.task("الأيام البيض", 0, 0)
        val thirteenth = requireNotNull(hijri.toGregorian(HijriDate(1448, 6, 13)))
        assertTrue(WirdPlanner.occursOn(task, thirteenth, hijri))
        assertTrue(WirdPlanner.occursOn(task, thirteenth.plusDays(2), hijri))
        assertFalse(WirdPlanner.occursOn(task, thirteenth.minusDays(1), hijri))
        assertFalse(WirdPlanner.occursOn(task, thirteenth.plusDays(3), hijri))
    }

    @Test
    fun `the thirtieth falls on the last day of a month of twenty-nine`() {
        val short = generateSequence(HijriMonth(1448, 1)) { it.next() }.first { hijri.lengthOf(it) == 29 }
        val task = WirdTask(title = "آخر الشهر", repeat = WirdRepeat.HIJRI_MONTHLY, hijriDays = setOf(30))
        val last = requireNotNull(hijri.toGregorian(HijriDate(short.year, short.month, 29)))
        assertTrue(WirdPlanner.occursOn(task, last, hijri))
        assertFalse(WirdPlanner.occursOn(task, last.minusDays(1), hijri))
    }

    @Test
    fun `a task by a prayer sits before or after its time, and a clock task at its minute`() {
        val calendar = calendar()
        val after = WirdTask(title = "", timeKind = WirdTimeKind.PRAYER, prayer = Prayer.FAJR, offsetMinutes = 15)
        val before = after.copy(offsetMinutes = -45)
        val clock = WirdTask(title = "", timeKind = WirdTimeKind.CLOCK, minuteOfDay = 21 * 60 + 30)
        assertEquals(LocalTime.of(5, 15), WirdPlanner.timeOn(after, monday, calendar)?.toLocalTime())
        assertEquals(LocalTime.of(4, 15), WirdPlanner.timeOn(before, monday, calendar)?.toLocalTime())
        assertEquals(LocalTime.of(21, 30), WirdPlanner.timeOn(clock, monday, calendar)?.toLocalTime())
        assertNull(WirdPlanner.timeOn(WirdTask(title = ""), monday, calendar))
    }

    @Test
    fun `without a place a task by a prayer has no time and no reminder`() {
        val calendar = calendar(prayers = null)
        val task = WirdTask(title = "", timeKind = WirdTimeKind.PRAYER, prayer = Prayer.ASR, offsetMinutes = 10, remind = true)
        assertNull(WirdPlanner.timeOn(task, monday, calendar))
        assertNull(WirdPlanner.nextReminder(listOf(task), at(monday, 8), calendar))
    }

    @Test
    fun `the next reminder is the earliest of the tasks that remind`() {
        val calendar = calendar()
        val evening = WirdPreset.EVENING.task("المساء", 1, 0).copy(id = 1, remind = true)
        val morning = WirdPreset.MORNING.task("الصباح", 2, 0).copy(id = 2, remind = true)
        val quiet = WirdTask(id = 3, title = "صامتة", timeKind = WirdTimeKind.CLOCK, minuteOfDay = 9 * 60, remind = false)
        val untimed = WirdTask(id = 4, title = "بلا وقت", remind = true)
        val tasks = listOf(evening, morning, quiet, untimed)
        val atNine = WirdPlanner.nextReminder(tasks, at(monday, 9), calendar)
        assertEquals(1L, atNine?.task?.id)
        assertEquals(at(monday, 15, 35), atNine?.at?.toInstant())
        // After the evening's, the next is tomorrow's morning.
        val atNight = WirdPlanner.nextReminder(tasks, at(monday, 16), calendar)
        assertEquals(2L, atNight?.task?.id)
        assertEquals(monday.plusDays(1), atNight?.day)
    }

    @Test
    fun `a task of one time reminds once, and never after its day`() {
        val calendar = calendar()
        val task = WirdTask(id = 1, title = "زيارة", repeat = WirdRepeat.ONCE, date = monday, timeKind = WirdTimeKind.CLOCK, minuteOfDay = 18 * 60, remind = true)
        assertEquals(at(monday, 18), WirdPlanner.nextReminder(listOf(task), at(monday, 9), calendar)?.at?.toInstant())
        assertNull(WirdPlanner.nextReminder(listOf(task), at(monday, 19), calendar))
    }

    @Test
    fun `the reminders between two moments leave out the first and keep the last`() {
        val calendar = calendar()
        val morning = WirdPreset.MORNING.task("الصباح", 1, 0).copy(id = 1, remind = true)
        val due = WirdPlanner.remindersBetween(listOf(morning), at(monday, 5, 15), at(monday.plusDays(1), 5, 15), calendar)
        assertEquals(listOf(monday.plusDays(1)), due.map { it.day })
    }
}
