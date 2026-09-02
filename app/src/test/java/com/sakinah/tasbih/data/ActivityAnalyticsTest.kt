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

    @Test
    fun `summaries are grouped by month and year without mixing activity sources`() {
        val analytics = ActivityAnalytics(
            daily = listOf(
                activity(LocalDate.of(2025, 12, 31), tasbih = 7, reader = 3),
                activity(LocalDate.of(2026, 1, 1), tasbih = 10, reader = 5),
                activity(LocalDate.of(2026, 1, 2), tasbih = 4, reader = 6),
                activity(LocalDate.of(2026, 2, 1), tasbih = 2, reader = 8),
            ),
        )

        val january = analytics.monthlyActivity().first { it.periodKey == "2026-01" }
        val year = analytics.yearlyActivity().first { it.periodKey == "2026" }

        assertEquals(25, january.totalCount)
        assertEquals(14, january.tasbihCount)
        assertEquals(11, january.readerCount)
        assertEquals(35, year.totalCount)
        assertEquals(2, january.activeDays)
    }

    @Test
    fun `hourly totals cover all 24 hours and honor the requested date range`() {
        val firstDay = LocalDate.of(2026, 9, 1)
        val secondDay = firstDay.plusDays(1)
        val analytics = ActivityAnalytics(
            hourly = listOf(
                hourly(firstDay, hour = 5, tasbih = 3, reader = 2),
                hourly(firstDay, hour = 18, tasbih = 4, reader = 1),
                hourly(secondDay, hour = 5, tasbih = 9, reader = 6),
            ),
        )

        val hours = analytics.hourlyTotalsBetween(firstDay, firstDay)

        assertEquals(24, hours.size)
        assertEquals(5, hours[5].totalCount)
        assertEquals(5, hours[18].totalCount)
        assertEquals(0, hours[6].totalCount)
    }

    @Test
    fun `tasbih phrase analytics only summarize that phrase history`() {
        val today = LocalDate.of(2026, 9, 2)
        val analytics = TasbihPhraseAnalytics(
            sourceId = "subhan_allah",
            daily = listOf(
                TasbihPhraseDailyActivity(today.minusDays(8).toString(), 40),
                TasbihPhraseDailyActivity(today.minusDays(2).toString(), 12),
                TasbihPhraseDailyActivity(today.minusDays(1).toString(), 8),
                TasbihPhraseDailyActivity(today.toString(), 15),
            ),
        )

        assertEquals(75, analytics.totalCount)
        assertEquals(15, analytics.countFor(today))
        assertEquals(35, analytics.countBetween(today.minusDays(6), today))
        assertEquals(4, analytics.activeDays)
        assertEquals(40, analytics.bestDay()?.count)
    }

    private fun activity(
        date: LocalDate,
        tasbih: Int = 6,
        reader: Int = 4,
    ) = DailyActivity(
        dayKey = date.toString(),
        totalCount = tasbih + reader,
        tasbihCount = tasbih,
        readerCount = reader,
        completions = 0,
    )

    private fun hourly(
        date: LocalDate,
        hour: Int,
        tasbih: Int,
        reader: Int,
    ) = HourlyActivity(
        dayKey = date.toString(),
        hourOfDay = hour,
        totalCount = tasbih + reader,
        tasbihCount = tasbih,
        readerCount = reader,
    )
}
