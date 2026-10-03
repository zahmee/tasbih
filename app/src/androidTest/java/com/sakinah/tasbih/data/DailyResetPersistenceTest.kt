package com.sakinah.tasbih.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.ZonedDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

class DailyResetPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val folder = TemporaryFolder(context.cacheDir)
    private var now = ZonedDateTime.parse("2026-09-11T16:00:00+03:00[Asia/Riyadh]")
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val file by lazy { File(folder.root, "daily.preferences_pb") }
    private var store = PreferenceDataStoreFactory.create(scope = scope) { file }
    private var preferences = AppPreferencesRepository(store) { now }
    private val database by lazy {
        Room.databaseBuilder(context, SakinahDatabase::class.java, File(folder.root, "daily-events.db").absolutePath)
            .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    }
    private val dao get() = database.activityDao()
    private val activity by lazy { ActivityRepository(dao) }
    private val sessions get() = ReadingSessionRepository(preferences, activity)
    private val collection = DhikrCollection("hisn_027", 27, "اختبار", null,
        listOf(DhikrEntry("one", "hisn_027", "ذكر أول", 5, ""), DhikrEntry("two", "hisn_027", "ذكر ثان", 5, "")))
    private val firstPhrase = DhikrCatalog.builtInTasbihPhrases[0]
    private val secondPhrase = DhikrCatalog.builtInTasbihPhrases[1]
    private suspend fun saved() = preferences.preferences.first()
    private suspend fun progress() = saved().readingProgress[collection.id] ?: ReadingProgress()

    @After fun close() = runBlocking { scope.cancel(); scope.coroutineContext[Job]!!.join(); database.close() }

    private suspend fun reopen() {
        scope.cancel(); scope.coroutineContext[Job]!!.join()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope) { file }
        preferences = AppPreferencesRepository(store) { now }
    }

    @Test fun legacySettingsRemainEnabledAtMidnightWithoutResettingSavedCounts() = runBlocking {
        store.edit {
            it[stringPreferencesKey("daily_state_day")] = "2026-09-11"
            it[intPreferencesKey("tasbih_counter")] = 17
        }
        assertEquals(DailyResetSettings(), saved().dailyReset)
        assertEquals(17, saved().tasbihCount)
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        assertEquals(17, saved().tasbihCount)
        now = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        assertEquals(0, saved().tasbihCount)
        assertTrue(preferences.rollOverDailyStateIfNeeded())
    }

    @Test fun customTimeSurvivesReopeningAndResetsAllPhrasesAndDailyAdhkarOnce() = runBlocking {
        repeat(3) { preferences.incrementCounter() }
        preferences.selectPhrase(secondPhrase)
        repeat(2) { preferences.incrementCounter() }
        sessions.increment(collection)
        preferences.saveReadingProgress("hisn_035", ReadingProgress(entryIndex = 3))
        preferences.toggleFavorite("one")
        preferences.recordVisitedEntry("one")
        preferences.setDailyResetTime(270)
        assertEquals(2, saved().tasbihCount)
        assertEquals(1, progress().repetitionCountFor(0))
        reopen()
        assertEquals(270, saved().dailyReset.minuteOfDay)
        now = now.plusDays(1).withHour(0)
        assertEquals(2, saved().tasbihCount)
        assertEquals(1, progress().repetitionCountFor(0))
        now = now.withHour(4).withMinute(29).withSecond(59)
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        now = now.plusSeconds(1)
        assertEquals(0, saved().tasbihCount)
        assertEquals(ReadingProgress(), progress())
        assertTrue(preferences.rollOverDailyStateIfNeeded())
        preferences.incrementCounter()
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        assertEquals(1, saved().tasbihCount)
        preferences.selectPhrase(firstPhrase)
        assertEquals(0, saved().tasbihCount)
        assertEquals(ReadingProgress(entryIndex = 3), saved().readingProgress["hisn_035"])
        assertEquals(setOf("one"), saved().favoriteEntryIds)
        assertEquals(listOf("one"), saved().recentEntryIds)
        assertEquals(1, dao.observeTotals().first().readerCount)
    }

    @Test fun eachSwitchControlsOnlyItsOwnCounters() = runBlocking {
        preferences.incrementCounter()
        sessions.increment(collection)
        preferences.setTasbihDailyReset(false)
        now = now.plusDays(1)
        preferences.rollOverDailyStateIfNeeded()
        assertEquals(1, saved().tasbihCount)
        assertEquals(ReadingProgress(), progress())
        preferences.setAdhkarDailyReset(false)
        preferences.setTasbihDailyReset(true)
        sessions.increment(collection)
        now = now.plusDays(1)
        preferences.rollOverDailyStateIfNeeded()
        assertEquals(0, saved().tasbihCount)
        assertEquals(1, progress().repetitionCountFor(0))
    }

    @Test fun disabledResetKeepsCountsAndUndoAcrossMissedDaysAndManualResetStillWorks() = runBlocking {
        preferences.setTasbihDailyReset(false)
        preferences.setAdhkarDailyReset(false)
        preferences.incrementCounter()
        sessions.increment(collection)
        now = now.plusDays(5)
        reopen()
        assertEquals(DailyResetSettings(false, false), saved().dailyReset)
        assertEquals(1, saved().tasbihCount)
        assertEquals(1, progress().repetitionCountFor(0))
        sessions.undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertEquals(0, dao.observeTotals().first().readerCount)
        preferences.resetCounter()
        assertEquals(0, saved().tasbihCount)
        sessions.increment(collection)
        sessions.restart(collection.id)
        assertEquals(ReadingProgress(), progress())
    }

    @Test fun reEnablingAndMovingTheTimeNeverResetsCurrentCountsImmediately() = runBlocking {
        preferences.setTasbihDailyReset(false)
        preferences.setAdhkarDailyReset(false)
        repeat(4) { preferences.incrementCounter() }
        sessions.increment(collection)
        now = now.plusDays(3)
        preferences.setTasbihDailyReset(true)
        preferences.setAdhkarDailyReset(true)
        preferences.setDailyResetTime(18 * 60)
        assertEquals(4, saved().tasbihCount)
        assertEquals(1, progress().repetitionCountFor(0))
        preferences.setDailyResetTime(4 * 60)
        assertEquals(4, saved().tasbihCount)
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        now = now.plusDays(1).withHour(3).withMinute(59)
        assertEquals(4, saved().tasbihCount)
        now = now.plusMinutes(1)
        assertEquals(0, saved().tasbihCount)
        assertEquals(ReadingProgress(), progress())
    }

    @Test fun backwardsClockChangeCannotWipeOrResetTheSameDayTwice() = runBlocking {
        preferences.incrementCounter()
        now = now.plusDays(1)
        preferences.rollOverDailyStateIfNeeded()
        preferences.incrementCounter()
        now = now.minusDays(1)
        assertEquals(1, saved().tasbihCount)
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        now = now.plusDays(1)
        assertFalse(preferences.rollOverDailyStateIfNeeded())
        assertEquals(1, saved().tasbihCount)
    }

    @Test fun customBoundaryDuringEventWriteRetriesWithoutRestoringTheOldSession() = runBlocking {
        now = now.withHour(4).withMinute(29).withSecond(59)
        preferences.setDailyResetTime(270)
        sessions.openEntry(collection, "two")
        sessions.increment(collection)
        var crossed = false
        val crossingDao = object : ActivityDao by dao {
            override suspend fun insert(event: ActivityEvent): Long {
                val id = dao.insert(event)
                if (!crossed) { crossed = true; now = now.plusSeconds(1) }
                return id
            }
        }
        ReadingSessionRepository(preferences, ActivityRepository(crossingDao)).increment(collection)
        assertEquals(0, progress().entryIndex)
        assertEquals(1, progress().repetitionCountFor(0))
        assertEquals(0, progress().repetitionCountFor(1))
        assertEquals(2, dao.observeTotals().first().readerCount)
        sessions.undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertEquals(1, dao.observeTotals().first().readerCount)
    }

    @Test fun scheduleChangeDuringEventWriteRejectsTheStalePolicyAndCountsOnce() = runBlocking {
        sessions.increment(collection)
        var changed = false
        val changingDao = object : ActivityDao by dao {
            override suspend fun insert(event: ActivityEvent): Long {
                val id = dao.insert(event)
                if (!changed) { changed = true; preferences.setDailyResetTime(270) }
                return id
            }
        }
        ReadingSessionRepository(preferences, ActivityRepository(changingDao)).increment(collection)
        assertEquals(2, progress().repetitionCountFor(0))
        assertEquals(2, dao.observeTotals().first().readerCount)
        sessions.undo(collection.id)
        assertEquals(1, progress().repetitionCountFor(0))
        assertEquals(1, dao.observeTotals().first().readerCount)
    }

    @Test fun customBoundaryDuringUndoKeepsHistoryAndDoesNotRestoreExpiredCounts() = runBlocking {
        now = now.withHour(4).withMinute(29).withSecond(59)
        preferences.setDailyResetTime(270)
        sessions.increment(collection)
        val crossingDao = object : ActivityDao by dao {
            override suspend fun eventsById(ids: List<Long>): List<ActivityEvent> {
                val events = dao.eventsById(ids)
                now = now.plusSeconds(1)
                return events
            }
        }
        ReadingSessionRepository(preferences, ActivityRepository(crossingDao)).undo(collection.id)
        assertEquals(ReadingProgress(), progress())
        assertTrue(saved().readerUndo.isEmpty())
        assertEquals(1, dao.observeTotals().first().readerCount)
    }
}
