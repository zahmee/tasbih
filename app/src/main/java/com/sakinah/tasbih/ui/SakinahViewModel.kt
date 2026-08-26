package com.sakinah.tasbih.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sakinah.tasbih.data.AppPreferencesRepository
import com.sakinah.tasbih.data.ActivityAnalytics
import com.sakinah.tasbih.data.ActivityRepository
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.DhikrCatalog
import com.sakinah.tasbih.data.DhikrEntry
import com.sakinah.tasbih.data.HisnCatalog
import com.sakinah.tasbih.data.HisnContentRepository
import com.sakinah.tasbih.data.ReadingProgress
import com.sakinah.tasbih.data.TasbihPhrase
import com.sakinah.tasbih.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val autoAdvanceDhikrEnabled: Boolean = true,
    val favoriteEntryIds: Set<String> = emptySet(),
    val readingProgress: Map<String, ReadingProgress> = emptyMap(),
    val activityAnalytics: ActivityAnalytics = ActivityAnalytics(),
) {
    val tasbihProgress: Float
        get() = if (tasbihTarget <= 0) 0f else {
            (tasbihCount.toFloat() / tasbihTarget).coerceIn(0f, 1f)
        }

    val isTasbihGoalComplete: Boolean
        get() = tasbihTarget > 0 && tasbihCount >= tasbihTarget

    fun progressFor(collectionId: String): ReadingProgress =
        readingProgress[collectionId] ?: ReadingProgress()
}

class SakinahViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = AppPreferencesRepository(application)
    private val activityRepository = ActivityRepository(application)
    private val contentRepository = HisnContentRepository(application)
    private val contentState = MutableStateFlow<ContentState>(ContentState.Loading)

    val uiState: StateFlow<SakinahUiState> = combine(
        preferencesRepository.preferences,
    contentState,
    activityRepository.analytics,
) { preferences, content, analytics ->
        val phrases = (
            DhikrCatalog.builtInTasbihPhrases
                .filterNot { it.id in preferences.hiddenBuiltInPhraseIds } +
                preferences.customPhrases
            ).ifEmpty { DhikrCatalog.builtInTasbihPhrases.take(1) }
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
            autoAdvanceDhikrEnabled = preferences.autoAdvanceDhikrEnabled,
            favoriteEntryIds = preferences.favoriteEntryIds,
            readingProgress = preferences.readingProgress,
            activityAnalytics = analytics,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = SakinahUiState(),
    )

    init {
        loadContent()
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
        val collection = uiState.value.catalog.collection(collectionId) ?: return@launch
        if (collection.entries.isEmpty()) return@launch
        val progress = uiState.value.progressFor(collectionId)
        if (progress.completed) return@launch

        val safeIndex = progress.entryIndex.coerceIn(0, collection.entries.lastIndex)
        val entry = collection.entries[safeIndex]
        if (progress.repetitionCount >= entry.repetitions) return@launch
        preferencesRepository.saveReadingProgress(
            collectionId = collectionId,
            progress = progress.copy(
                entryIndex = safeIndex,
                repetitionCount = (progress.repetitionCount + 1).coerceAtMost(entry.repetitions),
            ),
        )
        activityRepository.recordReader(entry, collection.title)
    }

    fun advanceDhikr(collectionId: String) = viewModelScope.launch {
        val collection = uiState.value.catalog.collection(collectionId) ?: return@launch
        if (collection.entries.isEmpty()) return@launch
        val progress = uiState.value.progressFor(collectionId)
        if (progress.completed) return@launch

        val safeIndex = progress.entryIndex.coerceIn(0, collection.entries.lastIndex)
        val entry = collection.entries[safeIndex]
        if (progress.repetitionCount < entry.repetitions) return@launch

        val nextProgress = if (safeIndex == collection.entries.lastIndex) {
            progress.copy(completed = true)
        } else {
            ReadingProgress(entryIndex = safeIndex + 1)
        }
        preferencesRepository.saveReadingProgress(collectionId, nextProgress)
        if (nextProgress.completed) activityRepository.recordCompletion(collection)
    }

    /**
     * Moves between dhikr entries without requiring the current repetition target first.
     * Manual navigation starts the destination entry from zero and never marks the session complete.
     */
    fun navigateDhikr(collectionId: String, direction: Int) = viewModelScope.launch {
        val collection = uiState.value.catalog.collection(collectionId) ?: return@launch
        if (collection.entries.isEmpty() || direction == 0) return@launch

        val progress = uiState.value.progressFor(collectionId)
        if (progress.completed) return@launch

        val currentIndex = progress.entryIndex.coerceIn(0, collection.entries.lastIndex)
        val targetIndex = (currentIndex + direction).coerceIn(0, collection.entries.lastIndex)
        if (targetIndex == currentIndex) return@launch

        preferencesRepository.saveReadingProgress(
            collectionId = collectionId,
            progress = ReadingProgress(entryIndex = targetIndex),
        )
    }

    fun restartCollection(collectionId: String) = viewModelScope.launch {
        preferencesRepository.saveReadingProgress(collectionId, ReadingProgress())
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

    fun setAutoAdvanceDhikrEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setAutoAdvanceDhikrEnabled(enabled)
    }

    private fun loadContent() {
        viewModelScope.launch(Dispatchers.IO) {
            contentState.value = runCatching { contentRepository.load("ar") }
                .fold(
                    onSuccess = ContentState::Ready,
                    onFailure = { ContentState.Failed },
                )
        }
    }

    private sealed interface ContentState {
        data object Loading : ContentState
        data class Ready(val catalog: HisnCatalog) : ContentState
        data object Failed : ContentState
    }
}
