package com.sakinah.tasbih.ui.prayer

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.StringRes
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerMethod
import com.sakinah.tasbih.prayer.PrayerPlace
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime

/** Prayer names, times and durations in Arabic with Arabic digits. */
class PrayerText(private val context: Context) {
    private val use24Hour = DateFormat.is24HourFormat(context)

    /** On Friday the noon prayer is the Jumu'ah. */
    fun name(prayer: Prayer, on: ZonedDateTime? = null): String =
        if (prayer == Prayer.DHUHR && on?.dayOfWeek == DayOfWeek.FRIDAY) {
            context.getString(R.string.prayer_jumuah)
        } else {
            context.getString(prayer.nameRes)
        }

    /** «٣:١٦ م», or «١٥:١٦» when the phone uses a 24-hour clock. */
    fun time(time: ZonedDateTime): String {
        val minute = arabicNumber(time.minute).padStart(2, '٠')
        if (use24Hour) return "${arabicNumber(time.hour)}:$minute"
        val hour = (time.hour % 12).let { if (it == 0) 12 else it }
        val half = context.getString(if (time.hour < 12) R.string.prayer_am else R.string.prayer_pm)
        return "${arabicNumber(hour)}:$minute $half"
    }

    /** «بعد ساعة و٢٠ دقيقة», counted in whole minutes and rounded up. */
    fun remaining(duration: Duration): String {
        val minutes = ((duration.seconds + 59) / 60).coerceAtLeast(0)
        if (minutes < 1) return context.getString(R.string.prayer_now)
        val hours = (minutes / 60).toInt()
        val rest = (minutes % 60).toInt()
        val parts = listOfNotNull(hours.takeIf { it > 0 }?.let(::hoursCount), rest.takeIf { it > 0 }?.let(::minutesCount))
        return context.getString(R.string.prayer_in, parts.joinToString(" و"))
    }

    fun methodName(method: PrayerMethod): String = context.getString(method.nameRes)

    /** «تندحة، عسير، المملكة العربية السعودية», with whatever parts the place has. */
    fun placeLine(place: PrayerPlace): String = listOf(place.name, place.region, place.country)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("، ")

    /** The region and country under the place's name, or empty for the phone's own position. */
    fun placeDetail(place: PrayerPlace): String = listOf(place.region, place.country)
        .filter { it.isNotBlank() && it != place.name }
        .distinct()
        .joinToString("، ")

    /** Kept left to right, so the degree sign stays after the number inside Arabic text. */
    fun degrees(value: Double): String = "⁦${arabicNumber(Math.round(value).toInt().mod(360))}°⁩"

    fun kilometres(value: Double): String =
        context.getString(R.string.qibla_km, arabicNumber(Math.round(value).toInt()))

    private fun hoursCount(value: Int): String = when {
        value == 1 -> "ساعة"
        value == 2 -> "ساعتين"
        value in 3..10 -> "${arabicNumber(value)} ساعات"
        else -> "${arabicNumber(value)} ساعة"
    }

    /** «٥ دقائق»، «١٥ دقيقة», as a number of minutes is written on its own. */
    fun minutes(value: Int): String = minutesCount(value)

    private fun minutesCount(value: Int): String = when {
        value == 1 -> "دقيقة"
        value == 2 -> "دقيقتين"
        value in 3..10 -> "${arabicNumber(value)} دقائق"
        else -> "${arabicNumber(value)} دقيقة"
    }
}

val Prayer.nameRes: Int
    @StringRes get() = when (this) {
        Prayer.FAJR -> R.string.prayer_fajr
        Prayer.SUNRISE -> R.string.prayer_sunrise
        Prayer.DHUHR -> R.string.prayer_dhuhr
        Prayer.ASR -> R.string.prayer_asr
        Prayer.MAGHRIB -> R.string.prayer_maghrib
        Prayer.ISHA -> R.string.prayer_isha
    }

val PrayerMethod.nameRes: Int
    @StringRes get() = when (this) {
        PrayerMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
        PrayerMethod.KUWAIT -> R.string.method_kuwait
        PrayerMethod.QATAR -> R.string.method_qatar
        PrayerMethod.GULF -> R.string.method_gulf
        PrayerMethod.DUBAI -> R.string.method_dubai
        PrayerMethod.EGYPT -> R.string.method_egypt
        PrayerMethod.JORDAN -> R.string.method_jordan
        PrayerMethod.MOROCCO -> R.string.method_morocco
        PrayerMethod.ALGERIA -> R.string.method_algeria
        PrayerMethod.TUNISIA -> R.string.method_tunisia
        PrayerMethod.TURKEY -> R.string.method_turkey
        PrayerMethod.KARACHI -> R.string.method_karachi
        PrayerMethod.INDONESIA -> R.string.method_indonesia
        PrayerMethod.MALAYSIA -> R.string.method_malaysia
        PrayerMethod.SINGAPORE -> R.string.method_singapore
        PrayerMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
        PrayerMethod.NORTH_AMERICA -> R.string.method_north_america
        PrayerMethod.MOONSIGHTING -> R.string.method_moonsighting
        PrayerMethod.FRANCE -> R.string.method_france
        PrayerMethod.RUSSIA -> R.string.method_russia
    }
