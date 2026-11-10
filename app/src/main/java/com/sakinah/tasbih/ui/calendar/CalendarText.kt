package com.sakinah.tasbih.ui.calendar

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.CalendarMonth
import com.sakinah.tasbih.calendar.CalendarSettings
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.calendar.HijriDate
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.calendar.Occasion
import com.sakinah.tasbih.calendar.WeekStart
import com.sakinah.tasbih.data.arabicNumber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Writes dates in Arabic with Arabic digits, for the calendar screens and the widgets alike.
 * Hijri month names come from the app's own strings, so they are always written in full.
 */
class CalendarText(private val context: Context, val settings: CalendarSettings) {
    val calendar: HijriCalendar = HijriCalendar.from(settings)
    private val hijriMonths = context.resources.getStringArray(R.array.cal_hijri_months)

    fun number(value: Int): String = arabicNumber(value)

    fun hijriMonthName(month: Int): String = hijriMonths[month - 1]

    fun gregorianMonthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Arabic)

    fun weekday(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Arabic)

    /** One letter, as the heading of a narrow column. */
    fun weekdayNarrow(day: DayOfWeek): String = day.getDisplayName(TextStyle.NARROW, Arabic)

    fun hijriDate(date: HijriDate): String =
        context.getString(R.string.cal_hijri_date, number(date.day), hijriMonthName(date.month), number(date.year))

    /** Empty outside the years the Hijri data covers. */
    fun hijriDate(date: LocalDate): String = calendar.fromGregorian(date)?.let(::hijriDate).orEmpty()

    fun gregorianDate(date: LocalDate): String =
        context.getString(R.string.cal_gregorian_date, number(date.dayOfMonth), gregorianMonthName(date.monthValue), number(date.year))

    fun hijriMonthYear(month: HijriMonth): String =
        context.getString(R.string.cal_hijri_month_year, hijriMonthName(month.month), number(month.year))

    fun gregorianMonthYear(month: YearMonth): String =
        context.getString(R.string.cal_gregorian_month_year, gregorianMonthName(month.monthValue), number(month.year))

    fun monthTitle(month: CalendarMonth): String = when (month) {
        is CalendarMonth.Hijri -> hijriMonthYear(month.month)
        is CalendarMonth.Gregorian -> gregorianMonthYear(month.month)
    }

    /** The months of the other calendar that this month spans, such as «أكتوبر – نوفمبر ٢٠٢٦ م». */
    fun monthSubtitle(month: CalendarMonth): String {
        val days = month.days(calendar)
        if (days.isEmpty()) return ""
        return when (month) {
            is CalendarMonth.Hijri -> {
                val first = YearMonth.from(days.first())
                val last = YearMonth.from(days.last())
                when {
                    first == last -> gregorianMonthYear(first)
                    first.year == last.year -> range(gregorianMonthName(first.monthValue), gregorianMonthYear(last))
                    else -> range(gregorianMonthYear(first), gregorianMonthYear(last))
                }
            }
            is CalendarMonth.Gregorian -> {
                val first = calendar.fromGregorian(days.first())?.yearMonth ?: return ""
                val last = calendar.fromGregorian(days.last())?.yearMonth ?: return ""
                when {
                    first == last -> hijriMonthYear(first)
                    first.year == last.year -> range(hijriMonthName(first.month), hijriMonthYear(last))
                    else -> range(hijriMonthYear(first), hijriMonthYear(last))
                }
            }
        }
    }

    fun occasionName(occasion: Occasion): String = context.getString(occasion.nameRes)

    /** «اليوم» or «غدًا» or «بعد ١٢ يومًا». */
    fun countdown(daysAway: Long): String = when (daysAway) {
        0L -> context.getString(R.string.cal_today)
        1L -> context.getString(R.string.cal_tomorrow)
        else -> context.getString(R.string.cal_in_days, daysCount(daysAway))
    }

    /** How far [date] is from [today], in either direction, for the date converter. */
    fun distanceFromToday(date: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when {
            days == -1L -> context.getString(R.string.cal_yesterday)
            days < 0 -> context.getString(R.string.cal_days_ago, daysCount(-days))
            else -> countdown(days)
        }
    }

    /**
     * The week's first day. Arabic alone cannot say it, as Arabic weeks start on Saturday, Sunday
     * or Monday depending on the country, so the phone's region decides.
     */
    fun firstDayOfWeek(): DayOfWeek = when (settings.weekStart) {
        WeekStart.SATURDAY -> DayOfWeek.SATURDAY
        WeekStart.SUNDAY -> DayOfWeek.SUNDAY
        WeekStart.MONDAY -> DayOfWeek.MONDAY
        WeekStart.AUTO -> {
            val region = Resources.getSystem().configuration.locales[0].country
            val locale = if (region.isNotEmpty()) Locale.Builder().setLanguage("ar").setRegion(region).build() else Arabic
            WeekFields.of(locale).firstDayOfWeek
        }
    }

    private fun range(from: String, to: String): String = context.getString(R.string.cal_range, from, to)

    companion object {
        val Arabic: Locale = Locale.forLanguageTag("ar")

        /**
         * A number of days as it follows «بعد» or «قبل»: «يومين»، «٣ أيام»، «١١ يومًا»، «١٠٠ يوم».
         * Kept apart from [com.sakinah.tasbih.data.dayQuantity], which names a count on its own.
         */
        fun daysCount(days: Long): String {
            val value = days.toInt()
            val lastTwo = value % 100
            return when {
                value == 1 -> "يوم واحد"
                value == 2 -> "يومين"
                lastTwo in 3..10 -> "${arabicNumber(value)} أيام"
                lastTwo in 11..99 -> "${arabicNumber(value)} يومًا"
                else -> "${arabicNumber(value)} يوم"
            }
        }
    }
}

val Occasion.nameRes: Int
    @StringRes get() = when (this) {
        Occasion.NEW_YEAR -> R.string.cal_occasion_new_year
        Occasion.TASUA -> R.string.cal_occasion_tasua
        Occasion.ASHURA -> R.string.cal_occasion_ashura
        Occasion.MAWLID -> R.string.cal_occasion_mawlid
        Occasion.ISRA_MIRAJ -> R.string.cal_occasion_isra_miraj
        Occasion.MID_SHABAN -> R.string.cal_occasion_mid_shaban
        Occasion.RAMADAN -> R.string.cal_occasion_ramadan
        Occasion.LAST_TEN_NIGHTS -> R.string.cal_occasion_last_ten_nights
        Occasion.EID_AL_FITR -> R.string.cal_occasion_eid_al_fitr
        Occasion.SIX_OF_SHAWWAL -> R.string.cal_occasion_six_of_shawwal
        Occasion.FIRST_TEN_OF_DHUL_HIJJAH -> R.string.cal_occasion_first_ten_dhul_hijjah
        Occasion.ARAFAH -> R.string.cal_occasion_arafah
        Occasion.EID_AL_ADHA -> R.string.cal_occasion_eid_al_adha
        Occasion.TASHREEQ -> R.string.cal_occasion_tashreeq
    }
