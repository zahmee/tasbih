package com.sakinah.tasbih.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phrase manager's edits, as the user met them going wrong before November 28, 2026. */
class TasbihPhraseEditingTest {
    /** A store in memory: DataStore's own file cannot be renamed over on Windows outside Android. */
    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val lock = Mutex()
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            lock.withLock { transform(state.value).also { state.value = it } }
    }

    private val repository = AppPreferencesRepository(MemoryStore()) {
        LocalDate.parse("2026-11-28").atStartOfDay(ZoneId.of("Asia/Riyadh"))
    }
    private val builtIn = DhikrCatalog.builtInTasbihPhrases
    private suspend fun prefs() = repository.preferences.first()
    private suspend fun texts() = prefs().tasbihPhrases.map { normalizeArabicForSearch(it.text) }

    @Test fun `editing a built-in phrase replaces it in its place, without a second copy`() = runBlocking {
        val second = builtIn[1]
        repository.selectPhrase(second)
        repeat(4) { repository.incrementCounter() }
        assertEquals(PhraseEditResult.Saved, repository.editPhrase(second.id, "الحمد لله رب العالمين", 10))
        val after = prefs()
        assertEquals(builtIn.size, after.tasbihPhrases.size)
        val replaced = after.tasbihPhrases[1]
        assertEquals("الحمد لله رب العالمين", replaced.text)
        assertTrue(replaced.isCustom)
        assertEquals(replaced.id, after.selectedPhraseId)
        assertEquals(4, after.tasbihCount)
        assertEquals(10, after.tasbihTarget)
    }

    @Test fun `changing only the goal of a built-in phrase does not show it twice`() = runBlocking {
        val first = builtIn[0]
        assertEquals(PhraseEditResult.Saved, repository.editPhrase(first.id, first.text, 100))
        assertEquals(builtIn.size, prefs().tasbihPhrases.size)
        assertEquals(texts().toSet().size, texts().size)
        assertEquals(100, prefs().tasbihPhrases[0].defaultGoal)
    }

    @Test fun `words another phrase has are refused, and adding them selects that phrase`() = runBlocking {
        val custom = repository.addCustomPhrase("أستغفر الله", 100)
        assertEquals(PhraseEditResult.Duplicate, repository.editPhrase(custom, builtIn[0].text, 33))
        val before = prefs().tasbihPhrases
        assertEquals(builtIn[0].id, repository.addCustomPhrase("سبحان الله", 33))
        assertEquals(before, prefs().tasbihPhrases)
        assertEquals(builtIn[0].id, prefs().selectedPhraseId)
    }

    @Test fun `a deleted built-in phrase can be brought back, and adding its words brings it back too`() = runBlocking {
        repository.deletePhrase(builtIn[0].id)
        repository.deletePhrase(builtIn[2].id)
        assertFalse(prefs().tasbihPhrases.any { it.id == builtIn[0].id })
        assertEquals(2, prefs().restorableBuiltInPhrases.size)

        assertEquals(builtIn[2].id, repository.addCustomPhrase(builtIn[2].text, 34))
        assertEquals(1, prefs().restorableBuiltInPhrases.size)

        repository.restoreBuiltInPhrases()
        assertEquals(builtIn.size, prefs().tasbihPhrases.size)
        assertTrue(prefs().restorableBuiltInPhrases.isEmpty())
    }

    @Test fun `an edited built-in phrase is not brought back beside its edit when the words are the same`() = runBlocking {
        repository.editPhrase(builtIn[0].id, builtIn[0].text, 50)
        assertTrue(prefs().restorableBuiltInPhrases.isEmpty())
        repository.restoreBuiltInPhrases()
        assertEquals(texts().toSet().size, texts().size)
    }

    @Test fun `deleting the user's own phrase removes it, and the last phrase is never removed`() = runBlocking {
        val custom = repository.addCustomPhrase("ذكر للحذف", 7)
        repository.deletePhrase(custom)
        assertFalse(prefs().tasbihPhrases.any { it.id == custom })
        prefs().tasbihPhrases.forEach { repository.deletePhrase(it.id) }
        assertEquals(1, prefs().tasbihPhrases.size)
    }
}
