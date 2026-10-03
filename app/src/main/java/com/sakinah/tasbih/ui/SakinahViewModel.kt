package com.sakinah.tasbih.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakinah.tasbih.data.AppPreferencesRepository
import com.sakinah.tasbih.data.CompletionSound
import com.sakinah.tasbih.data.DefaultCompletionSoundVolume
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.data.nextDailyReset
import com.sakinah.tasbih.data.ActivityAnalytics
import com.sakinah.tasbih.data.ActivityRepository
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.DhikrCatalog
import com.sakinah.tasbih.data.DhikrEntry
import com.sakinah.tasbih.data.HisnCatalog
import com.sakinah.tasbih.data.HisnContentRepository
import com.sakinah.tasbih.data.ReaderUndo
import com.sakinah.tasbih.data.ReadingSessionRepository
import com.sakinah.tasbih.data.ReadingProgress
import com.sakinah.tasbih.data.TasbihPhrase
import com.sakinah.tasbih.data.TasbihPhraseAnalytics
import com.sakinah.tasbih.data.ThemeMode
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal fun tasbihCycleCount(
    count: Int,
    target: Int,
    showCompletedCycle: Boolean = false,
): Int = when {
    target <= 0 -> 0
    showCompletedCycle -> target
    else -> count.coerceAtLeast(0) % target
}

internal fun tasbihCycleProgress(
    count: Int,
    target: Int,
    showCompletedCycle: Boolean = false,
): Float = if (target <= 0) {
    0f
} else {
    tasbihCycleCount(count, target, showCompletedCycle).toFloat() / target
}

internal fun hasCrossedTasbihCycle(previousCount: Int, currentCount: Int, target: Int): Boolean {
    if (target <= 0 || currentCount <= previousCount) return false
    return currentCount / target > previousCount.coerceAtLeast(0) / target
}

internal fun latestTasbihMilestone(count: Int, target: Int): Int = if (target <= 0 || count <= 0) {
    0
} else {
    (count / target) * target
}

data class SakinahUiState(
    val isLoading: Boolean = true,
    val contentLoadFailed: Boolean = false,
    val catalog: HisnCatalog = HisnCatalog.Empty,
    val tasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val selectedPhrase: TasbihPhrase = DhikrCatalog.builtInTasbihPhrases.first(),
    val tasbihPhrases: List<TasbihPhrase> = DhikrCatalog.builtInTasbihPhrases,
    val dynamicColorEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val arabicFontStyle: ArabicFontStyle = ArabicFontStyle.Sakinah,
    val hapticsEnabled: Boolean = true,
    val showDiacritics: Boolean = true,
    val textScale: Float = 1f,
    val tasbihTextScale: Float = 1f,
    val showReferenceByDefault: Boolean = false,
    val dhikrCompletionSoundEnabled: Boolean = true,
    val tasbihCompletionSoundEnabled: Boolean = true,
    val completionSound: CompletionSound = CompletionSound.ClearBell,
    val completionSoundVolume: Float = DefaultCompletionSoundVolume,
    val autoAdvanceDhikrEnabled: Boolean = true,
    val favoriteEntryIds: Set<String> = emptySet(),
    val readingProgress: Map<String, ReadingProgress> = emptyMap(),
    val readerUndo: Map<String, ReaderUndo> = emptyMap(),
    val recentEntryIds: List<String> = emptyList(),
    val dailyReset: DailyResetSettings = DailyResetSettings(),
    val activityAnalytics: ActivityAnalytics = ActivityAnalytics(),
    val selectedTasbihPhraseAnalytics: TasbihPhraseAnalytics = TasbihPhraseAnalytics(),
) {
    val tasbihProgress: Float
        get() = tasbihCycleProgress(tasbihCount, tasbihTarget)

    fun progressFor(collectionId: String): ReadingProgress =
        readingProgress[collectionId] ?: ReadingProgress()
}

class SakinahViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = AppPreferencesRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val contentRepository = HisnContentRepository(application)
    private val readingSessions = ReadingSessionRepository(preferencesRepository, activityRepository)
    private val contentState = MutableStateFlow<ContentState>(ContentState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val preferencesWithPhraseAnalytics = preferencesRepository.preferences.flatMapLatest { preferences ->
        activityRepository.observeTasbihPhraseAnalytics(preferences.selectedPhraseId).map { phraseAnalytics ->
            preferences to phraseAnalytics
        }
    }

    val uiState: StateFlow<SakinahUiState> = combine(
        preferencesWithPhraseAnalytics,
        contentState,
        activityRepository.analytics,
    ) { preferencesAndPhraseAnalytics, content, analytics ->
        val (preferences, phraseAnalytics) = preferencesAndPhraseAnalytics
        val phrases = preferences.tasbihPhrases
        val selectedPhrase = phrases.firstOrNull { it.id == preferences.selectedPhraseId }
            ?: phrases.first()
        SakinahUiState(
            isLoading = content is ContentState.Loading,
            contentLoadFailed = content is ContentState.Failed,
            catalog = (content as? ContentState.Ready)?.catalog ?: HisnCatalog.Empty,
            tasbihCount = preferences.tasbihCount,
            tasbihTarget = preferences.tasbihTarget,
            selectedPhrase = selectedPhrase,
            tasbihPhrases = phrases,
            dynamicColorEnabled = preferences.dynamicColorEnabled,
            themeMode = preferences.themeMode,
            arabicFontStyle = preferences.arabicFontStyle,
            hapticsEnabled = preferences.hapticsEnabled,
            showDiacritics = preferences.showDiacritics,
            textScale = preferences.textScale,
            tasbihTextScale = preferences.tasbihTextScale,
            showReferenceByDefault = preferences.showReferenceByDefault,
            dhikrCompletionSoundEnabled = preferences.dhikrCompletionSoundEnabled,
            tasbihCompletionSoundEnabled = preferences.tasbihCompletionSoundEnabled,
            completionSound = preferences.completionSound,
            completionSoundVolume = preferences.completionSoundVolume,
            autoAdvanceDhikrEnabled = preferences.autoAdvanceDhikrEnabled,
            favoriteEntryIds = preferences.favoriteEntryIds,
            readingProgress = preferences.readingProgress,
            readerUndo = preferences.readerUndo,
            dailyReset = preferences.dailyReset,
            recentEntryIds = preferences.recentEntryIds.ifEmpty {
                analytics.recent.filter { it.kind == com.sakinah.tasbih.data.ActivityKinds.Reader }.map { it.sourceId }.distinct().take(12)
            },
            activityAnalytics = analytics,
            selectedTasbihPhraseAnalytics = phraseAnalytics,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SakinahUiState(),
    )

    init {
        loadContent()
        watchForDailyRollover()
    }

    fun retryContentLoad() {
        contentState.value = ContentState.Loading
        loadContent()
    }

    fun incrementTasbih() = viewModelScope.launch {
        val phrase = uiState.value.selectedPhrase
        preferencesRepository.incrementCounter()
        activityRepository.recordTasbih(phrase)
    }

    fun decrementTasbih() = viewModelScope.launch {
        val state = uiState.value
        if (state.tasbihCount <= 0) return@launch
        preferencesRepository.decrementCounter()
        activityRepository.undoTasbih(state.selectedPhrase.id)
    }

    fun resetTasbih() = viewModelScope.launch {
        preferencesRepository.resetCounter()
    }

    fun setTasbihTarget(target: Int) = viewModelScope.launch {
        preferencesRepository.setTarget(target)
    }

    fun adjustTasbihTarget(change: Int) = setTasbihTarget(
        (uiState.value.tasbihTarget + change).coerceIn(0, 9_999),
    )

    fun selectPhrase(id: String) = viewModelScope.launch {
        uiState.value.tasbihPhrases.firstOrNull { it.id == id }?.let { phrase ->
            preferencesRepository.selectPhrase(phrase)
        }
    }

    fun addCustomPhrase(text: String, defaultGoal: Int) = viewModelScope.launch {
        if (text.isNotBlank()) preferencesRepository.addCustomPhrase(text, defaultGoal)
    }

    fun updateTasbihPhrase(id: String, text: String, defaultGoal: Int) = viewModelScope.launch {
        val phrase = uiState.value.tasbihPhrases.firstOrNull { it.id == id } ?: return@launch
        if (text.isNotBlank()) preferencesRepository.updatePhrase(phrase, text, defaultGoal)
    }

    fun moveTasbihPhrase(id: String, destinationIndex: Int) = viewModelScope.launch {
        preferencesRepository.moveTasbihPhrase(id, destinationIndex)
    }

    fun addEntryToTasbih(entry: DhikrEntry) = viewModelScope.launch {
        preferencesRepository.addCustomPhrase(entry.text, entry.repetitions)
    }

    fun deleteCustomPhrase(id: String) = viewModelScope.launch {
        preferencesRepository.deleteCustomPhrase(id)
    }

    fun deleteTasbihPhrase(id: String) = viewModelScope.launch {
        val phrase = uiState.value.tasbihPhrases.firstOrNull { it.id == id } ?: return@launch
        if (phrase.isCustom) {
            preferencesRepository.deleteCustomPhrase(id)
        } else {
            preferencesRepository.hideBuiltInPhrase(id)
        }
    }

    fun incrementDhikr(collectionId: String) = viewModelScope.launch {
        uiState.value.catalog.collection(collectionId)?.let { readingSessions.increment(it) }
    }

    fun advanceDhikr(collectionId: String) = viewModelScope.launch {
        uiState.value.catalog.collection(collectionId)?.let { readingSessions.advance(it) }
    }

    fun navigateDhikr(collectionId: String, direction: Int) = viewModelScope.launch {
        uiState.value.catalog.collection(collectionId)?.let { readingSessions.navigate(it, direction) }
    }

    fun openReaderEntry(entry: DhikrEntry) = viewModelScope.launch {
        uiState.value.catalog.collection(entry.collectionId)?.let { readingSessions.openEntry(it, entry.id) }
    }

    fun recordVisitedEntry(entryId: String) = viewModelScope.launch {
        preferencesRepository.recordVisitedEntry(entryId)
    }

    fun undoDhikr(collectionId: String) = viewModelScope.launch {
        readingSessions.undo(collectionId)
    }

    fun restartCollection(collectionId: String) = viewModelScope.launch {
        readingSessions.restart(collectionId)
    }

    fun toggleFavorite(entryId: String) = viewModelScope.launch {
        preferencesRepository.toggleFavorite(entryId)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setDynamicColor(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferencesRepository.setThemeMode(mode)
    }

    fun setArabicFontStyle(style: ArabicFontStyle) = viewModelScope.launch {
        preferencesRepository.setArabicFontStyle(style)
    }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setHaptics(enabled)
    }

    fun setShowDiacritics(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowDiacritics(enabled)
    }

    fun setTextScale(scale: Float) = viewModelScope.launch {
        preferencesRepository.setTextScale(scale)
    }

    fun setTasbihTextScale(scale: Float) = viewModelScope.launch {
        preferencesRepository.setTasbihTextScale(scale)
    }

    fun setShowReferenceByDefault(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setShowReferenceByDefault(enabled)
    }

    fun setDhikrCompletionSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setDhikrCompletionSoundEnabled(enabled)
    }

    fun setTasbihCompletionSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setTasbihCompletionSoundEnabled(enabled)
    }

    fun setCompletionSound(sound: CompletionSound) = viewModelScope.launch {
        preferencesRepository.setCompletionSound(sound)
    }

    fun setCompletionSoundVolume(volume: Float) = viewModelScope.launch {
        preferencesRepository.setCompletionSoundVolume(volume)
    }

    fun setAutoAdvanceDhikrEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setAutoAdvanceDhikrEnabled(enabled)
    }

    fun setTasbihDailyReset(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setTasbihDailyReset(enabled) }

    fun setAdhkarDailyReset(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setAdhkarDailyReset(enabled) }

    fun setDailyResetTime(minuteOfDay: Int) = viewModelScope.launch { preferencesRepository.setDailyResetTime(minuteOfDay) }

    fun refreshDailyState() = viewModelScope.launch { preferencesRepository.rollOverDailyStateIfNeeded() }

    private fun loadContent() {
        viewModelScope.launch(Dispatchers.IO) {
            contentState.value = runCatching { contentRepository.load("ar") }
                .fold(
                    onSuccess = ContentState::Ready,
                    onFailure = { ContentState.Failed },
                )
        }
    }

    private fun watchForDailyRollover() {
        viewModelScope.launch {
            preferencesRepository.preferences.map { it.dailyReset }.distinctUntilChanged().collectLatest { settings ->
                while (true) {
                    preferencesRepository.rollOverDailyStateIfNeeded()
                    // Recheck the wall clock as well as the exact boundary, including time-zone edits.
                    delay((millisUntilNextDailyRollover(settings = settings) + 100L).coerceAtMost(60_000L))
                }
            }
        }
    }

    private sealed interface ContentState {
        data object Loading : ContentState
        data class Ready(val catalog: HisnCatalog) : ContentState
        data object Failed : ContentState
    }
}

internal fun millisUntilNextDailyRollover(now: ZonedDateTime = ZonedDateTime.now(), settings: DailyResetSettings = DailyResetSettings()): Long {
    val nextDayStart = nextDailyReset(now, settings)
    return Duration.between(now, nextDayStart).toMillis().coerceAtLeast(1_000L)
}
