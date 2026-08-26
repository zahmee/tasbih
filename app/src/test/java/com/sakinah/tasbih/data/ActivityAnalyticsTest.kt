package com.sakinah.tasbih.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityAnalyticsTest {
    @Test
    fun `current streak includes yesterday when today has not started`() {
        val today = LocalDate.of(2026, 8, 25)
        val analytics = ActivityAnalytics(
            daily = listOf(
                activity(today.minusDays(3)),
                activity(today.minusDays(2)),
                activity(today.minusDays(1)),
            ),
        )

        assertEquals(3, analytics.currentStreak(today))
    }

    @Test
    fun `longest streak is calculated across the complete saved history`() {
        val start = LocalDate.of(2026, 1, 1)
        val analytics = ActivityAnalytics(
            daily = listOf(
                activity(start),
                activity(start.plusDays(1)),
                activity(start.plusDays(4)),
                activity(start.plusDays(5)),
                activity(start.plusDays(6)),
                activity(start.plusDays(7)),
            ),
        )

        assertEquals(4, analytics.longestStreak())
    }

    @Test
    fun `appearance values safely fall back for future or corrupt stored values`() {
        assertEquals(ThemeMode.System, ThemeMode.fromStorage("unexpected"))
        assertEquals(ArabicFontStyle.Sakinah, ArabicFontStyle.fromStorage(null))
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorage("dark"))
    }

    private fun activity(date: LocalDate) = DailyActivity(
        dayKey = date.toString(),
        totalCount = 10,
        tasbihCount = 6,
        readerCount = 4,
        completions = 0,
    )
}
