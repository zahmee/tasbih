package com.sakinah.tasbih.prayer

import java.time.Duration
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
        /** The Haram's opening takbir for a short sound, and Ali Ahmed Mulla's adhan from the Haram in full. */
        const val DEFAULT_TONE = "takbir_makkah"
        const val DEFAULT_ADHAN = "ali_mulla"
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
 * A moment the app must wake. [event] is the alert to raise then, or null when only the widgets
 * move on. [urgent] asks for an alarm clock, which Doze never holds back: it is kept for a
 * prayer that follows its own reminder by less than [AlertPlanner.DOZE_GAP_MINUTES], where a
 * second idle alarm so soon could be delayed by Android's limit on them.
 */
data class Wake(val at: ZonedDateTime, val event: AlertEvent?, val urgent: Boolean = false)

/**
 * Finds the moments the app must wake: every alert, and every prayer time, at which the widgets
 * move on to the following prayer. Until November 25, 2026 only one alarm was ever set, for the
 * next of them, and each alarm set the one after it. One alarm the phone never delivered then
 * silenced every alert after it until the app was opened, and left the widgets counting down
 * to a prayer long past. So the coming wakes are now all handed to Android together.
 */
object AlertPlanner {
    /** Below this gap after a reminder, the prayer's own alarm is set as an alarm clock. */
    const val DOZE_GAP_MINUTES = 10L

    /** How many wakes are handed to Android at once: a whole day at its busiest has 23. */
    const val MAX_WAKES = 24

    /**
     * The next wake after [now]. An alert at the very time the widgets change wakes once for
     * both, since raising an alert redraws the widgets too.
     */
    fun nextWake(schedule: PrayerSchedule, settings: AlertSettings, now: Instant, widgetsPlaced: Boolean): Wake? =
        upcomingWakes(schedule, settings, now, widgetsPlaced, limit = 1).firstOrNull()

    /**
     * The coming wakes after [now], soonest first, at most [limit] of them: every alert, and
     * while a widget is placed every time it changes. Each becomes an alarm of its own, so an
     * alarm the phone drops costs that one moment, and the next one mends what it missed. Only
     * the first can be [Wake.urgent]: an alarm clock set hours ahead would show in the status
     * bar all that while.
     */
    fun upcomingWakes(
        schedule: PrayerSchedule,
        settings: AlertSettings,
        now: Instant,
        widgetsPlaced: Boolean,
        limit: Int = MAX_WAKES,
    ): List<Wake> {
        val today = schedule.today(now)
        // From yesterday, as far north in summer Isha can fall after midnight.
        val days = (-1L..2L).map { schedule.day(today.plusDays(it)) }
        val wakes = sortedMapOf<Instant, Wake>()
        if (widgetsPlaced) {
            days.flatMap { boundaries(schedule, it) }.forEach { wakes[it.toInstant()] = Wake(it, null) }
        }
        if (settings.anyOn) {
            // A prayer's own alert last, so it is the one named where a reminder shares its minute.
            days.flatMap { events(it, settings) }.sortedBy { it.kind == AlertKind.PRAYER }
                .forEach { wakes[it.at.toInstant()] = Wake(it.at, it) }
        }
        return wakes.tailMap(now).values.filter { it.at.toInstant().isAfter(now) }.take(limit).mapIndexed { index, wake ->
            val alert = wake.event
            val reminder = alert?.let { settings.alertFor(it.prayer).reminderMinutes } ?: 0
            wake.copy(urgent = index == 0 && alert?.kind == AlertKind.PRAYER && reminder in 1 until DOZE_GAP_MINUTES)
        }
    }

    /** An alert this long overdue is no longer raised: its time was announced long ago. */
    val STALE: Duration = Duration.ofMinutes(20)

    /**
     * The alerts that came due while the alarm set for [pendingSince] was held back, up to
     * [now], oldest first. Android delivers an alarm late when the phone is idle or exact alarms
     * are not allowed, and by then a later alert's time may have passed too. Left out are alerts
     * more than [STALE] old, and a reminder whose prayer has already begun.
     */
    fun due(schedule: PrayerSchedule, settings: AlertSettings, pendingSince: Instant, now: Instant): List<AlertEvent> {
        if (!settings.anyOn || now.isBefore(pendingSince)) return emptyList()
        val from = maxOf(pendingSince, now.minus(STALE))
        val today = schedule.today(now)
        return (-1L..1L).asSequence()
            .map { schedule.day(today.plusDays(it)) }
            .flatMap { day -> events(day, settings) }
            .filter { !it.at.toInstant().isBefore(from) && !it.at.toInstant().isAfter(now) }
            .filter { it.kind == AlertKind.PRAYER || it.prayerTime.toInstant().isAfter(now) }
            .sortedBy { it.at.toInstant() }
            .toList()
    }

    /** The next alert after [now], within the next two days, or null when every alert is off. */
    fun nextAlert(schedule: PrayerSchedule, settings: AlertSettings, now: Instant): AlertEvent? {
        if (!settings.anyOn) return null
        val today = schedule.today(now)
        // From yesterday, as far north in summer Isha can fall after midnight.
        return (-1L..2L).asSequence()
            .map { schedule.day(today.plusDays(it)) }
            .flatMap { day -> events(day, settings) }
            .filter { it.at.toInstant().isAfter(now) }
            .minByOrNull { it.at.toInstant() }
    }

    /** The next of the day's times after [now], where the widgets change. */
    fun nextBoundary(schedule: PrayerSchedule, now: Instant): ZonedDateTime {
        val today = schedule.today(now)
        return (-1L..1L).asSequence()
            .map { schedule.day(today.plusDays(it)) }
            .flatMap { day -> boundaries(schedule, day) }
            .filter { it.toInstant().isAfter(now) }
            .minBy { it.toInstant() }
    }

    /**
     * Where the widgets change on [day]: at each of its times, at the iqamas and the ends of
     * the counts after them, which change the widgets' clock too, and at midnight, which turns
     * their weekday and Gregorian date.
     */
    private fun boundaries(schedule: PrayerSchedule, day: PrayerDay): List<ZonedDateTime> =
        Prayer.entries.map { day[it] } + schedule.iqamaChanges(day) + day.date.atStartOfDay(schedule.zone)

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
