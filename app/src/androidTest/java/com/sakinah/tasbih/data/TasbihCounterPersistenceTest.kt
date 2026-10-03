package com.sakinah.tasbih.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TasbihCounterPersistenceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    private var today = "2026-09-11"
    private var storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile by lazy { File(temporaryFolder.root, "tasbih.preferences_pb") }
    private var store = createStore()
    private var repository = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
    private val firstPhrase = DhikrCatalog.builtInTasbihPhrases[0]
    private val secondPhrase = DhikrCatalog.builtInTasbihPhrases[1]

    private fun createStore() = PreferenceDataStoreFactory.create(scope = storeScope) { storeFile }

    @After
    fun closeStore() = runBlocking {
        storeScope.cancel()
        storeScope.coroutineContext[Job]!!.join()
    }

    @Test
    fun soundSettingsSurviveReopeningAndDailyResetWithoutChangingTheCounter() = runBlocking {
        store.edit { it[androidx.datastore.preferences.core.booleanPreferencesKey("reader_completion_sound_enabled")] = false }
        assertFalse(repository.preferences.first().dhikrCompletionSoundEnabled)
        assertTrue(repository.preferences.first().tasbihCompletionSoundEnabled)
        repeat(7) { repository.incrementCounter() }
        repository.setCompletionSound(CompletionSound.TriplePulse)
        repository.setCompletionSoundVolume(0.7f)
        repository.setTasbihCompletionSoundEnabled(false)
        assertEquals(7, repository.preferences.first().tasbihCount)

        storeScope.cancel()
        storeScope.coroutineContext[Job]!!.join()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = createStore()
        repository = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
        val restored = repository.preferences.first()
        assertEquals(CompletionSound.TriplePulse, restored.completionSound)
        assertEquals(0.7f, restored.completionSoundVolume)
        assertFalse(restored.tasbihCompletionSoundEnabled)
        assertFalse(restored.dhikrCompletionSoundEnabled)
        today = "2026-09-12"
        repository.rollOverDailyStateIfNeeded()
        assertEquals(CompletionSound.TriplePulse, repository.preferences.first().completionSound)
        assertEquals(0.7f, repository.preferences.first().completionSoundVolume)
    }

    @Test
    fun legacyCountSurvivesPhraseNavigationAndReopeningStorage() = runBlocking {
        store.edit {
            it[stringPreferencesKey("daily_state_day")] = today
            // Older installations can have a count without an explicit selected phrase.
            it[intPreferencesKey("tasbih_counter")] = 12
        }
        assertEquals(12, repository.preferences.first().tasbihCount)

        repository.selectPhrase(secondPhrase)
        assertEquals(0, repository.preferences.first().tasbihCount)
        repeat(3) { repository.incrementCounter() }
        repository.selectPhrase(firstPhrase)
        assertEquals(12, repository.preferences.first().tasbihCount)

        storeScope.cancel()
        storeScope.coroutineContext[Job]!!.join()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = createStore()
        repository = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
        assertEquals(12, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        assertEquals(3, repository.preferences.first().tasbihCount)
    }

    @Test
    fun legacyCountStaysWithItsSelectedPhraseAndDoesNotReturnAfterDailyReset() = runBlocking {
        store.edit {
            it[stringPreferencesKey("daily_state_day")] = today
            it[stringPreferencesKey("selected_phrase")] = secondPhrase.id
            it[intPreferencesKey("tasbih_counter")] = 23
        }
        assertFalse(repository.rollOverDailyStateIfNeeded())
        assertEquals(23, repository.preferences.first().tasbihCount)
        repository.selectPhrase(firstPhrase)
        assertEquals(0, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        assertEquals(23, repository.preferences.first().tasbihCount)

        today = "2026-09-12"
        assertTrue(repository.rollOverDailyStateIfNeeded())
        assertEquals(0, repository.preferences.first().tasbihCount)
        repository.selectPhrase(firstPhrase)
        repository.selectPhrase(secondPhrase)
        assertEquals(0, repository.preferences.first().tasbihCount)
    }

    @Test
    fun resetAndUndoAffectOnlySelectedPhraseAndTargetChangesKeepItsCount() = runBlocking {
        repository.selectPhrase(firstPhrase)
        repeat(5) { repository.incrementCounter() }
        repository.setTarget(100)
        assertEquals(5, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        repeat(3) { repository.incrementCounter() }
        repository.decrementCounter()
        assertEquals(2, repository.preferences.first().tasbihCount)
        repository.resetCounter()
        assertEquals(0, repository.preferences.first().tasbihCount)
        repository.selectPhrase(firstPhrase)
        assertEquals(5, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        repository.decrementCounter()
        assertEquals(0, repository.preferences.first().tasbihCount)
    }

    @Test
    fun newDayClearsEveryPhraseOnceBeforeAcceptingNextTap() = runBlocking {
        repository.selectPhrase(firstPhrase)
        repeat(4) { repository.incrementCounter() }
        repository.selectPhrase(secondPhrase)
        repeat(2) { repository.incrementCounter() }
        assertFalse(repository.rollOverDailyStateIfNeeded())
        assertEquals(2, repository.preferences.first().tasbihCount)

        today = "2026-09-12"
        assertEquals(0, repository.preferences.first().tasbihCount)
        repository.incrementCounter()
        assertEquals(1, repository.preferences.first().tasbihCount)
        assertFalse(repository.rollOverDailyStateIfNeeded())
        repository.selectPhrase(firstPhrase)
        assertEquals(0, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        assertEquals(1, repository.preferences.first().tasbihCount)

        today = "2026-09-13"
        assertTrue(repository.rollOverDailyStateIfNeeded())
        assertEquals(0, repository.preferences.first().tasbihCount)
        assertFalse(repository.rollOverDailyStateIfNeeded())
    }

    @Test
    fun hidingImplicitDefaultPhraseSelectsAnotherPhraseWithItsOwnCount() = runBlocking {
        repeat(3) { repository.incrementCounter() }
        repository.hideBuiltInPhrase(firstPhrase.id)
        assertEquals(secondPhrase.id, repository.preferences.first().selectedPhraseId)
        assertEquals(0, repository.preferences.first().tasbihCount)
    }

    @Test
    fun editingExistingCustomPhrasePreservesCountsAndDeletionRestoresFallback() = runBlocking {
        repository.selectPhrase(firstPhrase)
        repeat(7) { repository.incrementCounter() }
        val customId = repository.addCustomPhrase("ذكر خاص للاختبار", 33)
        repeat(2) { repository.incrementCounter() }
        val phrase = repository.preferences.first().customPhrases.single()
        repository.updatePhrase(phrase, "ذكر خاص معدل", 100)
        assertEquals(2, repository.preferences.first().tasbihCount)
        repository.selectPhrase(firstPhrase)
        assertEquals(7, repository.preferences.first().tasbihCount)
        assertEquals(customId, repository.addCustomPhrase("ذكر خاص معدل", 100))
        assertEquals(2, repository.preferences.first().tasbihCount)

        repository.deleteCustomPhrase(customId)
        assertEquals(firstPhrase.id, repository.preferences.first().selectedPhraseId)
        assertEquals(7, repository.preferences.first().tasbihCount)
        repository.selectPhrase(secondPhrase)
        repeat(3) { repository.incrementCounter() }
        repository.hideBuiltInPhrase(secondPhrase.id)
        assertEquals(firstPhrase.id, repository.preferences.first().selectedPhraseId)
        assertEquals(7, repository.preferences.first().tasbihCount)
    }

    @Test
    fun phraseOrderSurvivesReopeningAndNewDayWithoutChangingSelectionCountsOrTarget() = runBlocking {
        repeat(5) { repository.incrementCounter() }
        repository.selectPhrase(secondPhrase)
        repeat(3) { repository.incrementCounter() }
        repository.selectPhrase(firstPhrase)
        repository.setTarget(77)
        val original = repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id)
        repository.moveTasbihPhrase(secondPhrase.id, 0)
        val expected = listOf(secondPhrase.id) + original.filterNot { it == secondPhrase.id }
        val reordered = repository.preferences.first()
        assertEquals(expected, reordered.tasbihPhrases.map(TasbihPhrase::id))
        assertEquals(firstPhrase.id, reordered.selectedPhraseId)
        assertEquals(5, reordered.tasbihCount)
        assertEquals(77, reordered.tasbihTarget)

        storeScope.cancel()
        storeScope.coroutineContext[Job]!!.join()
        storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = createStore()
        repository = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
        assertEquals(expected, repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id))
        assertEquals(5, repository.preferences.first().tasbihCount)
        assertEquals(77, repository.preferences.first().tasbihTarget)
        repository.selectPhrase(secondPhrase)
        assertEquals(3, repository.preferences.first().tasbihCount)

        today = "2026-09-12"
        repository.rollOverDailyStateIfNeeded()
        assertEquals(expected, repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id))
        assertEquals(0, repository.preferences.first().tasbihCount)
    }

    @Test
    fun addedAndEditedPhrasesKeepManualOrderAndDeletionUsesItsFirstRemainingPhrase() = runBlocking {
        val customId = repository.addCustomPhrase("ذكر مرتب", 33)
        repeat(2) { repository.incrementCounter() }
        repository.moveTasbihPhrase(customId, 0)
        val custom = repository.preferences.first().customPhrases.single()
        repository.updatePhrase(custom, "ذكر مرتب معدل", 100)
        assertEquals(customId, repository.preferences.first().tasbihPhrases.first().id)
        assertEquals(2, repository.preferences.first().tasbihCount)
        val orderBeforeAdding = repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id)
        val nextId = repository.addCustomPhrase("ذكر في النهاية", 7)
        assertEquals(orderBeforeAdding + nextId, repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id))
        repository.deleteCustomPhrase(nextId)
        assertEquals(customId, repository.preferences.first().selectedPhraseId)
        assertEquals(2, repository.preferences.first().tasbihCount)
        repository.hideBuiltInPhrase(firstPhrase.id)
        assertEquals(customId, repository.preferences.first().tasbihPhrases.first().id)
        repository.deleteCustomPhrase(customId)
        assertEquals(secondPhrase.id, repository.preferences.first().selectedPhraseId)
        assertEquals(secondPhrase.id, repository.preferences.first().tasbihPhrases.first().id)
    }

    @Test
    fun invalidMovesAreIgnoredAndMovingBetweenBothEndsNeverDropsAPhrase() = runBlocking {
        val original = repository.preferences.first()
        val ids = original.tasbihPhrases.map(TasbihPhrase::id)
        repository.moveTasbihPhrase(firstPhrase.id, -1)
        repository.moveTasbihPhrase(firstPhrase.id, ids.size)
        repository.moveTasbihPhrase("missing", 0)
        assertEquals(original, repository.preferences.first())
        repository.moveTasbihPhrase(ids.last(), 0)
        assertEquals(listOf(ids.last()) + ids.dropLast(1), repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id))
        repository.moveTasbihPhrase(ids.last(), ids.lastIndex)
        assertEquals(ids, repository.preferences.first().tasbihPhrases.map(TasbihPhrase::id))
    }
}
