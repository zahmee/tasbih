package com.sakinah.tasbih.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyStatePolicyTest {
    @Test
    fun `current day keeps the counter and all reading progress`() {
        val progress = mapOf(
            "hisn_027" to ReadingProgress(entryIndex = 2, repetitionCounts = mapOf(2 to 4)),
            "hisn_035" to ReadingProgress(entryIndex = 1, repetitionCounts = mapOf(1 to 2)),
        )

        val state = dailyStateForDate(
            storedDayKey = "2026-09-02",
            currentDayKey = "2026-09-02",
            tasbihCount = 81,
            readingProgress = progress,
        )

        assertEquals(81, state.tasbihCount)
        assertEquals(progress, state.readingProgress)
    }

    @Test
    fun `new day resets daily dhikr but preserves other library progress`() {
        val nonDailyProgress = ReadingProgress(entryIndex = 3, repetitionCounts = mapOf(3 to 1))
        val state = dailyStateForDate(
            storedDayKey = "2026-09-01",
            currentDayKey = "2026-09-02",
            tasbihCount = 301,
            readingProgress = mapOf(
                "hisn_001" to ReadingProgress(entryIndex = 1),
                "hisn_025" to ReadingProgress(entryIndex = 2),
                "hisn_027" to ReadingProgress(entryIndex = 4),
                "hisn_028" to ReadingProgress(entryIndex = 5),
                "hisn_034" to ReadingProgress(completed = true),
                "hisn_035" to nonDailyProgress,
            ),
        )

        assertEquals(0, state.tasbihCount)
        assertEquals(mapOf("hisn_035" to nonDailyProgress), state.readingProgress)
    }

    @Test
    fun `daily collection boundaries match recurring catalog groups`() {
        assertTrue(isDailyDhikrCollectionId("hisn_001"))
        assertTrue(isDailyDhikrCollectionId("hisn_034"))
        assertFalse(isDailyDhikrCollectionId("hisn_035"))
        assertFalse(isDailyDhikrCollectionId("custom_collection"))
    }
}
