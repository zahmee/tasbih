package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.CompletionSound
import kotlin.math.roundToInt

@Composable
internal fun completionSoundName(sound: CompletionSound): String = stringResource(
    when (sound) {
        CompletionSound.ClearBell -> R.string.sound_clear_bell
        CompletionSound.DoubleChime -> R.string.sound_double_chime
        CompletionSound.TriplePulse -> R.string.sound_triple_pulse
        CompletionSound.SoftChime -> R.string.sound_soft_chime
    },
)

/**
 * The sound at a dhikr's end, for the tasbih and the adhkar alike: whether each sounds, the one
 * tone they share and its loudness. The sound tab of the adhkar's and the tasbih's settings, where
 * it is the only place these are set; the reader's own options keep a switch for the adhkar's.
 */
internal fun LazyListScope.completionSoundItems(
    state: SakinahUiState,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    onPreview: (CompletionSound) -> Unit,
    onSetTasbihSoundEnabled: (Boolean) -> Unit,
    onSetReaderSoundEnabled: (Boolean) -> Unit,
    onSetSound: (CompletionSound) -> Unit,
) {
    item {
        Text(
            stringResource(R.string.sound_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        SettingToggleRow(
            title = stringResource(R.string.tasbih_completion_sound),
            description = stringResource(R.string.tasbih_completion_sound_description),
            checked = state.tasbihCompletionSoundEnabled,
            onCheckedChange = onSetTasbihSoundEnabled,
            tag = "tasbih_sound_enabled",
        )
    }
    item {
        SettingToggleRow(
            title = stringResource(R.string.reader_completion_sound),
            description = stringResource(R.string.reader_completion_sound_description),
            checked = state.dhikrCompletionSoundEnabled,
            onCheckedChange = onSetReaderSoundEnabled,
            tag = "reader_sound_enabled",
        )
    }
    item {
        HorizontalDivider(Modifier.padding(bottom = 14.dp))
        Text(stringResource(R.string.completion_sound_tone), style = MaterialTheme.typography.titleMedium)
    }
    item(key = "completion_tones") {
        // One group, so a screen reader says which of the four is chosen.
        Column(Modifier.selectableGroup()) {
            CompletionSound.entries.forEach { sound ->
                val name = completionSoundName(sound)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("sound_choice_${sound.storageId}")
                            .selectable(
                                selected = sound == state.completionSound,
                                role = Role.RadioButton,
                                onClick = { onSetSound(sound) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = sound == state.completionSound, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                    IconButton(
                        onClick = { onPreview(sound) },
                        enabled = volume > 0f,
                        modifier = Modifier.testTag("sound_preview_${sound.storageId}"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = stringResource(R.string.preview_named_sound, name),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    item {
        val volumeLabel = stringResource(R.string.completion_sound_volume)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(volumeLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.sound_volume_percent, (volume * 100).roundToInt()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("sound_volume_value"),
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = onVolumeChangeFinished,
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.testTag("sound_volume").semantics { contentDescription = volumeLabel },
                )
                Text(
                    stringResource(R.string.completion_sound_volume_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPreview(state.completionSound) },
                    enabled = volume > 0f,
                    modifier = Modifier.fillMaxWidth().testTag("sound_preview_selected"),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.preview_sound))
                }
            }
        }
    }
}
