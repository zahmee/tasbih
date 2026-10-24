package com.sakinah.tasbih.data

import android.content.Context
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object ActivityKinds {
    const val Tasbih = "tasbih"
    const val Reader = "reader"
    const val Completion = "completion"
}

@Entity(
    tableName = "activity_events",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["kind", "sourceId", "timestampMillis"]),
    ],
)
data class ActivityEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val dayKey: String,
    val kind: String,
    val sourceId: String,
    val title: String,
    val amount: Int = 1,
    // Stored with the event so an undo after a time-zone change subtracts from the same hour.
    @ColumnInfo(defaultValue = "0") val hourOfDay: Int = 0,
)

/**
 * Running totals per local hour, kept in step with [ActivityEvent] rows by SQLite triggers.
 * Reading statistics from here costs one row per active hour instead of one row per tap.
 */
@Entity(tableName = "activity_hour_summaries", primaryKeys = ["dayKey", "hourOfDay"])
data class ActivityHourSummary(
    val dayKey: String,
    val hourOfDay: Int,
    val totalCount: Int,
    val tasbihCount: Int,
    val readerCount: Int,
    val completions: Int,
)

@Entity(tableName = "tasbih_phrase_day_summaries", primaryKeys = ["sourceId", "dayKey"])
data class TasbihPhraseDaySummary(
    val sourceId: String,
    val dayKey: String,
    val count: Int,
)

data class DailyActivity(
    val dayKey: String,
    val totalCount: Int,
    val tasbihCount: Int,
    val readerCount: Int,
    val completions: Int,
)

data class HourlyActivity(
    val dayKey: String,
    val hourOfDay: Int,
    val totalCount: Int,
    val tasbihCount: Int,
    val readerCount: Int,
)

data class HourlyActivityTotal(
    val hourOfDay: Int,
    val totalCount: Int,
    val tasbihCount: Int,
    val readerCount: Int,
)

data class PeriodActivity(
    val periodKey: String,
    val totalCount: Int,
    val tasbihCount: Int,
    val readerCount: Int,
    val completions: Int,
    val activeDays: Int,
)

data class ActivityTotals(
    val totalCount: Int = 0,
    val tasbihCount: Int = 0,
    val readerCount: Int = 0,
    val completions: Int = 0,
    val activeDays: Int = 0,
    val firstActivityMillis: Long = 0,
)

data class TasbihPhraseDailyActivity(
    val dayKey: String,
    val count: Int,
)

data class TasbihPhraseAnalytics(
    val sourceId: String = "",
    val daily: List<TasbihPhraseDailyActivity> = emptyList(),
) {
    val totalCount: Int
        get() = daily.sumOf(TasbihPhraseDailyActivity::count)

    val activeDays: Int
        get() = daily.count { it.count > 0 }

    fun countFor(date: LocalDate): Int = daily.firstOrNull { it.dayKey == date.toString() }?.count ?: 0

    fun countBetween(startInclusive: LocalDate, endInclusive: LocalDate): Int = daily
        .asSequence()
        .filter { it.dayKey.isInside(startInclusive, endInclusive) }
        .sumOf(TasbihPhraseDailyActivity::count)

    fun bestDay(): TasbihPhraseDailyActivity? = daily.maxWithOrNull(
        compareBy<TasbihPhraseDailyActivity> { it.count }.thenBy { it.dayKey },
    )
}

data class ActivityAnalytics(
    val daily: List<DailyActivity> = emptyList(),
    val hourly: List<HourlyActivity> = emptyList(),
    val totals: ActivityTotals = ActivityTotals(),
    val recent: List<ActivityEvent> = emptyList(),
) {
    fun activityFor(date: LocalDate): DailyActivity? = daily.firstOrNull { it.dayKey == date.toString() }

    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        val activeDates = daily.asSequence()
            .filter { it.totalCount > 0 }
            .mapNotNull { runCatching { LocalDate.parse(it.dayKey) }.getOrNull() }
            .toSet()
        var cursor = if (today in activeDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in activeDates) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun longestStreak(): Int {
        val dates = daily.asSequence()
            .filter { it.totalCount > 0 }
            .mapNotNull { runCatching { LocalDate.parse(it.dayKey) }.getOrNull() }
            .distinct()
            .sorted()
            .toList()
        var longest = 0
        var running = 0
        var previous: LocalDate? = null
        dates.forEach { date ->
            running = if (previous?.plusDays(1) == date) running + 1 else 1
            longest = maxOf(longest, running)
            previous = date
        }
        return longest
    }

    fun summaryBetween(
        startInclusive: LocalDate? = null,
        endInclusive: LocalDate? = null,
    ): ActivityTotals {
        val matchingDays = daily.filter { activity ->
            activity.dayKey.isInside(startInclusive, endInclusive)
        }
        return ActivityTotals(
            totalCount = matchingDays.sumOf(DailyActivity::totalCount),
            tasbihCount = matchingDays.sumOf(DailyActivity::tasbihCount),
            readerCount = matchingDays.sumOf(DailyActivity::readerCount),
            completions = matchingDays.sumOf(DailyActivity::completions),
            activeDays = matchingDays.count { it.totalCount > 0 },
            firstActivityMillis = 0,
        )
    }

    fun hourlyTotalsBetween(
        startInclusive: LocalDate? = null,
        endInclusive: LocalDate? = null,
    ): List<HourlyActivityTotal> {
        val byHour = hourly.asSequence()
            .filter { activity -> activity.dayKey.isInside(startInclusive, endInclusive) }
            .groupBy(HourlyActivity::hourOfDay)
        return (0..23).map { hour ->
            val activities = byHour[hour].orEmpty()
            HourlyActivityTotal(
                hourOfDay = hour,
                totalCount = activities.sumOf(HourlyActivity::totalCount),
                tasbihCount = activities.sumOf(HourlyActivity::tasbihCount),
                readerCount = activities.sumOf(HourlyActivity::readerCount),
            )
        }
    }

    fun monthlyActivity(): List<PeriodActivity> = aggregatePeriods { activity ->
        activity.dayKey.take(7)
    }

    fun yearlyActivity(): List<PeriodActivity> = aggregatePeriods { activity ->
        activity.dayKey.take(4)
    }

    private fun aggregatePeriods(keyFor: (DailyActivity) -> String): List<PeriodActivity> =
        daily.groupBy(keyFor)
            .map { (periodKey, activities) ->
                PeriodActivity(
                    periodKey = periodKey,
                    totalCount = activities.sumOf(DailyActivity::totalCount),
                    tasbihCount = activities.sumOf(DailyActivity::tasbihCount),
                    readerCount = activities.sumOf(DailyActivity::readerCount),
                    completions = activities.sumOf(DailyActivity::completions),
                    activeDays = activities.count { it.totalCount > 0 },
                )
            }
            .sortedBy(PeriodActivity::periodKey)
}

private fun String.isInside(startInclusive: LocalDate?, endInclusive: LocalDate?): Boolean {
    val startKey = startInclusive?.toString()
    val endKey = endInclusive?.toString()
    return (startKey == null || this >= startKey) && (endKey == null || this <= endKey)
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(event: ActivityEvent): Long

    @Query("DELETE FROM activity_events WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<Long>)

    @Query("SELECT * FROM activity_events WHERE id IN (:ids)")
    suspend fun eventsById(ids: List<Long>): List<ActivityEvent>

    @Query(
        """
        DELETE FROM activity_events
        WHERE id = (
            SELECT id FROM activity_events
            WHERE kind = :kind AND sourceId = :sourceId
            ORDER BY timestampMillis DESC, id DESC
            LIMIT 1
        )
        """,
    )
    suspend fun deleteLatest(kind: String, sourceId: String)

    @Query(
        """
        SELECT
            dayKey,
            SUM(totalCount) AS totalCount,
            SUM(tasbihCount) AS tasbihCount,
            SUM(readerCount) AS readerCount,
            SUM(completions) AS completions
        FROM activity_hour_summaries
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """,
    )
    fun observeDailyActivity(): Flow<List<DailyActivity>>

    @Query(
        """
        SELECT dayKey, hourOfDay, totalCount, tasbihCount, readerCount
        FROM activity_hour_summaries
        WHERE totalCount > 0
        ORDER BY dayKey ASC, hourOfDay ASC
        """,
    )
    fun observeHourlyActivity(): Flow<List<HourlyActivity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(totalCount), 0) AS totalCount,
            COALESCE(SUM(tasbihCount), 0) AS tasbihCount,
            COALESCE(SUM(readerCount), 0) AS readerCount,
            COALESCE(SUM(completions), 0) AS completions,
            COUNT(DISTINCT CASE WHEN totalCount > 0 THEN dayKey END) AS activeDays,
            COALESCE((SELECT MIN(timestampMillis) FROM activity_events), 0) AS firstActivityMillis
        FROM activity_hour_summaries
        """,
    )
    fun observeTotals(): Flow<ActivityTotals>

    @Query("SELECT * FROM activity_events ORDER BY timestampMillis DESC, id DESC LIMIT 120")
    fun observeRecent(): Flow<List<ActivityEvent>>

    @Query(
        """
        SELECT dayKey, count
        FROM tasbih_phrase_day_summaries
        WHERE sourceId = :sourceId
        ORDER BY dayKey ASC
        """,
    )
    fun observeTasbihPhraseDaily(sourceId: String): Flow<List<TasbihPhraseDailyActivity>>
}

@Database(
    entities = [ActivityEvent::class, ActivityHourSummary::class, TasbihPhraseDaySummary::class],
    version = 2,
    exportSchema = true,
)
abstract class SakinahDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var instance: SakinahDatabase? = null

        fun get(context: Context): SakinahDatabase = instance ?: synchronized(this) {
            instance ?: open(context, "sakinah_activity.db").also { instance = it }
        }

        /** Every database, including test copies, needs the migration and the summary triggers. */
        internal fun open(context: Context, name: String): SakinahDatabase = Room.databaseBuilder(
            context.applicationContext,
            SakinahDatabase::class.java,
            name,
        )
            .setQueryCoroutineContext(Dispatchers.IO)
            .setDriver(AndroidSQLiteDriver())
            .addMigrations(MigrationAddActivitySummaries)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override suspend fun onCreate(connection: SQLiteConnection) {
                        createActivitySummaryTriggers(connection)
                    }
                },
            )
            .build()
    }
}

/**
 * Moves statistics from per-tap aggregation to running summaries without losing history:
 * every existing event is kept and folded into the new summary tables in one transaction.
 */
internal val MigrationAddActivitySummaries = Migration(1, 2) { connection ->
    connection.execSQL("ALTER TABLE `activity_events` ADD COLUMN `hourOfDay` INTEGER NOT NULL DEFAULT 0")
    // Version 1 derived the hour on every read from the device time zone; freeze that same value.
    connection.execSQL(
        "UPDATE `activity_events` SET `hourOfDay` = " +
            "CAST(strftime('%H', `timestampMillis` / 1000, 'unixepoch', 'localtime') AS INTEGER)",
    )
    connection.execSQL("DROP INDEX IF EXISTS `index_activity_events_dayKey`")
    connection.execSQL("DROP INDEX IF EXISTS `index_activity_events_kind_sourceId`")
    connection.execSQL(
        "CREATE INDEX IF NOT EXISTS `index_activity_events_kind_sourceId_timestampMillis` " +
            "ON `activity_events` (`kind`, `sourceId`, `timestampMillis`)",
    )
    connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `activity_hour_summaries` (`dayKey` TEXT NOT NULL, " +
            "`hourOfDay` INTEGER NOT NULL, `totalCount` INTEGER NOT NULL, `tasbihCount` INTEGER NOT NULL, " +
            "`readerCount` INTEGER NOT NULL, `completions` INTEGER NOT NULL, PRIMARY KEY(`dayKey`, `hourOfDay`))",
    )
    connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `tasbih_phrase_day_summaries` (`sourceId` TEXT NOT NULL, " +
            "`dayKey` TEXT NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`sourceId`, `dayKey`))",
    )
    connection.execSQL(
        """
        INSERT INTO activity_hour_summaries (dayKey, hourOfDay, totalCount, tasbihCount, readerCount, completions)
        SELECT
            dayKey,
            hourOfDay,
            SUM(CASE WHEN kind != 'completion' THEN amount ELSE 0 END),
            SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END),
            SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END),
            SUM(CASE WHEN kind = 'completion' THEN 1 ELSE 0 END)
        FROM activity_events
        GROUP BY dayKey, hourOfDay
        """,
    )
    connection.execSQL(
        """
        INSERT INTO tasbih_phrase_day_summaries (sourceId, dayKey, count)
        SELECT sourceId, dayKey, SUM(amount)
        FROM activity_events
        WHERE kind = 'tasbih'
        GROUP BY sourceId, dayKey
        """,
    )
    createActivitySummaryTriggers(connection)
}

/*
 * Events are only ever inserted or deleted (undo and its restore), so these two triggers keep
 * the summaries exact. They avoid UPSERT syntax, which the SQLite shipped with API 26-29 lacks,
 * and INSERT OR IGNORE, because SQLite applies the firing statement's conflict policy (Room's
 * INSERT OR ABORT) to the trigger body. Each missing row is created by a guarded insert instead.
 * Rows that drop to zero are removed so summaries list the same days and hours as the events.
 */
private fun createActivitySummaryTriggers(connection: SQLiteConnection) {
    connection.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS activity_events_summary_insert
        AFTER INSERT ON activity_events
        BEGIN
            INSERT INTO activity_hour_summaries
                (dayKey, hourOfDay, totalCount, tasbihCount, readerCount, completions)
            SELECT NEW.dayKey, NEW.hourOfDay, 0, 0, 0, 0
            WHERE NOT EXISTS (
                SELECT 1 FROM activity_hour_summaries WHERE dayKey = NEW.dayKey AND hourOfDay = NEW.hourOfDay
            );
            UPDATE activity_hour_summaries SET
                totalCount = totalCount + (CASE WHEN NEW.kind != 'completion' THEN NEW.amount ELSE 0 END),
                tasbihCount = tasbihCount + (CASE WHEN NEW.kind = 'tasbih' THEN NEW.amount ELSE 0 END),
                readerCount = readerCount + (CASE WHEN NEW.kind = 'reader' THEN NEW.amount ELSE 0 END),
                completions = completions + (CASE WHEN NEW.kind = 'completion' THEN 1 ELSE 0 END)
            WHERE dayKey = NEW.dayKey AND hourOfDay = NEW.hourOfDay;
            INSERT INTO tasbih_phrase_day_summaries (sourceId, dayKey, count)
            SELECT NEW.sourceId, NEW.dayKey, 0
            WHERE NEW.kind = 'tasbih' AND NOT EXISTS (
                SELECT 1 FROM tasbih_phrase_day_summaries WHERE sourceId = NEW.sourceId AND dayKey = NEW.dayKey
            );
            UPDATE tasbih_phrase_day_summaries SET count = count + NEW.amount
            WHERE NEW.kind = 'tasbih' AND sourceId = NEW.sourceId AND dayKey = NEW.dayKey;
        END
        """,
    )
    connection.execSQL(
        """
        CREATE TRIGGER IF NOT EXISTS activity_events_summary_delete
        AFTER DELETE ON activity_events
        BEGIN
            UPDATE activity_hour_summaries SET
                totalCount = totalCount - (CASE WHEN OLD.kind != 'completion' THEN OLD.amount ELSE 0 END),
                tasbihCount = tasbihCount - (CASE WHEN OLD.kind = 'tasbih' THEN OLD.amount ELSE 0 END),
                readerCount = readerCount - (CASE WHEN OLD.kind = 'reader' THEN OLD.amount ELSE 0 END),
                completions = completions - (CASE WHEN OLD.kind = 'completion' THEN 1 ELSE 0 END)
            WHERE dayKey = OLD.dayKey AND hourOfDay = OLD.hourOfDay;
            DELETE FROM activity_hour_summaries
            WHERE dayKey = OLD.dayKey AND hourOfDay = OLD.hourOfDay AND totalCount <= 0 AND completions <= 0;
            UPDATE tasbih_phrase_day_summaries SET count = count - OLD.amount
            WHERE OLD.kind = 'tasbih' AND sourceId = OLD.sourceId AND dayKey = OLD.dayKey;
            DELETE FROM tasbih_phrase_day_summaries
            WHERE OLD.kind = 'tasbih' AND sourceId = OLD.sourceId AND dayKey = OLD.dayKey AND count <= 0;
        END
        """,
    )
}

class ActivityRepository internal constructor(private val dao: ActivityDao) {
    constructor(context: Context) : this(SakinahDatabase.get(context).activityDao())

    val analytics: Flow<ActivityAnalytics> = combine(
        dao.observeDailyActivity(),
        dao.observeHourlyActivity(),
        dao.observeTotals(),
        dao.observeRecent(),
    ) { daily, hourly, totals, recent ->
        ActivityAnalytics(
            daily = daily,
            hourly = hourly,
            totals = totals,
            recent = recent,
        )
    }

    /** Readers opened most recently, for installs that predate the stored recent-entries list. */
    val recentReaderEntryIds: Flow<List<String>> = dao.observeRecent().map { recent ->
        recent.filter { it.kind == ActivityKinds.Reader }.map(ActivityEvent::sourceId).distinct().take(12)
    }

    suspend fun recordTasbih(phrase: TasbihPhrase, timestampMillis: Long = System.currentTimeMillis()) {
        dao.insert(
            event(
                timestampMillis = timestampMillis,
                kind = ActivityKinds.Tasbih,
                sourceId = phrase.id,
                title = phrase.text,
            ),
        )
    }

    suspend fun undoTasbih(phraseId: String) {
        dao.deleteLatest(ActivityKinds.Tasbih, phraseId)
    }

    fun observeTasbihPhraseAnalytics(sourceId: String): Flow<TasbihPhraseAnalytics> =
        dao.observeTasbihPhraseDaily(sourceId).map { daily ->
            TasbihPhraseAnalytics(sourceId = sourceId, daily = daily)
        }

    suspend fun recordReader(
        entry: DhikrEntry,
        collectionTitle: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ): Long {
        return dao.insert(
            event(
                timestampMillis = timestampMillis,
                kind = ActivityKinds.Reader,
                sourceId = entry.id,
                title = collectionTitle,
            ),
        )
    }

    suspend fun recordCompletion(
        collection: DhikrCollection,
        timestampMillis: Long = System.currentTimeMillis(),
    ): Long {
        return dao.insert(
            event(
                timestampMillis = timestampMillis,
                kind = ActivityKinds.Completion,
                sourceId = collection.id,
                title = collection.title,
                amount = 0,
            ),
        )
    }

    internal suspend fun removeReaderAction(undo: ReaderUndo): List<ActivityEvent> {
        val ids = listOf(undo.eventId, undo.completionEventId).filter { it > 0 }
        val events = dao.eventsById(ids)
        dao.deleteEvents(ids)
        return events
    }

    internal suspend fun restoreEvents(events: List<ActivityEvent>) {
        events.forEach { dao.insert(it) }
    }

    private fun event(
        timestampMillis: Long,
        kind: String,
        sourceId: String,
        title: String,
        amount: Int = 1,
    ): ActivityEvent {
        val time = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        return ActivityEvent(
            timestampMillis = timestampMillis,
            dayKey = time.toLocalDate().toString(),
            kind = kind,
            sourceId = sourceId,
            title = title,
            amount = amount,
            hourOfDay = time.hour,
        )
    }
}
