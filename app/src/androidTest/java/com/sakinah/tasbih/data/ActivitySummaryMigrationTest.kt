package com.sakinah.tasbih.data

import androidx.room3.executeSQL
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ActivitySummaryMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val folder = TemporaryFolder(context.cacheDir)
    private val zone = ZoneId.systemDefault()
    private val phrases = DhikrCatalog.builtInTasbihPhrases.take(3)
    private val collection = DhikrCollection("hisn_001", 1, "اختبار", null,
        listOf(DhikrEntry("one", "hisn_001", "ذكر أول", 1, ""), DhikrEntry("two", "hisn_001", "ذكر ثان", 2, "")))

    @Test fun migrationKeepsEveryEventAndShowsTheSameStatisticsAsVersionOne() = runBlocking {
        val file = File(folder.root, "activity-v1.db")
        val connection = AndroidSQLiteDriver().open(file.absolutePath)
        val (eventsBefore, statisticsBefore) = try {
            createVersionOne(connection)
            versionOneEvents().forEach { connection.insertVersionOne(it) }
            val sql = RawSql(connection)
            sql.events() to sql.statistics(VersionOneHour)
        } finally {
            connection.close()
        }

        val database = SakinahDatabase.open(context, file.absolutePath)
        try {
            val dao = database.activityDao()
            val sql = RoomSql(database)
            assertEquals(statisticsBefore, dao.statistics())
            assertEquals(eventsBefore, sql.events())
            assertEquals(statisticsBefore, sql.statistics(StoredHour))

            // Undo must subtract from the hour the migration assigned to the old event, and new
            // taps must add to summary rows the migration already created.
            val repository = ActivityRepository(dao)
            val migratedHour = LocalDate.parse("2026-10-01").atTime(5, 40).atZone(zone).toInstant().toEpochMilli()
            repository.undoTasbih(phrases[0].id)
            repository.recordTasbih(phrases[0], migratedHour)
            repository.recordTasbih(phrases[0], migratedHour)
            repository.recordTasbih(phrases[1])
            assertEquals(eventsBefore.size + 2, sql.events().size)
            assertEquals(sql.statistics(StoredHour), dao.statistics())
        } finally {
            database.close()
        }
    }

    @Test fun summariesStayExactThroughRecordsUndoAndRestore() = runBlocking {
        val database = SakinahDatabase.open(context, File(folder.root, "activity-v2.db").absolutePath)
        try {
            val dao = database.activityDao()
            val sql = RoomSql(database)
            val repository = ActivityRepository(dao)
            val random = Random(20261024)
            val start = LocalDate.parse("2026-10-20").atStartOfDay(zone).toInstant().toEpochMilli()
            val readerUndo = mutableListOf<ReaderUndo>()
            val removed = mutableListOf<List<ActivityEvent>>()
            repeat(400) {
                val time = start + random.nextLong(3 * 24 * 60 * 60 * 1000L)
                when (random.nextInt(10)) {
                    in 0..3 -> repository.recordTasbih(phrases.random(random), time)
                    4 -> repository.undoTasbih(phrases.random(random).id)
                    in 5..6 -> {
                        val entry = collection.entries.random(random)
                        val eventId = repository.recordReader(entry, collection.title, time)
                        val completionId = if (random.nextBoolean()) repository.recordCompletion(collection, time) else 0
                        readerUndo += ReaderUndo(ReadingProgress(), eventId, completionId, dayKey = "2026-10-20")
                    }
                    in 7..8 -> if (readerUndo.isNotEmpty()) {
                        removed += repository.removeReaderAction(readerUndo.removeAt(random.nextInt(readerUndo.size)))
                    }
                    else -> if (removed.isNotEmpty()) repository.restoreEvents(removed.removeAt(random.nextInt(removed.size)))
                }
            }

            assertTrue(sql.events().isNotEmpty())
            assertEquals(sql.statistics(StoredHour), dao.statistics())
            assertEquals(0L, sql.single("SELECT COUNT(*) FROM activity_hour_summaries WHERE totalCount <= 0 AND completions <= 0"))
            assertEquals(0L, sql.single("SELECT COUNT(*) FROM tasbih_phrase_day_summaries WHERE count <= 0"))

            // Removing every event leaves no statistics behind.
            database.useWriterConnection { it.executeSQL("DELETE FROM activity_events") }
            assertEquals(Statistics(emptyList(), emptyList(), ActivityTotals(), phrases.associate { it.id to emptyList() }), dao.statistics())
        } finally {
            database.close()
        }
    }

    private data class Statistics(
        val daily: List<DailyActivity>,
        val hourly: List<HourlyActivity>,
        val totals: ActivityTotals,
        val phrases: Map<String, List<TasbihPhraseDailyActivity>>,
    )

    private suspend fun ActivityDao.statistics() = Statistics(
        daily = observeDailyActivity().first(),
        hourly = observeHourlyActivity().first(),
        totals = observeTotals().first(),
        phrases = phrases.associate { it.id to observeTasbihPhraseDaily(it.id).first() },
    )

    /** The version 1 read queries, run directly over the event rows. */
    private suspend fun Sql.statistics(hour: String) = Statistics(
        daily = rows(
            """
            SELECT dayKey,
                COALESCE(SUM(CASE WHEN kind != 'completion' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'completion' THEN 1 ELSE 0 END), 0)
            FROM activity_events GROUP BY dayKey ORDER BY dayKey ASC
            """,
        ) { DailyActivity(it.getText(0), it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4)) },
        hourly = rows(
            """
            SELECT dayKey, $hour AS hourOfDay,
                COALESCE(SUM(amount), 0),
                COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0)
            FROM activity_events WHERE kind != 'completion'
            GROUP BY dayKey, hourOfDay ORDER BY dayKey ASC, hourOfDay ASC
            """,
        ) { HourlyActivity(it.getText(0), it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4)) },
        totals = rows(
            """
            SELECT
                COALESCE(SUM(CASE WHEN kind != 'completion' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN kind = 'completion' THEN 1 ELSE 0 END), 0),
                COUNT(DISTINCT CASE WHEN kind != 'completion' THEN dayKey END),
                COALESCE(MIN(timestampMillis), 0)
            FROM activity_events
            """,
        ) { ActivityTotals(it.getInt(0), it.getInt(1), it.getInt(2), it.getInt(3), it.getInt(4), it.getLong(5)) }.single(),
        phrases = phrases.associate { phrase ->
            phrase.id to rows(
                """
                SELECT dayKey, COALESCE(SUM(amount), 0) FROM activity_events
                WHERE kind = 'tasbih' AND sourceId = '${phrase.id}'
                GROUP BY dayKey ORDER BY dayKey ASC
                """,
            ) { TasbihPhraseDailyActivity(it.getText(0), it.getInt(1)) }
        },
    )

    private suspend fun Sql.events() = rows(
        "SELECT id, timestampMillis, dayKey, kind, sourceId, title, amount FROM activity_events ORDER BY id",
    ) { ActivityEvent(it.getLong(0), it.getLong(1), it.getText(2), it.getText(3), it.getText(4), it.getText(5), it.getInt(6)) }

    private suspend fun Sql.single(query: String): Long = rows(query) { it.getLong(0) }.single()

    /** Four days across several hours, both phrases, readers, completions and a completion-only day. */
    private fun versionOneEvents(): List<ActivityEvent> {
        val events = mutableListOf<ActivityEvent>()
        fun add(day: String, hour: Int, minute: Int, kind: String, sourceId: String, amount: Int = 1) {
            val time = LocalDate.parse(day).atTime(hour, minute).atZone(zone)
            events += ActivityEvent(
                timestampMillis = time.toInstant().toEpochMilli(),
                dayKey = time.toLocalDate().toString(),
                kind = kind,
                sourceId = sourceId,
                title = sourceId,
                amount = amount,
            )
        }
        repeat(33) { add("2026-10-01", 5, it, ActivityKinds.Tasbih, phrases[0].id) }
        repeat(10) { add("2026-10-01", 21, it, ActivityKinds.Tasbih, phrases[1].id) }
        repeat(3) { add("2026-10-01", 6, it, ActivityKinds.Reader, "hisn_001_01") }
        add("2026-10-01", 6, 30, ActivityKinds.Completion, collection.id, amount = 0)
        repeat(7) { add("2026-10-02", 23, it, ActivityKinds.Tasbih, phrases[0].id) }
        add("2026-10-03", 9, 0, ActivityKinds.Completion, collection.id, amount = 0)
        repeat(100) { add("2026-10-04", it % 24, it / 24, ActivityKinds.Tasbih, phrases[it % 2].id) }
        return events
    }

    private fun createVersionOne(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `activity_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`timestampMillis` INTEGER NOT NULL, `dayKey` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` INTEGER NOT NULL)",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_timestampMillis` ON `activity_events` (`timestampMillis`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_dayKey` ON `activity_events` (`dayKey`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_kind_sourceId` ON `activity_events` (`kind`, `sourceId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '716551e24de8dfcd4c00af0a7c2b0ecd')")
        connection.execSQL("PRAGMA user_version = 1")
    }

    private fun SQLiteConnection.insertVersionOne(event: ActivityEvent) {
        prepare("INSERT INTO activity_events (timestampMillis, dayKey, kind, sourceId, title, amount) VALUES (?, ?, ?, ?, ?, ?)").use {
            it.bindLong(1, event.timestampMillis)
            it.bindText(2, event.dayKey)
            it.bindText(3, event.kind)
            it.bindText(4, event.sourceId)
            it.bindText(5, event.title)
            it.bindInt(6, event.amount)
            it.step()
        }
    }

    private interface Sql {
        suspend fun <T> rows(query: String, read: (SQLiteStatement) -> T): List<T>
    }

    private class RawSql(private val connection: SQLiteConnection) : Sql {
        override suspend fun <T> rows(query: String, read: (SQLiteStatement) -> T): List<T> =
            connection.prepare(query).use { it.collect(read) }
    }

    private class RoomSql(private val database: SakinahDatabase) : Sql {
        override suspend fun <T> rows(query: String, read: (SQLiteStatement) -> T): List<T> =
            database.useReaderConnection { connection -> connection.usePrepared(query) { it.collect(read) } }
    }

    private companion object {
        const val VersionOneHour = "CAST(strftime('%H', timestampMillis / 1000, 'unixepoch', 'localtime') AS INTEGER)"
        const val StoredHour = "hourOfDay"

        fun <T> SQLiteStatement.collect(read: (SQLiteStatement) -> T): List<T> =
            buildList { while (step()) add(read(this@collect)) }
    }
}
