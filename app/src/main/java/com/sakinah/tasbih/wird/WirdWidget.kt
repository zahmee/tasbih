package com.sakinah.tasbih.wird

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.widget.runAsync

/**
 * Today's wird on the home screen, as on November 23, 2026 the user asked: each task with a ring
 * that ticks it done, without opening the app. A tap on a task's name opens it in the app, or
 * its adhkar when the task is their reading. It is drawn again whenever the wird changes, and
 * at the turn of the app's day, which the wird's alarm keeps while the widget is placed.
 */
class WirdWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync { WirdSync.changed(context) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        runAsync { WirdSync.refreshViews(context) }
    }

    override fun onEnabled(context: Context) {
        runAsync { WirdAlarms.schedule(context) }
    }

    override fun onDisabled(context: Context) {
        runAsync { WirdAlarms.schedule(context) }
    }

    companion object {
        private val ROWS = listOf(
            Row(R.id.wird_row_0, R.id.wird_check_0, R.id.wird_title_0, R.id.wird_time_0),
            Row(R.id.wird_row_1, R.id.wird_check_1, R.id.wird_title_1, R.id.wird_time_1),
            Row(R.id.wird_row_2, R.id.wird_check_2, R.id.wird_title_2, R.id.wird_time_2),
            Row(R.id.wird_row_3, R.id.wird_check_3, R.id.wird_title_3, R.id.wird_time_3),
            Row(R.id.wird_row_4, R.id.wird_check_4, R.id.wird_title_4, R.id.wird_time_4),
            Row(R.id.wird_row_5, R.id.wird_check_5, R.id.wird_title_5, R.id.wird_time_5),
        )

        private fun provider(context: Context) = ComponentName(context, WirdWidget::class.java)

        fun placedIds(context: Context): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(provider(context))

        fun anyPlaced(context: Context): Boolean = placedIds(context).isNotEmpty()

        fun updateAll(context: Context, board: WirdBoard) {
            val manager = AppWidgetManager.getInstance(context)
            val text = WirdText(context)
            placedIds(context).forEach { id -> manager.updateAppWidget(id, views(context, manager, id, board, text)) }
        }

        private fun views(context: Context, manager: AppWidgetManager, appWidgetId: Int, board: WirdBoard, text: WirdText): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.anaa_widget_wird)
            // Kept in the order of their times, ticked or not, so a row never moves under the finger.
            val items = board.todayItems.sortedWith(compareBy<WirdItem> { it.time == null }.thenBy { it.time?.toInstant() }.thenBy { it.task.sortOrder })
            views.setTextViewText(
                R.id.wird_count,
                context.getString(R.string.wird_count, arabicNumber(board.doneToday), arabicNumber(board.totalToday)),
            )
            views.setOnClickPendingIntent(R.id.wird_header, open(context, appWidgetId, null))
            val height = manager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            // The heading and the margins take some 44dp; each task a row of 36dp.
            val room = if (height <= 0) 3 else ((height - 44) / 36).coerceIn(1, ROWS.size)
            val shown = if (items.size > room) (room - 1).coerceAtLeast(1) else items.size
            ROWS.forEachIndexed { index, row ->
                val item = items.getOrNull(index)?.takeIf { index < shown }
                if (item == null) {
                    views.setViewVisibility(row.root, View.GONE)
                    return@forEachIndexed
                }
                views.setViewVisibility(row.root, View.VISIBLE)
                views.setImageViewResource(row.check, if (item.done) R.drawable.anaa_widget_check_on else R.drawable.anaa_widget_check_off)
                views.setContentDescription(
                    row.check,
                    context.getString(if (item.done) R.string.wird_untick else R.string.wird_tick, item.task.title),
                )
                views.setTextViewText(row.title, item.task.title)
                views.setTextColor(row.title, context.getColor(if (item.done) R.color.anaa_widget_text_soft else R.color.anaa_widget_text))
                val time = when {
                    item.late -> context.getString(R.string.wird_late)
                    else -> text.clock(item).orEmpty()
                }
                views.setTextViewText(row.time, time)
                views.setOnClickPendingIntent(
                    row.check,
                    WirdNotifier.action(context, WirdReceiver.ACTION_TOGGLE, item.task.id, item.day, 3),
                )
                views.setOnClickPendingIntent(row.title, open(context, appWidgetId, item.task))
            }
            val more = items.size - shown
            views.setViewVisibility(R.id.wird_more, if (more > 0) View.VISIBLE else View.GONE)
            if (more > 0) views.setTextViewText(R.id.wird_more, context.getString(R.string.wird_widget_more, arabicNumber(more)))
            views.setOnClickPendingIntent(R.id.wird_more, open(context, appWidgetId, null))
            views.setViewVisibility(R.id.wird_empty, if (items.isEmpty()) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(R.id.wird_empty, open(context, appWidgetId, null))
            return views
        }

        private fun open(context: Context, appWidgetId: Int, task: WirdTask?): PendingIntent {
            val intent = MainActivity.wirdIntent(context, task?.collectionId)
                .setData(Uri.parse("anaa://wird-widget/$appWidgetId/${task?.id ?: 0}"))
            return PendingIntent.getActivity(
                context,
                appWidgetId * 64 + ((task?.id ?: 0) % 64).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private data class Row(val root: Int, val check: Int, val title: Int, val time: Int)
    }
}
