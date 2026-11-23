package com.sakinah.tasbih.alerts

import com.sakinah.tasbih.prayer.AlertSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerSoundsTest {
    @Test
    fun `a retired tone carries over to one that stays`() {
        assertEquals("clear", PrayerSounds.tone("chime").id)
        assertEquals("light", PrayerSounds.tone("bell").id)
        assertEquals("simple", PrayerSounds.tone("soft").id)
        assertEquals("clear", PrayerSounds.currentToneId("ceres"))
        assertEquals("light", PrayerSounds.currentToneId("tethys"))
        assertEquals("simple", PrayerSounds.currentToneId("rhea"))
    }

    @Test
    fun `a current tone stays, and an unknown one falls back to the first`() {
        assertEquals("simple", PrayerSounds.currentToneId("simple"))
        assertEquals("takbir_makkah", PrayerSounds.tone("takbir_makkah").id)
        assertEquals(PrayerSounds.tones.first().id, PrayerSounds.tone("gone").id)
    }

    @Test
    fun `a retired adhan carries over to the default, and the default is the first`() {
        assertEquals(AlertSettings.DEFAULT_ADHAN, PrayerSounds.adhans.first().id)
        listOf("makkah_maghrib", "recording_1", "recording_3", "recording_4", "tone").forEach {
            assertEquals(AlertSettings.DEFAULT_ADHAN, PrayerSounds.currentAdhanId(it))
            assertEquals(AlertSettings.DEFAULT_ADHAN, PrayerSounds.adhan(it).id)
        }
        assertEquals("recording_2", PrayerSounds.adhan("recording_2").id)
        assertEquals("hamad_aldaghriri", PrayerSounds.currentAdhanId("hamad_aldaghriri"))
        assertEquals(AlertSettings.CUSTOM_ADHAN, PrayerSounds.currentAdhanId(AlertSettings.CUSTOM_ADHAN))
        assertTrue(PrayerSounds.adhans.none { it.id == AlertSettings.CUSTOM_ADHAN })
    }
}
