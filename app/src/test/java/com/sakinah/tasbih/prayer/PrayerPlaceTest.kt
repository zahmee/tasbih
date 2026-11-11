package com.sakinah.tasbih.prayer

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerPlaceTest {
    private val abha = PrayerPlace("أبها", "عسير", "السعودية", "SA", 18.2164, 42.5053, "Asia/Riyadh")
    private val now = Instant.parse("2026-11-11T10:00:00Z")

    @Test
    fun `a phone on the place's own time has not travelled`() {
        assertFalse(abha.keepsOtherTimeThan(ZoneId.of("Asia/Riyadh"), now))
        // Another zone name with the same time is the same as far as prayer times go.
        assertFalse(abha.keepsOtherTimeThan(ZoneId.of("Asia/Kuwait"), now))
    }

    @Test
    fun `a phone on another time has most likely travelled`() {
        assertTrue(abha.keepsOtherTimeThan(ZoneId.of("Africa/Cairo"), now))
        assertTrue(abha.keepsOtherTimeThan(ZoneId.of("Asia/Dubai"), now))
    }

    @Test
    fun `a place whose zone cannot be read never raises the notice`() {
        assertFalse(abha.copy(timeZone = "Nowhere/Unknown").keepsOtherTimeThan(ZoneId.of("Asia/Dubai"), now))
    }
}
