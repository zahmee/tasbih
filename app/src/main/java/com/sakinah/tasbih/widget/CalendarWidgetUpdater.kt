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
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import android.net.Uri
import com.sakinah.tasbih.MainActivity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.CalendarMonth
import com.sakinah.tasbih.calendar.AgendaEvent
import com.sakinah.tasbih.calendar.DayColor
import com.sakinah.tasbih.calendar.DeviceAgenda
import com.sakinah.tasbih.calendar.GridStyle
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
        val maghrib = maghribToday(text)
        CalendarWidgetKind.entries.forEach { kind ->
            kind.placedIds(appContext).forEach { draw(kind, it, text, maghrib) }
        }
    }

    /** Today's Maghrib, needed only when the user lets the Hijri date turn at it. */
    private suspend fun maghribToday(text: CalendarText): java.time.Instant? {
        if (!text.settings.hijriDayFromMaghrib) return null
        val prayer = com.sakinah.tasbih.data.PrayerPreferencesRepository(appContext).current()
        val place = prayer.place ?: return null
        return com.sakinah.tasbih.prayer.PrayerSchedule(place, prayer, text.calendar)
            .day(LocalDate.now())[com.sakinah.tasbih.prayer.Prayer.MAGHRIB].toInstant()
    }

    suspend fun update(appWidgetId: Int) {
        val kind = kindOf(appWidgetId) ?: return
        val text = CalendarText(appContext, repository.current())
        draw(kind, appWidgetId, text, maghribToday(text))
    }

    suspend fun turnMonth(appWidgetId: Int, direction: Int) {
        repository.turnMonthWidget(appWidgetId, direction, LocalDate.now().toEpochDay())
        update(appWidgetId)
    }

    suspend fun forget(appWidgetId: Int) = repository.removeMonthWidget(appWidgetId)

    /** The switch at the top of the month widget leads with [primary], here and in the app alike. */
    suspend fun setPrimary(primary: PrimaryCalendar) {
        repository.setPrimary(primary)
        updateAll()
    }

    private fun kindOf(appWidgetId: Int): CalendarWidgetKind? {
        val provider = manager.getAppWidgetInfo(appWidgetId)?.provider ?: return null
        return CalendarWidgetKind.entries.firstOrNull { it.receiver.name == provider.className }
    }

    private fun draw(kind: CalendarWidgetKind, appWidgetId: Int, text: CalendarText, maghrib: java.time.Instant?) {
        val views = when (kind) {
            CalendarWidgetKind.DATE -> dateViews(appWidgetId, text, maghrib)
            CalendarWidgetKind.OCCASION -> occasionViews(appWidgetId, text)
            CalendarWidgetKind.MONTH -> monthViews(appWidgetId, text)
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun dateViews(appWidgetId: Int, text: CalendarText, maghrib: java.time.Instant?): RemoteViews {
        val today = LocalDate.now()
        val hijri = text.hijriDate(com.sakinah.tasbih.calendar.hijriDayFor(today, java.time.Instant.now(), maghrib, text.settings.hijriDayFromMaghrib))
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

        // The switch: the leading calendar filled, the other outlined.
        listOf(PrimaryCalendar.HIJRI to R.id.cal_pick_hijri, PrimaryCalendar.GREGORIAN to R.id.cal_pick_gregorian).forEach { (choice, id) ->
            val on = settings.primary == choice
            views.setInt(id, "setBackgroundResource", if (on) R.drawable.anaa_widget_chip_on else R.drawable.anaa_widget_chip_off)
            applyColor(views, id, if (on) R.color.anaa_widget_chip_text else R.color.anaa_widget_text)
            // Widgets cannot mark a view selected, so the description says which calendar leads.
            val name = appContext.getString(if (choice == PrimaryCalendar.HIJRI) R.string.cal_hijri else R.string.cal_gregorian)
            views.setContentDescription(id, if (on) appContext.getString(R.string.cal_widget_leading, name) else name)
            views.setOnClickPendingIntent(id, primaryIntent(appWidgetId, choice))
        }

        val firstDay = text.firstDayOfWeek()
        val columns = weekDays(firstDay)
        columns.forEachIndexed { index, day ->
            views.setTextViewText(WEEKDAY_IDS[index], text.weekdayNarrow(day))
            // Days off are named in copper, as in the app.
            applyColor(views, WEEKDAY_IDS[index], if (day in settings.weekendDays) R.color.anaa_widget_occasion else R.color.anaa_widget_text_soft)
        }

        val sizes = DaySizes.of(appContext, appWidgetId)
        val days = month.days(calendar)
        val events = agendaFor(text, days.firstOrNull(), days.lastOrNull())
        views.removeAllViews(R.id.cal_grid)
        val grid = buildMonthGrid(month, calendar, firstDay)
        val lastWeek = grid.indexOfLast { week -> week.any { it != null } }
        grid.forEachIndexed { weekIndex, week ->
            val row = RemoteViews(appContext.packageName, R.layout.anaa_widget_month_row)
            week.forEachIndexed { index, cell ->
                val off = columns[index] in settings.weekendDays && weekIndex <= lastWeek
                drawDay(row, index, cell, text, today, sizes, off, cell?.let { events[it.date] }.orEmpty())
            }
            views.addView(R.id.cal_grid, row)
        }
        val shownDay = month.days(calendar).firstOrNull() ?: today
        views.setOnClickPendingIntent(R.id.cal_grid, openCalendar(appWidgetId, shownDay, showOccasions = false))
        return views
    }

    /**
     * One day drawn as in the app: today in a faint square, days off in their band, each in a
     * square of its own in the table, the user's colours for the numbers, and the phone's events
     * as dots. Every state is set on both paths, since a launcher may reapply an update onto older views.
     */
    private fun drawDay(
        row: RemoteViews,
        index: Int,
        cell: DayCell?,
        text: CalendarText,
        today: LocalDate,
        sizes: DaySizes,
        isDayOff: Boolean,
        events: List<AgendaEvent>,
    ) {
        val dayId = DAY_IDS[index]
        val otherId = OTHER_IDS[index]
        val dotsId = DOTS_IDS[index]
        val settings = text.settings
        val table = settings.gridStyle == GridStyle.TABLE
        if (cell == null) {
            row.setTextViewText(dayId, "")
            row.setTextViewText(otherId, "")
            row.setTextViewText(dotsId, "")
            row.setViewVisibility(dotsId, View.GONE)
            // The band runs on through an empty day; in the table, an empty day has no square at all.
            row.setInt(CELL_IDS[index], "setBackgroundResource", if (isDayOff && !table) R.drawable.anaa_widget_day_off else 0)
            return
        }
        val hijriFirst = settings.primary == PrimaryCalendar.HIJRI
        val gregorianDay = text.number(cell.date.dayOfMonth)
        val hijriDay = cell.hijri?.let { text.number(it.day) }.orEmpty()
        val isToday = cell.date == today
        val primary = if (hijriFirst) hijriDay else gregorianDay
        row.setTextViewText(dayId, if (isToday) bold(primary) else primary)
        row.setTextViewText(otherId, if (hijriFirst) gregorianDay else hijriDay)
        row.setInt(
            CELL_IDS[index],
            "setBackgroundResource",
            when {
                isToday -> R.drawable.anaa_widget_today
                table && isDayOff -> R.drawable.anaa_widget_cell_day_off
                table -> R.drawable.anaa_widget_cell
                isDayOff -> R.drawable.anaa_widget_day_off
                else -> 0
            },
        )

        val hasOccasion = cell.hijri?.let { Occasions.on(it).isNotEmpty() } == true ||
            text.personalOn(cell.date).isNotEmpty()
        val hijriInk = settings.hijriColor.widgetColor()
        val gregorianInk = settings.gregorianColor.widgetColor()
        val largeInk = if (hijriFirst) hijriInk else gregorianInk
        val smallInk = if (hijriFirst) gregorianInk else hijriInk
        applyColor(
            row,
            dayId,
            largeInk ?: when {
                isToday -> R.color.anaa_widget_accent
                hasOccasion -> R.color.anaa_widget_occasion
                else -> R.color.anaa_widget_text
            },
        )
        applyColor(row, otherId, smallInk ?: if (isToday) R.color.anaa_widget_accent else R.color.anaa_widget_text_soft)
        row.setTextViewTextSize(dayId, TypedValue.COMPLEX_UNIT_DIP, sizes.day)
        row.setTextViewTextSize(otherId, TypedValue.COMPLEX_UNIT_DIP, sizes.other)
        row.setViewVisibility(otherId, if (sizes.showOther) View.VISIBLE else View.GONE)

        // A dot for each colour among the day's events, up to three, on a line of their own.
        val dotColors = events.map { event -> settings.agendaColors[event.calendarId]?.widgetColor()?.let(appContext::getColor) ?: (event.color or OPAQUE) }
            .distinct().take(MAX_DOTS)
        if (dotColors.isEmpty()) {
            row.setTextViewText(dotsId, "")
            row.setViewVisibility(dotsId, View.GONE)
        } else {
            val dots = SpannableStringBuilder()
            dotColors.forEachIndexed { i, color ->
                if (i > 0) dots.append(' ')
                val start = dots.length
                dots.append(DOT)
                dots.setSpan(ForegroundColorSpan(color), start, dots.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            row.setTextViewText(dotsId, dots)
            row.setViewVisibility(dotsId, View.VISIBLE)
        }
    }

    /** The phone's events for the days shown, by day, while the user shows them and allowed it. */
    private fun agendaFor(text: CalendarText, from: LocalDate?, to: LocalDate?): Map<LocalDate, List<AgendaEvent>> {
        if (from == null || to == null || !text.settings.agendaEnabled || !DeviceAgenda.granted(appContext)) return emptyMap()
        return DeviceAgenda.events(appContext, from, to)
            .mapValues { (_, list) -> list.filter { it.calendarId !in text.settings.agendaHidden } }
    }

    /** The colour resource of a chosen day colour, light or dark with the home screen. Null keeps the widget's ink. */
    @ColorRes
    private fun DayColor.widgetColor(): Int? = when (this) {
        DayColor.THEME -> null
        DayColor.GREEN -> R.color.anaa_widget_ink_green
        DayColor.COPPER -> R.color.anaa_widget_ink_copper
        DayColor.BLUE -> R.color.anaa_widget_ink_blue
        DayColor.RED -> R.color.anaa_widget_ink_red
        DayColor.PURPLE -> R.color.anaa_widget_ink_purple
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

    private fun primaryIntent(appWidgetId: Int, primary: PrimaryCalendar): PendingIntent {
        val intent = Intent(appContext, CalendarMonthWidget::class.java)
            .setAction(BaseCalendarWidget.ACTION_SET_PRIMARY)
            .setData(Uri.parse("anaa://calendar-primary/$appWidgetId/${primary.name}"))
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(BaseCalendarWidget.EXTRA_PRIMARY, primary.name)
        return PendingIntent.getBroadcast(
            appContext,
            appWidgetId * 4 + primary.ordinal + 1,
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
            // The switch, the title row and the weekday names.
            private const val HEADER_DP = 34 + 44 + 20
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
        val DOTS_IDS = intArrayOf(
            R.id.cal_dots_0, R.id.cal_dots_1, R.id.cal_dots_2, R.id.cal_dots_3,
            R.id.cal_dots_4, R.id.cal_dots_5, R.id.cal_dots_6,
        )
        const val DOT = "\u25CF"
        const val MAX_DOTS = 3
        const val OPAQUE = 0xFF shl 24
        val OTHER_IDS = intArrayOf(
            R.id.cal_other_0, R.id.cal_other_1, R.id.cal_other_2, R.id.cal_other_3,
            R.id.cal_other_4, R.id.cal_other_5, R.id.cal_other_6,
        )
    }
}
