package com.sakinah.tasbih.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.theme.dhikrFontFor
import kotlin.math.roundToInt

enum class SettingsSection { Reading, Tasbih, Appearance, DailyReset }

@Composable
fun SettingsScreen(
    state: SakinahUiState,
    onOpenSection: (SettingsSection) -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    SakinahScreenBackground {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = SakinahContentMaxWidth).fillMaxSize().testTag("settings_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Small),
        ) {
            item { SakinahScreenHeader(stringResource(R.string.settings), subtitle = stringResource(R.string.settings_subtitle)) }
            item { SettingNavigationRow(stringResource(R.string.reading), stringResource(R.string.reading_settings_summary), Icons.Outlined.AutoStories, "open_reading_settings") { onOpenSection(SettingsSection.Reading) } }
            item { SettingNavigationRow(stringResource(R.string.tasbih_settings), stringResource(R.string.tasbih_settings_summary), Icons.Outlined.TouchApp, "open_tasbih_settings") { onOpenSection(SettingsSection.Tasbih) } }
            item { SettingNavigationRow(stringResource(R.string.sound_settings), completionSoundName(state.completionSound), Icons.AutoMirrored.Outlined.VolumeUp, "open_sound_settings", onOpenSoundSettings) }
            item { SettingNavigationRow(stringResource(R.string.daily_reset_settings), dailyResetSummary(state.dailyReset), Icons.Outlined.Update, "open_daily_reset_settings") { onOpenSection(SettingsSection.DailyReset) } }
            item { SettingNavigationRow(stringResource(R.string.appearance), stringResource(R.string.appearance_settings_summary), Icons.Outlined.Palette, "open_appearance_settings") { onOpenSection(SettingsSection.Appearance) } }
            item { Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp)) }
            item { SettingNavigationRow(stringResource(R.string.achievements), stringResource(R.string.activity_entry_summary), Icons.Outlined.BarChart, "open_achievements", onOpenAchievements) }
            item { SettingNavigationRow(stringResource(R.string.about_app), stringResource(R.string.about_app_entry_description), Icons.Outlined.Info, "open_about", onOpenAbout) }
        }
    }
}

@Composable
fun SettingsDetailScreen(
    section: SettingsSection,
    state: SakinahUiState,
    onBack: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetArabicFontStyle: (ArabicFontStyle) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetShowDiacritics: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetTasbihTextScale: (Float) -> Unit,
    onSetShowReference: (Boolean) -> Unit,
    onSetAutoAdvance: (Boolean) -> Unit,
    onSetTasbihDailyReset: (Boolean) -> Unit,
    onSetAdhkarDailyReset: (Boolean) -> Unit,
    onSetDailyResetTime: (Int) -> Unit,
) {
    val title = stringResource(when (section) {
        SettingsSection.Reading -> R.string.reading
        SettingsSection.Tasbih -> R.string.tasbih_settings
        SettingsSection.Appearance -> R.string.appearance
        SettingsSection.DailyReset -> R.string.daily_reset_settings
    })
    SettingsPage(title, onBack, "settings_detail_${section.name.lowercase()}") {
        when (section) {
            SettingsSection.DailyReset -> dailyResetSettingsItems(state.dailyReset, onSetTasbihDailyReset, onSetAdhkarDailyReset, onSetDailyResetTime)
            SettingsSection.Reading -> {
                item { TextScaleSetting(stringResource(R.string.text_size), state.textScale, dhikrFontFor(state.arabicFontStyle), 0.5f..1.4f, 8, "reader_text_scale", onSetTextScale) }
                item { SettingToggleRow(stringResource(R.string.show_diacritics), stringResource(R.string.show_diacritics_description), state.showDiacritics, onSetShowDiacritics, "setting_diacritics") }
                item { SettingToggleRow(stringResource(R.string.show_reference_by_default), stringResource(R.string.show_reference_by_default_description), state.showReferenceByDefault, onSetShowReference, "setting_reference") }
                item { SettingToggleRow(stringResource(R.string.auto_advance_dhikr), stringResource(R.string.auto_advance_dhikr_description), state.autoAdvanceDhikrEnabled, onSetAutoAdvance, "setting_auto_advance") }
            }
            SettingsSection.Tasbih -> {
                item { TextScaleSetting(stringResource(R.string.tasbih_text_size), state.tasbihTextScale, dhikrFontFor(state.arabicFontStyle), 0.7f..1.1f, 3, "tasbih_text_scale", onSetTasbihTextScale) }
                item { SettingToggleRow(stringResource(R.string.haptics), stringResource(R.string.haptics_description), state.hapticsEnabled, onSetHaptics, "setting_haptics") }
                item { SettingNavigationRow(stringResource(R.string.sound_settings), completionSoundName(state.completionSound), Icons.AutoMirrored.Outlined.VolumeUp, "tasbih_open_sound_settings", onOpenSoundSettings) }
            }
            SettingsSection.Appearance -> {
                item { Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(Modifier.selectableGroup()) {
                        ThemeMode.entries.forEach { mode ->
                            SettingRadioRow(stringResource(when (mode) {
                                ThemeMode.System -> R.string.theme_system
                                ThemeMode.Light -> R.string.theme_light
                                ThemeMode.Dark -> R.string.theme_dark
                            }), state.themeMode == mode, "theme_${mode.name.lowercase()}", { onSetThemeMode(mode) })
                        }
                    }
                }
                item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.arabic_font), style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(Modifier.selectableGroup()) {
                        ArabicFontStyle.entries.forEach { font ->
                            SettingRadioRow(stringResource(when (font) {
                                ArabicFontStyle.Sakinah -> R.string.font_sakinah
                                ArabicFontStyle.Amiri -> R.string.font_amiri
                                ArabicFontStyle.Ruqaa -> R.string.font_ruqaa
                                ArabicFontStyle.Baqiyat -> R.string.font_baqiyat
                            }), state.arabicFontStyle == font, "font_${font.name.lowercase()}", { onSetArabicFontStyle(font) }, dhikrFontFor(font))
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item { HorizontalDivider(); SettingToggleRow(stringResource(R.string.dynamic_colors), stringResource(R.string.dynamic_colors_description), state.dynamicColorEnabled, onSetDynamicColor, "setting_dynamic_color") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPage(title: String, onBack: () -> Unit, tag: String, content: LazyListScope.() -> Unit) {
    val compactInput = isCompactSettingsInput()
    Scaffold(
        modifier = Modifier.testTag(tag),
        topBar = { if (!compactInput) TopAppBar(windowInsets = WindowInsets(0, 0, 0, 0), title = { Text(title) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
        }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxSize().testTag("settings_detail_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (compactInput) 4.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Large), content = content,
            )
        }
    }
}

@Composable
internal fun SettingNavigationRow(title: String, description: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag(tag).clickable(role = Role.Button, onClick = onClick).heightIn(min = 72.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SettingToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, tag: String) {
    Row(
        Modifier.fillMaxWidth().testTag(tag).toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingRadioRow(title: String, selected: Boolean, tag: String, onClick: () -> Unit, font: FontFamily? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag).selectable(selected, role = Role.RadioButton, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (font != null) Text(stringResource(R.string.text_size_sample), fontFamily = font, fontSize = 22.sp, lineHeight = 36.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TextScaleSetting(title: String, scale: Float, font: FontFamily, valueRange: ClosedFloatingPointRange<Float>, steps: Int, tag: String, onChange: (Float) -> Unit) {
    var draft by remember(scale) { mutableFloatStateOf(scale) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(draft * 100).roundToInt()}٪", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.text_size_sample), style = TextStyle(fontFamily = font, fontSize = (23 * draft).sp, lineHeight = (39 * draft).sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
        Slider(value = draft, onValueChange = { draft = it }, onValueChangeFinished = { onChange(draft) }, valueRange = valueRange, steps = steps,
            modifier = Modifier.testTag(tag).semantics { contentDescription = title })
    }
}
