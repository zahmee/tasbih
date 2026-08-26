package com.sakinah.tasbih.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.theme.BaqiyatFontFamily
import com.sakinah.tasbih.ui.theme.DhikrFontFamily
import com.sakinah.tasbih.ui.theme.IslamicDisplayFontFamily
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import com.sakinah.tasbih.ui.theme.SakinahUiFontFamily
import com.sakinah.tasbih.ui.theme.dhikrFontFor

@Composable
fun SettingsScreen(
    state: SakinahUiState,
    onOpenAchievements: () -> Unit,
    onOpenAbout: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetArabicFontStyle: (ArabicFontStyle) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetShowDiacritics: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetTasbihTextScale: (Float) -> Unit,
) {
    SakinahScreenBackground {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SakinahContentMaxWidth)
                .fillMaxSize()
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                SakinahScreenHeader(
                    title = stringResource(R.string.settings),
                    subtitle = stringResource(R.string.settings_subtitle),
                )
            }

            item { AchievementsEntryCard(state = state, onClick = onOpenAchievements) }

            item { SakinahSectionHeader(stringResource(R.string.appearance)) }
            item { ThemeModeCard(selected = state.themeMode, onSelect = onSetThemeMode) }
            item { ArabicFontCard(selected = state.arabicFontStyle, onSelect = onSetArabicFontStyle) }
            item {
                SettingsSwitchCard(
                    title = stringResource(R.string.dynamic_colors),
                    description = stringResource(R.string.dynamic_colors_description),
                    checked = state.dynamicColorEnabled,
                    onCheckedChange = onSetDynamicColor,
                )
            }

            item { SakinahSectionHeader(stringResource(R.string.reading)) }
            item {
                SettingsSwitchCard(
                    title = stringResource(R.string.show_diacritics),
                    description = stringResource(R.string.show_diacritics_description),
                    checked = state.showDiacritics,
                    onCheckedChange = onSetShowDiacritics,
                )
            }
            item {
                TextScaleCard(
                    title = stringResource(R.string.text_size),
                    sample = stringResource(R.string.text_size_sample),
                    scale = state.textScale,
                    fontFamily = dhikrFontFor(state.arabicFontStyle),
                    baseFontSize = 23f,
                    baseLineHeight = 39f,
                    valueRange = 0.5f..1.4f,
                    steps = 8,
                    sliderTag = "reader_text_scale",
                    onScaleChange = onSetTextScale,
                )
            }
            item {
                SettingsSwitchCard(
                    title = stringResource(R.string.haptics),
                    description = stringResource(R.string.haptics_description),
                    checked = state.hapticsEnabled,
                    onCheckedChange = onSetHaptics,
                )
            }

            item { SakinahSectionHeader(stringResource(R.string.tasbih_settings)) }
            item {
                TextScaleCard(
                    title = stringResource(R.string.tasbih_text_size),
                    description = stringResource(R.string.tasbih_text_size_description),
                    sample = stringResource(R.string.tasbih_text_size_sample),
                    scale = state.tasbihTextScale,
                    fontFamily = dhikrFontFor(state.arabicFontStyle),
                    baseFontSize = 21f,
                    baseLineHeight = 30f,
                    valueRange = 0.7f..1.1f,
                    steps = 3,
                    sliderTag = "tasbih_text_scale",
                    onScaleChange = onSetTasbihTextScale,
                )
            }

            item { SakinahSectionHeader(stringResource(R.string.content_and_privacy)) }
            item {
                InfoCard(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.offline_first),
                    description = stringResource(R.string.offline_first_description),
                )
            }
            item {
                InfoCard(
                    icon = Icons.Outlined.AutoStories,
                    title = stringResource(R.string.hisn_source_title),
                    description = stringResource(R.string.hisn_source_description),
                )
            }
            item {
                InfoCard(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.language_status),
                    description = stringResource(R.string.language_status_description),
                )
            }

            item { SakinahSectionHeader(stringResource(R.string.about_section)) }
            item { AboutEntryCard(onClick = onOpenAbout) }
        }
    }
}

@Composable
private fun AchievementsEntryCard(state: SakinahUiState, onClick: () -> Unit) {
    val totals = state.activityAnalytics.totals
    val brand = LocalSakinahBrandColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("open_achievements")
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = brand.heroStart,
        contentColor = brand.onHero,
        shadowElevation = 3.dp,
        border = sakinahCardBorder(0.24f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = brand.onHero.copy(alpha = 0.14f),
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.BarChart, contentDescription = null)
                    }
                }
                Spacer(Modifier.padding(horizontal = 7.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.achievements), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.achievements_teaser),
                        style = MaterialTheme.typography.bodyMedium,
                        color = brand.onHero.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.achievements_teaser_count,
                    totals.totalCount,
                    totals.activeDays,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.view_achievements),
                style = MaterialTheme.typography.labelMedium,
                color = brand.onHero.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun AboutEntryCard(onClick: () -> Unit) {
    val brand = LocalSakinahBrandColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("open_about")
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = sakinahCardBorder(0.22f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color = brand.antiqueGold.copy(alpha = 0.14f),
                contentColor = brand.antiqueGold,
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                }
            }
            Spacer(Modifier.padding(horizontal = 7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.about_app), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.about_app_entry_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = brand.antiqueGold,
            )
        }
    }
}

@Composable
private fun ThemeModeCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SettingsContainer {
        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(3.dp))
        Text(
            stringResource(R.string.theme_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.theme_system),
                icon = Icons.Outlined.SettingsBrightness,
                selected = selected == ThemeMode.System,
                tag = "theme_system",
                onClick = { onSelect(ThemeMode.System) },
            )
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.theme_light),
                icon = Icons.Outlined.LightMode,
                selected = selected == ThemeMode.Light,
                tag = "theme_light",
                onClick = { onSelect(ThemeMode.Light) },
            )
            ThemeChoice(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.theme_dark),
                icon = Icons.Outlined.DarkMode,
                selected = selected == ThemeMode.Dark,
                tag = "theme_dark",
                onClick = { onSelect(ThemeMode.Dark) },
            )
        }
    }
}

@Composable
private fun ThemeChoice(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .testTag(tag)
            .clip(MaterialTheme.shapes.medium)
            .heightIn(min = 76.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ArabicFontCard(selected: ArabicFontStyle, onSelect: (ArabicFontStyle) -> Unit) {
    SettingsContainer {
        Text(stringResource(R.string.arabic_font), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(3.dp))
        Text(
            stringResource(R.string.arabic_font_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontChoice(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_sakinah),
                    fontFamily = SakinahUiFontFamily,
                    selected = selected == ArabicFontStyle.Sakinah,
                    tag = "font_sakinah",
                    onClick = { onSelect(ArabicFontStyle.Sakinah) },
                )
                FontChoice(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_amiri),
                    fontFamily = DhikrFontFamily,
                    selected = selected == ArabicFontStyle.Amiri,
                    tag = "font_amiri",
                    onClick = { onSelect(ArabicFontStyle.Amiri) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontChoice(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_ruqaa),
                    fontFamily = IslamicDisplayFontFamily,
                    selected = selected == ArabicFontStyle.Ruqaa,
                    tag = "font_ruqaa",
                    onClick = { onSelect(ArabicFontStyle.Ruqaa) },
                )
                FontChoice(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.font_baqiyat),
                    fontFamily = BaqiyatFontFamily,
                    selected = selected == ArabicFontStyle.Baqiyat,
                    tag = "font_baqiyat",
                    onClick = { onSelect(ArabicFontStyle.Baqiyat) },
                )
            }
        }
    }
}

@Composable
private fun FontChoice(
    modifier: Modifier,
    title: String,
    fontFamily: FontFamily,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .testTag(tag)
            .clip(MaterialTheme.shapes.medium)
            .heightIn(min = 82.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ذِكر", fontFamily = fontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(title, fontFamily = fontFamily, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.13f),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.13f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(horizontal = 8.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun TextScaleCard(
    title: String,
    sample: String,
    scale: Float,
    fontFamily: FontFamily,
    baseFontSize: Float,
    baseLineHeight: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    sliderTag: String,
    onScaleChange: (Float) -> Unit,
    description: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.13f),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = sample,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = (baseFontSize * scale).sp,
                        lineHeight = (baseLineHeight * scale).sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.testTag(sliderTag),
            )
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(horizontal = 7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(5.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
