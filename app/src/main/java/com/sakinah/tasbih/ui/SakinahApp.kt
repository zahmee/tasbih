package com.sakinah.tasbih.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.launch
import com.sakinah.tasbih.data.DhikrEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.ui.theme.SakinahTheme
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors

private sealed interface AppDestination {
    data object Home : AppDestination
    data object Library : AppDestination
    data object Tasbih : AppDestination
    data object TasbihPhrases : AppDestination
    data object Settings : AppDestination
    data object SoundSettings : AppDestination
    data class SettingsDetail(val section: SettingsSection) : AppDestination
    data object Achievements : AppDestination
    data object About : AppDestination
    data class Reader(val collectionId: String, val reviewCompleted: Boolean = false) : AppDestination
}

private fun AppDestination.route(): String = when (this) {
    AppDestination.Home -> "home"
    AppDestination.Library -> "library"
    AppDestination.Tasbih -> "tasbih"
    AppDestination.TasbihPhrases -> "phrases"
    AppDestination.Settings -> "settings"
    AppDestination.SoundSettings -> "sound"
    AppDestination.Achievements -> "achievements"
    AppDestination.About -> "about"
    is AppDestination.SettingsDetail -> "settings:${section.name}"
    is AppDestination.Reader -> "reader:$collectionId:$reviewCompleted"
}

private fun destinationFromRoute(route: String): AppDestination? = when (route) {
    "home" -> AppDestination.Home
    "library" -> AppDestination.Library
    "tasbih" -> AppDestination.Tasbih
    "phrases" -> AppDestination.TasbihPhrases
    "settings" -> AppDestination.Settings
    "sound" -> AppDestination.SoundSettings
    "achievements" -> AppDestination.Achievements
    "about" -> AppDestination.About
    else -> runCatching {
        val parts = route.split(':')
        when (parts.first()) {
            "reader" -> AppDestination.Reader(parts[1], parts.getOrNull(2).toBoolean())
            "settings" -> AppDestination.SettingsDetail(SettingsSection.valueOf(parts[1]))
            else -> null
        }
    }.getOrNull()
}

@Composable
fun SakinahApp(viewModel: SakinahViewModel = viewModel()) {
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDailyState()
        onPauseOrDispose { }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SideEffect {
        val style = if (darkTheme) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        (activity as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = style,
            navigationBarStyle = style,
        )
    }

    // Arabic is the only released locale for now. Keeping direction at the app root makes
    // the first launch correct even when the device language is not Arabic. When more
    // locales are shipped, this provider can follow the selected content language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SakinahTheme(
            dynamicColor = state.dynamicColorEnabled,
            themeMode = state.themeMode,
            arabicFontStyle = state.arabicFontStyle,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SakinahNavigation(
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun SakinahNavigation(
    state: SakinahUiState,
    viewModel: SakinahViewModel,
) {
    val backStack = rememberSaveable(saver = listSaver<SnapshotStateList<AppDestination>, String>(
        save = { stack -> stack.map(AppDestination::route) },
        restore = { routes -> routes.mapNotNull(::destinationFromRoute).ifEmpty { listOf(AppDestination.Home) }.toMutableStateList() },
    )) { mutableStateListOf<AppDestination>(AppDestination.Home) }
    val navigationScope = rememberCoroutineScope()
    var isTasbihFocusMode by rememberSaveable { mutableStateOf(false) }
    var isReaderFocusMode by rememberSaveable { mutableStateOf(false) }
    val latestState = rememberUpdatedState(state)
    val activity = LocalActivity.current
    val topLevelDestination = when (val destination = backStack.lastOrNull()) {
        is AppDestination.Reader -> backStack.lastOrNull { it.topLevelDestination() != null }
            .topLevelDestination() ?: AppDestination.Home
        else -> destination.topLevelDestination()
    }

    fun navigateTopLevel(destination: AppDestination) {
        if (destination != AppDestination.Tasbih) isTasbihFocusMode = false
        isReaderFocusMode = false
        backStack.clear()
        backStack.add(destination)
    }

    fun openReader(collectionId: String) {
        if (latestState.value.catalog.collection(collectionId) != null) {
            isReaderFocusMode = false
            backStack.add(AppDestination.Reader(collectionId, latestState.value.progressFor(collectionId).completed))
        }
    }

    fun openEntry(entry: DhikrEntry) {
        navigationScope.launch {
            viewModel.openReaderEntry(entry).join()
            isReaderFocusMode = false
            backStack.add(AppDestination.Reader(entry.collectionId, latestState.value.progressFor(entry.collectionId).completed))
        }
    }

    fun navigateBack() {
        if (isReaderFocusMode && backStack.lastOrNull() is AppDestination.Reader) {
            isReaderFocusMode = false
        } else if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            activity?.finish()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val compactNavigationRail = maxHeight < 480.dp
        val hideTopLevelNavigation = (isTasbihFocusMode &&
            topLevelDestination == AppDestination.Tasbih) ||
            (isReaderFocusMode && backStack.lastOrNull() is AppDestination.Reader)
        Scaffold(
            bottomBar = {
                if (topLevelDestination != null && !useNavigationRail && !hideTopLevelNavigation) {
                    AppNavigationBar(
                        selected = topLevelDestination,
                        onNavigate = ::navigateTopLevel,
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                if (topLevelDestination != null && useNavigationRail && !hideTopLevelNavigation) {
                    AppNavigationRail(
                        selected = topLevelDestination,
                        onNavigate = ::navigateTopLevel,
                        compact = compactNavigationRail,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
                onBack = ::navigateBack,
                entryProvider = { destination ->
                    when (destination) {
                        AppDestination.Home -> NavEntry(destination) {
                            HomeScreen(
                                state = latestState.value,
                                onOpenCollection = ::openReader,
                                onRetry = viewModel::retryContentLoad,
                            )
                        }

                        AppDestination.Library -> NavEntry(destination) {
                            LibraryScreen(
                                state = latestState.value,
                                onOpenCollection = ::openReader,
                                onOpenEntry = ::openEntry,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onRetry = viewModel::retryContentLoad,
                            )
                        }

                        AppDestination.Tasbih -> NavEntry(destination) {
                            TasbihScreen(
                                state = latestState.value,
                                isFocusMode = isTasbihFocusMode,
                                onFocusModeChange = { isTasbihFocusMode = it },
                                onIncrement = viewModel::incrementTasbih,
                                onDecrement = viewModel::decrementTasbih,
                                onReset = viewModel::resetTasbih,
                                onOpenPhraseManager = {
                                    backStack.add(AppDestination.TasbihPhrases)
                                },
                                onSelectPhrase = viewModel::selectPhrase,
                                onAddCustomPhrase = viewModel::addCustomPhrase,
                                onUpdatePhrase = viewModel::updateTasbihPhrase,
                            )
                        }

                        AppDestination.TasbihPhrases -> NavEntry(destination) {
                            TasbihPhraseManagerScreen(
                                state = latestState.value,
                                onBack = ::navigateBack,
                                onSelectPhrase = viewModel::selectPhrase,
                                onAddCustomPhrase = viewModel::addCustomPhrase,
                                onUpdatePhrase = viewModel::updateTasbihPhrase,
                                onMovePhrase = viewModel::moveTasbihPhrase,
                                onDeletePhrase = viewModel::deleteTasbihPhrase,
                            )
                        }

                        AppDestination.Settings -> NavEntry(destination) {
                            SettingsScreen(
                                state = latestState.value,
                                onOpenSection = { backStack.add(AppDestination.SettingsDetail(it)) },
                                onOpenAchievements = { backStack.add(AppDestination.Achievements) },
                                onOpenAbout = { backStack.add(AppDestination.About) },
                                onOpenSoundSettings = { backStack.add(AppDestination.SoundSettings) },
                            )
                        }

                        is AppDestination.SettingsDetail -> NavEntry(destination) {
                            SettingsDetailScreen(
                                section = destination.section,
                                state = latestState.value,
                                onBack = ::navigateBack,
                                onOpenSoundSettings = { backStack.add(AppDestination.SoundSettings) },
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetArabicFontStyle = viewModel::setArabicFontStyle,
                                onSetDynamicColor = viewModel::setDynamicColor,
                                onSetHaptics = viewModel::setHaptics,
                                onSetShowDiacritics = viewModel::setShowDiacritics,
                                onSetTextScale = viewModel::setTextScale,
                                onSetTasbihTextScale = viewModel::setTasbihTextScale,
                                onSetShowReference = viewModel::setShowReferenceByDefault,
                                onSetAutoAdvance = viewModel::setAutoAdvanceDhikrEnabled,
                                onSetTasbihDailyReset = viewModel::setTasbihDailyReset,
                                onSetAdhkarDailyReset = viewModel::setAdhkarDailyReset,
                                onSetDailyResetTime = viewModel::setDailyResetTime,
                            )
                        }

                        AppDestination.SoundSettings -> NavEntry(destination) {
                            SoundSettingsScreen(
                                state = latestState.value,
                                onBack = ::navigateBack,
                                onSetTasbihSoundEnabled = viewModel::setTasbihCompletionSoundEnabled,
                                onSetReaderSoundEnabled = viewModel::setDhikrCompletionSoundEnabled,
                                onSetSound = viewModel::setCompletionSound,
                                onSetVolume = viewModel::setCompletionSoundVolume,
                            )
                        }

                        AppDestination.Achievements -> NavEntry(destination) {
                            AchievementsScreen(
                                state = latestState.value,
                                onBack = ::navigateBack,
                            )
                        }

                        AppDestination.About -> NavEntry(destination) {
                            AboutScreen(onBack = ::navigateBack)
                        }

                        is AppDestination.Reader -> NavEntry(destination) {
                            ReaderScreen(
                                state = latestState.value,
                                collectionId = destination.collectionId,
                                reviewCompleted = destination.reviewCompleted,
                                onUndo = { viewModel.undoDhikr(destination.collectionId) },
                                onEntryVisible = viewModel::recordVisitedEntry,
                                isFocusMode = isReaderFocusMode,
                                onFocusModeChange = { isReaderFocusMode = it },
                                onBack = ::navigateBack,
                                onIncrement = { viewModel.incrementDhikr(destination.collectionId) },
                                onAdvance = { viewModel.advanceDhikr(destination.collectionId) },
                                onNavigateDhikr = { direction ->
                                    viewModel.navigateDhikr(destination.collectionId, direction)
                                },
                                onRestart = { viewModel.restartCollection(destination.collectionId) },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onAddToTasbih = viewModel::addEntryToTasbih,
                                onSetShowReferenceByDefault = viewModel::setShowReferenceByDefault,
                                onSetTextScale = viewModel::setTextScale,
                                onSetDhikrCompletionSoundEnabled = viewModel::setDhikrCompletionSoundEnabled,
                                onSetAutoAdvanceDhikrEnabled = viewModel::setAutoAdvanceDhikrEnabled,
                            )
                        }

                        else -> error("Unknown destination: $destination")
                    }
                },
                    )
                }
            }
        }
    }
}

private fun Any?.topLevelDestination(): AppDestination? = when (this) {
    AppDestination.Home -> AppDestination.Home
    AppDestination.Library -> AppDestination.Library
    AppDestination.Tasbih -> AppDestination.Tasbih
    AppDestination.Settings -> AppDestination.Settings
    else -> null
}

@Composable
private fun AppNavigationBar(
    selected: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val navigationContainer = MaterialTheme.colorScheme.surfaceContainerLow

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_navigation_bar")
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = navigationContainer,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            modifier = Modifier.testTag("nav_home"),
            selected = selected == AppDestination.Home,
            onClick = { onNavigate(AppDestination.Home) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Mihrab,
                    selected = selected == AppDestination.Home,
                )
            },
            label = { IslamicNavigationLabel(R.string.home) },
            colors = itemColors,
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav_library"),
            selected = selected == AppDestination.Library,
            onClick = { onNavigate(AppDestination.Library) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Manuscript,
                    selected = selected == AppDestination.Library,
                )
            },
            label = { IslamicNavigationLabel(R.string.library) },
            colors = itemColors,
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav_tasbih"),
            selected = selected == AppDestination.Tasbih,
            onClick = { onNavigate(AppDestination.Tasbih) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Tasbih,
                    selected = selected == AppDestination.Tasbih,
                )
            },
            label = { IslamicNavigationLabel(if (LocalDensity.current.fontScale > 1.5f) R.string.tasbih_short_label else R.string.tasbih) },
            colors = itemColors,
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav_settings"),
            selected = selected == AppDestination.Settings,
            onClick = { onNavigate(AppDestination.Settings) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.More,
                    selected = selected == AppDestination.Settings,
                )
            },
            label = { IslamicNavigationLabel(R.string.more) },
            colors = itemColors,
        )
    }
}

@Composable
private fun AppNavigationRail(
    selected: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    compact: Boolean,
) {
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val labels = listOf(stringResource(R.string.home), stringResource(R.string.library), stringResource(R.string.tasbih), stringResource(R.string.more))
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = if (compact) null else { {
            AnaaAppIcon(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .size(44.dp),
            )
        } },
    ) {
        NavigationRailItem(
            modifier = Modifier.testTag("nav_home").semantics { contentDescription = labels[0] },
            selected = selected == AppDestination.Home,
            onClick = { onNavigate(AppDestination.Home) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Mihrab,
                    selected = selected == AppDestination.Home,
                )
            },
            label = if (compact) null else { { IslamicNavigationLabel(R.string.home) } },
            colors = itemColors,
        )
        NavigationRailItem(
            modifier = Modifier.testTag("nav_library").semantics { contentDescription = labels[1] },
            selected = selected == AppDestination.Library,
            onClick = { onNavigate(AppDestination.Library) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Manuscript,
                    selected = selected == AppDestination.Library,
                )
            },
            label = if (compact) null else { { IslamicNavigationLabel(R.string.library) } },
            colors = itemColors,
        )
        NavigationRailItem(
            modifier = Modifier.testTag("nav_tasbih").semantics { contentDescription = labels[2] },
            selected = selected == AppDestination.Tasbih,
            onClick = { onNavigate(AppDestination.Tasbih) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Tasbih,
                    selected = selected == AppDestination.Tasbih,
                )
            },
            label = if (compact) null else { { IslamicNavigationLabel(R.string.tasbih) } },
            colors = itemColors,
        )
        NavigationRailItem(
            modifier = Modifier.testTag("nav_settings").semantics { contentDescription = labels[3] },
            selected = selected == AppDestination.Settings,
            onClick = { onNavigate(AppDestination.Settings) },
            icon = {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.More,
                    selected = selected == AppDestination.Settings,
                )
            },
            label = if (compact) null else { { IslamicNavigationLabel(R.string.more) } },
            colors = itemColors,
        )
    }
}

@Composable
private fun IslamicNavigationLabel(labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
    )
}
