package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.ui.platform.testTag
import com.sakinah.tasbih.calendar.weekDays
import com.sakinah.tasbih.calendar.GridStyle
import com.sakinah.tasbih.calendar.DayColor
import com.sakinah.tasbih.calendar.DeviceAgenda
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.calendar.WeekStart
import com.sakinah.tasbih.ui.SettingToggleRow
import com.sakinah.tasbih.ui.SettingsTab
import com.sakinah.tasbih.ui.TabbedSettingsPage
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.DayOfWeek
import kotlin.math.abs

/**
 * The three tabs the calendar's settings are gathered under. The colours were a tab of their own
 * and the widgets another until November 28, 2026, when the widgets went to their own section.
 */
private enum class CalendarTab { DISPLAY, HIJRI, AGENDA }

/**
 * The calendar's settings, in the same order as the prayer widgets' customisation: the week
 * that holds today stays on top as a preview, and the options sit in three tabs under it. Opened
 * from the calendar, from the settings, or from a calendar widget's own «customise».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarSettingsScreen(
    text: CalendarText,
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
    onOpenAllSettings: (() -> Unit)? = null,
) {
    val settings = text.settings
    val context = LocalContext.current
    val agenda by viewModel.agenda.collectAsStateWithLifecycle()
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setAgendaEnabled(granted)
    }
    LaunchedEffect(Unit) { viewModel.loadAgenda() }
    val auto = stringResource(R.string.cal_automatic)
    var tab by rememberSaveable { mutableStateOf(CalendarTab.DISPLAY) }
    TabbedSettingsPage(
        title = stringResource(R.string.cal_settings),
        onBack = onBack,
        tag = "calendar_settings",
        tabs = CalendarTab.entries.map { SettingsTab(calendarTabLabel(it), calendarTabIcon(it), "calendar_tab_${it.name.lowercase()}") },
        selected = tab.ordinal,
        onSelect = { tab = CalendarTab.entries[it] },
        header = { CalendarWeekPreview(text, LocalDate.now()) },
        actions = { com.sakinah.tasbih.ui.AllSettingsAction(onOpenAllSettings) },
    ) {
        when (tab) {
            CalendarTab.DISPLAY -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cal_main_calendar), style = MaterialTheme.typography.titleMedium)
                        CalendarChoices(
                            options = PrimaryCalendar.entries,
                            selected = settings.primary,
                            label = { stringResource(if (it == PrimaryCalendar.HIJRI) R.string.cal_hijri else R.string.cal_gregorian) },
                            onSelect = viewModel::setPrimary,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cal_week_start), style = MaterialTheme.typography.titleMedium)
                        CalendarChoices(
                            options = WeekStart.entries,
                            selected = settings.weekStart,
                            label = {
                                when (it) {
                                    WeekStart.AUTO -> auto
                                    WeekStart.SATURDAY -> text.weekday(DayOfWeek.SATURDAY)
                                    WeekStart.SUNDAY -> text.weekday(DayOfWeek.SUNDAY)
                                    WeekStart.MONDAY -> text.weekday(DayOfWeek.MONDAY)
                                }
                            },
                            onSelect = viewModel::setWeekStart,
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cal_weekend), style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("calendar_weekend"),
                        ) {
                            weekDays(text.firstDayOfWeek()).forEach { day ->
                                val selected = day in settings.weekendDays
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.toggleWeekendDay(day) },
                                    label = { Text(text.weekday(day)) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                        CalendarHint(stringResource(R.string.cal_weekend_hint))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cal_grid_style), style = MaterialTheme.typography.titleMedium)
                        CalendarChoices(
                            options = GridStyle.entries,
                            selected = settings.gridStyle,
                            label = { stringResource(if (it == GridStyle.TABLE) R.string.cal_grid_table else R.string.cal_grid_plain) },
                            onSelect = viewModel::setGridStyle,
                        )
                    }
                }
                item {
                    SettingToggleRow(
                        stringResource(R.string.cal_show_white_days),
                        stringResource(R.string.cal_show_white_days_hint),
                        settings.showWhiteDays,
                        viewModel::setShowWhiteDays,
                        "calendar_white_days",
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.cal_hijri_color), style = MaterialTheme.typography.titleMedium)
                        DayColorPicker(settings.hijriColor, viewModel::setHijriColor, Modifier.testTag("calendar_hijri_color"))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.cal_gregorian_color), style = MaterialTheme.typography.titleMedium)
                        DayColorPicker(settings.gregorianColor, viewModel::setGregorianColor, Modifier.testTag("calendar_gregorian_color"))
                        CalendarHint(stringResource(R.string.cal_color_hint))
                    }
                }
            }
            CalendarTab.HIJRI -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.cal_offset), style = MaterialTheme.typography.titleMedium)
                        CalendarChoices(
                            options = (-HijriCalendar.MAX_OFFSET..HijriCalendar.MAX_OFFSET).toList(),
                            selected = settings.hijriOffsetDays.coerceIn(-HijriCalendar.MAX_OFFSET, HijriCalendar.MAX_OFFSET),
                            label = { offset ->
                                // Kept left to right, or the sign would land after the digit in Arabic.
                                when {
                                    offset > 0 -> "\u2066+${text.number(offset)}\u2069"
                                    offset < 0 -> "\u2066−${text.number(abs(offset))}\u2069"
                                    else -> text.number(0)
                                }
                            },
                            onSelect = viewModel::setHijriOffset,
                        )
                        CalendarHint(stringResource(R.string.cal_offset_hint))
                    }
                }
                item {
                    val corrections = settings.monthCorrections.mapNotNull { (key, days) ->
                        HijriMonth.fromKey(key)?.let { it to days }
                    }.sortedBy { it.first }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.cal_corrected_months), style = MaterialTheme.typography.titleMedium)
                        if (corrections.isEmpty()) {
                            CalendarHint(stringResource(R.string.cal_corrected_months_none))
                        }
                        corrections.forEach { (month, days) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(text.hijriMonthYear(month), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(if (days > 0) R.string.cal_began_later else R.string.cal_began_earlier),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.setCorrection(month, 0) }) {
                                    Text(stringResource(R.string.cal_undo))
                                }
                            }
                        }
                    }
                }

                item {
                    SettingToggleRow(
                        stringResource(R.string.cal_day_from_maghrib),
                        stringResource(R.string.cal_day_from_maghrib_hint),
                        settings.hijriDayFromMaghrib,
                        viewModel::setHijriDayFromMaghrib,
                        "calendar_day_from_maghrib",
                    )
                }
            }
            CalendarTab.AGENDA -> {
                item {
                    SettingToggleRow(
                        stringResource(R.string.cal_agenda_enable),
                        stringResource(R.string.cal_agenda_enable_hint),
                        settings.agendaEnabled && agenda.granted,
                        { on -> if (!on) viewModel.setAgendaEnabled(false) else if (DeviceAgenda.granted(context)) viewModel.setAgendaEnabled(true) else askPermission.launch(DeviceAgenda.PERMISSION) },
                        "calendar_agenda",
                    )
                }
                if (settings.agendaEnabled && agenda.granted) {
                    if (agenda.calendars.isEmpty()) {
                        item { CalendarHint(stringResource(R.string.cal_agenda_none)) }
                    }
                    agenda.calendars.forEach { deviceCalendar ->
                        item(key = "agenda_${deviceCalendar.id}") {
                            val shown = deviceCalendar.id !in settings.agendaHidden
                            val shownLabel = stringResource(R.string.cal_agenda_shown)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text(deviceCalendar.name.ifBlank { deviceCalendar.account }, style = MaterialTheme.typography.titleMedium)
                                        if (deviceCalendar.account.isNotBlank() && deviceCalendar.account != deviceCalendar.name) {
                                            Text(
                                                "\u2068${deviceCalendar.account}\u2069",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = shown,
                                        onCheckedChange = { viewModel.setAgendaShown(deviceCalendar.id, it) },
                                        modifier = Modifier.semantics { contentDescription = shownLabel },
                                    )
                                }
                                if (shown) {
                                    DayColorPicker(
                                        selected = settings.agendaColors[deviceCalendar.id] ?: DayColor.THEME,
                                        onSelect = { viewModel.setAgendaColor(deviceCalendar.id, it) },
                                        ownInk = Color(deviceCalendar.color).copy(alpha = 1f),
                                        ownLabel = R.string.cal_color_own,
                                    )
                                }
                            }
                        }
                    }
                } else if (settings.agendaEnabled && !agenda.granted) {
                    item { CalendarHint(stringResource(R.string.cal_agenda_denied)) }
                }
            }
        }
    }
}

@Composable
private fun calendarTabLabel(tab: CalendarTab): String = stringResource(
    when (tab) {
        CalendarTab.DISPLAY -> R.string.settings_tab_display_colors
        CalendarTab.HIJRI -> R.string.cal_tab_hijri
        CalendarTab.AGENDA -> R.string.cal_tab_agenda
    },
)

private fun calendarTabIcon(tab: CalendarTab): ImageVector = when (tab) {
    CalendarTab.DISPLAY -> Icons.Outlined.CalendarViewWeek
    CalendarTab.HIJRI -> Icons.Outlined.NightsStay
    CalendarTab.AGENDA -> Icons.Outlined.Event
}
