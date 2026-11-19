package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.DayColor

/**
 * The ink of a chosen day colour, deep on a light page and soft on a dark one, so the
 * numbers keep their contrast in either. Null for [DayColor.THEME], the screen's own ink.
 */
@Composable
internal fun DayColor.ink(): Color? {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        DayColor.THEME -> null
        DayColor.GREEN -> if (dark) Color(0xFF86D3AF) else Color(0xFF1E6A4D)
        DayColor.COPPER -> if (dark) Color(0xFFE3AC7C) else Color(0xFF94562A)
        DayColor.BLUE -> if (dark) Color(0xFF94BDEB) else Color(0xFF235A92)
        DayColor.RED -> if (dark) Color(0xFFF0A0A0) else Color(0xFFA23636)
        DayColor.PURPLE -> if (dark) Color(0xFFC6A9E8) else Color(0xFF684592)
    }
}

private val DayColor.label: Int
    get() = when (this) {
        DayColor.THEME -> R.string.cal_color_theme
        DayColor.GREEN -> R.string.cal_color_green
        DayColor.COPPER -> R.string.cal_color_copper
        DayColor.BLUE -> R.string.cal_color_blue
        DayColor.RED -> R.string.cal_color_red
        DayColor.PURPLE -> R.string.cal_color_purple
    }

/** A row of colour circles; the chosen one carries a tick. The first keeps the screen's ink. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayColorPicker(
    selected: DayColor,
    onSelect: (DayColor) -> Unit,
    modifier: Modifier = Modifier,
    /** What the first circle stands for: the screen's ink, or a phone calendar's own colour. */
    ownInk: Color? = null,
    ownLabel: Int = R.string.cal_color_theme,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        DayColor.entries.forEach { option ->
            val ink = if (option == DayColor.THEME) ownInk else option.ink()
            val chosen = option == selected
            val name = stringResource(if (option == DayColor.THEME) ownLabel else option.label)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ink ?: colors.surfaceVariant)
                    .border(if (chosen) 3.dp else 1.dp, if (chosen) colors.primary else colors.outlineVariant, CircleShape)
                    .selectable(chosen, role = Role.RadioButton) { onSelect(option) }
                    .semantics { contentDescription = name },
            ) {
                when {
                    chosen -> Icon(Icons.Outlined.Check, null, tint = if (ink == null) colors.onSurface else Color.White.copy(alpha = 0.95f).takeIf { ink.luminance() < 0.5f } ?: Color.Black)
                    ink == null -> Icon(Icons.Outlined.FormatColorReset, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
