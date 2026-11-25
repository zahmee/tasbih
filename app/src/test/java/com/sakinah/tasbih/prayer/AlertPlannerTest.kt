package com.sakinah.tasbih.prayer

import com.sakinah.tasbih.calendar.HijriCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPlannerTest {
    private val riyadh = PrayerSchedule(
        PrayerPlace("الرياض", "", "", "SA", 24.68773, 46.72185, "Asia/Riyadh"),
        PrayerSettings(),
        HijriCalendar(),
    )
    private val date = LocalDate.parse("2026-11-11")

    private fun at(time: String) = ZonedDateTime.of(date, LocalTime.parse(time), riyadh.zone).toInstant()

    @Test
    fun `nothing is planned while every alert is off`() {
        assertNull(AlertPlanner.nextAlert(riyadh, AlertSettings(), at("10:00")))
    }

    @Test
    fun `the next alert is the soonest of the prayers that are on`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.DHUHR to PrayerAlert(AlertStyle.SOUND), Prayer.MAGHRIB to PrayerAlert(AlertStyle.ADHAN)))
        val dhuhr = AlertPlanner.nextAlert(riyadh, settings, at("10:00"))!!
        assertEquals(Prayer.DHUHR, dhuhr.prayer)
        assertEquals(AlertKind.PRAYER, dhuhr.kind)
        assertEquals(LocalTime.parse("11:37"), dhuhr.at.toLocalTime())
        val maghrib = AlertPlanner.nextAlert(riyadh, settings, at("12:00"))!!
        assertEquals(Prayer.MAGHRIB, maghrib.prayer)
    }

    @Test
    fun `a reminder comes the chosen minutes before its prayer`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.ASR to PrayerAlert(AlertStyle.OFF, reminderMinutes = 10)))
        val reminder = AlertPlanner.nextAlert(riyadh, settings, at("12:00"))!!
        assertEquals(AlertKind.REMINDER, reminder.kind)
        assertEquals(reminder.prayerTime.minusMinutes(10), reminder.at)
        assertEquals(Prayer.ASR, reminder.prayer)
    }

    @Test
    fun `after the last alert of the day comes tomorrow's`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.FAJR to PrayerAlert(AlertStyle.ADHAN)))
        val fajr = AlertPlanner.nextAlert(riyadh, settings, at("21:00"))!!
        assertEquals(Prayer.FAJR, fajr.prayer)
        assertEquals(date.plusDays(1), fajr.at.toLocalDate())
    }

    @Test
    fun `widgets move on at every time of the day, sunrise included`() {
        // Fajr's iqama at 05:11 first, then the end of the count after it, then sunrise.
        assertEquals(LocalTime.parse("05:11"), AlertPlanner.nextBoundary(riyadh, at("05:00")).toLocalTime())
        assertEquals(LocalTime.parse("06:06"), AlertPlanner.nextBoundary(riyadh, at("05:30")).toLocalTime())
        // Dhuhr's iqama at 11:57 and the end of the count after it at 12:12 come before Asr.
        assertEquals(LocalTime.parse("12:12"), AlertPlanner.nextBoundary(riyadh, at("12:00")).toLocalTime())
        assertEquals(LocalTime.parse("14:45"), AlertPlanner.nextBoundary(riyadh, at("12:30")).toLocalTime())
        val night = AlertPlanner.nextBoundary(riyadh, at("20:00"))
        assertEquals(date.plusDays(1), night.toLocalDate())
        // Midnight, where the widgets' weekday and Gregorian date turn, comes before Fajr.
        assertEquals(LocalTime.MIDNIGHT, night.toLocalTime())
    }

    @Test
    fun `an alert at the widgets' change wakes once for both`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.DHUHR to PrayerAlert(AlertStyle.ADHAN)))
        val wake = AlertPlanner.nextWake(riyadh, settings, at("10:00"), widgetsPlaced = true)!!
        assertEquals(LocalTime.parse("11:37"), wake.at.toLocalTime())
        assertEquals(Prayer.DHUHR, wake.event?.prayer)
        assertEquals(false, wake.urgent)
    }

    @Test
    fun `the widgets wake alone when their change comes before any alert`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.MAGHRIB to PrayerAlert(AlertStyle.SOUND)))
        val wake = AlertPlanner.nextWake(riyadh, settings, at("10:00"), widgetsPlaced = true)!!
        assertEquals(LocalTime.parse("11:37"), wake.at.toLocalTime())
        assertNull(wake.event)
        val alone = AlertPlanner.nextWake(riyadh, settings, at("10:00"), widgetsPlaced = false)!!
        assertEquals(Prayer.MAGHRIB, alone.event?.prayer)
    }

    @Test
    fun `nothing wakes with no alert and no widget`() {
        assertNull(AlertPlanner.nextWake(riyadh, AlertSettings(), at("10:00"), widgetsPlaced = false))
    }

    @Test
    fun `a prayer close behind its reminder rings as an alarm clock`() {
        val close = AlertSettings(alerts = mapOf(Prayer.ASR to PrayerAlert(AlertStyle.ADHAN, reminderMinutes = 5)))
        val reminder = AlertPlanner.nextWake(riyadh, close, at("12:00"), widgetsPlaced = false)!!
        assertEquals(AlertKind.REMINDER, reminder.event?.kind)
        assertEquals(false, reminder.urgent)
        val prayer = AlertPlanner.nextWake(riyadh, close, reminder.at.toInstant().plusSeconds(1), widgetsPlaced = false)!!
        assertEquals(AlertKind.PRAYER, prayer.event?.kind)
        assertEquals(true, prayer.urgent)
        val far = AlertSettings(alerts = mapOf(Prayer.ASR to PrayerAlert(AlertStyle.ADHAN, reminderMinutes = 15)))
        val calm = AlertPlanner.nextWake(riyadh, far, at("14:40"), widgetsPlaced = false)!!
        assertEquals(false, calm.urgent)
    }

    @Test
    fun `a day of wakes is planned at once, soonest first, each moment once`() {
        val settings = AlertSettings(
            alerts = mapOf(
                Prayer.DHUHR to PrayerAlert(AlertStyle.ADHAN, reminderMinutes = 10),
                Prayer.MAGHRIB to PrayerAlert(AlertStyle.SOUND),
            ),
        )
        val wakes = AlertPlanner.upcomingWakes(riyadh, settings, at("10:00"), widgetsPlaced = true)
        assertEquals(AlertPlanner.MAX_WAKES, wakes.size)
        val times = wakes.map { it.at.toInstant() }
        assertEquals(times.sorted().distinct(), times)
        assertEquals(true, times.all { it.isAfter(at("10:00")) })
        // The reminder first, then Dhuhr itself, which is also where the widgets change.
        assertEquals(LocalTime.parse("11:27"), wakes[0].at.toLocalTime())
        assertEquals(AlertKind.REMINDER, wakes[0].event?.kind)
        assertEquals(LocalTime.parse("11:37"), wakes[1].at.toLocalTime())
        assertEquals(AlertKind.PRAYER, wakes[1].event?.kind)
        // Dhuhr's iqama moves the widgets only.
        assertEquals(LocalTime.parse("11:57"), wakes[2].at.toLocalTime())
        assertNull(wakes[2].event)
        // The plan reaches into the next day, so the day after's alerts are set before it comes.
        assertEquals(true, wakes.any { it.at.toLocalDate() == date.plusDays(1) && it.event?.prayer == Prayer.DHUHR })
    }

    @Test
    fun `without widgets only the alerts wake, for days ahead`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.FAJR to PrayerAlert(AlertStyle.ADHAN)))
        val wakes = AlertPlanner.upcomingWakes(riyadh, settings, at("10:00"), widgetsPlaced = false)
        assertEquals(listOf(date.plusDays(1), date.plusDays(2)), wakes.map { it.at.toLocalDate() })
        assertEquals(true, wakes.all { it.event?.prayer == Prayer.FAJR })
        assertEquals(emptyList<Wake>(), AlertPlanner.upcomingWakes(riyadh, AlertSettings(), at("10:00"), widgetsPlaced = false))
    }

    @Test
    fun `only the wake at hand may ring as an alarm clock`() {
        val close = AlertSettings(alerts = mapOf(Prayer.ASR to PrayerAlert(AlertStyle.ADHAN, reminderMinutes = 5)))
        // Hours ahead, an alarm clock would show in the status bar all that while.
        assertEquals(emptyList<Wake>(), AlertPlanner.upcomingWakes(riyadh, close, at("12:00"), widgetsPlaced = false).filter { it.urgent })
        val afterReminder = AlertPlanner.upcomingWakes(riyadh, close, at("14:41"), widgetsPlaced = false)
        assertEquals(AlertKind.PRAYER, afterReminder.first().event?.kind)
        assertEquals(true, afterReminder.first().urgent)
        assertEquals(1, afterReminder.count { it.urgent })
    }

    @Test
    fun `an alarm delivered late still raises the alert that came due meanwhile`() {
        // Dhuhr is at 11:37. Its reminder's alarm, set for 11:27, arrives at 11:38.
        val settings = AlertSettings(alerts = mapOf(Prayer.DHUHR to PrayerAlert(AlertStyle.ADHAN, reminderMinutes = 10)))
        val due = AlertPlanner.due(riyadh, settings, at("11:27"), at("11:38"))
        assertEquals(listOf(AlertKind.PRAYER), due.map { it.kind })
        assertEquals(Prayer.DHUHR, due.single().prayer)
        // On time, the reminder alone is due, and the prayer is what comes next.
        assertEquals(listOf(AlertKind.REMINDER), AlertPlanner.due(riyadh, settings, at("11:27"), at("11:27")).map { it.kind })
        assertEquals(AlertKind.PRAYER, AlertPlanner.nextAlert(riyadh, settings, at("11:27"))?.kind)
    }

    @Test
    fun `an alert long overdue is not raised`() {
        val settings = AlertSettings(alerts = mapOf(Prayer.DHUHR to PrayerAlert(AlertStyle.SOUND), Prayer.ASR to PrayerAlert(AlertStyle.SOUND)))
        assertEquals(emptyList<AlertEvent>(), AlertPlanner.due(riyadh, settings, at("11:37"), at("13:00")))
        // After hours switched off, only what fell in the last minutes is raised.
        assertEquals(listOf(Prayer.ASR), AlertPlanner.due(riyadh, settings, at("11:37"), at("14:50")).map { it.prayer })
        assertEquals(emptyList<AlertEvent>(), AlertPlanner.due(riyadh, settings, at("11:37"), at("11:00")))
    }
}
