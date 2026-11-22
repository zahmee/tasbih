package com.sakinah.tasbih.ui.prayer

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import com.sakinah.tasbih.ui.SakinahContentMaxWidth
import com.sakinah.tasbih.ui.SakinahSpacing
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
import androidx.compose.runtime.LaunchedEffect
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

/** The four tabs the options are gathered under. */
private enum class StyleTab { CONTENT, COLORS, FONT, ORNAMENT }

/**
 * The prayer widgets' customisation: the preview of the chosen widget stays on top while the
 * options below it, gathered in four tabs, scroll. Opened from the prayer settings, or from a
 * widget's own «customise», on that widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerWidgetStyleScreen(
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    onBack: () -> Unit,
    startOnNext: Boolean = false,
    viewModel: PrayerWidgetStyleViewModel = viewModel(),
) {
    val style by viewModel.style.collectAsStateWithLifecycle()
    val current = style ?: return
    var next by rememberSaveable(startOnNext) { mutableStateOf(startOnNext) }
    var tab by rememberSaveable { mutableStateOf(StyleTab.CONTENT) }
    // On a short screen the preview scrolls with the options, so they keep room to be used.
    val roomy = LocalConfiguration.current.screenHeightDp >= 600

    Scaffold(
        modifier = Modifier.testTag("prayer_widget_style"),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.widget_style_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    IconButton(onClick = viewModel::reset, modifier = Modifier.testTag("widget_reset")) {
                        Icon(Icons.Outlined.Restore, stringResource(R.string.widget_reset))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val top: @Composable () -> Unit = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    WidgetSwitch(next) { next = it }
                    if (next) NextPreview(current, schedule, calendarText) else WidgetPreview(current, schedule, calendarText)
                }
            }
            if (roomy) top()
            PrimaryTabRow(selectedTabIndex = tab.ordinal, modifier = Modifier.widthIn(max = SakinahContentMaxWidth)) {
                StyleTab.entries.forEach { entry ->
                    Tab(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        // A size smaller than a tab's own, so the four names fit with large text too.
                        text = { Text(tabLabel(entry), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium) },
                        icon = { Icon(tabIcon(entry), null) },
                        modifier = Modifier.testTag("widget_tab_${entry.name.lowercase()}"),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxWidth().weight(1f).testTag("widget_style_options"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Large),
            ) {
                if (!roomy) item(key = "top") { top() }
                when (tab) {
                    StyleTab.CONTENT -> contentOptions(current, next, viewModel)
                    StyleTab.COLORS -> colorOptions(current, viewModel)
                    StyleTab.FONT -> fontOptions(current, viewModel)
                    StyleTab.ORNAMENT -> ornamentOptions(current, viewModel)
                }
            }
        }
    }
}

/** Which widget the preview shows and the lines belong to; the look is shared by both. */
@Composable
private fun WidgetSwitch(next: Boolean, onNext: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("widget_switch")) {
        listOf(false to R.string.widget_section_day, true to R.string.widget_section_next).forEachIndexed { index, (isNext, label) ->
            SegmentedButton(
                selected = next == isNext,
                onClick = { onNext(isNext) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
}

@Composable
private fun tabLabel(tab: StyleTab): String = stringResource(
    when (tab) {
        StyleTab.CONTENT -> R.string.widget_tab_content
        StyleTab.COLORS -> R.string.widget_tab_colors
        StyleTab.FONT -> R.string.widget_tab_font
        StyleTab.ORNAMENT -> R.string.widget_tab_ornament
    },
)

private fun tabIcon(tab: StyleTab): ImageVector = when (tab) {
    StyleTab.CONTENT -> Icons.AutoMirrored.Outlined.ViewList
    StyleTab.COLORS -> Icons.Outlined.Palette
    StyleTab.FONT -> Icons.Outlined.TextFields
    StyleTab.ORNAMENT -> Icons.Outlined.AutoAwesome
}

/** What the chosen widget says: its lines, the dates' month numbers, the bar, and a way to place it. */
private fun LazyListScope.contentOptions(current: PrayerWidgetStyle, next: Boolean, viewModel: PrayerWidgetStyleViewModel) {
    val lines = if (next) current.nextLines else current.lines
    val max = if (next) PrayerWidgetStyle.MAX_NEXT_LINES else PrayerWidgetStyle.MAX_LINES
    fun change(update: (MutableList<WidgetLine>) -> Unit) = viewModel.update { s ->
        if (next) s.copy(nextLines = s.nextLines.toMutableList().also(update)) else s.copy(lines = s.lines.toMutableList().also(update))
    }
    item(key = "lines_hint_$next") { CalendarHint(stringResource(if (next) R.string.widget_next_lines_hint else R.string.widget_lines_hint)) }
    lines.forEachIndexed { index, line ->
        item(key = "line_${next}_$index") {
            LineEditor(
                number = index + 1,
                line = line,
                style = current,
                // The day widget's first line is a heading, in the accent.
                fallback = if (!next && index == 0) WidgetPalette::accent else WidgetPalette::textSoft,
                onChange = { changed -> change { it[index] = changed } },
                onRemove = { change { it.removeAt(index) } },
            )
        }
    }
    if (lines.size < max) {
        item(key = "add_line_$next") {
            TextButton(
                onClick = { change { it.add(WidgetLine(LineItem.COUNTDOWN)) } },
                modifier = Modifier.testTag(if (next) "widget_add_next_line" else "widget_add_line"),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.widget_add_line))
            }
        }
    }
    item(key = "hijri_number") {
        SettingToggleRow(
            stringResource(R.string.widget_hijri_month_number),
            stringResource(R.string.widget_hijri_month_number_hint),
            current.hijriMonthNumber,
            { on -> viewModel.update { it.copy(hijriMonthNumber = on) } },
            "widget_hijri_month_number",
        )
    }
    item(key = "gregorian_number") {
        SettingToggleRow(
            stringResource(R.string.widget_gregorian_month_number),
            stringResource(R.string.widget_gregorian_month_number_hint),
            current.gregorianMonthNumber,
            { on -> viewModel.update { it.copy(gregorianMonthNumber = on) } },
            "widget_gregorian_month_number",
        )
    }
    item(key = "progress") {
        SettingToggleRow(
            stringResource(R.string.widget_progress),
            stringResource(R.string.widget_progress_hint),
            current.progress,
            { on -> viewModel.update { it.copy(progress = on) } },
            "widget_progress",
        )
    }
    item(key = "place_$next") { PlaceWidgetButton(next) }
}

/** Asks the launcher to place the chosen widget, or says how to where it cannot be asked. */
@Composable
private fun PlaceWidgetButton(next: Boolean) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val manager = AppWidgetManager.getInstance(context)
            val kind = if (next) PrayerWidgetKind.NEXT else PrayerWidgetKind.DAY
            val asked = manager.isRequestPinAppWidgetSupported &&
                manager.requestPinAppWidget(ComponentName(context, kind.receiver), null, null)
            if (!asked) Toast.makeText(context, R.string.cal_add_widget_manually, Toast.LENGTH_LONG).show()
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Icon(Icons.Outlined.Add, null)
        Text(stringResource(if (next) R.string.prayer_add_next_widget else R.string.prayer_add_day_widget), Modifier.padding(start = 8.dp))
    }
}

/** The paper and every colour of the words, for both widgets. */
private fun LazyListScope.colorOptions(current: PrayerWidgetStyle, viewModel: PrayerWidgetStyleViewModel) {
    item(key = "look_hint") { CalendarHint(stringResource(R.string.widget_look_hint)) }
    item(key = "surface") {
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
    item(key = "opacity") {
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
    item(key = "ink") {
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
    item(key = "accent") {
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
    item(key = "name_color") {
        LineColorRow(stringResource(R.string.widget_name_color), current, current.nameColor, WidgetPalette::textSoft, "widget_name_color") { choice ->
            viewModel.update { it.copy(nameColor = choice) }
        }
    }
    item(key = "time_color") {
        LineColorRow(stringResource(R.string.widget_time_color), current, current.timeColor, WidgetPalette::text, "widget_time_color") { choice ->
            viewModel.update { it.copy(timeColor = choice) }
        }
    }
}

/** The size of the words and their weights. */
private fun LazyListScope.fontOptions(current: PrayerWidgetStyle, viewModel: PrayerWidgetStyleViewModel) {
    item(key = "scale") {
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
    item(key = "weight") {
        Labelled(stringResource(R.string.widget_weight)) {
            CalendarChoices(
                options = WidgetWeight.entries,
                selected = current.weight,
                label = { weightLabel(it) },
                onSelect = { choice -> viewModel.update { it.copy(weight = choice) } },
            )
        }
    }
    item(key = "time_weight") {
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
}

/** The Islamic touch, and how the next prayer stands out. */
private fun LazyListScope.ornamentOptions(current: PrayerWidgetStyle, viewModel: PrayerWidgetStyleViewModel) {
    item(key = "ornament") {
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
        item(key = "pattern_intensity") {
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
    item(key = "next_mark") {
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
    item(key = "intensity") {
        Labelled(stringResource(R.string.widget_intensity)) {
            CalendarChoices(
                options = WidgetIntensity.entries,
                selected = current.intensity,
                label = { intensityLabel(it) },
                onSelect = { choice -> viewModel.update { it.copy(intensity = choice) } },
            )
        }
    }
}

/**
 * One colour, as a single row: its swatch and name, opening the whole choice in a dialog. Keeps
 * the colours tab short where a row of twelve swatches per colour made it long.
 */
@Composable
private fun LineColorRow(
    title: String,
    style: PrayerWidgetStyle,
    selected: LineColor,
    fallback: (WidgetPalette) -> Int,
    tag: String,
    onSelect: (LineColor) -> Unit,
) {
    val palette = WidgetPalette.of(style, MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    var open by rememberSaveable { mutableStateOf(false) }
    val shown = Color(palette.line(selected) ?: fallback(palette))
    val name = lineColorLabel(selected)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) { open = true }
            .semantics { contentDescription = "$title: $name" }
            .testTag(tag),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box(Modifier.size(22.dp).clip(CircleShape).background(shown).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                LineColors(style, selected, fallback) {
                    onSelect(it)
                    open = false
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } },
        )
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
private fun <T> Swatches(
    options: List<T>,
    selected: T,
    color: @Composable (T) -> Color,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    wrap: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    // A single row that scrolls sideways keeps a long choice to one line; a dialog wraps instead.
    val arrange: @Composable (@Composable () -> Unit) -> Unit = { content ->
        if (wrap) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
    arrange {
        options.forEach { option ->
            val chosen = option == selected
            val fill = color(option)
            val name = label(option)
            // The chosen colour is brought into sight when the row first shows, wherever it lies.
            val sight = remember { BringIntoViewRequester() }
            if (chosen && !wrap) LaunchedEffect(Unit) { sight.bringIntoView() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bringIntoViewRequester(sight)) {
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
        wrap = true,
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
        LineColorRow(stringResource(R.string.widget_line_color), style, line.color, fallback, "widget_line_color_$number") { onChange(line.copy(color = it)) }
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
