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
}
