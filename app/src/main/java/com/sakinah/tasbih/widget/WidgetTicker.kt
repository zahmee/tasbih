package com.sakinah.tasbih.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Moves the bar of the prayer widgets on, a little every few minutes. The alarm does not wake
 * the phone: while the screen is off it waits, and comes as soon as the phone is in use, which
 * is when anyone can see the bar. So it costs next to nothing, and needs no exact-alarm permission.
 */
object WidgetTicker {
    const val ACTION_TICK = "com.sakinah.tasbih.action.PRAYER_WIDGET_TICK"
    private const val EVERY_MS = 5 * 60 * 1000L
    private const val WINDOW_MS = 2 * 60 * 1000L

    fun set(context: Context, on: Boolean) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, PrayerDayWidget::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (on) {
            alarms.setWindow(AlarmManager.RTC, System.currentTimeMillis() + EVERY_MS, WINDOW_MS, intent)
        } else {
            alarms.cancel(intent)
        }
    }

    private const val REQUEST = 7301
}
