package com.sakinah.tasbih.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CompletionSoundPolicyTest {
    @Test
    fun unknownOrMissingSoundKeepsAnAudibleDefault() {
        assertEquals(CompletionSound.ClearBell, CompletionSound.fromStorage(null))
        assertEquals(CompletionSound.ClearBell, CompletionSound.fromStorage("removed_tone"))
    }

    @Test
    fun corruptedVolumeCannotReachAudioPlayback() {
        assertEquals(DefaultCompletionSoundVolume, normalizedCompletionSoundVolume(Float.NaN))
        assertEquals(DefaultCompletionSoundVolume, normalizedCompletionSoundVolume(Float.POSITIVE_INFINITY))
        assertEquals(0f, normalizedCompletionSoundVolume(-1f))
        assertEquals(1f, normalizedCompletionSoundVolume(2f))
        assertEquals(0.7f, normalizedCompletionSoundVolume(0.7f))
    }
}
