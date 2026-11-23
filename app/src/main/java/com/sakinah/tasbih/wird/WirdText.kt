package com.sakinah.tasbih.wird

import android.content.Context
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.ui.prayer.PrayerText
import com.sakinah.tasbih.ui.prayer.nameRes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** The wird's words: when a task falls, how often, and on which day, in Arabic with Arabic digits. */
class WirdText(private val context: Context) {
    private val prayerText = PrayerText(context)
    private val arabic = Locale.forLanguageTag("ar")

    /** «بعد الفجر بـ١٥ دقيقة»، «قبل الفجر بساعة»، «عند العصر»، «٩:٣٠ م», or «بلا وقت». */
    fun timeRule(task: WirdTask): String = when (task.timeKind) {
        WirdTimeKind.NONE -> context.getString(R.string.wird_time_none)
        WirdTimeKind.CLOCK -> prayerText.time(java.time.LocalDate.of(2000, 1, 1).atStartOfDay(java.time.ZoneOffset.UTC).plusMinutes(task.minuteOfDay.toLong()))
        WirdTimeKind.PRAYER -> {
            val prayer = task.prayer?.let { context.getString(it.nameRes) }.orEmpty()
            when {
                task.offsetMinutes == 0 -> context.getString(R.string.wird_time_at, prayer)
                task.offsetMinutes > 0 -> context.getString(R.string.wird_time_after, prayer, by(task.offsetMinutes))
                else -> context.getString(R.string.wird_time_before, prayer, by(abs(task.offsetMinutes)))
            }
        }
    }

    /** «يوميًا»، «كل أسبوع: الاثنين والخميس»، «كل شهر هجري: ١٣ و١٤ و١٥»، «مرة واحدة: غدًا». */
    fun repeatRule(task: WirdTask, today: LocalDate): String = when (task.repeat) {
        WirdRepeat.DAILY -> context.getString(R.string.wird_repeat_daily_rule)
        WirdRepeat.WEEKLY ->
            if (task.weekdays.size == 7) context.getString(R.string.wird_repeat_daily_rule)
            else context.getString(R.string.wird_repeat_weekly_rule, weekdays(task.weekdays))
        WirdRepeat.HIJRI_MONTHLY -> context.getString(R.string.wird_repeat_hijri_rule, task.hijriDays.sorted().joinToString(" و") { arabicNumber(it) })
        WirdRepeat.ONCE -> context.getString(R.string.wird_repeat_once_rule, task.date?.let { day(it, today) }.orEmpty())
    }

    fun weekdays(days: Set<DayOfWeek>): String = WeekOrder.filter { it in days }.joinToString(" و") { weekday(it) }

    fun weekday(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, arabic)

    /** «اليوم»، «غدًا»، «أمس», or «الجمعة ٢٨ نوفمبر», with the year when it is not this one. */
    fun day(date: LocalDate, today: LocalDate): String = when (date) {
        today -> context.getString(R.string.wird_day_today)
        today.plusDays(1) -> context.getString(R.string.wird_day_tomorrow)
        today.minusDays(1) -> context.getString(R.string.wird_day_yesterday)
        else -> {
            val month = date.month.getDisplayName(TextStyle.FULL_STANDALONE, arabic)
            val year = if (date.year != today.year) " ${arabicNumber(date.year)}" else ""
            "${weekday(date.dayOfWeek)} ${arabicNumber(date.dayOfMonth)} $month$year"
        }
    }

    /** «الاثنين ٢٣ نوفمبر», whatever day it is. */
    fun date(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.FULL_STANDALONE, arabic)
        return "${weekday(date.dayOfWeek)} ${arabicNumber(date.dayOfMonth)} $month"
    }

    /** The time a task falls at on a day, as the clock shows it. */
    fun clock(item: WirdItem): String? = item.time?.let(prayerText::time)

    /** A number of minutes after the preposition «بـ», as Arabic counts them. */
    private fun by(minutes: Int): String = when (minutes) {
        1 -> "بدقيقة"
        2 -> "بدقيقتين"
        in 3..10 -> "بـ${arabicNumber(minutes)} دقائق"
        60 -> "بساعة"
        90 -> "بساعة ونصف"
        120 -> "بساعتين"
        else -> "بـ${arabicNumber(minutes)} دقيقة"
    }

    companion object {
        /** The week as the Arabic calendar sets it out, from Saturday. */
        val WeekOrder = listOf(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
    }
}
