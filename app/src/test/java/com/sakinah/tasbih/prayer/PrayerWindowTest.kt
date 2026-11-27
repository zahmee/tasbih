package com.sakinah.tasbih.prayer

import com.sakinah.tasbih.calendar.HijriCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerWindowTest {
    private val place = PrayerPlace("الرياض", "", "", "SA", 24.68773, 46.72185, "Asia/Riyadh")
    private val schedule = PrayerSchedule(place, PrayerSettings(), HijriCalendar())
    private val wednesday = LocalDate.parse("2026-11-11")
    private fun at(time: String) = ZonedDateTime.of(wednesday, LocalTime.parse(time), ZoneId.of(place.timeZone)).toInstant()

    @Test
    fun `Fajr's time runs until sunrise`() {
        val day = schedule.day(wednesday)
        val window = schedule.window(day[Prayer.FAJR].plusMinutes(5).toInstant())
        assertEquals(Prayer.FAJR, window.prayer?.prayer)
        assertEquals(Prayer.SUNRISE, window.end.prayer)
    }

    @Test
    fun `between sunrise and Dhuhr no prayer is due`() {
        val window = schedule.window(at("09:00"))
        assertNull(window.prayer)
        assertEquals(Prayer.DHUHR, window.end.prayer)
        assertTrue(window.passed(at("09:00")) in 0f..1f)
    }

    @Test
    fun `Asr runs until Maghrib, and before Fajr it is still Isha's time`() {
        assertEquals(Prayer.MAGHRIB, schedule.window(at("15:30")).end.prayer)
        val night = schedule.window(at("02:00"))
        assertEquals(Prayer.ISHA, night.prayer?.prayer)
        assertEquals(Prayer.FAJR, night.end.prayer)
    }

    @Test
    fun `the share passed starts at nothing and ends whole`() {
        val window = schedule.window(at("15:30"))
        assertEquals(0f, window.passed(window.start.toInstant()), 0f)
        assertEquals(1f, window.passed(window.end.time.toInstant()), 0f)
    }
}
