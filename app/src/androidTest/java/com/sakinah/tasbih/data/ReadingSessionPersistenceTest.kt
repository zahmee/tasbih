package com.sakinah.tasbih.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class ReadingSessionPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val folder = TemporaryFolder(context.cacheDir)
    private var today = "2026-09-11"
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preferencesFile by lazy { File(folder.root, "reader.preferences_pb") }
    private var store = PreferenceDataStoreFactory.create(scope = scope) { preferencesFile }
    private var preferences = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
    private val database by lazy {
        Room.databaseBuilder(context, SakinahDatabase::class.java, File(folder.root, "events.db").absolutePath)
            .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    }
    private val dao get() = database.activityDao()
    private val activity by lazy { ActivityRepository(dao) }
    private lateinit var sessions: ReadingSessionRepository
    @Before fun initialize() { sessions = ReadingSessionRepository(preferences, activity) }
    private val collection = DhikrCollection("hisn_001", 1, "اختبار", null,
        listOf(DhikrEntry("one", "hisn_001", "ذكر أول", 1, ""), DhikrEntry("two", "hisn_001", "ذكر ثان", 2, "")))
    private suspend fun progress() = preferences.preferences.first().readingProgress[collection.id] ?: ReadingProgress()
    @After fun close() = runBlocking { scope.cancel(); scope.coroutineContext[Job]!!.join(); database.close() }

    @Test fun undoAfterAdvanceSurvivesStorageReopenAndRemovesOnlyItsEvent() = runBlocking {
        activity.recordTasbih(DhikrCatalog.builtInTasbihPhrases.first())
        sessions.increment(collection)
        sessions.advance(collection)
        assertEquals(1, progress().entryIndex)
        scope.cancel(); scope.coroutineContext[Job]!!.join()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope) { preferencesFile }
        preferences = AppPreferencesRepository(store) { java.time.LocalDate.parse(today).atStartOfDay(java.time.ZoneId.of("Asia/Riyadh")) }
        sessions = ReadingSessionRepository(preferences, activity)
        sessions.undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertEquals(1, dao.observeTotals().first().totalCount)
        assertEquals(ActivityKinds.Tasbih, dao.observeRecent().first().single().kind)
        sessions.undo(collection.id)
        assertEquals(1, dao.observeTotals().first().totalCount)
    }

    @Test fun undoWholeSessionCompletionCorrectsHistoryAndCanCompleteAgain() = runBlocking {
        sessions.increment(collection); sessions.advance(collection)
        sessions.increment(collection); sessions.increment(collection); sessions.advance(collection)
        assertTrue(progress().completed)
        assertEquals(1, dao.observeTotals().first().completions)
        sessions.undo(collection.id)
        assertFalse(progress().completed)
        assertEquals(1, progress().entryIndex)
        assertEquals(1, progress().repetitionCountFor(1))
        assertEquals(2, dao.observeTotals().first().readerCount)
        assertEquals(0, dao.observeTotals().first().completions)
        sessions.increment(collection); sessions.advance(collection)
        assertTrue(progress().completed)
        assertEquals(1, dao.observeTotals().first().completions)
    }

    @Test fun rapidTapsCannotOvercountAndSearchDoesNotEraseProgress() = runBlocking {
        sessions.openEntry(collection, "two")
        coroutineScope { repeat(10) { launch { sessions.increment(collection) } } }
        assertEquals(2, progress().repetitionCountFor(1))
        assertEquals(2, dao.observeTotals().first().readerCount)
        sessions.openEntry(collection, "one")
        assertEquals(0, progress().entryIndex)
        assertEquals(2, progress().repetitionCountFor(1))
        assertEquals(listOf("one", "two"), preferences.preferences.first().recentEntryIds)
        sessions.increment(collection); sessions.advance(collection)
        sessions.openEntry(collection, "two")
        assertTrue(progress().completed)
        assertEquals(1, progress().entryIndex)
        assertEquals(3, dao.observeTotals().first().readerCount)
    }

    @Test fun dayRolloverExpiresUndoButKeepsHistoryFavoritesAndRecentEntries() = runBlocking {
        preferences.toggleFavorite("one")
        sessions.openEntry(collection, "one")
        sessions.increment(collection)
        today = "2026-09-12"
        preferences.rollOverDailyStateIfNeeded()
        sessions.undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertTrue(preferences.preferences.first().readerUndo.isEmpty())
        assertTrue("one" in preferences.preferences.first().favoriteEntryIds)
        assertEquals(listOf("one"), preferences.preferences.first().recentEntryIds)
        assertEquals(1, dao.observeTotals().first().readerCount)
    }

    @Test fun recentEntriesStayUniqueBoundedAndNewestFirst() = runBlocking {
        repeat(15) { preferences.recordVisitedEntry("entry_$it") }
        preferences.recordVisitedEntry("entry_7")
        val ids = preferences.preferences.first().recentEntryIds
        assertEquals(12, ids.size)
        assertEquals(12, ids.distinct().size)
        assertEquals("entry_7", ids.first())
        assertEquals("entry_14", ids[1])
    }
    @Test fun midnightDuringEventWriteRetriesAgainstTheNewDailyState() = runBlocking {
        sessions.openEntry(collection, "two")
        sessions.increment(collection)
        var crossed = false
        val crossingDao = object : ActivityDao by dao {
            override suspend fun insert(event: ActivityEvent): Long {
                val id = dao.insert(event)
                if (!crossed) { crossed = true; today = "2026-09-12" }
                return id
            }
        }
        val crossingSession = ReadingSessionRepository(preferences, ActivityRepository(crossingDao))
        crossingSession.increment(collection)
        val current = progress()
        assertEquals(0, current.entryIndex)
        assertEquals(1, current.repetitionCountFor(0))
        assertEquals(0, current.repetitionCountFor(1))
        assertEquals(2, dao.observeTotals().first().readerCount)
        assertEquals("2026-09-12", preferences.preferences.first().readerUndo.getValue(collection.id).dayKey)
    }

    @Test fun midnightDuringUndoPreservesYesterdayHistoryAndDoesNotRestoreItsCounts() = runBlocking {
        sessions.increment(collection)
        val crossingDao = object : ActivityDao by dao {
            override suspend fun eventsById(ids: List<Long>): List<ActivityEvent> {
                val events = dao.eventsById(ids)
                today = "2026-09-12"
                return events
            }
        }
        ReadingSessionRepository(preferences, ActivityRepository(crossingDao)).undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertTrue(preferences.preferences.first().readerUndo.isEmpty())
        assertEquals(1, dao.observeTotals().first().readerCount)
    }

}
