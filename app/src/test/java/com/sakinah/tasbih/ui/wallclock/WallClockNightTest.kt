package com.sakinah.tasbih.ui.wallclock

import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.prayer.PrayerPlace
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.prayer.PrayerSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wall clock dims from a while after the Isha to a while before the Fajr, by the place's own times. */
class WallClockNightTest {
    private val place = PrayerPlace("الرياض", "", "", "SA", 24.68773, 46.72185, "Asia/Riyadh")
    private val riyadh = PrayerSchedule(place, PrayerSettings(), HijriCalendar())
    private val zone = ZoneId.of(place.timeZone)

    /** A Wednesday, whose Isha is near half past six and Fajr near five. */
    private val wednesday = LocalDate.parse("2026-11-11")

    private fun at(time: String, date: LocalDate = wednesday) = ZonedDateTime.of(date, LocalTime.parse(time), zone)

    @Test
    fun `the day and the evening prayers stay bright`() {
        assertFalse(isNight(riyadh, at("12:00")))
        assertFalse(isNight(riyadh, at("18:00")))
    }

    @Test
    fun `the night dims, across midnight, until a while before the Fajr`() {
        assertTrue(isNight(riyadh, at("22:00")))
        assertTrue(isNight(riyadh, at("03:00", wednesday.plusDays(1))))
        assertFalse(isNight(riyadh, at("06:00", wednesday.plusDays(1))))
    }

    @Test
    fun `without a place, a plain night of eleven to four`() {
        assertTrue(isNight(null, at("23:30")))
        assertTrue(isNight(null, at("03:59")))
        assertFalse(isNight(null, at("12:00")))
    }
}
