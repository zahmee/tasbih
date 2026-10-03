package com.sakinah.tasbih.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek

/** Both the canvas and its Arabic axis start with midnight on the right. */
internal fun hourPositionFraction(hour: Int, rtl: Boolean): Float {
    val fraction = (hour.coerceIn(0, 23) + 0.5f) / 24f
    return if (rtl) 1f - fraction else fraction
}

internal fun shortArabicWeekday(day: DayOfWeek): String = when (day) {
    DayOfWeek.SATURDAY -> "سبت"
    DayOfWeek.SUNDAY -> "أحد"
    DayOfWeek.MONDAY -> "إثن"
    DayOfWeek.TUESDAY -> "ثلا"
    DayOfWeek.WEDNESDAY -> "أرب"
    DayOfWeek.THURSDAY -> "خمي"
    DayOfWeek.FRIDAY -> "جمع"
}

/** Opaque, matched foreground/background roles preserve text contrast at every level. */
internal fun calendarCellColors(
    scheme: ColorScheme, count: Int, maximum: Int, enabled: Boolean,
): Pair<Color, Color> = when {
    !enabled -> scheme.surface to scheme.onSurface
    count <= 0 -> scheme.surfaceContainerLow to scheme.onSurface
    count.toFloat() / maximum.coerceAtLeast(1) >= 0.75f -> scheme.primary to scheme.onPrimary
    else -> scheme.primaryContainer to scheme.onPrimaryContainer
}
