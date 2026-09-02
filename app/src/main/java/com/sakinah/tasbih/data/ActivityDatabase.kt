package com.sakinah.tasbih.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
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
        Index(value = ["dayKey"]),
        Index(value = ["kind", "sourceId"]),
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
    suspend fun insert(event: ActivityEvent)

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
            COALESCE(SUM(CASE WHEN kind != 'completion' THEN amount ELSE 0 END), 0) AS totalCount,
            COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0) AS tasbihCount,
            COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0) AS readerCount,
            COALESCE(SUM(CASE WHEN kind = 'completion' THEN 1 ELSE 0 END), 0) AS completions
        FROM activity_events
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """,
    )
    fun observeDailyActivity(): Flow<List<DailyActivity>>

    @Query(
        """
        SELECT
            dayKey,
            CAST(strftime('%H', timestampMillis / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hourOfDay,
            COALESCE(SUM(amount), 0) AS totalCount,
            COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0) AS tasbihCount,
            COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0) AS readerCount
        FROM activity_events
        WHERE kind != 'completion'
        GROUP BY dayKey, hourOfDay
        ORDER BY dayKey ASC, hourOfDay ASC
        """,
    )
    fun observeHourlyActivity(): Flow<List<HourlyActivity>>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN kind != 'completion' THEN amount ELSE 0 END), 0) AS totalCount,
            COALESCE(SUM(CASE WHEN kind = 'tasbih' THEN amount ELSE 0 END), 0) AS tasbihCount,
            COALESCE(SUM(CASE WHEN kind = 'reader' THEN amount ELSE 0 END), 0) AS readerCount,
            COALESCE(SUM(CASE WHEN kind = 'completion' THEN 1 ELSE 0 END), 0) AS completions,
            COUNT(DISTINCT CASE WHEN kind != 'completion' THEN dayKey END) AS activeDays,
            COALESCE(MIN(timestampMillis), 0) AS firstActivityMillis
        FROM activity_events
        """,
    )
    fun observeTotals(): Flow<ActivityTotals>

    @Query("SELECT * FROM activity_events ORDER BY timestampMillis DESC, id DESC LIMIT 120")
    fun observeRecent(): Flow<List<ActivityEvent>>

    @Query(
        """
        SELECT
            dayKey,
            COALESCE(SUM(amount), 0) AS count
        FROM activity_events
        WHERE kind = 'tasbih' AND sourceId = :sourceId
        GROUP BY dayKey
        ORDER BY dayKey ASC
        """,
    )
    fun observeTasbihPhraseDaily(sourceId: String): Flow<List<TasbihPhraseDailyActivity>>
}

@Database(
    entities = [ActivityEvent::class],
    version = 1,
    exportSchema = true,
)
abstract class SakinahDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var instance: SakinahDatabase? = null

        fun get(context: Context): SakinahDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SakinahDatabase::class.java,
                "sakinah_activity.db",
            )
                .setQueryCoroutineContext(Dispatchers.IO)
                .setDriver(AndroidSQLiteDriver())
                .build()
                .also { instance = it }
        }
    }
}

class ActivityRepository(context: Context) {
    private val dao = SakinahDatabase.get(context).activityDao()

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
    ) {
        dao.insert(
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
    ) {
        dao.insert(
            event(
                timestampMillis = timestampMillis,
                kind = ActivityKinds.Completion,
                sourceId = collection.id,
                title = collection.title,
                amount = 0,
            ),
        )
    }

    private fun event(
        timestampMillis: Long,
        kind: String,
        sourceId: String,
        title: String,
        amount: Int = 1,
    ): ActivityEvent {
        val day = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return ActivityEvent(
            timestampMillis = timestampMillis,
            dayKey = day.toString(),
            kind = kind,
            sourceId = sourceId,
            title = title,
            amount = amount,
        )
    }
}
