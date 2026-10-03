package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSettingsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
    onSetTasbihSoundEnabled: (Boolean) -> Unit,
    onSetReaderSoundEnabled: (Boolean) -> Unit,
    onSetSound: (CompletionSound) -> Unit,
    onSetVolume: (Float) -> Unit,
) {
    val player = rememberCompletionSoundPlayer()
    var volume by remember(state.completionSoundVolume) { mutableFloatStateOf(state.completionSoundVolume) }
    val volumeLabel = stringResource(R.string.completion_sound_volume)
    Scaffold(
        modifier = Modifier.testTag("sound_settings_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.sound_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("sound_settings_back")) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = SakinahContentMaxWidth)
                .fillMaxSize()
                .testTag("sound_settings_list")
                .selectableGroup(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.sound_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
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
                HorizontalDivider(Modifier.padding(vertical = 14.dp))
                Text(stringResource(R.string.completion_sound_tone), style = MaterialTheme.typography.titleMedium)
            }
            items(CompletionSound.entries, key = { it.storageId }) { sound ->
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
                        onClick = { player.play(sound, volume) },
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
            item {
                Spacer(Modifier.height(10.dp))
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
                            onValueChange = { volume = it },
                            onValueChangeFinished = { onSetVolume(volume) },
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
                            onClick = { player.play(state.completionSound, volume) },
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
        }
    }
}
