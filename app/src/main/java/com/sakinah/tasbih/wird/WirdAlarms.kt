package com.sakinah.tasbih.wird

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sakinah.tasbih.alerts.PrayerAlarms
import com.sakinah.tasbih.data.WirdRepository
import com.sakinah.tasbih.widget.runAsync
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps one alarm for the wird: the next reminder, a reminder put off for later, or, while the
 * wird's widget is on a home screen, the turn of the app's day, whichever comes first. As with
 * the prayer alerts, the time the alarm was set for is kept, so an alarm Android delivers late
 * still raises every reminder that came due meanwhile, and opening the app mends a broken chain.
 * A task already done when its reminder comes, as adhkar read before their time, stays quiet.
 */
object WirdAlarms {
    private const val ACTION_WAKE = "com.sakinah.tasbih.action.WIRD_WAKE"
    private const val REQUEST_WAKE = 6100
    private const val STORE = "wird_alarms"
    private const val KEY_PENDING = "pending_at"
    private const val KEY_SNOOZED = "snoozed"
    val LATER: Duration = Duration.ofMinutes(10)
    private val WINDOW = Duration.ofMinutes(10)

    private val lock = Mutex()

    suspend fun schedule(context: Context) = lock.withLock {
        val app = context.applicationContext
        val store = app.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val pending = store.getLong(KEY_PENDING, 0)
        val now = Instant.now()
        val repository = WirdRepository(app)
        repository.ensureSeeded()
        val tasks = repository.currentTasks()
        val calendar = WirdEnvironment.calendar(app)
        val snoozed = Snooze.decode(store.getStringSet(KEY_SNOOZED, emptySet()).orEmpty())
            .filter { snooze -> tasks.any { it.id == snooze.taskId } }

        val reminder = WirdPlanner.nextReminder(tasks, now, calendar)?.at?.toInstant()
        val later = snoozed.map { it.at }.filter { it.isAfter(now) }.minOrNull()
        val turn = if (WirdWidget.anyPlaced(app)) calendar.window(calendar.today(now)).second else null
        val exactNext = listOfNotNull(reminder, later).minOrNull()
        val next = listOfNotNull(exactNext, turn).minOrNull()
        if (next == null) {
            cancel(app)
            store.edit().remove(KEY_PENDING).apply()
        } else {
            set(app, next, exact = next == exactNext)
            store.edit().putLong(KEY_PENDING, next.toEpochMilli()).apply()
        }

        val dueLater = snoozed.filter { !it.at.isAfter(now) }
        if (dueLater.isNotEmpty()) {
            store.edit().putStringSet(KEY_SNOOZED, Snooze.encode(snoozed - dueLater.toSet())).apply()
        }
        val from = if (pending > 0) maxOf(Instant.ofEpochMilli(pending - 1), now.minus(WirdPlanner.STALE)) else now
        val due = WirdPlanner.remindersBetween(tasks, from, now, calendar).map { it.task to it.day } +
            dueLater.filter { now.isBefore(it.at.plus(WirdPlanner.STALE)) }
                .mapNotNull { snooze -> tasks.firstOrNull { it.id == snooze.taskId }?.let { it to snooze.day } }
        if (due.isEmpty()) return@withLock
        val ticked = repository.currentTicked()
        val readings = repository.currentReadings(WirdBoard.readingsFrom(now, calendar))
        due.distinct().forEach { (task, day) ->
            if (!WirdBoard.doneOn(task, day, ticked, readings, calendar)) WirdNotifier.post(app, task, day)
        }
    }

    /** «Remind me later»: the same task and day, a little later. */
    suspend fun snooze(context: Context, taskId: Long, day: LocalDate) {
        val store = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        lock.withLock {
            val kept = Snooze.decode(store.getStringSet(KEY_SNOOZED, emptySet()).orEmpty()).filterNot { it.taskId == taskId }
            store.edit().putStringSet(KEY_SNOOZED, Snooze.encode(kept + Snooze(taskId, day, Instant.now().plus(LATER)))).apply()
        }
        schedule(context)
    }

    /** With leave to set exact alarms a reminder rings on the minute; without, Doze may hold it back a while. */
    private fun set(context: Context, at: Instant, exact: Boolean) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val millis = at.toEpochMilli()
        val operation = wakeIntent(context)
        when {
            !exact -> manager.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW.toMillis(), operation)
            PrayerAlarms.canScheduleExact(context) -> manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
            else -> manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(wakeIntent(context))
    }

    private fun wakeIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_WAKE,
        Intent(context, WirdReceiver::class.java).setAction(ACTION_WAKE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal data class Snooze(val taskId: Long, val day: LocalDate, val at: Instant) {
        companion object {
            fun encode(values: Collection<Snooze>): Set<String> = values.mapTo(mutableSetOf()) { "${it.taskId}|${it.day}|${it.at.toEpochMilli()}" }

            fun decode(values: Set<String>): List<Snooze> = values.mapNotNull { line ->
                val parts = line.split('|')
                runCatching { Snooze(parts[0].toLong(), LocalDate.parse(parts[1]), Instant.ofEpochMilli(parts[2].toLong())) }.getOrNull()
            }
        }
    }
}

/**
 * Whatever the app does after the wird changed: the alarm set again, the widget redrawn, and a
 * reminder still showing for a task now done taken away.
 */
object WirdSync {
    suspend fun changed(context: Context) {
        val app = context.applicationContext
        try {
            WirdAlarms.schedule(app)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("WirdSync", "The wird's alarm could not be set", failure)
        }
        refreshViews(app)
    }

    /** After a reading: nothing to set, but a task may be done now. */
    suspend fun refreshViews(context: Context) {
        val app = context.applicationContext
        try {
            val board = WirdEnvironment.board(app)
            board.todayItems.filter { it.done }.forEach { WirdNotifier.cancel(app, it.task.id) }
            WirdWidget.updateAll(app, board)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("WirdSync", "The wird's widget could not be redrawn", failure)
        }
    }
}

/**
 * The wird's alarm ringing, a reminder's buttons, a tick on the widget, and everything that
 * clears or moves alarms: a restart, an update of the app, the clock or time zone changing.
 */
class WirdReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val taskId = intent.getLongExtra(EXTRA_TASK, 0)
        val day = intent.getStringExtra(EXTRA_DAY)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        runAsync {
            when (intent.action) {
                ACTION_DONE, ACTION_TOGGLE -> if (day != null) {
                    val repository = WirdRepository(app)
                    val task = repository.task(taskId)
                    if (task != null) {
                        val done = if (intent.action == ACTION_DONE) {
                            true
                        } else {
                            !WirdBoard.doneOn(
                                task, day, repository.currentTicked(),
                                repository.currentReadings(WirdBoard.readingsFrom(java.time.Instant.now(), WirdEnvironment.calendar(app))),
                                WirdEnvironment.calendar(app),
                            )
                        }
                        repository.setDone(task, day, done)
                    }
                    WirdNotifier.cancel(app, taskId)
                    WirdSync.refreshViews(app)
                }
                ACTION_SNOOZE -> if (day != null) {
                    WirdNotifier.cancel(app, taskId)
                    WirdAlarms.snooze(app, taskId, day)
                }
                else -> WirdSync.changed(app)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.sakinah.tasbih.action.WIRD_DONE"
        const val ACTION_SNOOZE = "com.sakinah.tasbih.action.WIRD_SNOOZE"
        const val ACTION_TOGGLE = "com.sakinah.tasbih.action.WIRD_TOGGLE"
        const val EXTRA_TASK = "task"
        const val EXTRA_DAY = "day"
    }
}
