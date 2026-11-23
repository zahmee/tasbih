package com.sakinah.tasbih.wird

import androidx.annotation.StringRes
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.Prayer
import java.time.DayOfWeek

/**
 * Ready tasks the user may add with one tap, each from a practice with a sound basis, set at
 * the time it is usually kept. Their reminders start off; the user turns on those wanted.
 */
enum class WirdPreset(
    @StringRes val title: Int,
    val collectionId: String? = null,
    val repeat: WirdRepeat = WirdRepeat.DAILY,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val hijriDays: Set<Int> = emptySet(),
    val prayer: Prayer,
    val offsetMinutes: Int,
    /** Put on the list for everyone, so the day's adhkar are counted from the first day. */
    val byDefault: Boolean = false,
) {
    MORNING(R.string.wird_preset_morning, MORNING_ADHKAR, prayer = Prayer.FAJR, offsetMinutes = 15, byDefault = true),
    EVENING(R.string.wird_preset_evening, EVENING_ADHKAR, prayer = Prayer.ASR, offsetMinutes = 15, byDefault = true),
    SLEEP(R.string.wird_preset_sleep, SLEEP_ADHKAR, prayer = Prayer.ISHA, offsetMinutes = 90),
    QURAN(R.string.wird_preset_quran, prayer = Prayer.FAJR, offsetMinutes = 30),
    DUHA(R.string.wird_preset_duha, prayer = Prayer.SUNRISE, offsetMinutes = 20),
    WITR(R.string.wird_preset_witr, prayer = Prayer.ISHA, offsetMinutes = 60),
    KAHF(R.string.wird_preset_kahf, repeat = WirdRepeat.WEEKLY, weekdays = setOf(DayOfWeek.FRIDAY), prayer = Prayer.FAJR, offsetMinutes = 60),
    SALAWAT(R.string.wird_preset_salawat, repeat = WirdRepeat.WEEKLY, weekdays = setOf(DayOfWeek.FRIDAY), prayer = Prayer.ASR, offsetMinutes = 30),
    MONDAY_THURSDAY(
        R.string.wird_preset_monday_thursday,
        repeat = WirdRepeat.WEEKLY,
        weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        prayer = Prayer.FAJR,
        offsetMinutes = -45,
    ),
    WHITE_DAYS(R.string.wird_preset_white_days, repeat = WirdRepeat.HIJRI_MONTHLY, hijriDays = setOf(13, 14, 15), prayer = Prayer.FAJR, offsetMinutes = -45),
    ;

    val id: String get() = name.lowercase()

    fun task(title: String, sortOrder: Int, createdAtMillis: Long): WirdTask = WirdTask(
        title = title,
        collectionId = collectionId,
        presetId = id,
        repeat = repeat,
        weekdays = weekdays,
        hijriDays = hijriDays,
        timeKind = WirdTimeKind.PRAYER,
        prayer = prayer,
        offsetMinutes = offsetMinutes,
        remind = false,
        sortOrder = sortOrder,
        createdAtMillis = createdAtMillis,
    )

    companion object {
        fun of(id: String?): WirdPreset? = entries.firstOrNull { it.id == id }
    }
}

/** Hisn al-Muslim's collections, by their place in the book. */
const val MORNING_ADHKAR = "hisn_027"
const val EVENING_ADHKAR = "hisn_028"
const val SLEEP_ADHKAR = "hisn_029"
