package com.sakinah.tasbih.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerSoundsTest {
    @Test
    fun `a retired tone carries over to the one that took its place`() {
        assertEquals("ceres", PrayerSounds.tone("chime").id)
        assertEquals("tethys", PrayerSounds.tone("bell").id)
        assertEquals("rhea", PrayerSounds.tone("soft").id)
    }

    @Test
    fun `a current tone stays, and an unknown one falls back to the first`() {
        assertEquals("rhea", PrayerSounds.currentToneId("rhea"))
        assertEquals("takbir_makkah", PrayerSounds.tone("takbir_makkah").id)
        assertEquals(PrayerSounds.tones.first().id, PrayerSounds.tone("gone").id)
    }
}
