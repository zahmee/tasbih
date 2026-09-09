package com.sakinah.tasbih.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TasbihCycleTest {
    @Test
    fun progressStartsAgainAfterEachTargetWhileTotalKeepsGrowing() {
        assertEquals(32, tasbihCycleCount(count = 32, target = 33))
        assertEquals(33, tasbihCycleCount(count = 33, target = 33, showCompletedCycle = true))
        assertEquals(0, tasbihCycleCount(count = 33, target = 33))
        assertEquals(1, tasbihCycleCount(count = 34, target = 33))
        assertEquals(1, tasbihCycleCount(count = 67, target = 33))

        assertEquals(0f, tasbihCycleProgress(count = 33, target = 33), 0.0001f)
        assertEquals(1f / 33f, tasbihCycleProgress(count = 34, target = 33), 0.0001f)
    }

    @Test
    fun completionIsDetectedOnlyWhenCountCrossesANewCycle() {
        assertTrue(hasCrossedTasbihCycle(previousCount = 32, currentCount = 33, target = 33))
        assertTrue(hasCrossedTasbihCycle(previousCount = 65, currentCount = 67, target = 33))
        assertFalse(hasCrossedTasbihCycle(previousCount = 33, currentCount = 34, target = 33))
        assertFalse(hasCrossedTasbihCycle(previousCount = 34, currentCount = 33, target = 33))
        assertFalse(hasCrossedTasbihCycle(previousCount = 10, currentCount = 11, target = 0))
    }

    @Test
    fun latestMilestoneTracksTheCompletedTargetWithoutChangingTheTotal() {
        assertEquals(0, latestTasbihMilestone(count = 32, target = 33))
        assertEquals(33, latestTasbihMilestone(count = 33, target = 33))
        assertEquals(33, latestTasbihMilestone(count = 34, target = 33))
        assertEquals(66, latestTasbihMilestone(count = 67, target = 33))
    }
}
