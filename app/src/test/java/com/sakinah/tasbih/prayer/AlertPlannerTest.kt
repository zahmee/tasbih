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
        assertEquals(LocalTime.parse("06:06"), AlertPlanner.nextBoundary(riyadh, at("05:00")).toLocalTime())
        assertEquals(LocalTime.parse("14:45"), AlertPlanner.nextBoundary(riyadh, at("12:00")).toLocalTime())
        val night = AlertPlanner.nextBoundary(riyadh, at("20:00"))
        assertEquals(date.plusDays(1), night.toLocalDate())
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
}
