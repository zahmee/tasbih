package com.sakinah.tasbih.prayer

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import com.sakinah.tasbih.calendar.HijriCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One day's times at a place, in the place's own time zone. */
data class PrayerDay(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    operator fun get(prayer: Prayer): ZonedDateTime = times.getValue(prayer)
}

/** A prayer and when it falls. [tomorrow] is set for Fajr after the day's Isha. */
data class PrayerMoment(val prayer: Prayer, val time: ZonedDateTime, val tomorrow: Boolean = false)

/**
 * Works out the times on the phone, with no network: the same equations the online services
 * use, so the same place and method give the same times.
 */
class PrayerSchedule(
    val place: PrayerPlace,
    private val settings: PrayerSettings,
    private val hijri: HijriCalendar,
) {
    val zone: ZoneId = runCatching { ZoneId.of(place.timeZone) }.getOrDefault(ZoneId.systemDefault())
    val method: PrayerMethod = settings.method ?: PrayerMethod.forCountry(place.countryCode)
    val hanafi: Boolean = when (settings.asr) {
        AsrChoice.AUTO -> PrayerMethod.hanafiByDefault(place.countryCode)
        AsrChoice.SHAFI -> false
        AsrChoice.HANAFI -> true
    }
    private val coordinates = Coordinates(place.latitude, place.longitude)

    fun today(now: Instant = Instant.now()): LocalDate = now.atZone(zone).toLocalDate()

    fun day(date: LocalDate): PrayerDay {
        val ramadanIsha = if (method == PrayerMethod.UMM_AL_QURA && isRamadan(date)) RAMADAN_ISHA_MINUTES else 0
        val parameters = method.parameters().apply {
            madhab = if (hanafi) Madhab.HANAFI else Madhab.SHAFI
            highLatitudeRule = highLatitudeRule(settings.highLatitude, place.latitude)
            adjustments.fajr = adjustment(Prayer.FAJR)
            adjustments.sunrise = adjustment(Prayer.SUNRISE)
            adjustments.dhuhr = adjustment(Prayer.DHUHR)
            adjustments.asr = adjustment(Prayer.ASR)
            adjustments.maghrib = adjustment(Prayer.MAGHRIB)
            // Umm al-Qura puts Isha two hours after Maghrib in Ramadan, not an hour and a half.
            // Ramadan is read from the Hijri calendar with the user's corrections.
            adjustments.isha = adjustment(Prayer.ISHA) + ramadanIsha
        }
        val times = PrayerTimes(coordinates, DateComponents(date.year, date.monthValue, date.dayOfMonth), parameters)
        return PrayerDay(
            date,
            mapOf(
                Prayer.FAJR to times.fajr.inZone(),
                Prayer.SUNRISE to times.sunrise.inZone(),
                Prayer.DHUHR to times.dhuhr.inZone(),
                Prayer.ASR to times.asr.inZone(),
                Prayer.MAGHRIB to times.maghrib.inZone(),
                Prayer.ISHA to times.isha.inZone(),
            ),
        )
    }

    /** The next of the five, which after Isha is the next day's Fajr. */
    fun next(now: Instant = Instant.now()): PrayerMoment {
        val date = today(now)
        val day = day(date)
        ObligatoryPrayers.firstOrNull { day[it].toInstant().isAfter(now) }?.let { return PrayerMoment(it, day[it]) }
        return PrayerMoment(Prayer.FAJR, day(date.plusDays(1))[Prayer.FAJR], tomorrow = true)
    }

    /** The prayer whose time it is now, or null between sunrise and Dhuhr, when none is due. */
    fun current(now: Instant = Instant.now()): PrayerMoment? {
        val date = today(now)
        val day = day(date)
        if (now.isBefore(day[Prayer.FAJR].toInstant())) {
            return PrayerMoment(Prayer.ISHA, day(date.minusDays(1))[Prayer.ISHA])
        }
        if (!now.isBefore(day[Prayer.SUNRISE].toInstant()) && now.isBefore(day[Prayer.DHUHR].toInstant())) return null
        return ObligatoryPrayers.lastOrNull { !now.isBefore(day[it].toInstant()) }?.let { PrayerMoment(it, day[it]) }
    }

    /** Degrees clockwise from true north, as a compass that has been corrected would show. */
    val qiblaBearing: Double get() = Qibla(coordinates).direction

    val distanceToKaabaKm: Double get() = distanceKm(place.latitude, place.longitude, KAABA_LATITUDE, KAABA_LONGITUDE)

    private fun adjustment(prayer: Prayer): Int =
        (settings.adjustments[prayer] ?: 0).coerceIn(-PrayerSettings.MAX_ADJUSTMENT, PrayerSettings.MAX_ADJUSTMENT)

    private fun isRamadan(date: LocalDate): Boolean = hijri.fromGregorian(date)?.month == RAMADAN

    /** The library rounds to the minute but keeps the milliseconds of the moment it ran. */
    private fun java.util.Date.inZone(): ZonedDateTime =
        toInstant().truncatedTo(java.time.temporal.ChronoUnit.MINUTES).atZone(zone)

    companion object {
        const val KAABA_LATITUDE = 21.4225
        const val KAABA_LONGITUDE = 39.8262
        private const val RAMADAN = 9
        private const val RAMADAN_ISHA_MINUTES = 30
        private const val EARTH_RADIUS_KM = 6371.0088

        /** Far from the equator the night is too short for twilight angles, so a rule shares it out. */
        fun highLatitudeRule(choice: HighLatitudeChoice, latitude: Double): HighLatitudeRule = when (choice) {
            HighLatitudeChoice.MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            HighLatitudeChoice.SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            HighLatitudeChoice.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
            HighLatitudeChoice.AUTO -> if (kotlin.math.abs(latitude) > 48) HighLatitudeRule.SEVENTH_OF_THE_NIGHT else HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        }

        /** Distance along the earth's surface, by the haversine formula. */
        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
        }
    }
}
