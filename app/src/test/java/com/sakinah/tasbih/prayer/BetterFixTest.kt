package com.sakinah.tasbih.prayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BetterFixTest {
    private val now = 1_000_000L

    @Test
    fun `the first position is always taken`() {
        assertTrue(BetterFix.replaces(1500f, now, heldAccuracy = null, heldTimeMs = null))
    }

    @Test
    fun `a mast's position does not push aside a fine one that is still current`() {
        // The satellites paused indoors for half a minute; the network kept reporting.
        assertFalse(BetterFix.replaces(1500f, now + 30_000, heldAccuracy = 5f, heldTimeMs = now))
        assertFalse(BetterFix.replaces(1500f, now + 110_000, heldAccuracy = 5f, heldTimeMs = now))
    }

    @Test
    fun `a rough position wins once the fine one is too old to trust`() {
        assertTrue(BetterFix.replaces(1500f, now + BetterFix.SUPERSEDED_MS + 1, heldAccuracy = 5f, heldTimeMs = now))
    }

    @Test
    fun `a finer position wins at once, and a newer one of the same kind follows the user`() {
        assertTrue(BetterFix.replaces(5f, now + 2_000, heldAccuracy = 1500f, heldTimeMs = now))
        assertTrue(BetterFix.replaces(6f, now + 2_000, heldAccuracy = 5f, heldTimeMs = now))
    }

    @Test
    fun `an older position does not replace a newer one of the same accuracy`() {
        assertFalse(BetterFix.replaces(5f, now - 2_000, heldAccuracy = 5f, heldTimeMs = now))
    }

    @Test
    fun `a position without an accuracy counts as the roughest`() {
        assertFalse(BetterFix.replaces(null, now + 2_000, heldAccuracy = 30f, heldTimeMs = now))
        assertTrue(BetterFix.replaces(30f, now + 2_000, heldAccuracy = null, heldTimeMs = now))
    }
}
