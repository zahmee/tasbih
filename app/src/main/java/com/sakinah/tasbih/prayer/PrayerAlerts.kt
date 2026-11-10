package com.sakinah.tasbih.prayer

import java.time.Instant
import java.time.ZonedDateTime

/** What happens at a prayer's time. [ADHAN] plays a whole recording; [SOUND] is a short tone. */
enum class AlertStyle { OFF, SILENT, SOUND, ADHAN }

/** The prayer's own alert, and a reminder this many minutes before it, or none for zero. */
data class PrayerAlert(val style: AlertStyle = AlertStyle.OFF, val reminderMinutes: Int = 0)

data class AlertSettings(
    val alerts: Map<Prayer, PrayerAlert> = emptyMap(),
    /** The short tone for [AlertStyle.SOUND]. */
    val tone: String = DEFAULT_TONE,
    /** The recording for [AlertStyle.ADHAN], and Fajr's own, which is often a different one. */
    val adhan: String = DEFAULT_ADHAN,
    val fajrAdhan: String = DEFAULT_ADHAN,
    /** A file the user chose from the phone, used where [CUSTOM_ADHAN] is picked. */
    val customAdhanUri: String? = null,
) {
    fun alertFor(prayer: Prayer): PrayerAlert = alerts[prayer] ?: PrayerAlert()

    val anyOn: Boolean get() = AlertPrayers.any { alertFor(it).style != AlertStyle.OFF || alertFor(it).reminderMinutes > 0 }

    companion object {
        /** The Haram's opening takbir for a short sound, and the Haram's Maghrib adhan in full. */
        const val DEFAULT_TONE = "takbir_makkah"
        const val DEFAULT_ADHAN = "makkah_maghrib"
        /** A tone in place of a voice, for those who want the alarm without the adhan. */
        const val TONE_ADHAN = "tone"
        const val CUSTOM_ADHAN = "custom"
        val REMINDER_CHOICES = listOf(0, 5, 10, 15, 20, 30)
    }
}

/** The prayers that can be announced: the five, and sunrise as the end of Fajr's time. */
val AlertPrayers: List<Prayer> = listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

enum class AlertKind { REMINDER, PRAYER }

/** One alert to raise. [prayerTime] is the prayer's own time, which a reminder counts down to. */
data class AlertEvent(val prayer: Prayer, val kind: AlertKind, val at: ZonedDateTime, val prayerTime: ZonedDateTime)

/**
 * Finds the next moment the app must wake: the next alert, and the next prayer time, at which
 * the widgets move on to the following prayer. Only one alarm is ever set, for whichever comes
 * first, and each alarm sets the next one, so a change of day or time zone cannot be missed.
 */
object AlertPlanner {
    /** The next alert after [now], within the next two days, or null when every alert is off. */
    fun nextAlert(schedule: PrayerSchedule, settings: AlertSettings, now: Instant): AlertEvent? {
        if (!settings.anyOn) return null
        val today = schedule.today(now)
        return (0L..2L).asSequence()
            .map { schedule.day(today.plusDays(it)) }
            .flatMap { day -> events(day, settings) }
            .filter { it.at.toInstant().isAfter(now) }
            .minByOrNull { it.at.toInstant() }
    }

    /** The next of the day's times after [now], where the widgets change. */
    fun nextBoundary(schedule: PrayerSchedule, now: Instant): ZonedDateTime {
        val today = schedule.today(now)
        return (0L..1L).asSequence()
            .map { schedule.day(today.plusDays(it)) }
            .flatMap { day -> Prayer.entries.asSequence().map { day[it] } }
            .first { it.toInstant().isAfter(now) }
    }

    private fun events(day: PrayerDay, settings: AlertSettings): Sequence<AlertEvent> = AlertPrayers.asSequence().flatMap { prayer ->
        val alert = settings.alertFor(prayer)
        val time = day[prayer]
        sequenceOf(
            AlertEvent(prayer, AlertKind.PRAYER, time, time).takeIf { alert.style != AlertStyle.OFF },
            AlertEvent(prayer, AlertKind.REMINDER, time.minusMinutes(alert.reminderMinutes.toLong()), time)
                .takeIf { alert.reminderMinutes > 0 },
        ).filterNotNull()
    }
}
