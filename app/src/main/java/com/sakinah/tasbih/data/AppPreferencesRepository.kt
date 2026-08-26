package com.sakinah.tasbih.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.nio.charset.StandardCharsets
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
)

class AppPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.sakinahDataStore

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { stored -> stored.toAppPreferences() }

    suspend fun incrementCounter() {
        dataStore.edit { preferences ->
            preferences[Keys.counter] = (preferences[Keys.counter] ?: 0) + 1
        }
    }

    suspend fun decrementCounter() {
        dataStore.edit { preferences ->
            preferences[Keys.counter] = ((preferences[Keys.counter] ?: 0) - 1).coerceAtLeast(0)
        }
    }

    suspend fun resetCounter() {
        dataStore.edit { preferences -> preferences[Keys.counter] = 0 }
    }

    suspend fun setTarget(target: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.target] = target.coerceIn(NoTarget, MaximumTarget)
        }
    }

    suspend fun selectPhrase(phrase: TasbihPhrase) {
        dataStore.edit { preferences ->
            if (preferences[Keys.selectedPhraseId] != phrase.id) {
                preferences[Keys.selectedPhraseId] = phrase.id
                preferences[Keys.target] = phrase.defaultGoal.coerceIn(NoTarget, MaximumTarget)
                preferences[Keys.counter] = 0
            }
        }
    }

    suspend fun addCustomPhrase(text: String, defaultGoal: Int): String {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty())
        var resolvedId = ""

        dataStore.edit { preferences ->
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
                preferences[Keys.customPhrases] = (phrases + phrase)
                    .mapTo(mutableSetOf(), TasbihPhrase::encodeForPreferences)
            }
            preferences[Keys.selectedPhraseId] = phrase.id
            preferences[Keys.target] = phrase.defaultGoal
            preferences[Keys.counter] = 0
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

        dataStore.edit { preferences ->
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
            preferences[Keys.counter] = 0
        }
    }

    suspend fun deleteCustomPhrase(id: String) {
        dataStore.edit { preferences ->
            val phrases = decodeCustomPhrases(preferences[Keys.customPhrases].orEmpty())
            val remaining = phrases
                .filterNot { it.id == id }
            if (remaining.size == phrases.size) return@edit

            val hiddenBuiltInIds = preferences[Keys.hiddenBuiltInPhraseIds].orEmpty()
            val availablePhrases = visibleBuiltInTasbihPhrases(hiddenBuiltInIds) + remaining
            if (availablePhrases.isEmpty()) return@edit

            preferences[Keys.customPhrases] = remaining
                .mapTo(mutableSetOf(), TasbihPhrase::encodeForPreferences)

            if (preferences[Keys.selectedPhraseId] == id) {
                val fallback = availablePhrases.first()
                preferences[Keys.selectedPhraseId] = fallback.id
                preferences[Keys.target] = fallback.defaultGoal
                preferences[Keys.counter] = 0
            }
        }
    }

    suspend fun hideBuiltInPhrase(id: String) {
        dataStore.edit { preferences ->
            if (DhikrCatalog.builtInTasbihPhrases.none { it.id == id }) return@edit

            val hiddenBuiltInIds = preferences[Keys.hiddenBuiltInPhraseIds].orEmpty()
            if (id in hiddenBuiltInIds) return@edit

            val customPhrases = decodeCustomPhrases(preferences[Keys.customPhrases].orEmpty())
            val updatedHiddenIds = hiddenBuiltInIds + id
            val availablePhrases = visibleBuiltInTasbihPhrases(updatedHiddenIds) + customPhrases
            if (availablePhrases.isEmpty()) return@edit

            preferences[Keys.hiddenBuiltInPhraseIds] = updatedHiddenIds
            if (preferences[Keys.selectedPhraseId] == id) {
                val fallback = availablePhrases.first()
                preferences[Keys.selectedPhraseId] = fallback.id
                preferences[Keys.target] = fallback.defaultGoal
                preferences[Keys.counter] = 0
            }
        }
    }

    suspend fun saveReadingProgress(collectionId: String, progress: ReadingProgress) {
        dataStore.edit { preferences ->
            val progressMap = decodeReadingProgress(preferences[Keys.readingProgress].orEmpty())
                .toMutableMap()
            progressMap[collectionId] = progress
            preferences[Keys.readingProgress] = progressMap
                .mapTo(mutableSetOf()) { (id, value) -> value.encodeForPreferences(id) }
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

    private fun Preferences.toAppPreferences() = AppPreferences(
        tasbihCount = (this[Keys.counter] ?: 0).coerceAtLeast(0),
        tasbihTarget = (this[Keys.target] ?: DefaultTarget).coerceIn(NoTarget, MaximumTarget),
        selectedPhraseId = this[Keys.selectedPhraseId] ?: DefaultPhraseId,
        customPhrases = decodeCustomPhrases(this[Keys.customPhrases].orEmpty()),
        hiddenBuiltInPhraseIds = this[Keys.hiddenBuiltInPhraseIds].orEmpty(),
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
        autoAdvanceDhikrEnabled = this[Keys.autoAdvanceDhikrEnabled] ?: true,
        favoriteEntryIds = this[Keys.favorites].orEmpty(),
        readingProgress = decodeReadingProgress(this[Keys.readingProgress].orEmpty()),
    )

    private object Keys {
        val counter = intPreferencesKey("tasbih_counter")
        val target = intPreferencesKey("tasbih_target")
        val selectedPhraseId = stringPreferencesKey("selected_phrase")
        val customPhrases = stringSetPreferencesKey("custom_tasbih_phrases")
        val hiddenBuiltInPhraseIds = stringSetPreferencesKey("hidden_builtin_tasbih_phrases")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val themeMode = stringPreferencesKey("theme_mode")
        val arabicFontStyle = stringPreferencesKey("arabic_font_style")
        val haptics = booleanPreferencesKey("haptics")
        val showDiacritics = booleanPreferencesKey("show_diacritics")
        val textScale = floatPreferencesKey("dhikr_text_scale")
        val tasbihTextScale = floatPreferencesKey("tasbih_phrase_text_scale")
        val showReferenceByDefault = booleanPreferencesKey("reader_show_reference_by_default")
        val dhikrCompletionSoundEnabled = booleanPreferencesKey("reader_completion_sound_enabled")
        val autoAdvanceDhikrEnabled = booleanPreferencesKey("reader_auto_advance_enabled")
        val favorites = stringSetPreferencesKey("favorite_dhikr_entries")
        val readingProgress = stringSetPreferencesKey("reading_progress")
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

private fun ReadingProgress.encodeForPreferences(collectionId: String): String =
    "$collectionId|$entryIndex|$repetitionCount|${if (completed) 1 else 0}"

private fun decodeReadingProgress(values: Set<String>): Map<String, ReadingProgress> =
    values.mapNotNull { value ->
        runCatching {
            val parts = value.split('|', limit = 4)
            require(parts.size == 4)
            parts[0] to ReadingProgress(
                entryIndex = parts[1].toInt().coerceAtLeast(0),
                repetitionCount = parts[2].toInt().coerceAtLeast(0),
                completed = parts[3] == "1",
            )
        }.getOrNull()
    }.toMap()
