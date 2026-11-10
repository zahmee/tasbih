package com.sakinah.tasbih.ui.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sakinah.tasbih.ui.SettingNavigationRow
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.prayer.AsrChoice
import com.sakinah.tasbih.prayer.HighLatitudeChoice
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerMethod
import com.sakinah.tasbih.prayer.PrayerSettings
import com.sakinah.tasbih.ui.SakinahSectionHeader
import com.sakinah.tasbih.ui.SettingsPage
import com.sakinah.tasbih.ui.calendar.CalendarChoices
import com.sakinah.tasbih.ui.calendar.CalendarHint
import kotlin.math.abs

@Composable
fun PrayerSettingsScreen(
    state: PrayerState,
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    val settings = state.settings
    val countryMethod = settings.place?.let { PrayerMethod.forCountry(it.countryCode) }

    var choosingMethod by rememberSaveable { mutableStateOf(false) }
    if (choosingMethod) {
        // Twenty methods are a long list, so they open on their own instead of pushing the rest down.
        AlertDialog(
            onDismissRequest = { choosingMethod = false },
            title = { Text(stringResource(R.string.prayer_section_method)) },
            text = {
                Column(
                    Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState()),
                ) {
                    MethodRow(
                        title = stringResource(R.string.prayer_method_auto),
                        detail = countryMethod?.let(text::methodName),
                        selected = settings.method == null,
                        onClick = {
                            viewModel.setMethod(null)
                            choosingMethod = false
                        },
                    )
                    PrayerMethod.entries.forEach { method ->
                        MethodRow(text.methodName(method), null, settings.method == method) {
                            viewModel.setMethod(method)
                            choosingMethod = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choosingMethod = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    SettingsPage(stringResource(R.string.prayer_settings), onBack, "prayer_settings") {
        item { SakinahSectionHeader(stringResource(R.string.prayer_section_method)) }
        item {
            val method = settings.method ?: countryMethod
            SettingNavigationRow(
                title = method?.let(text::methodName) ?: stringResource(R.string.prayer_method_auto),
                description = stringResource(if (settings.method == null) R.string.prayer_method_auto else R.string.prayer_method_chosen),
                icon = Icons.Outlined.Calculate,
                tag = "prayer_choose_method",
                onClick = { choosingMethod = true },
            )
        }
        item { CalendarHint(stringResource(R.string.prayer_method_hint)) }

        item { SakinahSectionHeader(stringResource(R.string.prayer_section_asr)) }
        item {
            CalendarChoices(
                options = AsrChoice.entries,
                selected = settings.asr,
                label = {
                    stringResource(
                        when (it) {
                            AsrChoice.AUTO -> R.string.cal_automatic
                            AsrChoice.SHAFI -> R.string.prayer_asr_shafi
                            AsrChoice.HANAFI -> R.string.prayer_asr_hanafi_short
                        },
                    )
                },
                onSelect = viewModel::setAsr,
            )
        }
        item { CalendarHint(stringResource(R.string.prayer_asr_hint)) }

        item { SakinahSectionHeader(stringResource(R.string.prayer_section_adjust)) }
        item { CalendarHint(stringResource(R.string.prayer_adjust_hint)) }
        Prayer.entries.forEach { prayer ->
            item(key = "adjust-${prayer.name}") {
                AdjustRow(
                    name = text.name(prayer),
                    minutes = settings.adjustments[prayer] ?: 0,
                    onChange = { viewModel.setAdjustment(prayer, it) },
                )
            }
        }

        item { SakinahSectionHeader(stringResource(R.string.prayer_section_high_latitude)) }
        item {
            CalendarChoices(
                options = HighLatitudeChoice.entries,
                selected = settings.highLatitude,
                label = {
                    stringResource(
                        when (it) {
                            HighLatitudeChoice.AUTO -> R.string.cal_automatic
                            HighLatitudeChoice.MIDDLE_OF_THE_NIGHT -> R.string.prayer_high_middle
                            HighLatitudeChoice.SEVENTH_OF_THE_NIGHT -> R.string.prayer_high_seventh
                            HighLatitudeChoice.TWILIGHT_ANGLE -> R.string.prayer_high_angle
                        },
                    )
                },
                onSelect = viewModel::setHighLatitude,
            )
        }
        item { CalendarHint(stringResource(R.string.prayer_high_latitude_hint)) }
    }
}

@Composable
private fun MethodRow(title: String, detail: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        RadioButton(selected, onClick = null)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AdjustRow(name: String, minutes: Int, onChange: (Int) -> Unit) {
    val max = PrayerSettings.MAX_ADJUSTMENT
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(minutes - 1) }, enabled = minutes > -max) {
            Icon(Icons.Outlined.Remove, stringResource(R.string.prayer_adjust_less, name))
        }
        // Kept left to right, or the sign would land after the digits in Arabic.
        val value = when {
            minutes > 0 -> "⁦+${arabicNumber(minutes)}⁩"
            minutes < 0 -> "⁦−${arabicNumber(abs(minutes))}⁩"
            else -> arabicNumber(0)
        }
        Text(
            stringResource(R.string.prayer_adjust_minutes, value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(88.dp),
        )
        IconButton(onClick = { onChange(minutes + 1) }, enabled = minutes < max) {
            Icon(Icons.Outlined.Add, stringResource(R.string.prayer_adjust_more, name))
        }
    }
}
