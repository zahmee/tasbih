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
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
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
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetTicker.ACTION_TICK) {
            runAsync { PrayerWidgetUpdater(context).updateAll() }
            return
        }
        super.onReceive(context, intent)
    }

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
        if (!PrayerWidgetKind.anyAtAll(app)) {
            WidgetTicker.set(app, on = false)
            return
        }
        val settings = PrayerPreferencesRepository(app).current()
        val calendarSettings = CalendarPreferencesRepository(app).current()
        val schedule = settings.place?.let { PrayerSchedule(it, settings, HijriCalendar.from(calendarSettings)) }
        val calendarText = CalendarText(app, calendarSettings)
        val text = PrayerText(app)
        val style = PrayerWidgetStyleRepository(app).current()
        val nextIds = PrayerWidgetKind.NEXT.placedIds(app)
        val dayIds = PrayerWidgetKind.DAY.placedIds(app)
        nextIds.forEach { manager.updateAppWidget(it, nextViews(it, schedule, text, calendarText, style)) }
        dayIds.forEach { manager.updateAppWidget(it, dayViews(it, schedule, text, calendarText, style)) }
        // Only a bar has to move between prayers; the countdown ticks by itself.
        WidgetTicker.set(app, on = style.progress && schedule != null && (nextIds.isNotEmpty() || dayIds.isNotEmpty()))
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

    /** The ids of the parts both prayer widgets share. */
    private data class Chrome(
        val root: Int,
        val bg: Int,
        val pattern: Int,
        val divider: Int,
        val progress: Int,
        val lines: List<LineIds>,
    )

    /** What a widget is drawn with, worked out once per update. */
    private inner class Look(val style: PrayerWidgetStyle) {
        val night = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val palette = WidgetPalette.of(style, night)
        // Anaa's own paper and ink follow day and night by themselves from Android 12.
        val followsTheme = style.surface == WidgetSurface.AUTO && style.ink == WidgetInk.AUTO
        val scale = when (style.scale) {
            WidgetScale.SMALL -> 0.86f
            WidgetScale.MEDIUM -> 1f
            WidgetScale.LARGE -> 1.18f
        }
        val pattern = style.ornament == WidgetOrnament.PATTERN || style.ornament == WidgetOrnament.FRAME
        val divider = style.ornament != WidgetOrnament.NONE
    }

    /** The paper, the pattern over it, and the ruled star's colour, as the user chose them. */
    private fun drawChrome(views: RemoteViews, ids: Chrome, look: Look, appWidgetId: Int) {
        val palette = look.palette
        tintImage(views, ids.bg, palette.surface, if (look.style.surface == WidgetSurface.AUTO) R.color.anaa_widget_surface else null)
        views.setInt(ids.bg, "setImageAlpha", look.style.opacity * 255 / 100)
        val density = app.resources.displayMetrics.density
        views.setViewPadding(ids.root, (10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
        if (look.pattern) {
            // Drawn at the widget's own size, so its stars keep their shape whatever the size.
            val options = manager.getAppWidgetOptions(appWidgetId)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).takeIf { it > 0 } ?: 250
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: 110
            val radius = app.resources.getDimension(R.dimen.anaa_widget_radius) / density
            val color = (palette.accent and 0x00FFFFFF) or (palette.patternAlpha shl 24)
            views.setImageViewBitmap(ids.pattern, IslamicPattern.bitmap(width, height, density, radius, color))
            views.setViewVisibility(ids.pattern, View.VISIBLE)
        } else {
            views.setViewVisibility(ids.pattern, View.GONE)
        }
        tintImage(views, ids.divider, palette.accent, null)
        views.setInt(ids.divider, "setImageAlpha", 170)
    }

    /**
     * The bar of the time passed since the last prayer. It moves as the widget is drawn again,
     * which [WidgetTicker] asks for every few minutes while the screen is on.
     */
    private fun drawProgress(views: RemoteViews, id: Int, look: Look, schedule: PrayerSchedule, now: Instant) {
        if (!look.style.progress) {
            views.setViewVisibility(id, View.GONE)
            return
        }
        val share = passedShare(schedule.previous(now).time.toInstant(), schedule.next(now).time.toInstant(), now)
        views.setViewVisibility(id, View.VISIBLE)
        views.setProgressBar(id, PROGRESS_MAX, share, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(id, "setProgressTintList", ColorStateList.valueOf(look.palette.accent))
            views.setColorStateList(id, "setProgressBackgroundTintList", ColorStateList.valueOf(look.palette.textSoft and 0x00FFFFFF or (0x40 shl 24)))
        }
    }

    /** The user's lines, each side text or a countdown; returns what they say, for the screen reader. */
    private fun drawLines(
        views: RemoteViews,
        all: List<LineIds>,
        shown: List<WidgetLine>,
        look: Look,
        schedule: PrayerSchedule,
        day: com.sakinah.tasbih.prayer.PrayerDay,
        text: PrayerText,
        calendarText: CalendarText,
        now: Instant,
        firstIsHeading: Boolean,
    ): List<String> {
        val palette = look.palette
        val next = schedule.next(now)
        val said = mutableListOf<String>()
        all.forEachIndexed { index, ids ->
            val line = shown.getOrNull(index)
            views.setViewVisibility(ids.row, if (line == null) View.GONE else View.VISIBLE)
            if (line == null) return@forEachIndexed
            val single = line.right == LineItem.NONE || line.left == LineItem.NONE
            listOf(
                Triple(line.right, ids.right, ids.rightClock),
                Triple(line.left, ids.left, ids.leftClock),
            ).forEachIndexed { side, (item, textId, clockId) ->
                if (item == LineItem.NONE) {
                    hide(views, textId, clockId)
                    return@forEachIndexed
                }
                // The user's colour for the line; else a heading line is in the accent, the rest in the soft ink.
                val heading = firstIsHeading && index == 0
                val chosen = palette.line(line.color)
                val color = chosen ?: if (heading) palette.accent else palette.textSoft
                val auto = if (chosen != null || heading || !look.followsTheme) null else R.color.anaa_widget_text_soft
                val gravity = when {
                    single -> Gravity.CENTER
                    side == 0 -> Gravity.START or Gravity.CENTER_VERTICAL
                    else -> Gravity.END or Gravity.CENTER_VERTICAL
                }
                if (item == LineItem.COUNTDOWN) {
                    // A Chronometer counts down in the launcher itself, so the widget needs no
                    // wake every second; the alarm at the prayer's time draws the next one.
                    views.setViewVisibility(textId, View.GONE)
                    views.setViewVisibility(clockId, View.VISIBLE)
                    val left = java.time.Duration.between(now, next.time.toInstant()).toMillis().coerceAtLeast(0)
                    views.setChronometer(
                        clockId,
                        SystemClock.elapsedRealtime() + left,
                        app.getString(R.string.widget_countdown, text.name(next.prayer, next.time), "%s"),
                        true,
                    )
                    views.setChronometerCountDown(clockId, true)
                    views.setInt(clockId, "setGravity", gravity)
                    views.setTextViewTextSize(clockId, TypedValue.COMPLEX_UNIT_SP, 12 * look.scale)
                    textColor(views, clockId, color, auto)
                    said += app.getString(R.string.widget_countdown, text.name(next.prayer, next.time), text.time(next.time))
                } else {
                    val words = when (item) {
                        LineItem.HIJRI_DATE -> calendarText.hijriDate(day.date, look.style.hijriMonthNumber)
                        LineItem.GREGORIAN_DATE -> calendarText.gregorianDate(day.date, look.style.gregorianMonthNumber)
                        LineItem.WEEKDAY -> calendarText.weekday(day.date.dayOfWeek)
                        LineItem.PLACE -> schedule.place.name
                        LineItem.NEXT_PRAYER -> "${text.name(next.prayer, next.time)} ${text.time(next.time)}"
                        LineItem.DHIKR -> WidgetDhikr[java.time.LocalDate.now().dayOfYear % WidgetDhikr.size]
                        LineItem.COUNTDOWN, LineItem.NONE -> ""
                    }
                    showText(views, textId, clockId, words, single, look.style.weight)
                    views.setInt(textId, "setGravity", gravity)
                    views.setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, 12 * look.scale)
                    textColor(views, textId, color, auto)
                    said += words
                }
            }
        }
        return said
    }

    private fun widgetHeight(appWidgetId: Int, fallback: Int): Int =
        manager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 } ?: fallback

    private fun nextViews(
        appWidgetId: Int,
        schedule: PrayerSchedule?,
        text: PrayerText,
        calendarText: CalendarText,
        style: PrayerWidgetStyle,
    ): RemoteViews {
        val views = RemoteViews(app.packageName, R.layout.anaa_widget_prayer_next)
        views.setOnClickPendingIntent(R.id.nx_root, openPrayer(appWidgetId))
        val look = Look(style)
        drawChrome(views, NEXT_CHROME, look, appWidgetId)
        val palette = look.palette
        textColor(views, R.id.nx_name, palette.line(style.nameColor) ?: palette.accent, null)
        val timeColor = palette.line(style.timeColor)
        textColor(views, R.id.nx_time, timeColor ?: palette.text, if (timeColor == null && look.followsTheme) R.color.anaa_widget_text else null)
        if (schedule == null) {
            views.setTextViewText(R.id.nx_name, app.getString(R.string.prayer_title))
            views.setTextViewText(R.id.nx_time, app.getString(R.string.prayer_choose_place))
            NEXT_CHROME.lines.forEach { views.setViewVisibility(it.row, View.GONE) }
            views.setViewVisibility(R.id.nx_divider, View.GONE)
            views.setViewVisibility(R.id.nx_progress, View.GONE)
            return views
        }
        val now = Instant.now()
        val next = schedule.next(now)
        val day = schedule.day(next.time.toLocalDate())
        val name = text.name(next.prayer, next.time)
        views.setTextViewText(R.id.nx_name, weighted(name, style.weight))
        views.setTextViewText(R.id.nx_time, weighted(text.time(next.time), style.timeWeight))

        // The name and time come first, then the bar, then the lines; the ruled star last.
        val height = widgetHeight(appWidgetId, 110)
        val lineHeight = 17 * look.scale
        val room = height - 12 - 52 - (if (style.progress) 8 else 0)
        val lines = style.nextLines.filterNot { it.isEmpty }
        val shown = lines.take((room / lineHeight).toInt().coerceIn(0, lines.size))
        val dividerFits = room - shown.size * lineHeight >= DIVIDER_DP
        views.setViewVisibility(R.id.nx_divider, if (look.divider && shown.isNotEmpty() && dividerFits) View.VISIBLE else View.GONE)
        val said = drawLines(views, NEXT_CHROME.lines, shown, look, schedule, day, text, calendarText, now, firstIsHeading = false)
        drawProgress(views, R.id.nx_progress, look, schedule, now)
        views.setContentDescription(
            R.id.nx_root,
            (listOf(app.getString(R.string.prayer_next), name, text.time(next.time)) + said).filter { it.isNotBlank() }.joinToString("، "),
        )
        return views
    }

    private fun dayViews(
        appWidgetId: Int,
        schedule: PrayerSchedule?,
        text: PrayerText,
        calendarText: CalendarText,
        style: PrayerWidgetStyle,
    ): RemoteViews {
        val views = RemoteViews(app.packageName, R.layout.anaa_widget_prayer_day)
        views.setOnClickPendingIntent(R.id.pd_root, openPrayer(appWidgetId))
        val look = Look(style)
        val palette = look.palette
        drawChrome(views, DAY_CHROME, look, appWidgetId)

        if (schedule == null) {
            LINES.forEachIndexed { index, line -> views.setViewVisibility(line.row, if (index == 0) View.VISIBLE else View.GONE) }
            val first = LINES[0]
            showText(views, first.right, first.rightClock, app.getString(R.string.prayer_choose_place), centred = true)
            hide(views, first.left, first.leftClock)
            textColor(views, first.right, palette.accent, null)
            views.setViewVisibility(R.id.pd_divider, View.GONE)
            views.setViewVisibility(R.id.pd_progress, View.GONE)
            CELLS.forEachIndexed { index, (_, name, time) ->
                views.setTextViewText(name, "")
                views.setTextViewText(time, "")
                views.setViewVisibility(MARKS[index], View.INVISIBLE)
            }
            return views
        }

        // After Isha the widget already shows the next day, whose Fajr is the next prayer.
        val now = Instant.now()
        val next = schedule.next(now)
        val day = schedule.day(next.time.toLocalDate())

        // The prayers come first; then the lines, the first ones first; the ruled star only
        // where room is left over.
        val lineHeight = 18 * look.scale
        val room = widgetHeight(appWidgetId, 110) - 12 - 54 * look.scale - (if (style.progress) 8 else 0)
        val lines = style.lines.filterNot { it.isEmpty }
        val shown = lines.take((room / lineHeight).toInt().coerceIn(0, lines.size))
        val dividerFits = room - shown.size * lineHeight >= DIVIDER_DP
        views.setViewVisibility(R.id.pd_divider, if (look.divider && shown.isNotEmpty() && dividerFits) View.VISIBLE else View.GONE)
        val said = drawLines(views, LINES, shown, look, schedule, day, text, calendarText, now, firstIsHeading = true)

        val nameColor = palette.line(style.nameColor)
        val timeColor = palette.line(style.timeColor)
        ObligatoryPrayers.forEachIndexed { index, prayer ->
            val (_, name, time) = CELLS[index]
            val mark = MARKS[index]
            val isNext = prayer == next.prayer
            val nameText = text.name(prayer, day[prayer])
            // The next prayer's name is bold, as its time alone carries the mark.
            views.setTextViewText(name, weighted(nameText, if (isNext) WidgetWeight.BOLD else style.weight))
            views.setTextViewText(time, weighted(text.time(day[prayer]), style.timeWeight))
            views.setTextViewTextSize(name, TypedValue.COMPLEX_UNIT_SP, 13 * look.scale)
            views.setTextViewTextSize(time, TypedValue.COMPLEX_UNIT_SP, 15 * look.scale)
            if (isNext) {
                views.setViewVisibility(mark, View.VISIBLE)
                views.setImageViewResource(
                    mark,
                    if (style.nextMark == NextMark.OUTLINE) R.drawable.anaa_widget_mark_outline else R.drawable.anaa_widget_mark_filled,
                )
                tintImage(views, mark, palette.accent, null)
                views.setInt(mark, "setImageAlpha", palette.markAlpha)
                textColor(views, name, nameColor ?: palette.accent, null)
                textColor(views, time, palette.nextText(style.nextMark), null)
            } else {
                views.setViewVisibility(mark, View.INVISIBLE)
                textColor(views, name, nameColor ?: palette.textSoft, if (nameColor == null && look.followsTheme) R.color.anaa_widget_text_soft else null)
                textColor(views, time, timeColor ?: palette.text, if (timeColor == null && look.followsTheme) R.color.anaa_widget_text else null)
            }
        }
        drawProgress(views, R.id.pd_progress, look, schedule, now)
        val prayers = ObligatoryPrayers.joinToString("، ") { "${text.name(it, day[it])} ${text.time(day[it])}" }
        views.setContentDescription(
            R.id.pd_root,
            (said + app.getString(R.string.widget_next_said, text.name(next.prayer, next.time)) + prayers).filter { it.isNotBlank() }.joinToString("، "),
        )
        return views
    }

    private fun showText(views: RemoteViews, textId: Int, clockId: Int, words: String, centred: Boolean, weight: WidgetWeight = WidgetWeight.REGULAR) {
        views.setViewVisibility(clockId, View.GONE)
        views.setViewVisibility(textId, View.VISIBLE)
        views.setTextViewText(textId, weighted(words, weight))
        if (centred) views.setInt(textId, "setGravity", Gravity.CENTER)
    }

    /** Words in [weight]: bold is a span, the one kind a widget keeps. */
    private fun weighted(words: String, weight: WidgetWeight): CharSequence =
        if (weight == WidgetWeight.REGULAR) {
            words
        } else {
            SpannableString(words).apply { setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE) }
        }

    private fun hide(views: RemoteViews, textId: Int, clockId: Int) {
        views.setViewVisibility(textId, View.GONE)
        views.setViewVisibility(clockId, View.GONE)
    }

    /**
     * Words in [color]; with [auto] from Android 12 the launcher takes Anaa's own colour instead,
     * so the widget follows day and night without being drawn again.
     */
    private fun textColor(views: RemoteViews, viewId: Int, color: Int, @ColorRes auto: Int?) {
        if (auto != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setTextColor", auto)
        } else {
            views.setTextColor(viewId, color)
        }
    }

    /**
     * A white picture tinted to [color]. A colour filter keeps what is clear clear, so a ruled
     * frame stays a line; only the paper, which is solid, may take Anaa's own day-and-night
     * colour, [auto], from Android 12.
     */
    private fun tintImage(views: RemoteViews, viewId: Int, color: Int, @ColorRes auto: Int?) {
        if ((viewId == R.id.pd_bg || viewId == R.id.nx_bg) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (auto != null) {
                views.setColorStateList(viewId, "setImageTintList", auto)
            } else {
                views.setColorStateList(viewId, "setImageTintList", ColorStateList.valueOf(color))
            }
        } else {
            views.setInt(viewId, "setColorFilter", color)
        }
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

    /** One of the three lines: its row, and for each side a text and a countdown. */
    private data class LineIds(val row: Int, val right: Int, val rightClock: Int, val left: Int, val leftClock: Int)

    private companion object {
        const val DIVIDER_DP = 16
        const val PROGRESS_MAX = 1000
        val LINES = listOf(
            LineIds(R.id.pd_line_0, R.id.pd_l0_r, R.id.pd_l0_rc, R.id.pd_l0_l, R.id.pd_l0_lc),
            LineIds(R.id.pd_line_1, R.id.pd_l1_r, R.id.pd_l1_rc, R.id.pd_l1_l, R.id.pd_l1_lc),
            LineIds(R.id.pd_line_2, R.id.pd_l2_r, R.id.pd_l2_rc, R.id.pd_l2_l, R.id.pd_l2_lc),
        )
        val MARKS = listOf(R.id.pd_mark_0, R.id.pd_mark_1, R.id.pd_mark_2, R.id.pd_mark_3, R.id.pd_mark_4)
        val DAY_CHROME = Chrome(
            root = R.id.pd_root,
            bg = R.id.pd_bg,
            pattern = R.id.pd_pattern,
            divider = R.id.pd_divider,
            progress = R.id.pd_progress,
            lines = LINES,
        )
        val NEXT_CHROME = Chrome(
            root = R.id.nx_root,
            bg = R.id.nx_bg,
            pattern = R.id.nx_pattern,
            divider = R.id.nx_divider,
            progress = R.id.nx_progress,
            lines = listOf(
                LineIds(R.id.nx_line_0, R.id.nx_l0_r, R.id.nx_l0_rc, R.id.nx_l0_l, R.id.nx_l0_lc),
                LineIds(R.id.nx_line_1, R.id.nx_l1_r, R.id.nx_l1_rc, R.id.nx_l1_l, R.id.nx_l1_lc),
            ),
        )
        val CELLS = listOf(
            Triple(R.id.pd_cell_0, R.id.pd_name_0, R.id.pd_time_0),
            Triple(R.id.pd_cell_1, R.id.pd_name_1, R.id.pd_time_1),
            Triple(R.id.pd_cell_2, R.id.pd_name_2, R.id.pd_time_2),
            Triple(R.id.pd_cell_3, R.id.pd_name_3, R.id.pd_time_3),
            Triple(R.id.pd_cell_4, R.id.pd_name_4, R.id.pd_time_4),
        )
    }
}
