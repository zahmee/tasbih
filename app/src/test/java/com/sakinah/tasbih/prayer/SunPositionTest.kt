package com.sakinah.tasbih.prayer

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunPositionTest {
    private val riyadh = ZoneId.of("Asia/Riyadh")

    @Test
    fun `the declination follows the seasons`() {
        assertEquals(0.0, SunPosition.declination(LocalDate.parse("2026-03-20")), 0.5)
        assertEquals(23.44, SunPosition.declination(LocalDate.parse("2026-06-21")), 0.1)
        assertEquals(-23.44, SunPosition.declination(LocalDate.parse("2026-12-21")), 0.1)
    }

    @Test
    fun `the sun rises in the east, crosses the south and sets in the west, seen from Abha`() {
        fun at(time: String) = SunPosition.at(
            ZonedDateTime.of(LocalDate.parse("2026-11-11"), java.time.LocalTime.parse(time), riyadh).toInstant(),
            18.2164,
            42.5053,
        )
        val morning = at("08:00")
        assertTrue(morning.azimuth in 100.0..150.0)
        assertTrue(morning.elevation in 10.0..40.0)
        val noon = at("11:42")
        assertEquals(180.0, noon.azimuth, 5.0)
        // At noon the sun is ninety degrees less the gap between latitude and declination.
        assertEquals(90 - (18.2164 - SunPosition.declination(LocalDate.parse("2026-11-11"))), noon.elevation, 0.5)
        val evening = at("16:30")
        assertTrue(evening.azimuth in 210.0..260.0)
        assertTrue(at("23:00").elevation < 0)
    }

    @Test
    fun `the sun stands over the Kaaba in late May and mid July`() {
        val moments = SunPosition.overKaaba(2026).map { it.atZone(riyadh) }
        assertEquals(2, moments.size)
        val (may, july) = moments
        // The published times are 12:18 on 27 or 28 May and 12:27 on 15 or 16 July, Makkah time.
        assertEquals(5, may.monthValue)
        assertTrue(may.dayOfMonth in 27..28)
        assertEquals(12 * 60 + 18.0, (may.hour * 60 + may.minute).toDouble(), 2.0)
        assertEquals(7, july.monthValue)
        assertTrue(july.dayOfMonth in 15..16)
        assertEquals(12 * 60 + 27.0, (july.hour * 60 + july.minute).toDouble(), 2.0)
        val overhead = SunPosition.at(may.toInstant(), SunPosition.KAABA_LATITUDE, SunPosition.KAABA_LONGITUDE)
        assertTrue(overhead.elevation > 89.5)
    }

    @Test
    fun `turning from the shadow reaches the Qibla`() {
        // Sun due east in the morning: the shadow points west, and the Qibla to the north-west is to the right.
        assertEquals(30.0, turnFromShadow(SunPlace(90.0, 20.0), 300.0), 0.001)
        assertEquals(-30.0, turnFromShadow(SunPlace(90.0, 20.0), 240.0), 0.001)
        // The Qibla straight behind the shadow is half a turn either way.
        assertEquals(180.0, kotlin.math.abs(turnFromShadow(SunPlace(0.0, 20.0), 0.0)), 0.001)
    }
}
