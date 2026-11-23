package com.sakinah.tasbih.wird

import android.content.Context
import com.sakinah.tasbih.calendar.CalendarSettings
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.data.AppPreferencesRepository
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.DailyResetSettings
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.data.WirdRepository
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerDay
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.prayer.PrayerSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first

/** Reads what the wird's days and times depend on, for the alarms and the widget, outside the app's screens. */
object WirdEnvironment {
    fun calendar(calendarSettings: CalendarSettings, prayerSettings: PrayerSettings, reset: DailyResetSettings): WirdCalendar {
        val hijri = HijriCalendar.from(calendarSettings)
        val schedule = prayerSettings.place?.let { PrayerSchedule(it, prayerSettings, hijri) }
        return WirdCalendar(hijri, schedule?.let(::clock), ZoneId.systemDefault(), reset)
    }

    suspend fun calendar(context: Context): WirdCalendar = calendar(
        CalendarPreferencesRepository(context).current(),
        PrayerPreferencesRepository(context).current(),
        AppPreferencesRepository(context).preferences.first().dailyReset,
    )

    /** A schedule's times, each day worked out once however many tasks ask for it. */
    fun clock(schedule: PrayerSchedule): PrayerClock {
        val days = ConcurrentHashMap<LocalDate, PrayerDay>()
        return PrayerClock { date: LocalDate, prayer: Prayer -> days.getOrPut(date) { schedule.day(date) }.times[prayer] }
    }

    suspend fun board(context: Context, now: Instant = Instant.now()): WirdBoard {
        val repository = WirdRepository(context)
        repository.ensureSeeded()
        val calendar = calendar(context)
        return WirdBoard.build(
            repository.currentTasks(),
            repository.currentTicked(),
            repository.currentReadings(WirdBoard.readingsFrom(now, calendar)),
            now,
            calendar,
        )
    }
}
