package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.data.arabicNumber

private fun resetClockText(settings: DailyResetSettings): String =
    "${arabicNumber(settings.time.hour).padStart(2, '٠')}:${arabicNumber(settings.time.minute).padStart(2, '٠')}"

@Composable
internal fun isCompactSettingsInput(): Boolean {
    val density = LocalDensity.current
    val height = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    return height < 400.dp && WindowInsets.ime.getBottom(density) > 0
}

@Composable
internal fun dailyResetSummary(settings: DailyResetSettings): String = when {
    !settings.tasbihEnabled && !settings.adhkarEnabled -> stringResource(R.string.daily_reset_off_summary)
    settings.tasbihEnabled && settings.adhkarEnabled -> stringResource(R.string.daily_reset_both_summary, resetClockText(settings))
    settings.tasbihEnabled -> stringResource(R.string.daily_reset_tasbih_summary, resetClockText(settings))
    else -> stringResource(R.string.daily_reset_adhkar_summary, resetClockText(settings))
}

internal fun LazyListScope.dailyResetSettingsItems(
    settings: DailyResetSettings,
    onSetTasbih: (Boolean) -> Unit,
    onSetAdhkar: (Boolean) -> Unit,
    onSetTime: (Int) -> Unit,
) {
    item {
        Text(stringResource(R.string.daily_reset_intro), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    item { SettingToggleRow(stringResource(R.string.daily_reset_tasbih), stringResource(R.string.daily_reset_tasbih_description), settings.tasbihEnabled, onSetTasbih, "setting_daily_reset_tasbih") }
    item { SettingToggleRow(stringResource(R.string.daily_reset_adhkar), stringResource(R.string.daily_reset_adhkar_description), settings.adhkarEnabled, onSetAdhkar, "setting_daily_reset_adhkar") }
    item { HorizontalDivider(); DailyResetTimeSetting(settings, onSetTime) }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dailyResetSummary(settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("daily_reset_summary"))
            Text(stringResource(R.string.daily_reset_history_note), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyResetTimeSetting(settings: DailyResetSettings, onSetTime: (Int) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (!editing) {
        SettingNavigationRow(
            stringResource(R.string.daily_reset_time),
            stringResource(if (settings.minuteOfDay == 0) R.string.daily_reset_midnight else R.string.daily_reset_clock, resetClockText(settings)),
            Icons.Outlined.Schedule, "daily_reset_time", { editing = true },
        )
    } else {
        DailyResetTimeEditor(settings, onSave = { onSetTime(it); editing = false }, onCancel = { editing = false })
    }
}

@Composable
private fun DailyResetTimeEditor(settings: DailyResetSettings, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var hour by rememberSaveable { mutableStateOf(arabicNumber(settings.time.hour).padStart(2, '٠')) }
    var minute by rememberSaveable { mutableStateOf(arabicNumber(settings.time.minute).padStart(2, '٠')) }
    val validHour = hour.toIntOrNull()?.takeIf { it in 0..23 }
    val validMinute = minute.toIntOrNull()?.takeIf { it in 0..59 }
    val focus = LocalFocusManager.current
    val compactInput = isCompactSettingsInput()
    val save = {
        if (validHour != null && validMinute != null) {
            focus.clearFocus()
            onSave(validHour * 60 + validMinute)
        }
    }
    val fields: @Composable RowScope.() -> Unit = {
            OutlinedTextField(
                value = hour, onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.weight(1f).testTag("daily_reset_hour"),
                label = { Text(stringResource(R.string.daily_reset_hour)) },
                supportingText = if (compactInput) null else { { Text(stringResource(R.string.daily_reset_hour_range)) } },
                singleLine = true, isError = validHour == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = minute, onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                modifier = Modifier.weight(1f).testTag("daily_reset_minute"),
                label = { Text(stringResource(R.string.daily_reset_minute)) },
                supportingText = if (compactInput) null else { { Text(stringResource(R.string.daily_reset_minute_range)) } },
                singleLine = true, isError = validMinute == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
    }
    val actions: @Composable () -> Unit = {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = save, enabled = validHour != null && validMinute != null, modifier = Modifier.heightIn(min = 48.dp).testTag("daily_reset_time_save")) {
                Text(stringResource(R.string.daily_reset_save_time))
            }
            TextButton(onClick = { focus.clearFocus(); onCancel() }, modifier = Modifier.heightIn(min = 48.dp).testTag("daily_reset_time_cancel")) { Text(stringResource(R.string.cancel)) }
        }
    }
    Column(Modifier.fillMaxWidth().testTag("daily_reset_time_editor").padding(top = if (compactInput) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!compactInput) {
            Text(stringResource(R.string.daily_reset_time), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.daily_reset_time_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Keep the fields at the same composition location when the IME changes the layout,
        // otherwise recreating a focused field would immediately dismiss the keyboard.
        Row(
            modifier = if (compactInput) Modifier.fillMaxWidth() else Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), content = fields)
            if (compactInput) actions()
        }
        if (!compactInput) {
            Text(stringResource(R.string.daily_reset_change_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}
