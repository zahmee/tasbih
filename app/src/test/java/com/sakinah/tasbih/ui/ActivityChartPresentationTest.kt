package com.sakinah.tasbih.ui

import androidx.compose.ui.graphics.luminance
import com.sakinah.tasbih.ui.theme.LightColors
import com.sakinah.tasbih.ui.theme.DarkColors
import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Test

class ActivityChartPresentationTest {
    @Test fun hoursRunFromRightToLeftInArabicAndReverseForLtr() {
        assertTrue(hourPositionFraction(0, true) > hourPositionFraction(6, true))
        assertTrue(hourPositionFraction(6, true) > hourPositionFraction(23, true))
        for (hour in 0..23) assertEquals(1f, hourPositionFraction(hour, true) + hourPositionFraction(hour, false), 0.0001f)
        assertEquals(6.5f / 24f, hourPositionFraction(6, false), 0.0001f)
    }
    @Test fun weekdaysHaveSevenReadableDistinctLabels() {
        val labels = DayOfWeek.entries.map(::shortArabicWeekday)
        assertEquals(7, labels.distinct().size)
        assertTrue(labels.all { it.length >= 3 })
    }
    @Test fun calendarNumbersMeetContrastAcrossEveryActivityLevelAndBothThemes() {
        for (scheme in listOf(LightColors, DarkColors)) for (count in 0..100) for (enabled in listOf(true, false)) {
            val (background, foreground) = calendarCellColors(scheme, count, 100, enabled)
            val light = maxOf(background.luminance(), foreground.luminance())
            val dark = minOf(background.luminance(), foreground.luminance())
            assertTrue("count=$count enabled=$enabled", (light + 0.05f) / (dark + 0.05f) >= 4.5f)
            assertEquals(1f, background.alpha, 0f)
            assertEquals(1f, foreground.alpha, 0f)
        }
    }
}
