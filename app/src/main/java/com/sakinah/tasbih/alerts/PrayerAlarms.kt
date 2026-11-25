package com.sakinah.tasbih.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.prayer.AlertKind
import com.sakinah.tasbih.prayer.AlertPlanner
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.AlertStyle
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.widget.PrayerWidgetKind
import com.sakinah.tasbih.widget.PrayerWidgetUpdater
import com.sakinah.tasbih.widget.runAsync
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps an alarm for each of the coming wakes: every prayer alert, and every change of the
 * widgets while one is placed. Until November 25, 2026 a single alarm was kept, for the next of
 * them, and each alarm set the one after it. The user's phone then went without alerts, and its
 * widget stood at a prayer long past, whenever one alarm was not delivered: nothing was left to
 * ring until the app was opened. Now a day of wakes is handed to Android at once, each an alarm
 * of its own, and every alarm that rings hands over the day after it. An alarm the phone drops
 * costs that one moment, and the next mends the rest.
 *
 * The times the alarms were set for are remembered, so an alarm that Android delivers late
 * still raises every alert that came due meanwhile, and [WakeLog] can tell an alarm that rang
 * from one that never did. With the user's leave to set exact alarms they ring on the minute,
 * even in Doze. Without it they ring within ten minutes while the phone is awake, a second
 * alarm of the kind Doze lets through stands behind the first for a phone left idle, and the
 * full adhan gives way to a notification tone.
 */
object PrayerAlarms {
    private const val ACTION_WAKE = "com.sakinah.tasbih.action.PRAYER_WAKE"
    private const val ACTION_TEST = "com.sakinah.tasbih.action.PRAYER_TEST"
    private const val EXTRA_PRAYER = "prayer"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_TIME = "time"

    /** The time a wake's alarm was set for, which tells how late it rang. */
    private const val EXTRA_AT = "at"
    private const val ACTION_WAKE_IDLE = "com.sakinah.tasbih.action.PRAYER_WAKE_IDLE"

    /** The single alarm of the versions before 2.0.3, cancelled wherever it is still set. */
    private const val REQUEST_WAKE = 1
    private const val REQUEST_WAKE_IDLE = 2
    private const val REQUEST_TEST = 3

    /** The wakes' alarms are told apart by their request codes, counted from here. */
    private const val REQUEST_WAKES = 100
    private val WINDOW = Duration.ofMinutes(10)
    internal const val STORE = "prayer_alarms"

    /** The one time the versions before 2.0.3 remembered. */
    private const val KEY_PENDING = "pending_at"
    private const val KEY_PLANNED = "planned_at"

    /** The whole adhan is not started this long after its time; the short tone stands in. */
    private val ADHAN_LATE = Duration.ofMinutes(5)

    /** Scheduling reads, raises and writes; two callers at once would raise an alert twice. */
    private val lock = Mutex()

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /**
     * Sets an alarm for each of the coming wakes, then raises whatever alert came due since
     * they were last set. The alarms are set first, so that no failure in raising can leave the
     * phone without them. [rangFor] is the time the alarm that brought the app here was set
     * for, or null when something else did: the app opening, a widget, a restart. [judgeMissed]
     * is off where the alarms were cleared or moved by Android, so none was dropped. With
     * [onlyIfOverdue] nothing is done unless an alarm is overdue: an alert on its way is left
     * to its own alarm, which alone may start the full adhan.
     */
    suspend fun schedule(
        context: Context,
        rangFor: Long? = null,
        judgeMissed: Boolean = true,
        onlyIfOverdue: Boolean = false,
    ) = lock.withLock {
        val app = context.applicationContext
        val store = app.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        val planned = store.getString(KEY_PLANNED, null)?.split(',')?.mapNotNull { it.toLongOrNull() }
            ?: listOfNotNull(store.getLong(KEY_PENDING, 0).takeIf { it > 0 })
        val pending = planned.minOrNull() ?: 0
        val now = Instant.now()
        if (onlyIfOverdue && (pending == 0L || pending > now.toEpochMilli() - WakeLog.GRACE_MS)) return@withLock
        val loaded = load(app)
        val wakes = loaded?.let { (schedule, alerts) ->
            AlertPlanner.upcomingWakes(schedule, alerts, now, PrayerWidgetKind.anyPlaced(app) || dateTurnsAtMaghrib(app))
        }.orEmpty()
        wakes.forEachIndexed { index, wake -> set(app, wake.at, wakeIntent(app, ACTION_WAKE, REQUEST_WAKES + index, wake.at), wake.urgent) }
        (wakes.size until AlertPlanner.MAX_WAKES).forEach { cancel(app, REQUEST_WAKES + it, ACTION_WAKE) }
        cancel(app, REQUEST_WAKE, ACTION_WAKE)
        setIdleBackup(app, wakes.firstOrNull()?.at)
        store.edit()
            .putString(KEY_PLANNED, wakes.joinToString(",") { it.at.toInstant().toEpochMilli().toString() })
            .remove(KEY_PENDING)
            .apply()
        WakeLog.record(app, planned, rangFor, now.toEpochMilli(), judgeMissed)
        if (loaded != null && pending in 1..now.toEpochMilli()) {
            val (schedule, alerts) = loaded
            AlertPlanner.due(schedule, alerts, Instant.ofEpochMilli(pending), now).forEach { event ->
                val late = Duration.between(event.at.toInstant(), now) > ADHAN_LATE
                val style = alerts.alertFor(event.prayer).style.let { if (late && it == AlertStyle.ADHAN) AlertStyle.SOUND else it }
                raise(app, event.prayer, event.kind, style, event.prayerTime.withZoneSameInstant(ZoneId.systemDefault()), alerts)
            }
        }
    }

    /**
     * [schedule] for a caller with other work to do after it, as a widget being drawn: a
     * failure here is written down and does not take that work with it.
     */
    suspend fun mend(context: Context, onlyIfOverdue: Boolean = false) {
        try {
            schedule(context, onlyIfOverdue = onlyIfOverdue)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("PrayerAlarms", "The alarms could not be set again", failure)
        }
    }

    /** A trial in one minute, so the user can lock the screen and hear what a prayer will sound like. */
    fun scheduleTest(context: Context, prayer: Prayer) {
        val at = ZonedDateTime.now().plusMinutes(1)
        set(context, at, intent(context, ACTION_TEST, REQUEST_TEST, prayer, AlertKind.PRAYER, at))
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        // The time this alarm was set for, or null for what is no alarm of a wake: the trial, a
        // restart, an update of the app, a change of the clock.
        val rang = intent.getLongExtra(EXTRA_AT, 0).takeIf { it > 0 && (intent.action == ACTION_WAKE || intent.action == ACTION_WAKE_IDLE) }
        if (intent.action == ACTION_TEST) {
            val prayer = Prayer.entries.getOrNull(intent.getIntExtra(EXTRA_PRAYER, -1))
            val kind = AlertKind.entries.getOrNull(intent.getIntExtra(EXTRA_KIND, -1))
            val time = intent.getLongExtra(EXTRA_TIME, 0)
            if (prayer != null && kind != null) {
                val alerts = PrayerPreferencesRepository(app).currentAlerts()
                raise(app, prayer, kind, testStyle(alerts), Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()), alerts)
            }
        } else {
            // Raises the alert this alarm rang for, and any other that came due, and sets the
            // coming alarms again. A restart, an update of the app and a change of the clock
            // come here too, with no alarm behind them: they clear or move the alarms themselves.
            schedule(app, rangFor = rang, judgeMissed = rang != null)
        }
        // The widgets come last and may fail without taking the alerts down with them.
        try {
            PrayerWidgetUpdater(app).updateAll()
            if (dateTurnsAtMaghrib(app)) com.sakinah.tasbih.widget.CalendarWidgetUpdater(app).updateAll()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("PrayerAlarms", "The widgets could not be redrawn", failure)
        }
        // The wird keeps a single alarm of its own. Each prayer alarm that rings sets it again,
        // and raises a reminder the phone dropped, so the wird's chain is mended many times a
        // day. The restart, the update and the clock reach the wird's own receiver themselves.
        if (rang != null) com.sakinah.tasbih.wird.WirdSync.changed(app)
        if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val place = PrayerPreferencesRepository(app).current().place
            if (place != null && place.keepsOtherTimeThan(ZoneId.systemDefault(), Instant.now())) {
                AlertNotifier.postTravel(app, place.name)
            }
        }
    }

    private fun raise(context: Context, prayer: Prayer, kind: AlertKind, style: AlertStyle, time: ZonedDateTime, alerts: AlertSettings) {
        if (kind == AlertKind.PRAYER && style == AlertStyle.ADHAN && AlertNotifier.canPost(context)) {
            val started = runCatching {
                AdhanService.start(context, prayer, time.toInstant().toEpochMilli(), adhanSource(context, prayer, alerts))
            }.isSuccess
            // Android refuses a background start without an exact alarm; the tone stands in.
            if (started) return
        }
        AlertNotifier.post(context, prayer, kind, style, time, alerts.tone)
    }

    private fun adhanSource(context: Context, prayer: Prayer, alerts: AlertSettings): Uri {
        val id = if (prayer == Prayer.FAJR) alerts.fajrAdhan else alerts.adhan
        val custom = alerts.customAdhanUri?.let(Uri::parse)
        if (id == AlertSettings.CUSTOM_ADHAN && custom != null && canRead(context, custom)) return custom
        return PrayerSounds.uri(context, PrayerSounds.adhan(id).raw)
    }

    private fun canRead(context: Context, uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }

    /** The trial sounds like the fullest alert the user has turned on. */
    private fun testStyle(alerts: AlertSettings): AlertStyle {
        val styles = Prayer.entries.map { alerts.alertFor(it).style }
        return listOf(AlertStyle.ADHAN, AlertStyle.SOUND, AlertStyle.SILENT).firstOrNull { it in styles } ?: AlertStyle.SOUND
    }

    /** The date widget must also wake at Maghrib when the user lets the Hijri date turn then. */
    private suspend fun dateTurnsAtMaghrib(context: Context): Boolean =
        CalendarPreferencesRepository(context).current().hijriDayFromMaghrib &&
            com.sakinah.tasbih.widget.CalendarWidgetKind.DATE.placedIds(context).isNotEmpty()

    private suspend fun load(context: Context): Pair<PrayerSchedule, AlertSettings>? {
        val repository = PrayerPreferencesRepository(context)
        val settings = repository.current()
        val place = settings.place ?: return null
        val calendar = HijriCalendar.from(CalendarPreferencesRepository(context).current())
        return PrayerSchedule(place, settings, calendar) to repository.currentAlerts()
    }

    /**
     * An [urgent] alarm is an alarm clock: Doze never delays it, at the cost of an alarm icon in
     * the status bar until it rings, so it is kept for a prayer that comes right after its reminder.
     */
    private fun set(context: Context, at: ZonedDateTime, operation: PendingIntent, urgent: Boolean = false) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val millis = at.toInstant().toEpochMilli()
        try {
            when {
                !canScheduleExact(context) -> manager.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW.toMillis(), operation)
                urgent -> manager.setAlarmClock(AlarmManager.AlarmClockInfo(millis, AlertNotifier.openPrayerTimes(context)), operation)
                else -> manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
            }
        } catch (refused: SecurityException) {
            // The leave to set exact alarms was withdrawn between the check and the call.
            Log.w("PrayerAlarms", "An exact alarm was refused", refused)
            manager.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW.toMillis(), operation)
        }
    }

    /**
     * Without exact alarms, the windowed alarms above are held back for as long as the phone
     * lies idle, which at Fajr can be hours. This second one, for the next wake at [at], is of
     * the kind Doze lets through, though less punctual while the phone is awake. Whichever
     * rings first raises the alert and sets them all again, and the other then finds nothing due.
     */
    private fun setIdleBackup(context: Context, at: ZonedDateTime?) {
        if (at == null || canScheduleExact(context)) {
            cancel(context, REQUEST_WAKE_IDLE, ACTION_WAKE_IDLE)
            return
        }
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            at.toInstant().toEpochMilli(),
            wakeIntent(context, ACTION_WAKE_IDLE, REQUEST_WAKE_IDLE, at),
        )
    }

    /** A wake's alarm. It carries the time it was set for, to tell how late Android delivers it. */
    private fun wakeIntent(context: Context, action: String, request: Int, at: ZonedDateTime): PendingIntent = PendingIntent.getBroadcast(
        context,
        request,
        Intent(context, PrayerAlarmReceiver::class.java).setAction(action).putExtra(EXTRA_AT, at.toInstant().toEpochMilli()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The action must match the one the alarm was set with, or Android finds no alarm to cancel. */
    private fun cancel(context: Context, request: Int, action: String) {
        val operation = PendingIntent.getBroadcast(
            context,
            request,
            Intent(context, PrayerAlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(operation)
        operation.cancel()
    }

    private fun intent(
        context: Context,
        action: String,
        request: Int,
        prayer: Prayer?,
        kind: AlertKind?,
        prayerTime: ZonedDateTime?,
    ): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).setAction(action)
        prayer?.let { intent.putExtra(EXTRA_PRAYER, it.ordinal) }
        kind?.let { intent.putExtra(EXTRA_KIND, it.ordinal) }
        prayerTime?.let { intent.putExtra(EXTRA_TIME, it.toInstant().toEpochMilli()) }
        return PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

/**
 * Prayer alarms ringing, and everything that clears or moves them: a restart, an update of the
 * app, the clock or time zone changing, and the exact alarm permission being granted.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync { PrayerAlarms.handle(context, intent) }
    }
}
