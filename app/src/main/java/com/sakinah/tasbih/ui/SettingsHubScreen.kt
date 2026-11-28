package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.AlertHealth
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.prayer.AlertPrayers
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.PrayerMethod
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.prayer.PrayerState
import com.sakinah.tasbih.ui.prayer.PrayerText

/**
 * The sections of the one settings tree, in the order its page lists them. Each tab's gear opens
 * its own section directly; the gear on the day's page opens the whole list. Before November 28,
 * 2026 each tab had pages of its own, which held the same sound, reset time and widgets twice.
 */
enum class SettingsArea { APPEARANCE, LOCATION, PRAYER, ALERTS, CALENDAR, DHIKR, WIDGETS, ABOUT }

/**
 * Every section of the settings, each with what it is set to now, so most questions are answered
 * without opening one. On a wide screen the chosen section shows beside this list, [selected].
 */
@Composable
fun SettingsHubScreen(
    state: SakinahUiState,
    prayer: PrayerState?,
    alerts: AlertSettings?,
    calendarText: CalendarText?,
    selected: SettingsArea?,
    onOpen: (SettingsArea) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prayerText = remember(context) { PrayerText(context) }
    SettingsPage(stringResource(R.string.settings_hub_title), onBack, "settings_hub") {
        SettingsArea.entries.forEach { area ->
            item(key = area.name) {
                Column {
                    SettingNavigationRow(
                        title = areaTitle(area),
                        description = areaSummary(area, state, prayer, alerts, calendarText, prayerText),
                        icon = areaIcon(area),
                        tag = if (area == SettingsArea.ABOUT) "open_about" else "settings_open_${area.name.lowercase()}",
                        onClick = { onOpen(area) },
                        selected = area == selected,
                    )
                    // Lines part the whole app's look and place, the tabs' own sections, and about the app.
                    if (area == SettingsArea.LOCATION || area == SettingsArea.WIDGETS) HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

/** What a wide screen shows beside the list before a section is chosen. */
@Composable
fun SettingsPickSection() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize().testTag("settings_pick_section")) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.widthIn(max = 320.dp).padding(24.dp),
            ) {
                KhatamMedallion(Icons.Outlined.Settings)
                Text(
                    stringResource(R.string.settings_pick_section),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun areaTitle(area: SettingsArea): String = stringResource(
    when (area) {
        SettingsArea.APPEARANCE -> R.string.settings_appearance
        SettingsArea.LOCATION -> R.string.settings_location
        SettingsArea.PRAYER -> R.string.settings_prayer
        SettingsArea.ALERTS -> R.string.alert_title
        SettingsArea.CALENDAR -> R.string.settings_calendar
        SettingsArea.DHIKR -> R.string.settings_dhikr
        SettingsArea.WIDGETS -> R.string.settings_widgets
        SettingsArea.ABOUT -> R.string.about_app
    },
)

private fun areaIcon(area: SettingsArea): ImageVector = when (area) {
    SettingsArea.APPEARANCE -> Icons.Outlined.Palette
    SettingsArea.LOCATION -> Icons.Outlined.LocationOn
    SettingsArea.PRAYER -> Icons.Outlined.Schedule
    SettingsArea.ALERTS -> Icons.Outlined.NotificationsActive
    SettingsArea.CALENDAR -> Icons.Outlined.CalendarMonth
    SettingsArea.DHIKR -> Icons.Outlined.AutoStories
    SettingsArea.WIDGETS -> Icons.Outlined.Widgets
    SettingsArea.ABOUT -> Icons.Outlined.Info
}

@Composable
private fun areaSummary(
    area: SettingsArea,
    state: SakinahUiState,
    prayer: PrayerState?,
    alerts: AlertSettings?,
    calendarText: CalendarText?,
    prayerText: PrayerText,
): String = when (area) {
    SettingsArea.APPEARANCE -> stringResource(
        R.string.settings_appearance_summary,
        stringResource(
            when (state.themeMode) {
                ThemeMode.System -> R.string.settings_theme_follow_phone
                ThemeMode.Light -> R.string.theme_light
                ThemeMode.Dark -> R.string.theme_dark
            },
        ),
        stringResource(
            when (state.arabicFontStyle) {
                ArabicFontStyle.Sakinah -> R.string.font_sakinah
                ArabicFontStyle.Amiri -> R.string.font_amiri
                ArabicFontStyle.Ruqaa -> R.string.font_ruqaa
                ArabicFontStyle.Baqiyat -> R.string.font_baqiyat
            },
        ),
    )
    SettingsArea.LOCATION -> {
        val settings = prayer?.settings
        val place = settings?.place
        when {
            place == null -> stringResource(R.string.settings_location_none)
            settings.followDevice -> stringResource(R.string.settings_location_follow, place.name)
            else -> place.name
        }
    }
    SettingsArea.PRAYER -> {
        val settings = prayer?.settings
        val method = settings?.method ?: settings?.place?.let { PrayerMethod.forCountry(it.countryCode) }
        method?.let(prayerText::methodName) ?: stringResource(R.string.prayer_method_auto)
    }
    SettingsArea.ALERTS -> alertsSummary(alerts, prayerText)
    SettingsArea.CALENDAR -> calendarText?.let { text ->
        stringResource(
            R.string.settings_calendar_summary,
            stringResource(if (text.settings.primary == PrimaryCalendar.HIJRI) R.string.cal_hijri else R.string.cal_gregorian),
            text.weekday(text.firstDayOfWeek()),
        )
    } ?: ""
    SettingsArea.DHIKR -> stringResource(R.string.settings_dhikr_summary)
    SettingsArea.WIDGETS -> stringResource(R.string.settings_widgets_summary)
    SettingsArea.ABOUT -> stringResource(R.string.about_app_entry_description)
}

/** Which prayers alert, or that something on the phone keeps them from coming on time. */
@Composable
private fun alertsSummary(alerts: AlertSettings?, prayerText: PrayerText): String {
    if (alerts == null || !alerts.anyOn) return stringResource(R.string.settings_alerts_off)
    val context = LocalContext.current
    val healthy = remember(alerts) { AlertHealth.read(context, alerts.tone).allGood }
    if (!healthy) return stringResource(R.string.settings_alerts_attention)
    val on = AlertPrayers.filter { alerts.alertFor(it).style != AlertStyle.OFF || alerts.alertFor(it).reminderMinutes > 0 }
    return stringResource(R.string.settings_alerts_on, on.joinToString("، ") { prayerText.name(it) })
}
