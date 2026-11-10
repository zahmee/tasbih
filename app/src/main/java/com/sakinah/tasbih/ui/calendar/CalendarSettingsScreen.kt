package com.sakinah.tasbih.ui.calendar

import android.appwidget.AppWidgetManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sakinah.tasbih.ui.SakinahSectionHeader
import com.sakinah.tasbih.ui.SettingToggleRow
import com.sakinah.tasbih.ui.SettingsPage
import com.sakinah.tasbih.widget.CalendarWidgetKind
import java.time.DayOfWeek
import kotlin.math.abs

@Composable
fun CalendarSettingsScreen(
    text: CalendarText,
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
) {
    val settings = text.settings
    val context = LocalContext.current
    val auto = stringResource(R.string.cal_automatic)
    SettingsPage(stringResource(R.string.cal_settings), onBack, "calendar_settings") {
        item { SakinahSectionHeader(stringResource(R.string.cal_section_display)) }
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

        item { SakinahSectionHeader(stringResource(R.string.cal_section_adjust)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.cal_offset), style = MaterialTheme.typography.titleMedium)
                CalendarChoices(
                    options = (-HijriCalendar.MAX_OFFSET..HijriCalendar.MAX_OFFSET).toList(),
                    selected = settings.hijriOffsetDays.coerceIn(-HijriCalendar.MAX_OFFSET, HijriCalendar.MAX_OFFSET),
                    label = { offset ->
                        // Kept left to right, or the sign would land after the digit in Arabic.
                        when {
                            offset > 0 -> "⁦+${text.number(offset)}⁩"
                            offset < 0 -> "⁦−${text.number(abs(offset))}⁩"
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

        item { SakinahSectionHeader(stringResource(R.string.cal_occasions)) }
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
            SettingToggleRow(
                stringResource(R.string.cal_show_other),
                stringResource(R.string.cal_show_other_hint),
                settings.showOtherOccasions,
                viewModel::setShowOtherOccasions,
                "calendar_other_occasions",
            )
        }

        item { SakinahSectionHeader(stringResource(R.string.cal_section_widgets)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarHint(stringResource(R.string.cal_widgets_hint))
                listOf(
                    CalendarWidgetKind.DATE to R.string.cal_add_date_widget,
                    CalendarWidgetKind.MONTH to R.string.cal_add_month_widget,
                    CalendarWidgetKind.OCCASION to R.string.cal_add_occasion_widget,
                ).forEach { (kind, label) ->
                    OutlinedButton(
                        onClick = {
                            val manager = AppWidgetManager.getInstance(context)
                            val asked = manager.isRequestPinAppWidgetSupported &&
                                manager.requestPinAppWidget(kind.provider(context), null, null)
                            if (!asked) Toast.makeText(context, R.string.cal_add_widget_manually, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Text(stringResource(label), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
    }
}
