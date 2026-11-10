package com.sakinah.tasbih.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The calendar widgets. They need no setup screen and no permission. */
enum class CalendarWidgetKind(val receiver: Class<out AppWidgetProvider>) {
    DATE(CalendarDateWidget::class.java),
    MONTH(CalendarMonthWidget::class.java),
    OCCASION(CalendarOccasionWidget::class.java),
    ;

    fun provider(context: Context): ComponentName = ComponentName(context, receiver)

    fun placedIds(context: Context): List<Int> =
        AppWidgetManager.getInstance(context).getAppWidgetIds(provider(context)).toList()

    companion object {
        fun anyPlaced(context: Context): Boolean = entries.any { it.placedIds(context).isNotEmpty() }
    }
}

/** Widget work outlives the broadcast that started it, so it runs here and not in the receiver. */
internal val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    widgetScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** What every calendar widget does when Android tells it something changed. */
abstract class BaseCalendarWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MONTH_PAGE) {
            super.onReceive(context, intent)
            return
        }
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val direction = intent.getIntExtra(EXTRA_DIRECTION, 0)
        runAsync { CalendarWidgetUpdater(context).turnMonth(appWidgetId, direction) }
    }

    /** Also how the midnight alarm comes back after a restart: Android updates every widget then. */
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync {
            val updater = CalendarWidgetUpdater(context)
            appWidgetIds.forEach { updater.update(it) }
            CalendarRefresh.schedule(context)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        runAsync { CalendarWidgetUpdater(context).update(appWidgetId) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync {
            val updater = CalendarWidgetUpdater(context)
            appWidgetIds.forEach { updater.forget(it) }
        }
    }

    override fun onEnabled(context: Context) = CalendarRefresh.schedule(context)

    override fun onDisabled(context: Context) = CalendarRefresh.cancelIfUnused(context)

    companion object {
        /** Zero goes back to the current month. */
        const val ACTION_MONTH_PAGE = "com.sakinah.tasbih.action.CALENDAR_MONTH_PAGE"
        const val EXTRA_DIRECTION = "direction"
    }
}

// These names are how Android remembers widgets already on a home screen; they must not change.
class CalendarDateWidget : BaseCalendarWidget()

class CalendarMonthWidget : BaseCalendarWidget()

class CalendarOccasionWidget : BaseCalendarWidget()

/**
 * Keeps the widgets on the right day without any permission. An inexact alarm shortly after
 * midnight turns the day, so the new date may appear some minutes late. Android also redraws the
 * widgets every six hours, which repairs anything the alarm missed.
 */
object CalendarRefresh {
    private const val ACTION_MIDNIGHT = "com.sakinah.tasbih.action.CALENDAR_MIDNIGHT"

    /** Android lets an app without the exact alarm permission ask for a window of ten minutes. */
    private val WINDOW = Duration.ofMinutes(10)

    /** A little after midnight, so the clock has surely turned when the widgets are drawn. */
    private val AFTER_MIDNIGHT = Duration.ofSeconds(5)

    fun schedule(context: Context) {
        if (!CalendarWidgetKind.anyPlaced(context)) {
            cancel(context)
            return
        }
        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
        val at = midnight.toInstant().plus(AFTER_MIDNIGHT).toEpochMilli()
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW.toMillis(), midnightIntent(context))
    }

    fun cancelIfUnused(context: Context) {
        if (!CalendarWidgetKind.anyPlaced(context)) cancel(context)
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, CalendarRefreshReceiver::class.java).setAction(ACTION_MIDNIGHT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Midnight, a change of the clock or time zone, and an update of the app all move the date. */
class CalendarRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync {
            CalendarWidgetUpdater(context).updateAll()
            CalendarRefresh.schedule(context)
        }
    }
}
