package com.sakinah.tasbih.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.calendar.CalendarHint
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.prayer.PlacedPrayerWidgets
import com.sakinah.tasbih.widget.CalendarWidgetKind
import com.sakinah.tasbih.widget.PrayerWidgetKind

/**
 * Every widget of the app in one section: the prayer and qibla widgets with the look of each one
 * placed, and the calendar's. They were a tab of the prayer settings and a tab of the calendar's
 * until November 28, 2026.
 */
@Composable
fun WidgetSettingsScreen(
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    onBack: () -> Unit,
    onOpenAllSettings: (() -> Unit)?,
    /** Opens the look of a widget on the home screen, or with null the default look. */
    onOpenWidgetStyle: (Int?) -> Unit,
    onOpenCalendarSettings: () -> Unit,
) {
    val context = LocalContext.current
    SettingsPage(stringResource(R.string.settings_widgets), onBack, "widget_settings", actions = { AllSettingsAction(onOpenAllSettings) }) {
        item(key = "prayer-header") { SakinahSectionHeader(stringResource(R.string.settings_widgets_prayer)) }
        item(key = "placed") { PlacedPrayerWidgets(schedule, calendarText, onOpenWidgetStyle) }
        item(key = "default-look") {
            SettingNavigationRow(
                stringResource(R.string.widget_style_entry),
                stringResource(R.string.widget_style_entry_hint),
                Icons.Outlined.Widgets,
                "prayer_open_widget_style",
                { onOpenWidgetStyle(null) },
            )
        }
        item(key = "prayer-add") {
            AddWidgetButtons(
                listOf(
                    R.string.prayer_add_next_widget to { ComponentName(context, PrayerWidgetKind.NEXT.receiver) },
                    R.string.prayer_add_day_widget to { ComponentName(context, PrayerWidgetKind.DAY.receiver) },
                    R.string.prayer_add_qibla_widget to { ComponentName(context, PrayerWidgetKind.QIBLA.receiver) },
                ),
            )
        }
        item(key = "calendar-header") { SakinahSectionHeader(stringResource(R.string.settings_widgets_calendar)) }
        item(key = "calendar-hint") { CalendarHint(stringResource(R.string.cal_widgets_hint)) }
        item(key = "calendar-add") {
            AddWidgetButtons(
                listOf(
                    R.string.cal_add_date_widget to { CalendarWidgetKind.DATE.provider(context) },
                    R.string.cal_add_month_widget to { CalendarWidgetKind.MONTH.provider(context) },
                    R.string.cal_add_occasion_widget to { CalendarWidgetKind.OCCASION.provider(context) },
                ),
            )
        }
        item(key = "calendar-look") {
            SettingNavigationRow(
                stringResource(R.string.settings_widgets_calendar_look),
                stringResource(R.string.settings_widgets_calendar_look_hint),
                Icons.Outlined.Palette,
                "widgets_open_calendar_settings",
                onOpenCalendarSettings,
            )
        }
    }
}

/** Buttons that ask the launcher to place a widget, or say how to when it cannot be asked. */
@Composable
private fun AddWidgetButtons(widgets: List<Pair<Int, () -> ComponentName>>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        widgets.forEach { (label, provider) ->
            OutlinedButton(
                onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    val asked = manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(provider(), null, null)
                    if (!asked) Toast.makeText(context, R.string.cal_add_widget_manually, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Outlined.Add, null)
                Text(stringResource(label), Modifier.padding(start = 8.dp))
            }
        }
    }
}
