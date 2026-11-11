package com.sakinah.tasbih.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Where the sun stands: [azimuth] clockwise from true north, [elevation] above the horizon. */
data class SunPlace(val azimuth: Double, val elevation: Double)

/**
 * The sun's place in the sky, worked out on the phone with the NOAA solar equations, which are
 * good to a fraction of a degree for these centuries. It gives a second way to find the Qibla,
 * by one's shadow, that needs no compass and is not thrown off by metal or a magnetic case.
 */
object SunPosition {
    /** The Kaaba's latitude, where the sun stands overhead twice a year. */
    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262

    fun at(instant: Instant, latitude: Double, longitude: Double): SunPlace {
        val solar = Solar(instant)
        val utcMinutes = instant.atOffset(ZoneOffset.UTC).let { it.hour * 60 + it.minute + it.second / 60.0 }
        val trueSolarMinutes = (utcMinutes + solar.equationOfTime + 4 * longitude).mod(1440.0)
        val hourAngle = Math.toRadians(trueSolarMinutes / 4 - 180)
        val lat = Math.toRadians(latitude)
        val decl = solar.declination
        val elevation = asin(sin(lat) * sin(decl) + cos(lat) * cos(decl) * cos(hourAngle))
        // Measured from the south by this formula, so half a turn brings it to north.
        val fromSouth = atan2(sin(hourAngle), cos(hourAngle) * sin(lat) - tan(decl) * cos(lat))
        val azimuth = (Math.toDegrees(fromSouth) + 180).mod(360.0)
        return SunPlace(azimuth, Math.toDegrees(elevation))
    }

    /** The sun's declination at noon on [date], in degrees. */
    fun declination(date: LocalDate): Double =
        Math.toDegrees(Solar(date.atTime(12, 0).toInstant(ZoneOffset.UTC)).declination)

    /**
     * The two moments of [year] when the sun is overhead at the Kaaba, around late May and mid
     * July. At those moments every shadow, wherever the sun is up, points away from the Qibla.
     */
    fun overKaaba(year: Int): List<Instant> {
        val days = generateSequence(LocalDate.of(year, 4, 1)) { it.plusDays(1) }.takeWhile { it.monthValue <= 8 }.toList()
        // The declination climbs past the Kaaba's latitude in May and comes back down in July.
        val crossings = days.zipWithNext().filter { (a, b) ->
            (declination(a) - KAABA_LATITUDE) * (declination(b) - KAABA_LATITUDE) <= 0
        }
        return crossings.map { (a, b) ->
            val day = if (abs(declination(a) - KAABA_LATITUDE) <= abs(declination(b) - KAABA_LATITUDE)) a else b
            solarNoonAtKaaba(day)
        }
    }

    private fun solarNoonAtKaaba(date: LocalDate): Instant {
        // Two passes: the equation of time is read at the first guess of noon, then refined.
        var noon = date.atTime(12, 0).toInstant(ZoneOffset.UTC)
        repeat(2) {
            val minutes = 720 - 4 * KAABA_LONGITUDE - Solar(noon).equationOfTime
            noon = date.atStartOfDay().toInstant(ZoneOffset.UTC).plusSeconds((minutes * 60).toLong())
        }
        return noon
    }

    /** The sun's declination, in radians, and the equation of time, in minutes, at [instant]. */
    private class Solar(instant: Instant) {
        val declination: Double
        val equationOfTime: Double

        init {
            val julianDay = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5
            val t = (julianDay - 2_451_545.0) / 36_525
            val meanLongitude = Math.toRadians((280.46646 + t * (36_000.76983 + t * 0.0003032)).mod(360.0))
            val meanAnomaly = Math.toRadians(357.52911 + t * (35_999.05029 - 0.0001537 * t))
            val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
            val centre = sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) +
                sin(3 * meanAnomaly) * 0.000289
            val omega = Math.toRadians(125.04 - 1934.136 * t)
            val apparentLongitude = Math.toRadians(Math.toDegrees(meanLongitude) + centre - 0.00569 - 0.00478 * sin(omega))
            val meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60
            val obliquity = Math.toRadians(meanObliquity + 0.00256 * cos(omega))
            declination = asin(sin(obliquity) * sin(apparentLongitude))
            val y = tan(obliquity / 2).let { it * it }
            equationOfTime = 4 * Math.toDegrees(
                y * sin(2 * meanLongitude) -
                    2 * eccentricity * sin(meanAnomaly) +
                    4 * eccentricity * y * sin(meanAnomaly) * cos(2 * meanLongitude) -
                    0.5 * y * y * sin(4 * meanLongitude) -
                    1.25 * eccentricity * eccentricity * sin(2 * meanAnomaly),
            )
        }
    }
}

/**
 * How far to turn from one's shadow to face the Qibla, from -180 to 180 degrees: positive to
 * the right. Standing with the sun behind, the shadow falls straight ahead, half a turn from it.
 */
fun turnFromShadow(sun: SunPlace, qiblaBearing: Double): Double {
    val shadow = (sun.azimuth + 180).mod(360.0)
    return (qiblaBearing - shadow + 540).mod(360.0) - 180
}
