package com.sakinah.tasbih.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.PrayerAlarms
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.prayer.ObligatoryPrayers
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.prayer.PrayerText
import java.time.Instant

/**
 * The prayer widgets: the next prayer, the day's five, and the Qibla. Only the first two change
 * with the prayer times; the Qibla changes only when the place does.
 */
enum class PrayerWidgetKind(val receiver: Class<out AppWidgetProvider>, val followsPrayers: Boolean) {
    NEXT(PrayerNextWidget::class.java, followsPrayers = true),
    DAY(PrayerDayWidget::class.java, followsPrayers = true),
    QIBLA(PrayerQiblaWidget::class.java, followsPrayers = false),
    ;

    fun placedIds(context: Context): List<Int> =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, receiver)).toList()

    companion object {
        /** Whether a widget on the home screen needs waking at each prayer's time. */
        fun anyPlaced(context: Context): Boolean = entries.any { it.followsPrayers && it.placedIds(context).isNotEmpty() }

        fun anyAtAll(context: Context): Boolean = entries.any { it.placedIds(context).isNotEmpty() }
    }
}

abstract class BasePrayerWidget : AppWidgetProvider() {
    /** Also after a restart, when Android updates every widget: this sets the alarms again. */
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync {
            PrayerWidgetUpdater(context).updateAll()
            PrayerAlarms.schedule(context)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        runAsync { PrayerWidgetUpdater(context).updateAll() }
    }

    override fun onEnabled(context: Context) {
        runAsync { PrayerAlarms.schedule(context) }
    }

    override fun onDisabled(context: Context) {
        runAsync { PrayerAlarms.schedule(context) }
    }
}

// These names are how Android remembers widgets already on a home screen; they must not change.
class PrayerNextWidget : BasePrayerWidget()

class PrayerDayWidget : BasePrayerWidget()

class PrayerQiblaWidget : BasePrayerWidget()

/**
 * Draws the prayer widgets: text on Anaa's paper, nothing that moves. The next prayer changes at
 * each prayer's time, when the alarm chain redraws them, so no countdown has to tick here.
 */
class PrayerWidgetUpdater(context: Context) {
    private val app = context.applicationContext
    private val manager get() = AppWidgetManager.getInstance(app)

    suspend fun updateAll() {
        if (!PrayerWidgetKind.anyAtAll(app)) return
        val settings = PrayerPreferencesRepository(app).current()
        val calendarSettings = CalendarPreferencesRepository(app).current()
        val schedule = settings.place?.let { PrayerSchedule(it, settings, HijriCalendar.from(calendarSettings)) }
        val calendarText = CalendarText(app, calendarSettings)
        val text = PrayerText(app)
        PrayerWidgetKind.NEXT.placedIds(app).forEach { manager.updateAppWidget(it, nextViews(it, schedule, text)) }
        PrayerWidgetKind.DAY.placedIds(app).forEach { manager.updateAppWidget(it, dayViews(it, schedule, text, calendarText)) }
        PrayerWidgetKind.QIBLA.placedIds(app).forEach { manager.updateAppWidget(it, qiblaViews(it, schedule, text)) }
    }

    private fun qiblaViews(appWidgetId: Int, schedule: PrayerSchedule?, text: PrayerText): RemoteViews =
        RemoteViews(app.packageName, R.layout.anaa_widget_qibla).apply {
            val bearing = schedule?.let { text.degrees(it.qiblaBearing) }
            setTextViewText(R.id.qb_bearing, bearing ?: app.getString(R.string.qibla_title))
            setContentDescription(
                R.id.qb_root,
                bearing?.let { app.getString(R.string.qibla_bearing, it) } ?: app.getString(R.string.prayer_choose_place),
            )
            setOnClickPendingIntent(
                R.id.qb_root,
                PendingIntent.getActivity(
                    app,
                    appWidgetId,
                    MainActivity.qiblaIntent(app).setData(Uri.parse("anaa://qibla/$appWidgetId")),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

    private fun nextViews(appWidgetId: Int, schedule: PrayerSchedule?, text: PrayerText): RemoteViews =
        RemoteViews(app.packageName, R.layout.anaa_widget_lines).apply {
            val next = schedule?.next(Instant.now())
            val top = next?.let { text.name(it.prayer, it.time) } ?: app.getString(R.string.prayer_title)
            val main = next?.let { text.time(it.time) } ?: app.getString(R.string.prayer_choose_place)
            val sub = schedule?.place?.name.orEmpty()
            setTextViewText(R.id.cal_line_top, top)
            setTextViewText(R.id.cal_line_main, main)
            setTextViewText(R.id.cal_line_sub, sub)
            setContentDescription(R.id.cal_root, listOf(app.getString(R.string.prayer_next), top, main, sub).filter { it.isNotEmpty() }.joinToString("، "))
            setOnClickPendingIntent(R.id.cal_root, openPrayer(appWidgetId))
        }

    private fun dayViews(appWidgetId: Int, schedule: PrayerSchedule?, text: PrayerText, calendarText: CalendarText): RemoteViews {
        val views = RemoteViews(app.packageName, R.layout.anaa_widget_prayer_day)
        views.setOnClickPendingIntent(R.id.pd_root, openPrayer(appWidgetId))
        val tall = (manager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)) >= HEADER_MIN_HEIGHT_DP
        if (schedule == null) {
            views.setViewVisibility(R.id.pd_header, View.VISIBLE)
            views.setTextViewText(R.id.pd_header, app.getString(R.string.prayer_choose_place))
            CELLS.forEach { (cell, name, time) ->
                views.setTextViewText(name, "")
                views.setTextViewText(time, "")
                views.setInt(cell, "setBackgroundResource", 0)
            }
            return views
        }
        // After Isha the widget already shows the next day, whose Fajr is the next prayer.
        val next = schedule.next(Instant.now())
        val day = schedule.day(next.time.toLocalDate())
        views.setViewVisibility(R.id.pd_header, if (tall) View.VISIBLE else View.GONE)
        views.setTextViewText(
            R.id.pd_header,
            listOf(schedule.place.name, calendarText.hijriDate(day.date)).filter { it.isNotEmpty() }.joinToString(" – "),
        )
        ObligatoryPrayers.forEachIndexed { index, prayer ->
            val (cell, name, time) = CELLS[index]
            val isNext = prayer == next.prayer
            views.setTextViewText(name, text.name(prayer, day[prayer]))
            views.setTextViewText(time, text.time(day[prayer]))
            views.setInt(cell, "setBackgroundResource", if (isNext) R.drawable.anaa_widget_today else 0)
            applyColor(views, name, if (isNext) R.color.anaa_widget_on_today else R.color.anaa_widget_text_soft)
            applyColor(views, time, if (isNext) R.color.anaa_widget_on_today else R.color.anaa_widget_text)
        }
        return views
    }

    /** From Android 12 the launcher resolves the colour itself, so it follows day and night mode. */
    private fun applyColor(views: RemoteViews, viewId: Int, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setTextColor", color)
        } else {
            views.setTextColor(viewId, app.getColor(color))
        }
    }

    private fun openPrayer(appWidgetId: Int): PendingIntent = PendingIntent.getActivity(
        app,
        appWidgetId,
        MainActivity.prayerIntent(app).setData(Uri.parse("anaa://prayer/$appWidgetId")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val HEADER_MIN_HEIGHT_DP = 100
        val CELLS = listOf(
            Triple(R.id.pd_cell_0, R.id.pd_name_0, R.id.pd_time_0),
            Triple(R.id.pd_cell_1, R.id.pd_name_1, R.id.pd_time_1),
            Triple(R.id.pd_cell_2, R.id.pd_name_2, R.id.pd_time_2),
            Triple(R.id.pd_cell_3, R.id.pd_name_3, R.id.pd_time_3),
            Triple(R.id.pd_cell_4, R.id.pd_name_4, R.id.pd_time_4),
        )
    }
}
