package com.sakinah.tasbih.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.wird.WirdPreset
import com.sakinah.tasbih.wird.WirdReading
import com.sakinah.tasbih.wird.WirdRepeat
import com.sakinah.tasbih.wird.WirdTask
import com.sakinah.tasbih.wird.WirdTimeKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock

/** A task of the wird as stored. Its names are kept as text, so a reordered enum cannot shift them. */
@Entity(tableName = "wird_tasks")
data class WirdTaskRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val collectionId: String?,
    val presetId: String?,
    val repeat: String,
    /** Monday is the lowest bit, Sunday the seventh. */
    val weekdays: Int,
    /** The Hijri days, joined by commas. */
    val hijriDays: String,
    val onceDay: String?,
    val timeKind: String,
    val minuteOfDay: Int,
    val prayer: String?,
    val offsetMinutes: Int,
    val remind: Boolean,
    val sortOrder: Int,
    val createdAtMillis: Long,
)

/** A task ticked done on a day: a recurring task's own day, or the date of a task of one time. */
@Entity(tableName = "wird_done", primaryKeys = ["taskId", "dayKey"], indices = [Index(value = ["dayKey"])])
data class WirdDoneRow(val taskId: Long, val dayKey: String, val doneAtMillis: Long)

/** A collection read to its end, from the reading statistics already kept. */
data class WirdReadingRow(val sourceId: String, val timestampMillis: Long)

@Dao
interface WirdDao {
    @Query("SELECT * FROM wird_tasks ORDER BY sortOrder ASC, id ASC")
    fun observeTasks(): Flow<List<WirdTaskRow>>

    @Query("SELECT * FROM wird_tasks ORDER BY sortOrder ASC, id ASC")
    suspend fun tasks(): List<WirdTaskRow>

    @Query("SELECT * FROM wird_tasks WHERE id = :id")
    suspend fun task(id: Long): WirdTaskRow?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM wird_tasks")
    suspend fun lastOrder(): Int

    @Insert
    suspend fun insert(task: WirdTaskRow): Long

    @Update
    suspend fun update(task: WirdTaskRow)

    @Query("DELETE FROM wird_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM wird_done WHERE taskId = :id")
    suspend fun deleteDoneOf(id: Long)

    @Query("SELECT * FROM wird_done")
    fun observeDone(): Flow<List<WirdDoneRow>>

    @Query("SELECT * FROM wird_done")
    suspend fun done(): List<WirdDoneRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDone(row: WirdDoneRow)

    @Query("DELETE FROM wird_done WHERE taskId = :taskId AND dayKey = :dayKey")
    suspend fun clearDone(taskId: Long, dayKey: String)

    /** Ticks older than the history the page shows, of recurring tasks only. */
    @Query(
        """
        DELETE FROM wird_done WHERE dayKey < :before AND taskId IN
        (SELECT id FROM wird_tasks WHERE `repeat` != 'ONCE')
        """,
    )
    suspend fun pruneBefore(before: String)

    @Query("SELECT sourceId, timestampMillis FROM activity_events WHERE kind = 'completion' AND timestampMillis >= :fromMillis")
    fun observeReadings(fromMillis: Long): Flow<List<WirdReadingRow>>

    @Query("SELECT sourceId, timestampMillis FROM activity_events WHERE kind = 'completion' AND timestampMillis >= :fromMillis")
    suspend fun readings(fromMillis: Long): List<WirdReadingRow>
}

internal fun WirdTaskRow.toTask(): WirdTask = WirdTask(
    id = id,
    title = title,
    collectionId = collectionId,
    presetId = presetId,
    repeat = enumOr(repeat, WirdRepeat.DAILY),
    weekdays = DayOfWeek.entries.filter { weekdays and (1 shl (it.value - 1)) != 0 }.toSet(),
    hijriDays = hijriDays.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { day -> day in 1..WirdTask.LAST_HIJRI_DAY } }.toSet(),
    date = onceDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    timeKind = enumOr(timeKind, WirdTimeKind.NONE),
    minuteOfDay = minuteOfDay.coerceIn(0, 1439),
    prayer = prayer?.let { name -> Prayer.entries.firstOrNull { it.name == name } },
    offsetMinutes = offsetMinutes,
    remind = remind,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

internal fun WirdTask.toRow(): WirdTaskRow = WirdTaskRow(
    id = id,
    title = title.trim().take(WirdTask.MAX_TITLE),
    collectionId = collectionId,
    presetId = presetId,
    repeat = repeat.name,
    weekdays = weekdays.fold(0) { bits, day -> bits or (1 shl (day.value - 1)) },
    hijriDays = hijriDays.sorted().joinToString(","),
    onceDay = date?.toString(),
    timeKind = timeKind.name,
    minuteOfDay = minuteOfDay.coerceIn(0, 1439),
    prayer = prayer?.name,
    offsetMinutes = offsetMinutes,
    remind = remind,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

/** The days each task was ticked done on. */
internal fun List<WirdDoneRow>.byTask(): Map<Long, Set<LocalDate>> = groupBy(WirdDoneRow::taskId)
    .mapValues { (_, rows) -> rows.mapNotNull { runCatching { LocalDate.parse(it.dayKey) }.getOrNull() }.toSet() }

internal fun List<WirdReadingRow>.toReadings(): List<WirdReading> = map { WirdReading(it.sourceId, Instant.ofEpochMilli(it.timestampMillis)) }

/**
 * The wird's tasks and ticks. The morning and evening adhkar are put on the list once, the first
 * time it is opened, with their reminders off, so the home screen counts them from the start.
 */
class WirdRepository internal constructor(
    private val dao: WirdDao,
    private val seeded: android.content.SharedPreferences?,
    private val presetTitle: (WirdPreset) -> String,
) {
    constructor(context: Context) : this(
        SakinahDatabase.get(context).wirdDao(),
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE),
        { preset -> context.applicationContext.getString(preset.title) },
    )

    val tasks: Flow<List<WirdTask>> = dao.observeTasks().map { rows -> rows.map(WirdTaskRow::toTask) }
    val ticked: Flow<Map<Long, Set<LocalDate>>> = dao.observeDone().map { it.byTask() }

    fun readingsSince(from: Instant): Flow<List<WirdReading>> = dao.observeReadings(from.toEpochMilli()).map { it.toReadings() }

    suspend fun currentTasks(): List<WirdTask> = dao.tasks().map(WirdTaskRow::toTask)
    suspend fun currentTicked(): Map<Long, Set<LocalDate>> = dao.done().byTask()
    suspend fun currentReadings(from: Instant): List<WirdReading> = dao.readings(from.toEpochMilli()).toReadings()
    suspend fun task(id: Long): WirdTask? = dao.task(id)?.toTask()

    /** The page, the alarm and the widget may all ask at the first start; one of them seeds. */
    suspend fun ensureSeeded(now: Long = System.currentTimeMillis()) {
        val store = seeded ?: return
        if (store.getBoolean(KEY_SEEDED, false)) return
        seedLock.withLock {
            if (store.getBoolean(KEY_SEEDED, false)) return
            if (dao.tasks().isEmpty()) {
                WirdPreset.entries.filter { it.byDefault }.forEachIndexed { index, preset ->
                    dao.insert(preset.task(presetTitle(preset), index + 1, now).toRow())
                }
            }
            store.edit().putBoolean(KEY_SEEDED, true).commit()
        }
    }

    /** Saves a new task at the end of the list, or the changes to one already there. */
    suspend fun save(task: WirdTask, now: Long = System.currentTimeMillis()): Long =
        if (task.id == 0L) {
            dao.insert(task.copy(sortOrder = dao.lastOrder() + 1, createdAtMillis = now).toRow())
        } else {
            dao.update(task.toRow())
            task.id
        }

    suspend fun addPreset(preset: WirdPreset, now: Long = System.currentTimeMillis()): Long =
        save(preset.task(presetTitle(preset), 0, now), now)

    suspend fun delete(id: Long) {
        dao.deleteDoneOf(id)
        dao.deleteTask(id)
    }

    /** A task of one time is ticked on its own date, wherever it is ticked from. */
    suspend fun setDone(task: WirdTask, day: LocalDate, done: Boolean, now: Long = System.currentTimeMillis()) {
        val key = (if (task.repeat == WirdRepeat.ONCE) task.date ?: day else day).toString()
        if (done) dao.setDone(WirdDoneRow(task.id, key, now)) else dao.clearDone(task.id, key)
    }

    /** Keeps the history the page shows, and a margin. */
    suspend fun prune(today: LocalDate) = dao.pruneBefore(today.minusDays(KEEP_DAYS).toString())

    private companion object {
        const val STORE = "wird"
        const val KEY_SEEDED = "seeded"
        const val KEEP_DAYS = 60L
        val seedLock = kotlinx.coroutines.sync.Mutex()
    }
}
