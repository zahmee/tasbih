package com.sakinah.tasbih.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

/**
 * Keeps one alarm for the next prayer alert and one for the widgets' next change. Each alarm,
 * when it rings, sets the one after it, so the chain survives a new day or a new time zone.
 * With the user's leave to set exact alarms they ring on the minute, even in Doze. Without it
 * they ring within ten minutes, and the full adhan gives way to a notification tone.
 */
object PrayerAlarms {
    private const val ACTION_ALERT = "com.sakinah.tasbih.action.PRAYER_ALERT"
    private const val ACTION_BOUNDARY = "com.sakinah.tasbih.action.PRAYER_BOUNDARY"
    private const val ACTION_TEST = "com.sakinah.tasbih.action.PRAYER_TEST"
    private const val EXTRA_PRAYER = "prayer"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_TIME = "time"
    private const val REQUEST_ALERT = 1
    private const val REQUEST_BOUNDARY = 2
    private const val REQUEST_TEST = 3
    private val WINDOW = Duration.ofMinutes(10)
    private val AFTER_BOUNDARY = Duration.ofSeconds(5)

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    suspend fun schedule(context: Context) {
        val app = context.applicationContext
        val (schedule, alerts) = load(app) ?: run {
            cancel(app, REQUEST_ALERT, ACTION_ALERT)
            cancel(app, REQUEST_BOUNDARY, ACTION_BOUNDARY)
            return
        }
        val now = Instant.now()
        val next = AlertPlanner.nextAlert(schedule, alerts, now)
        if (next == null) {
            cancel(app, REQUEST_ALERT, ACTION_ALERT)
        } else {
            set(app, REQUEST_ALERT, next.at, intent(app, ACTION_ALERT, REQUEST_ALERT, next.prayer, next.kind, next.prayerTime))
        }
        if (PrayerWidgetKind.anyPlaced(app)) {
            val boundary = AlertPlanner.nextBoundary(schedule, now).plus(AFTER_BOUNDARY)
            set(app, REQUEST_BOUNDARY, boundary, intent(app, ACTION_BOUNDARY, REQUEST_BOUNDARY, null, null, null))
        } else {
            cancel(app, REQUEST_BOUNDARY, ACTION_BOUNDARY)
        }
    }

    /** A trial in one minute, so the user can lock the screen and hear what a prayer will sound like. */
    fun scheduleTest(context: Context, prayer: Prayer) {
        val at = ZonedDateTime.now().plusMinutes(1)
        set(context, REQUEST_TEST, at, intent(context, ACTION_TEST, REQUEST_TEST, prayer, AlertKind.PRAYER, at))
    }

    internal suspend fun handle(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_ALERT, ACTION_TEST -> {
                val prayer = Prayer.entries.getOrNull(intent.getIntExtra(EXTRA_PRAYER, -1))
                val kind = AlertKind.entries.getOrNull(intent.getIntExtra(EXTRA_KIND, -1))
                val time = intent.getLongExtra(EXTRA_TIME, 0)
                if (prayer != null && kind != null) {
                    val alerts = PrayerPreferencesRepository(app).currentAlerts()
                    val style = if (intent.action == ACTION_TEST) testStyle(alerts) else alerts.alertFor(prayer).style
                    raise(app, prayer, kind, style, Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()), alerts)
                }
            }
        }
        PrayerWidgetUpdater(app).updateAll()
        if (intent.action != ACTION_TEST) schedule(app)
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

    private suspend fun load(context: Context): Pair<PrayerSchedule, AlertSettings>? {
        val repository = PrayerPreferencesRepository(context)
        val settings = repository.current()
        val place = settings.place ?: return null
        val calendar = HijriCalendar.from(CalendarPreferencesRepository(context).current())
        return PrayerSchedule(place, settings, calendar) to repository.currentAlerts()
    }

    private fun set(context: Context, request: Int, at: ZonedDateTime, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val millis = at.toInstant().toEpochMilli()
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        } else {
            manager.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW.toMillis(), operation)
        }
    }

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
