package com.sakinah.tasbih.ui.prayer

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.AlertHealth
import com.sakinah.tasbih.alerts.PrayerSounds
import com.sakinah.tasbih.prayer.AlertPrayers
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerAlert
import com.sakinah.tasbih.ui.SakinahSectionHeader
import com.sakinah.tasbih.ui.SettingsPage
import com.sakinah.tasbih.ui.calendar.CalendarChoices
import com.sakinah.tasbih.ui.calendar.CalendarHint

@Composable
fun AlertSettingsScreen(
    alerts: AlertSettings,
    viewModel: PrayerViewModel,
    onOpenReliability: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    var health by remember { mutableStateOf(AlertHealth.read(context, alerts.tone)) }
    LifecycleResumeEffect(alerts.tone) {
        health = AlertHealth.read(context, alerts.tone)
        viewModel.refresh()
        onPauseOrDispose { }
    }
    var askNotifications by rememberSaveable { mutableStateOf(false) }
    var testScheduled by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        health = AlertHealth.read(context, alerts.tone)
    }
    val preview = rememberPreview()

    fun change(prayer: Prayer, alert: PrayerAlert) {
        val turningOn = alert.style != AlertStyle.OFF || alert.reminderMinutes > 0
        if (turningOn && !health.notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications = true
        viewModel.setAlert(prayer, alert)
    }

    if (askNotifications) {
        AlertDialog(
            onDismissRequest = { askNotifications = false },
            title = { Text(stringResource(R.string.alert_permission_title)) },
            text = { Text(stringResource(R.string.alert_permission_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    askNotifications = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text(stringResource(R.string.prayer_location_continue)) }
            },
            dismissButton = { TextButton(onClick = { askNotifications = false }) { Text(stringResource(R.string.prayer_not_now)) } },
        )
    }

    // Which adhan list asked for a file, kept across the file picker.
    var pickingForFajr by rememberSaveable { mutableStateOf(false) }
    // A file the user picks is kept by address, with a lasting right to read it.
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.setCustomAdhan(uri.toString(), fajr = pickingForFajr)
        }
    }

    SettingsPage(stringResource(R.string.alert_title), onBack, "prayer_alerts") {
        if (alerts.anyOn && !health.allGood) {
            item {
                Surface(
                    onClick = onOpenReliability,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, null)
                        Text(
                            stringResource(R.string.alert_needs_attention),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp).weight(1f),
                        )
                    }
                }
            }
        }

        item { SakinahSectionHeader(stringResource(R.string.alert_section_prayers)) }
        AlertPrayers.forEach { prayer ->
            item(key = "alert-${prayer.name}") {
                val alert = alerts.alertFor(prayer)
                // Sunrise ends Fajr's time and is no prayer, so it has no adhan.
                val styles = if (prayer == Prayer.SUNRISE) AlertStyle.entries - AlertStyle.ADHAN else AlertStyle.entries
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text.name(prayer), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        ReminderChooser(alert.reminderMinutes, text) { change(prayer, alert.copy(reminderMinutes = it)) }
                    }
                    CalendarChoices(
                        options = styles,
                        selected = alert.style,
                        label = { stringResource(it.labelRes) },
                        onSelect = { change(prayer, alert.copy(style = it)) },
                    )
                }
            }
        }
        item { CalendarHint(stringResource(R.string.alert_styles_hint)) }

        item { SakinahSectionHeader(stringResource(R.string.alert_section_tone)) }
        PrayerSounds.tones.forEach { tone ->
            item(key = "tone-${tone.id}") {
                SoundRow(
                    title = stringResource(tone.label),
                    selected = alerts.tone == tone.id,
                    playing = preview.playing == "tone-${tone.id}",
                    onSelect = { viewModel.setTone(tone.id) },
                    onPreview = { preview.toggle("tone-${tone.id}", PrayerSounds.uri(context, tone.raw)) },
                )
            }
        }

        listOf(false, true).forEach { fajr ->
            item { SakinahSectionHeader(stringResource(if (fajr) R.string.alert_section_fajr_adhan else R.string.alert_section_adhan)) }
            val chosen = if (fajr) alerts.fajrAdhan else alerts.adhan
            PrayerSounds.adhans.forEach { adhan ->
                item(key = "adhan-$fajr-${adhan.id}") {
                    SoundRow(
                        title = stringResource(adhan.label),
                        detail = adhan.credit,
                        selected = chosen == adhan.id,
                        playing = preview.playing == "adhan-${adhan.id}",
                        onSelect = { viewModel.setAdhan(adhan.id, fajr) },
                        onPreview = { preview.toggle("adhan-${adhan.id}", PrayerSounds.uri(context, adhan.raw)) },
                    )
                }
            }
            item(key = "adhan-$fajr-custom") {
                val custom = alerts.customAdhanUri?.let(Uri::parse)
                SoundRow(
                    title = stringResource(R.string.alert_adhan_custom),
                    detail = stringResource(if (custom == null) R.string.alert_adhan_custom_none else R.string.alert_adhan_custom_chosen),
                    selected = chosen == AlertSettings.CUSTOM_ADHAN,
                    playing = preview.playing == "custom",
                    onSelect = {
                        pickingForFajr = fajr
                        pickFile.launch(arrayOf("audio/*"))
                    },
                    onPreview = custom?.let { { preview.toggle("custom", it) } },
                    icon = Icons.Outlined.AudioFile,
                )
            }
        }
        item { CalendarHint(stringResource(R.string.alert_adhan_hint)) }

        item { SakinahSectionHeader(stringResource(R.string.alert_section_test)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.testAlert()
                        testScheduled = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.alert_test)) }
                CalendarHint(stringResource(if (testScheduled) R.string.alert_test_scheduled else R.string.alert_test_hint))
                TextButton(onClick = onOpenReliability) { Text(stringResource(R.string.alert_reliability_title)) }
            }
        }
    }
}

@Composable
private fun ReminderChooser(minutes: Int, text: PrayerText, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                if (minutes == 0) stringResource(R.string.alert_no_reminder) else stringResource(R.string.alert_reminder_before, text.minutes(minutes)),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AlertSettings.REMINDER_CHOICES.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Text(if (choice == 0) stringResource(R.string.alert_no_reminder) else stringResource(R.string.alert_reminder_before, text.minutes(choice)))
                    },
                    onClick = {
                        open = false
                        onChange(choice)
                    },
                )
            }
        }
    }
}

@Composable
private fun SoundRow(
    title: String,
    selected: Boolean,
    playing: Boolean,
    onSelect: () -> Unit,
    onPreview: (() -> Unit)?,
    detail: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(selected, onClick = null)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!detail.isNullOrBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (icon != null && onPreview == null) Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        if (onPreview != null) {
            IconButton(onClick = onPreview) {
                Icon(
                    if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    stringResource(if (playing) R.string.alert_preview_stop else R.string.alert_preview_play, title),
                )
            }
        }
    }
}

/** Plays one sound at a time inside the screen, and stops when the screen goes. */
private class Preview(private val context: android.content.Context) {
    var playing by mutableStateOf<String?>(null)
        private set
    private var player: MediaPlayer? = null

    fun toggle(id: String, uri: Uri) {
        val wasPlaying = playing == id
        stop()
        if (wasPlaying) return
        player = runCatching {
            MediaPlayer.create(context, uri)?.apply {
                setOnCompletionListener { stop() }
                start()
            }
        }.getOrNull()
        playing = if (player != null) id else null
    }

    fun stop() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
        playing = null
    }
}

@Composable
private fun rememberPreview(): Preview {
    val context = LocalContext.current
    val preview = remember { Preview(context) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    return preview
}

val AlertStyle.labelRes: Int
    get() = when (this) {
        AlertStyle.OFF -> R.string.alert_style_off
        AlertStyle.SILENT -> R.string.alert_style_silent
        AlertStyle.SOUND -> R.string.alert_style_sound
        AlertStyle.ADHAN -> R.string.alert_style_adhan
    }
