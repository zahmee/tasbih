package com.sakinah.tasbih.calendar

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HijriDayTest {
    private val today = LocalDate.parse("2026-11-11")
    private val maghrib = Instant.parse("2026-11-11T14:33:00Z")

    @Test
    fun `the Hijri date turns at midnight unless the user chose Maghrib`() {
        assertEquals(today, hijriDayFor(today, maghrib.plusSeconds(3600), maghrib, fromMaghrib = false))
    }

    @Test
    fun `from Maghrib the Hijri date is already tomorrow's`() {
        assertEquals(today, hijriDayFor(today, maghrib.minusSeconds(60), maghrib, fromMaghrib = true))
        assertEquals(today.plusDays(1), hijriDayFor(today, maghrib, maghrib, fromMaghrib = true))
        assertEquals(today.plusDays(1), hijriDayFor(today, maghrib.plusSeconds(3600), maghrib, fromMaghrib = true))
    }

    @Test
    fun `without a place there is no Maghrib, so midnight stays the turn`() {
        assertEquals(today, hijriDayFor(today, maghrib.plusSeconds(3600), null, fromMaghrib = true))
    }
}
