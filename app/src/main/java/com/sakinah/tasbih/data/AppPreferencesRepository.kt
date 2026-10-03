package com.sakinah.tasbih.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sakinahDataStore by preferencesDataStore(name = "sakinah_preferences")

data class AppPreferences(
    val tasbihCount: Int = 0,
    val tasbihTarget: Int = 33,
    val selectedPhraseId: String = "subhan_allah",
    val customPhrases: List<TasbihPhrase> = emptyList(),
    val hiddenBuiltInPhraseIds: Set<String> = emptySet(),
    val tasbihPhraseOrder: List<String> = emptyList(),
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
    internal val dailyStateKey: String = "",
) {
    val tasbihPhrases: List<TasbihPhrase>
        get() {
            val available = (visibleBuiltInTasbihPhrases(hiddenBuiltInPhraseIds) + customPhrases)
                .ifEmpty { DhikrCatalog.builtInTasbihPhrases.take(1) }
            val byId = available.associateBy(TasbihPhrase::id)
            return (tasbihPhraseOrder.mapNotNull(byId::get) + available).distinctBy(TasbihPhrase::id)
        }
}

class AppPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val now: () -> ZonedDateTime = { ZonedDateTime.now() },
) {
    constructor(context: Context) : this(context.applicationContext.sakinahDataStore)

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { stored -> stored.toAppPreferences() }

    suspend fun incrementCounter() {
        editDailyState { preferences ->
            val key = Keys.phraseCounter(preferences.selectedPhraseId())
            preferences[key] = preferences.currentPhraseCount() + 1
        }
    }

    suspend fun decrementCounter() {
        editDailyState { preferences ->
            val key = Keys.phraseCounter(preferences.selectedPhraseId())
            preferences[key] = (preferences.currentPhraseCount() - 1).coerceAtLeast(0)
        }
    }

    suspend fun resetCounter() {
        editDailyState { preferences ->
            preferences[Keys.phraseCounter(preferences.selectedPhraseId())] = 0
        }
    }

    suspend fun setTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.target] = target.coerceIn(NoTarget, MaximumTarget)
        }
    }

    suspend fun selectPhrase(phrase: TasbihPhrase) {
        editDailyState { preferences ->
            if (preferences.selectedPhraseId() != phrase.id) {
                preferences[Keys.selectedPhraseId] = phrase.id
                preferences[Keys.target] = phrase.defaultGoal.coerceIn(NoTarget, MaximumTarget)
            }
        }
    }

    suspend fun moveTasbihPhrase(id: String, destinationIndex: Int) {
        dataStore.edit { preferences ->
            val phrases = preferences.toAppPreferences().tasbihPhrases.toMutableList()
            val sourceIndex = phrases.indexOfFirst { it.id == id }
            if (sourceIndex < 0 || destinationIndex !in phrases.indices || sourceIndex == destinationIndex) return@edit
            phrases.add(destinationIndex, phrases.removeAt(sourceIndex))
            preferences[Keys.phraseOrder] = phrases.joinToString("|", transform = TasbihPhrase::id)
        }
    }

    suspend fun addCustomPhrase(text: String, defaultGoal: Int): String {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty())
        var resolvedId = ""

        editDailyState { preferences ->
            val phrases = decodeCustomPhrases(preferences[Keys.customPhrases].orEmpty())
            val existing = phrases.firstOrNull {
                normalizeArabicForSearch(it.text) == normalizeArabicForSearch(cleanText)
            }
            val phrase = existing ?: TasbihPhrase(
                id = "custom_${UUID.randomUUID()}",
                text = cleanText,
                defaultGoal = defaultGoal.coerceIn(NoTarget, MaximumTarget),
                isCustom = true,
            )
            resolvedId = phrase.id

            if (existing == null) {
                val existingOrder = preferences.toAppPreferences().tasbihPhrases.map(TasbihPhrase::id)
                preferences[Keys.customPhrases] = (phrases + phrase)
                    .mapTo(mutableSetOf(), TasbihPhrase::encodeForPreferences)
                preferences[Keys.phraseOrder] = (existingOrder + phrase.id).joinToString("|")
            }
            preferences[Keys.selectedPhraseId] = phrase.id
            preferences[Keys.target] = phrase.defaultGoal
        }

        return resolvedId
    }

    suspend fun updatePhrase(phrase: TasbihPhrase, text: String, defaultGoal: Int) {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty())
        val cleanGoal = defaultGoal.coerceIn(NoTarget, MaximumTarget)

        // Built-in phrases remain immutable. Editing one creates a personal version,
        // which keeps the original wording available in the carousel.
        if (!phrase.isCustom) {
            addCustomPhrase(cleanText, cleanGoal)
            return
        }

        editDailyState { preferences ->
            val phrases = decodeCustomPhrases(preferences[Keys.customPhrases].orEmpty())
            val updated = TasbihPhrase(
                id = phrase.id,
                text = cleanText,
                defaultGoal = cleanGoal,
                isCustom = true,
            )
            val updatedPhrases = if (phrases.any { it.id == phrase.id }) {
                phrases.map { stored -> if (stored.id == phrase.id) updated else stored }
            } else {
                phrases + updated
            }
            preferences[Keys.customPhrases] = updatedPhrases
                .mapTo(mutableSetOf(), TasbihPhrase::encodeForPreferences)
            preferences[Keys.selectedPhraseId] = updated.id
            preferences[Keys.target] = updated.defaultGoal
        }
    }

    suspend fun deleteCustomPhrase(id: String) {
        editDailyState dailyEdit@ { preferences ->
            val phrases = decodeCustomPhrases(preferences[Keys.customPhrases].orEmpty())
            val remaining = phrases
                .filterNot { it.id == id }
            if (remaining.size == phrases.size) return@dailyEdit

            val availablePhrases = preferences.toAppPreferences().tasbihPhrases.filterNot { it.id == id }
            if (availablePhrases.isEmpty()) return@dailyEdit

            preferences[Keys.customPhrases] = remaining
                .mapTo(mutableSetOf(), TasbihPhrase::encodeForPreferences)
            preferences[Keys.phraseOrder] = availablePhrases.joinToString("|", transform = TasbihPhrase::id)
            preferences.remove(Keys.phraseCounter(id))

            if (preferences.selectedPhraseId() == id) {
                val fallback = availablePhrases.first()
                preferences[Keys.selectedPhraseId] = fallback.id
                preferences[Keys.target] = fallback.defaultGoal
            }
        }
    }

    suspend fun hideBuiltInPhrase(id: String) {
        editDailyState dailyEdit@ { preferences ->
            if (DhikrCatalog.builtInTasbihPhrases.none { it.id == id }) return@dailyEdit

            val hiddenBuiltInIds = preferences[Keys.hiddenBuiltInPhraseIds].orEmpty()
            if (id in hiddenBuiltInIds) return@dailyEdit

            val updatedHiddenIds = hiddenBuiltInIds + id
            val availablePhrases = preferences.toAppPreferences().tasbihPhrases.filterNot { it.id == id }
            if (availablePhrases.isEmpty()) return@dailyEdit

            preferences[Keys.hiddenBuiltInPhraseIds] = updatedHiddenIds
            preferences[Keys.phraseOrder] = availablePhrases.joinToString("|", transform = TasbihPhrase::id)
            if (preferences.selectedPhraseId() == id) {
                val fallback = availablePhrases.first()
                preferences[Keys.selectedPhraseId] = fallback.id
                preferences[Keys.target] = fallback.defaultGoal
            }
        }
    }

    suspend fun saveReadingProgress(collectionId: String, progress: ReadingProgress) {
        editDailyState { preferences ->
            val progressMap = decodeReadingProgress(preferences[Keys.readingProgress].orEmpty())
                .toMutableMap()
            progressMap[collectionId] = progress
            preferences[Keys.readingProgress] = encodeReadingProgress(progressMap)
        }
    }

    /**
     * Starts a fresh daily session while retaining progress for non-daily library collections.
     * The saved activity database is intentionally untouched because it is historical data.
     */
    suspend fun rollOverDailyStateIfNeeded(): Boolean {
        var didRollOver = false
        dataStore.edit { preferences ->
            didRollOver = rollOverDailyState(preferences, preferences.effectiveDayKey())
        }
        return didRollOver
    }

    suspend fun saveReaderAction(
        collectionId: String, progress: ReadingProgress, undo: ReaderUndo?, expectedDayKey: String? = null,
    ): Boolean {
        var accepted = false
        editDailyState { preferences ->
            if (expectedDayKey != null && preferences.stateToken(preferences[Keys.dailyStateDay].orEmpty()) != expectedDayKey) return@editDailyState
            val progressMap = decodeReadingProgress(preferences[Keys.readingProgress].orEmpty()) + (collectionId to progress)
            val undoMap = decodeReaderUndo(preferences[Keys.readerUndo].orEmpty()).toMutableMap()
            if (undo == null) undoMap.remove(collectionId) else undoMap[collectionId] = undo
            preferences[Keys.readingProgress] = encodeReadingProgress(progressMap)
            preferences[Keys.readerUndo] = undoMap.mapTo(mutableSetOf()) { (id, value) -> value.encode(id) }
            accepted = true
        }
        return accepted
    }

    suspend fun recordVisitedEntry(entryId: String) {
        dataStore.edit { preferences ->
            val previous = preferences[Keys.recentEntries].orEmpty().split('|').filter(String::isNotBlank)
            if (previous.firstOrNull() != entryId) {
                preferences[Keys.recentEntries] = (listOf(entryId) + previous).distinct().take(12).joinToString("|")
            }
        }
    }

    suspend fun toggleFavorite(entryId: String) {
        dataStore.edit { preferences ->
            val favorites = preferences[Keys.favorites].orEmpty().toMutableSet()
            if (!favorites.add(entryId)) favorites.remove(entryId)
            preferences[Keys.favorites] = favorites
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.dynamicColor] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.themeMode] = mode.name }
    }

    suspend fun setArabicFontStyle(style: ArabicFontStyle) {
        dataStore.edit { preferences -> preferences[Keys.arabicFontStyle] = style.name }
    }

    suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.haptics] = enabled }
    }

    suspend fun setShowDiacritics(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.showDiacritics] = enabled }
    }

    suspend fun setTextScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.textScale] = scale.coerceIn(MinimumTextScale, MaximumTextScale)
        }
    }

    suspend fun setTasbihTextScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.tasbihTextScale] = scale.coerceIn(
                MinimumTasbihTextScale,
                MaximumTasbihTextScale,
            )
        }
    }

    suspend fun setShowReferenceByDefault(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.showReferenceByDefault] = enabled }
    }

    suspend fun setDhikrCompletionSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.dhikrCompletionSoundEnabled] = enabled }
    }

    suspend fun setAutoAdvanceDhikrEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.autoAdvanceDhikrEnabled] = enabled }
    }

    suspend fun setTasbihCompletionSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.tasbihCompletionSoundEnabled] = enabled }
    }

    suspend fun setCompletionSound(sound: CompletionSound) {
        dataStore.edit { it[Keys.completionSound] = sound.storageId }
    }

    suspend fun setCompletionSoundVolume(volume: Float) {
        dataStore.edit { it[Keys.completionSoundVolume] = normalizedCompletionSoundVolume(volume) }
    }

    suspend fun setTasbihDailyReset(enabled: Boolean) = updateDailyReset { it.copy(tasbihEnabled = enabled) }

    suspend fun setAdhkarDailyReset(enabled: Boolean) = updateDailyReset { it.copy(adhkarEnabled = enabled) }

    suspend fun setDailyResetTime(minuteOfDay: Int) = updateDailyReset { it.copy(minuteOfDay = minuteOfDay.coerceIn(0, 1439)) }

    private suspend fun updateDailyReset(transform: (DailyResetSettings) -> DailyResetSettings) {
        dataStore.edit { preferences ->
            val previous = preferences.dailyResetSettings()
            val updated = transform(previous)
            if (updated == previous) return@edit
            val instant = now()
            // Apply an already due reset under the old policy, then start the new schedule
            // from this moment. Moving the clock or re-enabling a switch cannot wipe counts.
            rollOverDailyState(preferences, preferences.effectiveDayKey(instant))
            preferences[Keys.dailyResetTasbih] = updated.tasbihEnabled
            preferences[Keys.dailyResetAdhkar] = updated.adhkarEnabled
            preferences[Keys.dailyResetMinute] = updated.minuteOfDay
            preferences[Keys.dailyResetRevision] = (preferences[Keys.dailyResetRevision] ?: 0L) + 1L
            val day = dailyResetDay(instant, updated).toString()
            val token = preferences.stateToken(day)
            val undo = decodeReaderUndo(preferences[Keys.readerUndo].orEmpty())
            preferences[Keys.readerUndo] = undo.mapTo(mutableSetOf()) { (id, receipt) -> receipt.copy(dayKey = token).encode(id) }
            preferences[Keys.dailyStateDay] = day
        }
    }

    private fun Preferences.dailyResetSettings() = DailyResetSettings(
        tasbihEnabled = this[Keys.dailyResetTasbih] ?: true,
        adhkarEnabled = this[Keys.dailyResetAdhkar] ?: true,
        minuteOfDay = (this[Keys.dailyResetMinute] ?: 0).coerceIn(0, 1439),
    )

    private fun Preferences.effectiveDayKey(instant: ZonedDateTime = now()): String {
        val current = dailyResetDay(instant, dailyResetSettings()).toString()
        val stored = this[Keys.dailyStateDay]?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
        // A backwards device-clock/time-zone change must not reset the same session twice.
        return if (stored != null && stored > current) stored else current
    }

    private fun Preferences.stateToken(day: String): String = dailyStateToken(day, this[Keys.dailyResetRevision] ?: 0L)

    private fun Preferences.currentReaderUndo(day: String, settings: DailyResetSettings): Map<String, ReaderUndo> {
        val previousDay = this[Keys.dailyStateDay] ?: return emptyMap()
        val previousToken = stateToken(previousDay)
        return decodeReaderUndo(this[Keys.readerUndo].orEmpty())
            .filter { (id, receipt) ->
                receipt.dayKey == previousToken &&
                    (previousDay == day || !settings.adhkarEnabled || !isDailyDhikrCollectionId(id))
            }
            .mapValues { (_, receipt) -> receipt.copy(dayKey = stateToken(day)) }
    }

    private fun Preferences.toAppPreferences(): AppPreferences {
        val settings = dailyResetSettings()
        val day = effectiveDayKey()
        val dailyState = dailyStateForDate(
            storedDayKey = this[Keys.dailyStateDay],
            currentDayKey = day,
            tasbihCount = currentPhraseCount(),
            readingProgress = decodeReadingProgress(this[Keys.readingProgress].orEmpty()),
            settings = settings,
        )
        return AppPreferences(
            tasbihCount = dailyState.tasbihCount,
            tasbihTarget = (this[Keys.target] ?: DefaultTarget).coerceIn(NoTarget, MaximumTarget),
            selectedPhraseId = selectedPhraseId(),
            customPhrases = decodeCustomPhrases(this[Keys.customPhrases].orEmpty()),
            hiddenBuiltInPhraseIds = this[Keys.hiddenBuiltInPhraseIds].orEmpty(),
            tasbihPhraseOrder = this[Keys.phraseOrder].orEmpty().split('|').filter(String::isNotBlank).distinct(),
            dynamicColorEnabled = this[Keys.dynamicColor] ?: false,
            themeMode = ThemeMode.fromStorage(this[Keys.themeMode]),
            arabicFontStyle = ArabicFontStyle.fromStorage(this[Keys.arabicFontStyle]),
            hapticsEnabled = this[Keys.haptics] ?: true,
            showDiacritics = this[Keys.showDiacritics] ?: true,
            textScale = (this[Keys.textScale] ?: 1f).coerceIn(MinimumTextScale, MaximumTextScale),
            tasbihTextScale = (this[Keys.tasbihTextScale] ?: 1f).coerceIn(
                MinimumTasbihTextScale,
                MaximumTasbihTextScale,
            ),
            showReferenceByDefault = this[Keys.showReferenceByDefault] ?: false,
            dhikrCompletionSoundEnabled = this[Keys.dhikrCompletionSoundEnabled] ?: true,
            tasbihCompletionSoundEnabled = this[Keys.tasbihCompletionSoundEnabled] ?: true,
            completionSound = CompletionSound.fromStorage(this[Keys.completionSound]),
            completionSoundVolume = normalizedCompletionSoundVolume(
                this[Keys.completionSoundVolume] ?: DefaultCompletionSoundVolume,
            ),
            autoAdvanceDhikrEnabled = this[Keys.autoAdvanceDhikrEnabled] ?: true,
            favoriteEntryIds = this[Keys.favorites].orEmpty(),
            readingProgress = dailyState.readingProgress,
            readerUndo = currentReaderUndo(day, settings),
            recentEntryIds = this[Keys.recentEntries].orEmpty().split('|').filter(String::isNotBlank).distinct().take(12),
            dailyReset = settings,
            dailyStateKey = stateToken(day),
        )
    }

    private suspend fun editDailyState(transform: (MutablePreferences) -> Unit) {
        dataStore.edit { preferences ->
            rollOverDailyState(preferences, preferences.effectiveDayKey())
            transform(preferences)
        }
    }

    private fun Preferences.selectedPhraseId(): String = this[Keys.selectedPhraseId] ?: DefaultPhraseId

    private fun Preferences.currentPhraseCount(): Int = (
        this[Keys.phraseCounter(selectedPhraseId())] ?: this[Keys.legacyCounter] ?: 0
    ).coerceAtLeast(0)

    private fun rollOverDailyState(preferences: MutablePreferences, todayKey: String): Boolean {
        // Migrate the previous single counter before changing selection, in the same
        // transaction. Existing counts belong to the phrase selected when they were saved.
        preferences[Keys.legacyCounter]?.let { count ->
            val key = Keys.phraseCounter(preferences.selectedPhraseId())
            if (preferences[key] == null) preferences[key] = count.coerceAtLeast(0)
            preferences.remove(Keys.legacyCounter)
        }
        val storedDayKey = preferences[Keys.dailyStateDay]
        if (storedDayKey == todayKey) return false

        val settings = preferences.dailyResetSettings()
        val undo = preferences.currentReaderUndo(todayKey, settings)
        val dailyState = dailyStateForDate(
            storedDayKey = storedDayKey,
            currentDayKey = todayKey,
            tasbihCount = preferences.currentPhraseCount(),
            readingProgress = decodeReadingProgress(preferences[Keys.readingProgress].orEmpty()),
            settings = settings,
        )
        // Reset every phrase, including ones the user has not revisited today.
        if (settings.tasbihEnabled) {
            preferences.asMap().keys
                .filter { it.name.startsWith(TasbihCounterKeyPrefix) }
                .forEach { preferences.remove(it) }
        }
        preferences[Keys.readingProgress] = encodeReadingProgress(dailyState.readingProgress)
        preferences[Keys.dailyStateDay] = todayKey
        preferences[Keys.readerUndo] = undo.mapTo(mutableSetOf()) { (id, receipt) -> receipt.encode(id) }
        return true
    }

    private object Keys {
        val legacyCounter = intPreferencesKey("tasbih_counter")
        fun phraseCounter(id: String) = intPreferencesKey("$TasbihCounterKeyPrefix$id")
        val target = intPreferencesKey("tasbih_target")
        val selectedPhraseId = stringPreferencesKey("selected_phrase")
        val customPhrases = stringSetPreferencesKey("custom_tasbih_phrases")
        val hiddenBuiltInPhraseIds = stringSetPreferencesKey("hidden_builtin_tasbih_phrases")
        val phraseOrder = stringPreferencesKey("tasbih_phrase_order")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val themeMode = stringPreferencesKey("theme_mode")
        val arabicFontStyle = stringPreferencesKey("arabic_font_style")
        val haptics = booleanPreferencesKey("haptics")
        val showDiacritics = booleanPreferencesKey("show_diacritics")
        val textScale = floatPreferencesKey("dhikr_text_scale")
        val tasbihTextScale = floatPreferencesKey("tasbih_phrase_text_scale")
        val showReferenceByDefault = booleanPreferencesKey("reader_show_reference_by_default")
        val dhikrCompletionSoundEnabled = booleanPreferencesKey("reader_completion_sound_enabled")
        val tasbihCompletionSoundEnabled = booleanPreferencesKey("tasbih_completion_sound_enabled")
        val completionSound = stringPreferencesKey("completion_sound")
        val completionSoundVolume = floatPreferencesKey("completion_sound_volume")
        val autoAdvanceDhikrEnabled = booleanPreferencesKey("reader_auto_advance_enabled")
        val favorites = stringSetPreferencesKey("favorite_dhikr_entries")
        val readingProgress = stringSetPreferencesKey("reading_progress")
        val readerUndo = stringSetPreferencesKey("reader_last_action")
        val recentEntries = stringPreferencesKey("recent_reader_entries")
        val dailyStateDay = stringPreferencesKey("daily_state_day")
        val dailyResetTasbih = booleanPreferencesKey("daily_reset_tasbih_enabled")
        val dailyResetAdhkar = booleanPreferencesKey("daily_reset_adhkar_enabled")
        val dailyResetMinute = intPreferencesKey("daily_reset_minute")
        val dailyResetRevision = longPreferencesKey("daily_reset_revision")
    }

    private companion object {
        const val DefaultTarget = 33
        const val NoTarget = 0
        const val MaximumTarget = 9_999
        const val MinimumTextScale = 0.5f
        const val MaximumTextScale = 1.4f
        const val MinimumTasbihTextScale = 0.7f
        const val MaximumTasbihTextScale = 1.1f
        const val DefaultPhraseId = "subhan_allah"
        const val TasbihCounterKeyPrefix = "tasbih_phrase_counter_"
    }
}

private fun visibleBuiltInTasbihPhrases(hiddenPhraseIds: Set<String>): List<TasbihPhrase> =
    DhikrCatalog.builtInTasbihPhrases.filterNot { it.id in hiddenPhraseIds }

private fun TasbihPhrase.encodeForPreferences(): String {
    val encodedText = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(text.toByteArray(StandardCharsets.UTF_8))
    return "$id|$defaultGoal|$encodedText"
}

private fun decodeCustomPhrases(values: Set<String>): List<TasbihPhrase> = values.mapNotNull { value ->
    runCatching {
        val parts = value.split('|', limit = 3)
        require(parts.size == 3)
        TasbihPhrase(
            id = parts[0],
            defaultGoal = parts[1].toInt().coerceIn(0, 9_999),
            text = String(Base64.getUrlDecoder().decode(parts[2]), StandardCharsets.UTF_8),
            isCustom = true,
        )
    }.getOrNull()
}.sortedBy(TasbihPhrase::id)

private const val ReadingProgressStorageVersion = "v2"

internal fun ReadingProgress.encodeForPreferences(collectionId: String): String {
    val completedIndicesValue = completedEntryIndices
        .asSequence()
        .filter { it >= 0 }
        .sorted()
        .joinToString(",")
        .ifEmpty { "-" }
    val repetitionCountsValue = repetitionCounts
        .asSequence()
        .filter { (index, count) -> index >= 0 && count > 0 }
        .sortedBy { (index, _) -> index }
        .joinToString(",") { (index, count) -> "$index:$count" }
        .ifEmpty { "-" }
    return listOf(
        collectionId,
        ReadingProgressStorageVersion,
        entryIndex.coerceAtLeast(0).toString(),
        if (completed) "1" else "0",
        completedIndicesValue,
        repetitionCountsValue,
    ).joinToString("|")
}

private fun encodeReadingProgress(values: Map<String, ReadingProgress>): Set<String> =
    values.mapTo(mutableSetOf()) { (id, value) -> value.encodeForPreferences(id) }

internal fun decodeReadingProgress(values: Set<String>): Map<String, ReadingProgress> =
    values.mapNotNull { value ->
        runCatching {
            val parts = value.split('|')
            when {
                parts.size == 6 && parts[1] == ReadingProgressStorageVersion -> {
                    parts[0] to ReadingProgress(
                        entryIndex = parts[2].toInt().coerceAtLeast(0),
                        completed = parts[3] == "1",
                        completedEntryIndices = decodeCompletedEntryIndices(parts[4]),
                        repetitionCounts = decodeEntryRepetitionCounts(parts[5]),
                    )
                }

                parts.size == 4 -> {
                    val entryIndex = parts[1].toInt().coerceAtLeast(0)
                    val repetitionCount = parts[2].toInt().coerceAtLeast(0)
                    val completed = parts[3] == "1"
                    parts[0] to ReadingProgress(
                        entryIndex = entryIndex,
                        repetitionCounts = if (repetitionCount > 0) {
                            mapOf(entryIndex to repetitionCount)
                        } else {
                            emptyMap()
                        },
                        completedEntryIndices = if (completed) {
                            emptySet()
                        } else {
                            (0 until entryIndex).toSet()
                        },
                        completed = completed,
                    )
                }

                else -> error("Unsupported reading progress value")
            }
        }.getOrNull()
    }.toMap()

private fun decodeCompletedEntryIndices(value: String): Set<Int> = value
    .takeUnless { it == "-" }
    ?.split(',')
    ?.mapNotNull { encodedIndex -> encodedIndex.toIntOrNull()?.takeIf { it >= 0 } }
    ?.toSet()
    .orEmpty()

private fun decodeEntryRepetitionCounts(value: String): Map<Int, Int> = value
    .takeUnless { it == "-" }
    ?.split(',')
    ?.mapNotNull { encodedCount ->
        val parts = encodedCount.split(':', limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val index = parts[0].toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
        val count = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        index to count
    }
    ?.toMap()
    .orEmpty()

internal data class DailyStateValues(
    val tasbihCount: Int,
    val readingProgress: Map<String, ReadingProgress>,
)

internal fun dailyStateForDate(
    storedDayKey: String?,
    currentDayKey: String,
    tasbihCount: Int,
    readingProgress: Map<String, ReadingProgress>,
    settings: DailyResetSettings = DailyResetSettings(),
): DailyStateValues = if (storedDayKey == currentDayKey) {
    DailyStateValues(
        tasbihCount = tasbihCount.coerceAtLeast(0),
        readingProgress = readingProgress,
    )
} else {
    DailyStateValues(
        tasbihCount = if (settings.tasbihEnabled) 0 else tasbihCount.coerceAtLeast(0),
        readingProgress = readingProgress.filterKeys { collectionId ->
            !settings.adhkarEnabled || !isDailyDhikrCollectionId(collectionId)
        },
    )
}

internal fun isDailyDhikrCollectionId(collectionId: String): Boolean {
    val order = collectionId.removePrefix("hisn_")
        .takeIf { collectionId.startsWith("hisn_") }
        ?.toIntOrNull()
        ?: return false
    return order in DhikrGroup.DailyLife.firstOrder..DhikrGroup.MorningEvening.lastOrder
}
