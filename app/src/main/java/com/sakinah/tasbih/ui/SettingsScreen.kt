package com.sakinah.tasbih.ui

import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sakinah.tasbih.data.ReaderTextScaleRange
import com.sakinah.tasbih.data.TasbihTextScaleRange
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.theme.dhikrFontFor
import kotlin.math.roundToInt

enum class SettingsSection { Reading, Tasbih, Appearance, DailyReset }

/**
 * The look of the whole app: light or dark, the Arabic font, and Android's own colours. In the
 * general settings, which the gear on the day's page opens, since the user moved each tab's
 * settings into that tab on November 23, 2026 and the «more» tab went.
 */
internal fun LazyListScope.appearanceItems(
    state: SakinahUiState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetArabicFontStyle: (ArabicFontStyle) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = if (compactInput) 4.dp else 0.dp, bottom = if (compactInput) 4.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Large),
            ) {
                if (!compactInput) item(key = "page_ornament") { KhatamDivider() }
                content()
            }
        }
    }
}

/** One tab of a [TabbedSettingsPage]: its name, its icon, and a tag for the device tests. */
internal data class SettingsTab(val label: String, val icon: ImageVector, val tag: String)

/**
 * A settings page whose options are gathered in tabs, for pages long enough to scatter the eye.
 * An optional [header], a preview say, stays on top while the chosen tab's options scroll under
 * it; on a short screen it scrolls with them instead, so the options keep room to be used.
 * The tabs' names take a size below a tab's own, so they fit with large text too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TabbedSettingsPage(
    title: String,
    onBack: () -> Unit,
    tag: String,
    tabs: List<SettingsTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    /** A page that adds things, as the wird's, keeps its button here, and its list room under it. */
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val roomy = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= 600
    Scaffold(
        modifier = Modifier.testTag(tag),
        floatingActionButton = { floatingActionButton?.invoke() },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) } },
                actions = actions,
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val top: @Composable () -> Unit = {
                Box(Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) { header?.invoke() }
            }
            if (header != null && roomy) top()
            PrimaryTabRow(selectedTabIndex = selected, modifier = Modifier.widthIn(max = SakinahContentMaxWidth)) {
                tabs.forEachIndexed { index, tab ->
                    // Own content rather than a tab's text and icon, which keep 16dp either side
                    // of the name and so cut it short when five tabs share a phone's width.
                    Tab(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        modifier = Modifier.testTag(tab.tag),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                        ) {
                            Icon(tab.icon, null)
                            Text(tab.label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxWidth().weight(1f).testTag("${tag}_options"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (floatingActionButton != null) 96.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Large),
            ) {
                if (header != null && !roomy) item(key = "header") { top() }
                content()
            }
        }
    }
}

@Composable
internal fun SettingNavigationRow(title: String, description: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag(tag).clickable(role = Role.Button, onClick = onClick).heightIn(min = 72.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KhatamMedallion(icon)
        Spacer(Modifier.width(14.dp))
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

/** The three tabs the adhkar's settings are gathered under. */
private enum class AdhkarTab { READING, SOUND, RESET }

/**
 * The adhkar's own settings, opened by the gear on the adhkar's page: reading, the sound at a
 * dhikr's end, and the daily reset of the day's adhkar. The record of what was done opens from
 * the top bar, as it is the adhkar's and the tasbih's alike.
 */
@Composable
fun AdhkarSettingsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onSetShowDiacritics: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetShowReference: (Boolean) -> Unit,
    onSetAutoAdvance: (Boolean) -> Unit,
    onSetReaderSoundEnabled: (Boolean) -> Unit,
    onSetAdhkarDailyReset: (Boolean) -> Unit,
    onSetDailyResetTime: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(AdhkarTab.READING) }
    TabbedSettingsPage(
        title = stringResource(R.string.adhkar_settings),
        onBack = onBack,
        tag = "adhkar_settings",
        tabs = listOf(
            SettingsTab(stringResource(R.string.reading), Icons.Outlined.AutoStories, "adhkar_tab_reading"),
            SettingsTab(stringResource(R.string.settings_tab_sound), Icons.AutoMirrored.Outlined.VolumeUp, "adhkar_tab_sound"),
            SettingsTab(stringResource(R.string.settings_tab_reset), Icons.Outlined.Update, "adhkar_tab_reset"),
        ),
        selected = tab.ordinal,
        onSelect = { tab = AdhkarTab.entries[it] },
        actions = { AchievementsAction(onOpenAchievements) },
    ) {
        when (tab) {
            AdhkarTab.READING -> {
                item { TextScaleSetting(stringResource(R.string.text_size), state.textScale, dhikrFontFor(state.arabicFontStyle), ReaderTextScaleRange, 8, "reader_text_scale", onSetTextScale) }
                item { SettingToggleRow(stringResource(R.string.show_diacritics), stringResource(R.string.show_diacritics_description), state.showDiacritics, onSetShowDiacritics, "setting_diacritics") }
                item { SettingToggleRow(stringResource(R.string.show_reference_by_default), stringResource(R.string.show_reference_by_default_description), state.showReferenceByDefault, onSetShowReference, "setting_reference") }
                item { SettingToggleRow(stringResource(R.string.auto_advance_dhikr), stringResource(R.string.auto_advance_dhikr_description), state.autoAdvanceDhikrEnabled, onSetAutoAdvance, "setting_auto_advance") }
            }
            AdhkarTab.SOUND -> {
                item {
                    SettingToggleRow(
                        stringResource(R.string.reader_completion_sound),
                        stringResource(R.string.reader_completion_sound_description),
                        state.dhikrCompletionSoundEnabled,
                        onSetReaderSoundEnabled,
                        "reader_sound_enabled",
                    )
                }
                item { CompletionToneRow(state, onOpenSoundSettings) }
            }
            AdhkarTab.RESET -> dailyResetSettingsItems(state.dailyReset, {}, onSetAdhkarDailyReset, onSetDailyResetTime, showTasbih = false)
        }
    }
}

/** The three tabs the tasbih's settings are gathered under. */
private enum class TasbihTab { TASBIH, SOUND, RESET }

/** The tasbih's own settings, opened by the gear on the tasbih's page, in the adhkar's form. */
@Composable
fun TasbihSettingsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onSetTasbihTextScale: (Float) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetTasbihSoundEnabled: (Boolean) -> Unit,
    onSetTasbihDailyReset: (Boolean) -> Unit,
    onSetDailyResetTime: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(TasbihTab.TASBIH) }
    TabbedSettingsPage(
        title = stringResource(R.string.tasbih_settings_title),
        onBack = onBack,
        tag = "tasbih_settings",
        tabs = listOf(
            SettingsTab(stringResource(R.string.tasbih_settings), Icons.Outlined.TouchApp, "tasbih_tab_tasbih"),
            SettingsTab(stringResource(R.string.settings_tab_sound), Icons.AutoMirrored.Outlined.VolumeUp, "tasbih_tab_sound"),
            SettingsTab(stringResource(R.string.settings_tab_reset), Icons.Outlined.Update, "tasbih_tab_reset"),
        ),
        selected = tab.ordinal,
        onSelect = { tab = TasbihTab.entries[it] },
        actions = { AchievementsAction(onOpenAchievements) },
    ) {
        when (tab) {
            TasbihTab.TASBIH -> {
                item { TextScaleSetting(stringResource(R.string.tasbih_text_size), state.tasbihTextScale, dhikrFontFor(state.arabicFontStyle), TasbihTextScaleRange, 7, "tasbih_text_scale", onSetTasbihTextScale) }
                item { SettingToggleRow(stringResource(R.string.haptics), stringResource(R.string.haptics_description), state.hapticsEnabled, onSetHaptics, "setting_haptics") }
            }
            TasbihTab.SOUND -> {
                item {
                    SettingToggleRow(
                        stringResource(R.string.tasbih_completion_sound),
                        stringResource(R.string.tasbih_completion_sound_description),
                        state.tasbihCompletionSoundEnabled,
                        onSetTasbihSoundEnabled,
                        "tasbih_sound_enabled",
                    )
                }
                item { CompletionToneRow(state, onOpenSoundSettings) }
            }
            TasbihTab.RESET -> dailyResetSettingsItems(state.dailyReset, onSetTasbihDailyReset, {}, onSetDailyResetTime, showAdhkar = false)
        }
    }
}

/** The tone and its loudness are one for the adhkar and the tasbih, so both open the same page. */
@Composable
private fun CompletionToneRow(state: SakinahUiState, onOpenSoundSettings: () -> Unit) {
    SettingNavigationRow(
        stringResource(R.string.completion_sound_tone),
        completionSoundName(state.completionSound),
        Icons.AutoMirrored.Outlined.VolumeUp,
        "open_sound_settings",
        onOpenSoundSettings,
    )
}

@Composable
private fun AchievementsAction(onOpenAchievements: () -> Unit) {
    IconButton(onClick = onOpenAchievements, modifier = Modifier.testTag("open_achievements")) {
        Icon(Icons.Outlined.BarChart, stringResource(R.string.achievements))
    }
}

/**
 * What belongs to the whole app and to no one tab: its look, and about the app. Opened by the
 * gear on the day's page.
 */
@Composable
fun GeneralSettingsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetArabicFontStyle: (ArabicFontStyle) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    SettingsPage(stringResource(R.string.general_settings), onBack, "general_settings") {
        item { SakinahSectionHeader(stringResource(R.string.appearance)) }
        appearanceItems(state, onSetThemeMode, onSetArabicFontStyle, onSetDynamicColor)
        item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); DigitsSetting() }
        item { HorizontalDivider() }
        item { SettingNavigationRow(stringResource(R.string.about_app), stringResource(R.string.about_app_entry_description), Icons.Outlined.Info, "open_about", onOpenAbout) }
    }
}

/**
 * The digits numbers are written in, for the app and its widgets: «١٢٣» or «123». Every screen
 * is made again at once, and the widgets drawn again, so nothing keeps the old digits.
 */
@Composable
internal fun DigitsSetting() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = androidx.activity.compose.LocalActivity.current
    var western by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.sakinah.tasbih.data.DigitStyle.western) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.digits_title), style = MaterialTheme.typography.titleMedium)
        com.sakinah.tasbih.ui.calendar.CalendarChoices(
            options = listOf(false, true),
            selected = western,
            label = { stringResource(if (it) R.string.digits_western else R.string.digits_hindi) },
            onSelect = { choice ->
                if (choice != western) {
                    western = choice
                    val app = context.applicationContext
                    com.sakinah.tasbih.data.DigitStyle.save(app, choice)
                    com.sakinah.tasbih.widget.widgetScope.launch {
                        runCatching { com.sakinah.tasbih.widget.PrayerWidgetUpdater(app).updateAll() }
                        runCatching { com.sakinah.tasbih.widget.CalendarWidgetUpdater(app).updateAll() }
                        runCatching { com.sakinah.tasbih.wird.WirdSync.refreshViews(app) }
                    }
                    activity?.recreate()
                }
            },
            modifier = Modifier.testTag("setting_digits"),
        )
        com.sakinah.tasbih.ui.calendar.CalendarHint(stringResource(R.string.digits_hint))
    }
}

/** The gear that opens a tab's own settings, in the same place on every page that has one. */
@Composable
internal fun SettingsGear(onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.testTag(tag)) {
        Icon(Icons.Outlined.Settings, stringResource(R.string.open_settings))
    }
}
