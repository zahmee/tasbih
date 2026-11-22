package com.sakinah.tasbih.ui.prayer

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.ObligatoryPrayers
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.KhatamDivider
import com.sakinah.tasbih.ui.KhatamMark
import com.sakinah.tasbih.ui.SakinahSectionHeader
import com.sakinah.tasbih.ui.SettingsPage
import com.sakinah.tasbih.ui.SettingToggleRow
import com.sakinah.tasbih.ui.calendar.CalendarChoices
import com.sakinah.tasbih.ui.calendar.CalendarHint
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.widget.IslamicPattern
import com.sakinah.tasbih.widget.LineColor
import com.sakinah.tasbih.widget.LineItem
import com.sakinah.tasbih.widget.NextMark
import com.sakinah.tasbih.widget.PrayerWidgetKind
import com.sakinah.tasbih.widget.PrayerWidgetStyle
import com.sakinah.tasbih.widget.PrayerWidgetStyleRepository
import com.sakinah.tasbih.widget.PrayerWidgetUpdater
import com.sakinah.tasbih.widget.WidgetAccent
import com.sakinah.tasbih.widget.WidgetDhikr
import com.sakinah.tasbih.widget.WidgetInk
import com.sakinah.tasbih.widget.WidgetIntensity
import com.sakinah.tasbih.widget.WidgetLine
import com.sakinah.tasbih.widget.WidgetOrnament
import com.sakinah.tasbih.widget.WidgetPalette
import com.sakinah.tasbih.widget.WidgetScale
import com.sakinah.tasbih.widget.WidgetSurface
import com.sakinah.tasbih.widget.WidgetWeight
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrayerWidgetStyleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PrayerWidgetStyleRepository(application)

    val style: StateFlow<PrayerWidgetStyle?> = repository.style.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Saved, then every prayer widget on the home screen is drawn again with it. */
    fun update(change: (PrayerWidgetStyle) -> PrayerWidgetStyle) {
        val current = style.value ?: return
        viewModelScope.launch {
            repository.save(change(current))
            PrayerWidgetUpdater(getApplication()).updateAll()
        }
    }

    fun reset() = update { PrayerWidgetStyle() }
}

@Composable
fun PrayerWidgetStyleScreen(
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    onBack: () -> Unit,
    viewModel: PrayerWidgetStyleViewModel = viewModel(),
) {
    val style by viewModel.style.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val current = style ?: return
    SettingsPage(stringResource(R.string.widget_style_title), onBack, "prayer_widget_style") {
        item { SakinahSectionHeader(stringResource(R.string.widget_section_day)) }
        item { WidgetPreview(current, schedule, calendarText) }
        item {
            OutlinedButton(
                onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    val asked = manager.isRequestPinAppWidgetSupported &&
                        manager.requestPinAppWidget(ComponentName(context, PrayerWidgetKind.DAY.receiver), null, null)
                    if (!asked) Toast.makeText(context, R.string.cal_add_widget_manually, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Outlined.Add, null)
                Text(stringResource(R.string.prayer_add_day_widget), Modifier.padding(start = 8.dp))
            }
        }

        item { SakinahSectionHeader(stringResource(R.string.widget_section_lines)) }
        item { CalendarHint(stringResource(R.string.widget_lines_hint)) }
        current.lines.forEachIndexed { index, line ->
            item(key = "line_$index") {
                LineEditor(
                    number = index + 1,
                    line = line,
                    style = current,
                    // The day widget's first line is a heading, in the accent.
                    fallback = if (index == 0) WidgetPalette::accent else WidgetPalette::textSoft,
                    onChange = { changed -> viewModel.update { s -> s.copy(lines = s.lines.toMutableList().also { it[index] = changed }) } },
                    onRemove = { viewModel.update { s -> s.copy(lines = s.lines.toMutableList().also { it.removeAt(index) }) } },
                )
            }
        }
        item {
            SettingToggleRow(
                stringResource(R.string.widget_hijri_month_number),
                stringResource(R.string.widget_hijri_month_number_hint),
                current.hijriMonthNumber,
                { on -> viewModel.update { it.copy(hijriMonthNumber = on) } },
                "widget_hijri_month_number",
            )
        }
        item {
            SettingToggleRow(
                stringResource(R.string.widget_gregorian_month_number),
                stringResource(R.string.widget_gregorian_month_number_hint),
                current.gregorianMonthNumber,
                { on -> viewModel.update { it.copy(gregorianMonthNumber = on) } },
                "widget_gregorian_month_number",
            )
        }
        if (current.lines.size < PrayerWidgetStyle.MAX_LINES) {
            item {
                TextButton(
                    onClick = { viewModel.update { s -> s.copy(lines = s.lines + WidgetLine(LineItem.COUNTDOWN)) } },
                    modifier = Modifier.testTag("widget_add_line"),
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_add_line))
                }
            }
        }

        item { SakinahSectionHeader(stringResource(R.string.widget_section_next)) }
        item { NextPreview(current, schedule, calendarText) }
        item {
            OutlinedButton(
                onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    val asked = manager.isRequestPinAppWidgetSupported &&
                        manager.requestPinAppWidget(ComponentName(context, PrayerWidgetKind.NEXT.receiver), null, null)
                    if (!asked) Toast.makeText(context, R.string.cal_add_widget_manually, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Outlined.Add, null)
                Text(stringResource(R.string.prayer_add_next_widget), Modifier.padding(start = 8.dp))
            }
        }
        item { CalendarHint(stringResource(R.string.widget_next_lines_hint)) }
        current.nextLines.forEachIndexed { index, line ->
            item(key = "next_line_$index") {
                LineEditor(
                    number = index + 1,
                    line = line,
                    style = current,
                    fallback = WidgetPalette::textSoft,
                    onChange = { changed -> viewModel.update { s -> s.copy(nextLines = s.nextLines.toMutableList().also { it[index] = changed }) } },
                    onRemove = { viewModel.update { s -> s.copy(nextLines = s.nextLines.toMutableList().also { it.removeAt(index) }) } },
                )
            }
        }
        if (current.nextLines.size < PrayerWidgetStyle.MAX_NEXT_LINES) {
            item {
                TextButton(
                    onClick = { viewModel.update { s -> s.copy(nextLines = s.nextLines + WidgetLine(LineItem.COUNTDOWN)) } },
                    modifier = Modifier.testTag("widget_add_next_line"),
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.widget_add_line))
                }
            }
        }

        item { SakinahSectionHeader(stringResource(R.string.widget_section_look)) }
        item { CalendarHint(stringResource(R.string.widget_look_hint)) }
        item {
            SettingToggleRow(
                stringResource(R.string.widget_progress),
                stringResource(R.string.widget_progress_hint),
                current.progress,
                { on -> viewModel.update { it.copy(progress = on) } },
                "widget_progress",
            )
        }
        item {
            Labelled(stringResource(R.string.widget_surface)) {
                Swatches(
                    options = WidgetSurface.entries,
                    selected = current.surface,
                    color = { surfaceSample(it) },
                    label = { surfaceLabel(it) },
                    onSelect = { choice -> viewModel.update { it.copy(surface = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_opacity, com.sakinah.tasbih.data.arabicNumber(current.opacity))) {
                // Saved when the finger lifts, so the widget is not redrawn at every step.
                var value by remember(current.opacity) { mutableStateOf(current.opacity.toFloat()) }
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { viewModel.update { it.copy(opacity = value.toInt()) } },
                    valueRange = 0f..100f,
                    steps = 19,
                    modifier = Modifier.testTag("widget_opacity"),
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_ink)) {
                CalendarChoices(
                    options = WidgetInk.entries,
                    selected = current.ink,
                    label = {
                        stringResource(
                            when (it) {
                                WidgetInk.AUTO -> R.string.cal_automatic
                                WidgetInk.DARK -> R.string.widget_ink_dark
                                WidgetInk.LIGHT -> R.string.widget_ink_light
                            },
                        )
                    },
                    onSelect = { choice -> viewModel.update { it.copy(ink = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_accent)) {
                Swatches(
                    options = WidgetAccent.entries,
                    selected = current.accent,
                    color = { Color(WidgetPalette.of(current.copy(accent = it, surface = WidgetSurface.IVORY), false).accent) },
                    label = { accentLabel(it) },
                    onSelect = { choice -> viewModel.update { it.copy(accent = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_next_mark)) {
                CalendarChoices(
                    options = NextMark.entries,
                    selected = current.nextMark,
                    label = {
                        stringResource(
                            when (it) {
                                NextMark.FILLED -> R.string.widget_mark_filled
                                NextMark.FAINT -> R.string.widget_mark_faint
                                NextMark.OUTLINE -> R.string.widget_mark_outline
                            },
                        )
                    },
                    onSelect = { choice -> viewModel.update { it.copy(nextMark = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_intensity)) {
                CalendarChoices(
                    options = WidgetIntensity.entries,
                    selected = current.intensity,
                    label = { intensityLabel(it) },
                    onSelect = { choice -> viewModel.update { it.copy(intensity = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_scale)) {
                CalendarChoices(
                    options = WidgetScale.entries,
                    selected = current.scale,
                    label = {
                        stringResource(
                            when (it) {
                                WidgetScale.SMALL -> R.string.widget_scale_small
                                WidgetScale.MEDIUM -> R.string.widget_scale_medium
                                WidgetScale.LARGE -> R.string.widget_scale_large
                            },
                        )
                    },
                    onSelect = { choice -> viewModel.update { it.copy(scale = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_weight)) {
                CalendarChoices(
                    options = WidgetWeight.entries,
                    selected = current.weight,
                    label = { weightLabel(it) },
                    onSelect = { choice -> viewModel.update { it.copy(weight = choice) } },
                )
            }
        }
        item {
            Labelled(stringResource(R.string.widget_time_weight)) {
                CalendarChoices(
                    options = WidgetWeight.entries,
                    selected = current.timeWeight,
                    label = { weightLabel(it) },
                    onSelect = { choice -> viewModel.update { it.copy(timeWeight = choice) } },
                )
                CalendarHint(stringResource(R.string.widget_weight_hint))
            }
        }
        item {
            Labelled(stringResource(R.string.widget_name_color)) {
                LineColors(current, current.nameColor, fallback = WidgetPalette::textSoft) { choice -> viewModel.update { it.copy(nameColor = choice) } }
            }
        }
        item {
            Labelled(stringResource(R.string.widget_time_color)) {
                LineColors(current, current.timeColor, fallback = WidgetPalette::text) { choice -> viewModel.update { it.copy(timeColor = choice) } }
            }
        }
        item {
            Labelled(stringResource(R.string.widget_ornament)) {
                CalendarChoices(
                    // The frame is kept only to read old choices, which now show as the pattern.
                    options = WidgetOrnament.entries - WidgetOrnament.FRAME,
                    selected = current.ornament,
                    label = {
                        stringResource(
                            when (it) {
                                WidgetOrnament.NONE -> R.string.widget_ornament_none
                                WidgetOrnament.DIVIDER -> R.string.widget_ornament_divider
                                WidgetOrnament.PATTERN, WidgetOrnament.FRAME -> R.string.widget_ornament_pattern
                            },
                        )
                    },
                    onSelect = { choice -> viewModel.update { it.copy(ornament = choice) } },
                )
                CalendarHint(stringResource(R.string.widget_ornament_hint))
            }
        }
        if (current.ornament == WidgetOrnament.PATTERN) {
            item {
                Labelled(stringResource(R.string.widget_pattern_intensity)) {
                    CalendarChoices(
                        options = WidgetIntensity.entries,
                        selected = current.patternIntensity,
                        label = { intensityLabel(it) },
                        onSelect = { choice -> viewModel.update { it.copy(patternIntensity = choice) } },
                    )
                }
            }
        }
        item {
            TextButton(onClick = viewModel::reset, modifier = Modifier.testTag("widget_reset")) {
                Text(stringResource(R.string.widget_reset))
            }
        }
    }
}

@Composable
private fun Labelled(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Swatches(options: List<T>, selected: T, color: @Composable (T) -> Color, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val colors = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val chosen = option == selected
            val fill = color(option)
            val name = label(option)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .border(if (chosen) 3.dp else 1.dp, if (chosen) colors.primary else colors.outlineVariant, CircleShape)
                        .selectable(chosen, role = Role.RadioButton) { onSelect(option) }
                        .semantics { contentDescription = name },
                ) {
                    if (chosen) Icon(Icons.Outlined.Check, null, tint = if (fill.luminance() < 0.5f) Color.White else Color.Black)
                }
                Text(name, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun surfaceSample(surface: WidgetSurface): Color =
    Color(WidgetPalette.of(PrayerWidgetStyle(surface = surface), MaterialTheme.colorScheme.surface.luminance() < 0.5f).surface)

@Composable
private fun surfaceLabel(surface: WidgetSurface): String = stringResource(
    when (surface) {
        WidgetSurface.AUTO -> R.string.cal_automatic
        WidgetSurface.IVORY -> R.string.widget_surface_ivory
        WidgetSurface.WHITE -> R.string.widget_surface_white
        WidgetSurface.SAND -> R.string.widget_surface_sand
        WidgetSurface.STONE -> R.string.widget_surface_stone
        WidgetSurface.SKY -> R.string.widget_surface_sky
        WidgetSurface.ROSE -> R.string.widget_surface_rose
        WidgetSurface.PINE -> R.string.widget_surface_pine
        WidgetSurface.NAVY -> R.string.widget_surface_navy
        WidgetSurface.MAROON -> R.string.widget_surface_maroon
        WidgetSurface.COFFEE -> R.string.widget_surface_coffee
        WidgetSurface.NIGHT -> R.string.widget_surface_night
        WidgetSurface.BLACK -> R.string.widget_surface_black
    },
)

@Composable
private fun weightLabel(weight: WidgetWeight): String = stringResource(
    when (weight) {
        WidgetWeight.REGULAR -> R.string.widget_weight_regular
        WidgetWeight.BOLD -> R.string.widget_weight_bold
    },
)

@Composable
private fun intensityLabel(intensity: WidgetIntensity): String = stringResource(
    when (intensity) {
        WidgetIntensity.SOFT -> R.string.widget_intensity_soft
        WidgetIntensity.NORMAL -> R.string.widget_intensity_normal
        WidgetIntensity.STRONG -> R.string.widget_intensity_strong
    },
)

@Composable
private fun lineColorLabel(color: LineColor): String = stringResource(
    when (color) {
        LineColor.AUTO -> R.string.cal_automatic
        LineColor.INK -> R.string.widget_color_ink
        LineColor.SOFT -> R.string.widget_color_soft
        LineColor.ACCENT -> R.string.widget_color_accent
        LineColor.GREEN -> R.string.cal_color_green
        LineColor.COPPER -> R.string.cal_color_copper
        LineColor.GOLD -> R.string.widget_accent_gold
        LineColor.BLUE -> R.string.cal_color_blue
        LineColor.RED -> R.string.cal_color_red
        LineColor.PURPLE -> R.string.cal_color_purple
        LineColor.WHITE -> R.string.widget_color_white
        LineColor.BLACK -> R.string.widget_color_black
    },
)

/**
 * The colours a line of words may take, each shown as it will look on the chosen paper;
 * «automatic» shows the widget's own colour for it, [fallback].
 */
@Composable
private fun LineColors(style: PrayerWidgetStyle, selected: LineColor, fallback: (WidgetPalette) -> Int, onSelect: (LineColor) -> Unit) {
    val palette = WidgetPalette.of(style, MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    Swatches(
        options = LineColor.entries,
        selected = selected,
        color = { Color(palette.line(it) ?: fallback(palette)) },
        label = { lineColorLabel(it) },
        onSelect = onSelect,
    )
}

/** Android's own font, the one a widget is drawn in, so the preview's letters are the widget's. */
private val WidgetFont = FontFamily.Default

private fun WidgetWeight.font(): FontWeight = if (this == WidgetWeight.BOLD) FontWeight.Bold else FontWeight.Normal

/** The pattern of the widget, drawn by the widget's own code under the preview's words. */
private fun Modifier.pattern(style: PrayerWidgetStyle, palette: WidgetPalette): Modifier =
    if (style.ornament != WidgetOrnament.PATTERN && style.ornament != WidgetOrnament.FRAME) {
        this
    } else {
        drawBehind {
            val color = (palette.accent and 0x00FFFFFF) or (palette.patternAlpha shl 24)
            drawIntoCanvas {
                IslamicPattern.draw(it.nativeCanvas, size.width, size.height, IslamicPattern.CELL_DP.dp.toPx(), color, IslamicPattern.STROKE_DP.dp.toPx())
            }
        }
    }

@Composable
private fun accentLabel(accent: WidgetAccent): String = stringResource(
    when (accent) {
        WidgetAccent.GREEN -> R.string.cal_color_green
        WidgetAccent.COPPER -> R.string.cal_color_copper
        WidgetAccent.GOLD -> R.string.widget_accent_gold
        WidgetAccent.BLUE -> R.string.cal_color_blue
        WidgetAccent.RED -> R.string.cal_color_red
        WidgetAccent.PURPLE -> R.string.cal_color_purple
    },
)

@Composable
private fun itemLabel(item: LineItem): String = stringResource(
    when (item) {
        LineItem.NONE -> R.string.widget_item_none
        LineItem.HIJRI_DATE -> R.string.widget_item_hijri
        LineItem.GREGORIAN_DATE -> R.string.widget_item_gregorian
        LineItem.WEEKDAY -> R.string.widget_item_weekday
        LineItem.PLACE -> R.string.widget_item_place
        LineItem.NEXT_PRAYER -> R.string.widget_item_next
        LineItem.COUNTDOWN -> R.string.widget_item_countdown
        LineItem.DHIKR -> R.string.widget_item_dhikr
    },
)

/** The bar of the time passed since the last prayer, as the widgets draw it. */
@Composable
private fun ProgressPreview(schedule: PrayerSchedule, now: Instant, palette: WidgetPalette) {
    val share = com.sakinah.tasbih.widget.passedShare(schedule.previous(now).time.toInstant(), schedule.next(now).time.toInstant(), now) / 1000f
    val track = Color(palette.textSoft).copy(alpha = 0.25f)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(track),
    ) {
        Box(Modifier.fillMaxWidth(share).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(palette.accent)))
    }
}

/** The small widget of the next prayer, as it will look. */
@Composable
private fun NextPreview(style: PrayerWidgetStyle, schedule: PrayerSchedule?, calendarText: CalendarText?) {
    val context = LocalContext.current
    val night = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val palette = WidgetPalette.of(style, night)
    val text = remember(context) { PrayerText(context) }
    val accent = Color(palette.accent)
    val now = Instant.now()
    val next = schedule?.next(now)
    val countdownTemplate = stringResource(R.string.widget_countdown)
    val weight = style.weight.font()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (night) Color(0xFF1B2A35) else Color(0xFF6E8FA8))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(190.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(palette.surface).copy(alpha = style.opacity / 100f))
                .pattern(style, palette)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("widget_next_preview"),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    next?.let { text.name(it.prayer, it.time) } ?: stringResource(R.string.prayer_title),
                    color = Color(palette.line(style.nameColor) ?: palette.accent),
                    fontSize = 15.sp,
                    fontFamily = WidgetFont,
                    fontWeight = weight,
                )
                Text(
                    next?.let { text.time(it.time) } ?: "–",
                    color = Color(palette.line(style.timeColor) ?: palette.text),
                    fontSize = 26.sp,
                    fontFamily = WidgetFont,
                    fontWeight = style.timeWeight.font(),
                )
                val lines = style.nextLines.filterNot { it.isEmpty }
                if (style.ornament != WidgetOrnament.NONE && lines.isNotEmpty()) {
                    KhatamDivider(Modifier.padding(horizontal = 30.dp, vertical = 2.dp), color = accent.copy(alpha = 0.7f))
                }
                lines.forEach { line ->
                    Row(Modifier.fillMaxWidth()) {
                        val single = line.right == LineItem.NONE || line.left == LineItem.NONE
                        listOf(line.right, line.left).filter { it != LineItem.NONE }.forEachIndexed { side, item ->
                            Text(
                                sample(item, style, schedule, calendarText, text, now, countdownTemplate),
                                color = Color(palette.line(line.color) ?: palette.textSoft),
                                fontSize = 12.sp,
                                fontFamily = WidgetFont,
                                fontWeight = if (item == LineItem.COUNTDOWN) FontWeight.Normal else weight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = if (single) TextAlign.Center else if (side == 0) TextAlign.Start else TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (style.progress && schedule != null) ProgressPreview(schedule, now, palette)
            }
        }
    }
}

/** One line: what stands on its right and on its left, its colour, and a way to remove it. */
@Composable
private fun LineEditor(
    number: Int,
    line: WidgetLine,
    style: PrayerWidgetStyle,
    fallback: (WidgetPalette) -> Int,
    onChange: (WidgetLine) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.widget_line_number, com.sakinah.tasbih.data.arabicNumber(number)),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, stringResource(R.string.widget_remove_line)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemChooser(stringResource(R.string.widget_side_right), line.right, { onChange(line.copy(right = it)) }, Modifier.weight(1f))
            ItemChooser(stringResource(R.string.widget_side_left), line.left, { onChange(line.copy(left = it)) }, Modifier.weight(1f))
        }
        Text(stringResource(R.string.widget_line_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LineColors(style, line.color, fallback) { onChange(line.copy(color = it)) }
    }
}

@Composable
private fun ItemChooser(label: String, selected: LineItem, onSelect: (LineItem) -> Unit, modifier: Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(itemLabel(selected), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LineItem.entries.forEach { item ->
                DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = {
                    open = false
                    onSelect(item)
                })
            }
        }
    }
}

/**
 * The widget as it will look, drawn from the same palette as the widget itself. A sketch, not
 * the launcher's own drawing, so sizes are close rather than exact.
 */
@Composable
private fun WidgetPreview(style: PrayerWidgetStyle, schedule: PrayerSchedule?, calendarText: CalendarText?) {
    val context = LocalContext.current
    val night = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val palette = WidgetPalette.of(style, night)
    val text = remember(context) { PrayerText(context) }
    val scale = when (style.scale) {
        WidgetScale.SMALL -> 0.86f
        WidgetScale.MEDIUM -> 1f
        WidgetScale.LARGE -> 1.18f
    }
    val weight = style.weight.font()
    val accent = Color(palette.accent)
    val shape = RoundedCornerShape(22.dp)
    val now = Instant.now()
    val next = schedule?.next(now)
    val day = next?.let { schedule.day(it.time.toLocalDate()) }
    val countdownTemplate = stringResource(R.string.widget_countdown)

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.widget_preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // A strip of the home screen behind it, so transparency can be seen.
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (night) Color(0xFF1B2A35) else Color(0xFF6E8FA8))
                .padding(14.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(palette.surface).copy(alpha = style.opacity / 100f))
                    .pattern(style, palette)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("widget_preview"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    style.lines.filterNot { it.isEmpty }.forEachIndexed { index, line ->
                        val color = Color(palette.line(line.color) ?: if (index == 0) palette.accent else palette.textSoft)
                        Row(Modifier.fillMaxWidth()) {
                            val single = line.right == LineItem.NONE || line.left == LineItem.NONE
                            listOf(line.right, line.left).filter { it != LineItem.NONE }.forEachIndexed { side, item ->
                                Text(
                                    sample(item, style, schedule, calendarText, text, now, countdownTemplate),
                                    color = color,
                                    fontSize = (12 * scale).sp,
                                    fontFamily = WidgetFont,
                                    fontWeight = if (item == LineItem.COUNTDOWN) FontWeight.Normal else weight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = if (single) TextAlign.Center else if (side == 0) TextAlign.Start else TextAlign.End,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (style.ornament != WidgetOrnament.NONE && style.lines.any { !it.isEmpty }) {
                        KhatamDivider(Modifier.padding(horizontal = 60.dp, vertical = 2.dp), color = accent.copy(alpha = 0.7f))
                    }
                    Row(Modifier.fillMaxWidth().height((52 * scale).dp)) {
                        val nameColor = palette.line(style.nameColor)
                        val timeColor = palette.line(style.timeColor)
                        ObligatoryPrayers.forEach { prayer ->
                            val isNext = prayer == next?.prayer
                            val tile = RoundedCornerShape(12.dp)
                            val markAlpha = palette.markAlpha / 255f
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f).padding(2.dp),
                            ) {
                                Text(
                                    text.name(prayer, day?.get(prayer)),
                                    color = Color(nameColor ?: if (isNext) palette.accent else palette.textSoft),
                                    fontSize = (13 * scale).sp,
                                    fontFamily = WidgetFont,
                                    fontWeight = (if (isNext) WidgetWeight.BOLD else style.weight).font(),
                                    maxLines = 1,
                                )
                                // As in the widget: the next prayer is marked round its time alone.
                                Text(
                                    day?.let { text.clock(it[prayer]) } ?: "–",
                                    color = Color(if (isNext) palette.nextText(style.nextMark) else timeColor ?: palette.text),
                                    fontSize = (15 * scale).sp,
                                    fontFamily = WidgetFont,
                                    fontWeight = style.timeWeight.font(),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .then(
                                            when {
                                                !isNext -> Modifier
                                                style.nextMark == NextMark.OUTLINE -> Modifier.border(1.5.dp, accent.copy(alpha = markAlpha), tile)
                                                else -> Modifier.background(accent.copy(alpha = markAlpha), tile)
                                            },
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                    if (style.progress && schedule != null) ProgressPreview(schedule, now, palette)
                }
            }
        }
    }
}

private fun sample(item: LineItem, style: PrayerWidgetStyle, schedule: PrayerSchedule?, calendarText: CalendarText?, text: PrayerText, now: Instant, countdown: String): String {
    val next = schedule?.next(now)
    val date = next?.time?.toLocalDate() ?: LocalDate.now()
    return when (item) {
        LineItem.NONE -> ""
        LineItem.HIJRI_DATE -> calendarText?.hijriDate(date, style.hijriMonthNumber).orEmpty()
        LineItem.GREGORIAN_DATE -> calendarText?.gregorianDate(date, style.gregorianMonthNumber).orEmpty()
        LineItem.WEEKDAY -> calendarText?.weekday(date.dayOfWeek).orEmpty()
        LineItem.PLACE -> schedule?.place?.name.orEmpty()
        LineItem.NEXT_PRAYER -> next?.let { "${text.name(it.prayer, it.time)} ${text.time(it.time)}" }.orEmpty()
        LineItem.COUNTDOWN -> next?.let {
            val left = Duration.between(now, it.time.toInstant())
            val clock = "%d:%02d:%02d".format(java.util.Locale.ROOT, left.toHours(), left.toMinutes() % 60, left.seconds % 60)
            countdown.format(text.name(it.prayer, it.time), "\u2066$clock\u2069")
        }.orEmpty()
        LineItem.DHIKR -> WidgetDhikr[LocalDate.now().dayOfYear % WidgetDhikr.size]
    }
}
