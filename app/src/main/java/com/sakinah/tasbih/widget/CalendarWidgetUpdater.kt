package com.sakinah.tasbih.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import android.net.Uri
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.CalendarMonth
import com.sakinah.tasbih.calendar.DAYS_IN_WEEK
import com.sakinah.tasbih.calendar.DayCell
import com.sakinah.tasbih.calendar.Occasions
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.calendar.WEEKS
import com.sakinah.tasbih.calendar.buildMonthGrid
import com.sakinah.tasbih.calendar.weekDays
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.ui.calendar.CalendarText
import java.time.LocalDate

/**
 * Draws the calendar widgets. Each is text on Anaa's paper with no pictures, so they stay far
 * below the memory Android 17 allows a widget, and no view in them scrolls.
 */
class CalendarWidgetUpdater(context: Context) {
    private val appContext = context.applicationContext
    private val repository = CalendarPreferencesRepository(appContext)
    private val manager get() = AppWidgetManager.getInstance(appContext)

    suspend fun updateAll() {
        if (!CalendarWidgetKind.anyPlaced(appContext)) return
        val text = CalendarText(appContext, repository.current())
        CalendarWidgetKind.entries.forEach { kind ->
            kind.placedIds(appContext).forEach { draw(kind, it, text) }
        }
    }

    suspend fun update(appWidgetId: Int) {
        val kind = kindOf(appWidgetId) ?: return
        draw(kind, appWidgetId, CalendarText(appContext, repository.current()))
    }

    suspend fun turnMonth(appWidgetId: Int, direction: Int) {
        repository.turnMonthWidget(appWidgetId, direction, LocalDate.now().toEpochDay())
        update(appWidgetId)
    }

    suspend fun forget(appWidgetId: Int) = repository.removeMonthWidget(appWidgetId)

    private fun kindOf(appWidgetId: Int): CalendarWidgetKind? {
        val provider = manager.getAppWidgetInfo(appWidgetId)?.provider ?: return null
        return CalendarWidgetKind.entries.firstOrNull { it.receiver.name == provider.className }
    }

    private fun draw(kind: CalendarWidgetKind, appWidgetId: Int, text: CalendarText) {
        val views = when (kind) {
            CalendarWidgetKind.DATE -> dateViews(appWidgetId, text)
            CalendarWidgetKind.OCCASION -> occasionViews(appWidgetId, text)
            CalendarWidgetKind.MONTH -> monthViews(appWidgetId, text)
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun dateViews(appWidgetId: Int, text: CalendarText): RemoteViews {
        val today = LocalDate.now()
        val hijri = text.hijriDate(today)
        val gregorian = text.gregorianDate(today)
        val hijriFirst = text.settings.primary == PrimaryCalendar.HIJRI && hijri.isNotEmpty()
        return lineViews(
            top = text.weekday(today.dayOfWeek),
            main = if (hijriFirst) hijri else gregorian,
            sub = if (hijriFirst) gregorian else hijri,
            onClick = openCalendar(appWidgetId, today, showOccasions = false),
        )
    }

    private fun occasionViews(appWidgetId: Int, text: CalendarText): RemoteViews {
        val today = LocalDate.now()
        val next = Occasions.nextMajor(text.calendar, today)
        return lineViews(
            top = next?.let { text.occasionName(it.occasion) }.orEmpty(),
            main = next?.let { text.countdown(it.daysAway) }.orEmpty(),
            sub = next?.let { text.gregorianDate(it.date) }.orEmpty(),
            onClick = openCalendar(appWidgetId, today, showOccasions = true),
        )
    }

    private fun lineViews(top: String, main: String, sub: String, onClick: PendingIntent) =
        RemoteViews(appContext.packageName, R.layout.anaa_widget_lines).apply {
            setTextViewText(R.id.cal_line_top, top)
            setTextViewText(R.id.cal_line_main, main)
            setTextViewText(R.id.cal_line_sub, sub)
            setContentDescription(R.id.cal_root, listOf(top, main, sub).filter { it.isNotEmpty() }.joinToString("، "))
            setOnClickPendingIntent(R.id.cal_root, onClick)
        }

    private fun monthViews(appWidgetId: Int, text: CalendarText): RemoteViews {
        val settings = text.settings
        val calendar = text.calendar
        val today = LocalDate.now()
        val views = RemoteViews(appContext.packageName, R.layout.anaa_widget_month)

        val (first, last) = CalendarMonth.range(settings.primary, calendar)
        val thisMonth = CalendarMonth.containing(today, settings.primary, calendar) ?: first
        val page = settings.monthWidgetPage(appWidgetId, today.toEpochDay())
            .coerceIn(first.minus(thisMonth), last.minus(thisMonth))
        val month = thisMonth.plus(page)

        views.setTextViewText(R.id.cal_title, text.monthTitle(month))
        views.setTextViewText(R.id.cal_subtitle, text.monthSubtitle(month))
        views.setViewVisibility(R.id.cal_previous, if (month == first) View.INVISIBLE else View.VISIBLE)
        views.setViewVisibility(R.id.cal_next, if (month == last) View.INVISIBLE else View.VISIBLE)
        views.setOnClickPendingIntent(R.id.cal_previous, monthPageIntent(appWidgetId, -1))
        views.setOnClickPendingIntent(R.id.cal_next, monthPageIntent(appWidgetId, 1))
        views.setOnClickPendingIntent(R.id.cal_title_box, monthPageIntent(appWidgetId, 0))

        val firstDay = text.firstDayOfWeek()
        weekDays(firstDay).forEachIndexed { index, day ->
            views.setTextViewText(WEEKDAY_IDS[index], text.weekdayNarrow(day))
        }

        val sizes = DaySizes.of(appContext, appWidgetId)
        views.removeAllViews(R.id.cal_grid)
        buildMonthGrid(month, calendar, firstDay).forEach { week ->
            val row = RemoteViews(appContext.packageName, R.layout.anaa_widget_month_row)
            week.forEachIndexed { index, cell -> drawDay(row, index, cell, text, today, sizes) }
            views.addView(R.id.cal_grid, row)
        }
        val shownDay = month.days(calendar).firstOrNull() ?: today
        views.setOnClickPendingIntent(R.id.cal_grid, openCalendar(appWidgetId, shownDay, showOccasions = false))
        return views
    }

    /** Every state is set on both paths, since a launcher may reapply an update onto older views. */
    private fun drawDay(row: RemoteViews, index: Int, cell: DayCell?, text: CalendarText, today: LocalDate, sizes: DaySizes) {
        val dayId = DAY_IDS[index]
        val otherId = OTHER_IDS[index]
        if (cell == null) {
            row.setTextViewText(dayId, "")
            row.setTextViewText(otherId, "")
            row.setInt(CELL_IDS[index], "setBackgroundResource", 0)
            return
        }
        val hijriFirst = text.settings.primary == PrimaryCalendar.HIJRI
        val gregorianDay = text.number(cell.date.dayOfMonth)
        val hijriDay = cell.hijri?.let { text.number(it.day) }.orEmpty()
        val isToday = cell.date == today
        val primary = if (hijriFirst) hijriDay else gregorianDay
        row.setTextViewText(dayId, if (isToday) bold(primary) else primary)
        row.setTextViewText(otherId, if (hijriFirst) gregorianDay else hijriDay)
        row.setInt(CELL_IDS[index], "setBackgroundResource", if (isToday) R.drawable.anaa_widget_today else 0)

        val hasOccasion = cell.hijri?.let { Occasions.on(it, text.settings.showOtherOccasions).isNotEmpty() } == true
        when {
            isToday -> {
                applyColor(row, dayId, R.color.anaa_widget_on_today)
                applyColor(row, otherId, R.color.anaa_widget_on_today)
            }
            hasOccasion -> {
                applyColor(row, dayId, R.color.anaa_widget_occasion)
                applyColor(row, otherId, R.color.anaa_widget_text_soft)
            }
            else -> {
                applyColor(row, dayId, R.color.anaa_widget_text)
                applyColor(row, otherId, R.color.anaa_widget_text_soft)
            }
        }
        row.setTextViewTextSize(dayId, TypedValue.COMPLEX_UNIT_DIP, sizes.day)
        row.setTextViewTextSize(otherId, TypedValue.COMPLEX_UNIT_DIP, sizes.other)
        row.setViewVisibility(otherId, if (sizes.showOther) View.VISIBLE else View.GONE)
    }

    /**
     * From Android 12 the launcher resolves the color itself, so it follows day and night mode.
     * Older versions take the color as it is now.
     */
    private fun applyColor(views: RemoteViews, viewId: Int, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setTextColor", color)
        } else {
            views.setTextColor(viewId, appContext.getColor(color))
        }
    }

    private fun bold(text: String): CharSequence = SpannableString(text).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    private fun openCalendar(appWidgetId: Int, date: LocalDate, showOccasions: Boolean): PendingIntent {
        val intent = MainActivity.calendarIntent(appContext, date, showOccasions)
            .setData(Uri.parse("anaa://calendar/$appWidgetId"))
        return PendingIntent.getActivity(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun monthPageIntent(appWidgetId: Int, direction: Int): PendingIntent {
        val intent = Intent(appContext, CalendarMonthWidget::class.java)
            .setAction(BaseCalendarWidget.ACTION_MONTH_PAGE)
            .setData(Uri.parse("anaa://calendar-page/$appWidgetId/$direction"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(BaseCalendarWidget.EXTRA_DIRECTION, direction)
        return PendingIntent.getBroadcast(
            appContext,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Text sizes for one day of the month widget, so six weeks fit whatever size it was given. */
    private data class DaySizes(val day: Float, val other: Float, val showOther: Boolean) {
        companion object {
            private const val FALLBACK_WIDTH_DP = 300
            private const val FALLBACK_HEIGHT_DP = 300
            private const val PADDING_DP = 16
            private const val HEADER_DP = 44 + 20
            private const val CELL_MARGIN_DP = 2
            private const val MIN_HEIGHT_FOR_OTHER_DP = 28f

            fun of(context: Context, appWidgetId: Int): DaySizes {
                val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
                val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val width = options.getInt(
                    if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                ).takeIf { it > 0 } ?: FALLBACK_WIDTH_DP
                val height = options.getInt(
                    if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                ).takeIf { it > 0 } ?: FALLBACK_HEIGHT_DP
                val cellWidth = (width - PADDING_DP) / DAYS_IN_WEEK.toFloat() - CELL_MARGIN_DP
                val cellHeight = (height - PADDING_DP - HEADER_DP) / WEEKS.toFloat() - CELL_MARGIN_DP
                val showOther = cellHeight >= MIN_HEIGHT_FOR_OTHER_DP
                val day = if (showOther) {
                    minOf(cellHeight * 0.48f, cellWidth * 0.42f, 20f)
                } else {
                    minOf(cellHeight * 0.68f, cellWidth * 0.42f, 18f)
                }.coerceAtLeast(9f)
                return DaySizes(day, (day * 0.62f).coerceAtLeast(8f), showOther)
            }
        }
    }

    private companion object {
        val WEEKDAY_IDS = intArrayOf(
            R.id.cal_weekday_0, R.id.cal_weekday_1, R.id.cal_weekday_2, R.id.cal_weekday_3,
            R.id.cal_weekday_4, R.id.cal_weekday_5, R.id.cal_weekday_6,
        )
        val CELL_IDS = intArrayOf(
            R.id.cal_cell_0, R.id.cal_cell_1, R.id.cal_cell_2, R.id.cal_cell_3,
            R.id.cal_cell_4, R.id.cal_cell_5, R.id.cal_cell_6,
        )
        val DAY_IDS = intArrayOf(
            R.id.cal_day_0, R.id.cal_day_1, R.id.cal_day_2, R.id.cal_day_3,
            R.id.cal_day_4, R.id.cal_day_5, R.id.cal_day_6,
        )
        val OTHER_IDS = intArrayOf(
            R.id.cal_other_0, R.id.cal_other_1, R.id.cal_other_2, R.id.cal_other_3,
            R.id.cal_other_4, R.id.cal_other_5, R.id.cal_other_6,
        )
    }
}
