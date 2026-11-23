package com.sakinah.tasbih.prayer

import com.sakinah.tasbih.calendar.HijriCalendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerPhaseTest {
    private val place = PrayerPlace("الرياض", "", "", "SA", 24.68773, 46.72185, "Asia/Riyadh")
    private fun riyadh(settings: PrayerSettings = PrayerSettings()) = PrayerSchedule(place, settings, HijriCalendar())

    /** A Wednesday, whose Dhuhr is at 11:37 and Asr at 14:45. */
    private val wednesday = LocalDate.parse("2026-11-11")
    private val friday = LocalDate.parse("2026-11-13")

    private fun at(time: String, date: LocalDate = wednesday) = ZonedDateTime.of(date, LocalTime.parse(time), place.let { java.time.ZoneId.of(it.timeZone) }).toInstant()

    @Test
    fun `before the adhan the clock counts to it`() {
        val phase = riyadh().phase(at("10:00"))
        assertTrue(phase is PrayerPhase.ToAdhan)
        assertEquals(Prayer.DHUHR, (phase as PrayerPhase.ToAdhan).next.prayer)
    }

    @Test
    fun `from the adhan it counts to the iqama, then up from it for a quarter of an hour`() {
        val schedule = riyadh()
        val toIqama = schedule.phase(at("11:40"))
        assertTrue(toIqama is PrayerPhase.ToIqama)
        assertEquals(LocalTime.parse("11:57"), (toIqama as PrayerPhase.ToIqama).iqama.toLocalTime())
        val since = schedule.phase(at("12:05"))
        assertTrue(since is PrayerPhase.SinceIqama)
        assertEquals(LocalTime.parse("12:12"), (since as PrayerPhase.SinceIqama).until.toLocalTime())
        val after = schedule.phase(at("12:12"))
        assertEquals(Prayer.ASR, (after as PrayerPhase.ToAdhan).next.prayer)
    }

    @Test
    fun `Friday's Dhuhr has no iqama, and a zero turns a prayer's off`() {
        assertTrue(riyadh().phase(at("11:45", friday)) is PrayerPhase.ToAdhan)
        val none = riyadh(PrayerSettings(iqama = PrayerSettings.DEFAULT_IQAMA + (Prayer.DHUHR to 0)))
        assertTrue(none.phase(at("11:45")) is PrayerPhase.ToAdhan)
        assertTrue(none.iqamaChanges(none.day(wednesday)).none { it.toLocalTime().hour in 11..12 })
    }

    @Test
    fun `Fajr's count ends by sunrise`() {
        val schedule = riyadh(PrayerSettings(iqama = PrayerSettings.DEFAULT_IQAMA + (Prayer.FAJR to 60)))
        val day = schedule.day(wednesday)
        val beforeSunrise = day[Prayer.SUNRISE].minusMinutes(1).toInstant()
        val phase = schedule.phase(beforeSunrise)
        assertTrue(phase !is PrayerPhase.SinceIqama || !phase.until.isAfter(day[Prayer.SUNRISE]))
        assertTrue(schedule.phase(day[Prayer.SUNRISE].plusMinutes(1).toInstant()) is PrayerPhase.ToAdhan)
    }

    @Test
    fun `the widgets change at the iqama and at the end of the count after it`() {
        val schedule = riyadh()
        assertEquals(LocalTime.parse("11:57"), AlertPlanner.nextBoundary(schedule, at("11:40")).toLocalTime())
        assertEquals(LocalTime.parse("12:12"), AlertPlanner.nextBoundary(schedule, at("12:00")).toLocalTime())
        assertEquals(LocalTime.parse("14:45"), AlertPlanner.nextBoundary(schedule, at("12:30")).toLocalTime())
    }
}
