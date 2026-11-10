package com.sakinah.tasbih.prayer

import com.sakinah.tasbih.calendar.HijriCalendar
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reference times were read from the Aladhan service on 2026-11-11, for each city with its
 * country's method. The second date falls in Ramadan 1448, when Umm al-Qura's Isha moves later.
 */
class PrayerScheduleTest {
    private data class Fixture(
        val city: String,
        val date: String,
        val method: PrayerMethod,
        val hanafi: Boolean,
        val latitude: Double,
        val longitude: Double,
        val zone: String,
        val times: List<String>,
    )

    private val fixtures = listOf(
        Fixture("Makkah", "2026-11-11", PrayerMethod.UMM_AL_QURA, hanafi = false, 21.42664, 39.82563, "Asia/Riyadh", listOf("05:10", "06:29", "12:05", "15:17", "17:40", "19:10")),
        Fixture("Riyadh", "2026-11-11", PrayerMethod.UMM_AL_QURA, hanafi = false, 24.68773, 46.72185, "Asia/Riyadh", listOf("04:46", "06:06", "11:37", "14:46", "17:08", "18:38")),
        Fixture("Jeddah", "2026-11-11", PrayerMethod.UMM_AL_QURA, hanafi = false, 21.49012, 39.18624, "Asia/Riyadh", listOf("05:13", "06:32", "12:07", "15:20", "17:42", "19:12")),
        Fixture("KhamisMushait", "2026-11-11", PrayerMethod.UMM_AL_QURA, hanafi = false, 18.3, 42.73333, "Asia/Riyadh", listOf("04:56", "06:13", "11:53", "15:09", "17:33", "19:03")),
        Fixture("Dammam", "2026-11-11", PrayerMethod.UMM_AL_QURA, hanafi = false, 26.43442, 50.10326, "Asia/Riyadh", listOf("04:34", "05:55", "11:24", "14:30", "16:51", "18:21")),
        Fixture("KuwaitCity", "2026-11-11", PrayerMethod.KUWAIT, hanafi = false, 29.36972, 47.97833, "Asia/Kuwait", listOf("04:47", "06:09", "11:32", "14:35", "16:55", "18:15")),
        Fixture("Doha", "2026-11-11", PrayerMethod.QATAR, hanafi = false, 25.28545, 51.53096, "Asia/Qatar", listOf("04:29", "05:48", "11:18", "14:26", "16:47", "18:17")),
        Fixture("Dubai", "2026-11-11", PrayerMethod.DUBAI, hanafi = false, 25.07725, 55.30927, "Asia/Dubai", listOf("05:13", "06:33", "12:06", "15:11", "17:36", "18:52")),
        Fixture("Cairo", "2026-11-11", PrayerMethod.EGYPT, hanafi = false, 30.06263, 31.24967, "Africa/Cairo", listOf("04:47", "06:17", "11:39", "14:40", "17:01", "18:21")),
        Fixture("Amman", "2026-11-11", PrayerMethod.JORDAN, hanafi = false, 31.95522, 35.94503, "Asia/Amman", listOf("05:37", "07:01", "12:20", "15:19", "17:44", "19:03")),
        Fixture("Casablanca", "2026-11-11", PrayerMethod.MOROCCO, hanafi = false, 33.58831, -7.61138, "Africa/Casablanca", listOf("05:28", "06:58", "12:19", "15:10", "17:35", "18:51")),
        Fixture("Algiers", "2026-11-11", PrayerMethod.ALGERIA, hanafi = false, 36.73225, 3.08746, "Africa/Algiers", listOf("05:52", "07:21", "12:32", "15:21", "17:42", "19:05")),
        Fixture("Istanbul", "2026-11-11", PrayerMethod.TURKEY, hanafi = false, 41.01384, 28.94966, "Europe/Istanbul", listOf("06:12", "07:40", "12:53", "15:33", "17:56", "19:18")),
        Fixture("Karachi", "2026-11-11", PrayerMethod.KARACHI, hanafi = true, 24.8608, 67.0104, "Asia/Karachi", listOf("05:27", "06:45", "12:16", "16:11", "17:46", "19:05")),
        Fixture("Jakarta", "2026-11-11", PrayerMethod.INDONESIA, hanafi = false, -6.21462, 106.84513, "Asia/Jakarta", listOf("04:04", "05:25", "11:37", "14:58", "17:48", "19:01")),
        Fixture("London", "2026-11-11", PrayerMethod.MUSLIM_WORLD_LEAGUE, hanafi = false, 51.50853, -0.12574, "Europe/London", listOf("05:16", "07:11", "11:45", "13:55", "16:17", "18:06")),
        Fixture("Makkah", "2027-02-10", PrayerMethod.UMM_AL_QURA, hanafi = false, 21.42664, 39.82563, "Asia/Riyadh", listOf("05:37", "06:54", "12:35", "15:50", "18:16", "20:16")),
        Fixture("Riyadh", "2027-02-10", PrayerMethod.UMM_AL_QURA, hanafi = false, 24.68773, 46.72185, "Asia/Riyadh", listOf("05:11", "06:31", "12:07", "15:19", "17:44", "19:44")),
        Fixture("Jeddah", "2027-02-10", PrayerMethod.UMM_AL_QURA, hanafi = false, 21.49012, 39.18624, "Asia/Riyadh", listOf("05:39", "06:57", "12:37", "15:52", "18:18", "20:18")),
        Fixture("KhamisMushait", "2027-02-10", PrayerMethod.UMM_AL_QURA, hanafi = false, 18.3, 42.73333, "Asia/Riyadh", listOf("05:23", "06:39", "12:23", "15:40", "18:08", "20:08")),
        Fixture("Dammam", "2027-02-10", PrayerMethod.UMM_AL_QURA, hanafi = false, 26.43442, 50.10326, "Asia/Riyadh", listOf("04:59", "06:19", "11:54", "15:04", "17:29", "19:29")),
        Fixture("KuwaitCity", "2027-02-10", PrayerMethod.KUWAIT, hanafi = false, 29.36972, 47.97833, "Asia/Kuwait", listOf("05:11", "06:32", "12:02", "15:09", "17:33", "18:51")),
        Fixture("Doha", "2027-02-10", PrayerMethod.QATAR, hanafi = false, 25.28545, 51.53096, "Asia/Qatar", listOf("04:55", "06:12", "11:48", "14:59", "17:24", "18:54")),
        Fixture("Dubai", "2027-02-10", PrayerMethod.DUBAI, hanafi = false, 25.07725, 55.30927, "Asia/Dubai", listOf("05:38", "06:57", "12:36", "15:45", "18:12", "19:28")),
        Fixture("Cairo", "2027-02-10", PrayerMethod.EGYPT, hanafi = false, 30.06263, 31.24967, "Africa/Cairo", listOf("05:11", "06:39", "12:09", "15:15", "17:39", "18:58")),
        Fixture("Amman", "2027-02-10", PrayerMethod.JORDAN, hanafi = false, 31.95522, 35.94503, "Asia/Amman", listOf("06:01", "07:23", "12:50", "15:54", "18:23", "19:41")),
        Fixture("Casablanca", "2027-02-10", PrayerMethod.MOROCCO, hanafi = false, 33.58831, -7.61138, "Africa/Casablanca", listOf("05:51", "07:20", "12:50", "15:46", "18:15", "19:29")),
        Fixture("Algiers", "2027-02-10", PrayerMethod.ALGERIA, hanafi = false, 36.73225, 3.08746, "Africa/Algiers", listOf("06:14", "07:42", "13:02", "15:58", "18:22", "19:45")),
        Fixture("Istanbul", "2027-02-10", PrayerMethod.TURKEY, hanafi = false, 41.01384, 28.94966, "Europe/Istanbul", listOf("06:33", "07:58", "13:23", "16:10", "18:39", "20:00")),
        Fixture("Karachi", "2027-02-10", PrayerMethod.KARACHI, hanafi = true, 24.8608, 67.0104, "Asia/Karachi", listOf("05:52", "07:10", "12:46", "16:46", "18:23", "19:40")),
        Fixture("Jakarta", "2027-02-10", PrayerMethod.INDONESIA, hanafi = false, -6.21462, 106.84513, "Asia/Jakarta", listOf("04:37", "05:57", "12:07", "15:22", "18:17", "19:28")),
        Fixture("London", "2027-02-10", PrayerMethod.MUSLIM_WORLD_LEAGUE, hanafi = false, 51.50853, -0.12574, "Europe/London", listOf("05:31", "07:24", "12:15", "14:36", "17:06", "18:53")),
    )

    private fun schedule(fixture: Fixture, settings: PrayerSettings = PrayerSettings()) = PrayerSchedule(
        PrayerPlace(fixture.city, "", "", "", fixture.latitude, fixture.longitude, fixture.zone),
        settings.copy(method = fixture.method, asr = if (fixture.hanafi) AsrChoice.HANAFI else AsrChoice.SHAFI),
        HijriCalendar(),
    )

    /**
     * Within a minute, as each side rounds on its own. Two known differences are allowed:
     * - Asr far from the equator, up to three minutes, from how each side takes the sun's
     *   declination. The Saudi cities show no such difference.
     * - Dubai's sunrise and Asr: the library keeps the minutes Dubai's own table adds, and the
     *   service marks its Dubai method as experimental.
     */
    @Test
    fun `times agree with the online service`() {
        val misses = fixtures.flatMap { fixture ->
            val day = schedule(fixture).day(LocalDate.parse(fixture.date))
            Prayer.entries.zip(fixture.times).mapNotNull { (prayer, expected) ->
                val ours = day[prayer].toLocalTime()
                val minutes = kotlin.math.abs(Duration.between(LocalTime.parse(expected), ours).toMinutes())
                val allowed = when {
                    fixture.method == PrayerMethod.UMM_AL_QURA -> 1
                    fixture.method == PrayerMethod.DUBAI && prayer in setOf(Prayer.SUNRISE, Prayer.ASR) -> 4
                    prayer == Prayer.ASR -> 3
                    else -> 1
                }
                if (minutes > allowed) "${fixture.city} ${fixture.date} $prayer: $ours, service $expected" else null
            }
        }
        assertTrue(misses.joinToString("\n"), misses.isEmpty())
    }

    @Test
    fun `umm al-qura puts isha two hours after maghrib in ramadan only`() {
        val makkah = fixtures.first { it.city == "Makkah" }
        val outside = schedule(makkah).day(LocalDate.parse("2026-11-11"))
        assertEquals(90, Duration.between(outside[Prayer.MAGHRIB], outside[Prayer.ISHA]).toMinutes())
        val ramadan = schedule(makkah).day(LocalDate.parse("2027-02-10"))
        assertEquals(120, Duration.between(ramadan[Prayer.MAGHRIB], ramadan[Prayer.ISHA]).toMinutes())
    }

    @Test
    fun `a user's minutes are added to each time`() {
        val riyadh = fixtures.first { it.city == "Riyadh" }
        val date = LocalDate.parse("2026-11-11")
        val plain = schedule(riyadh).day(date)
        val moved = schedule(riyadh, PrayerSettings(adjustments = mapOf(Prayer.FAJR to 2, Prayer.ISHA to -3))).day(date)
        assertEquals(2, Duration.between(plain[Prayer.FAJR], moved[Prayer.FAJR]).toMinutes())
        assertEquals(-3, Duration.between(plain[Prayer.ISHA], moved[Prayer.ISHA]).toMinutes())
        assertEquals(plain[Prayer.DHUHR], moved[Prayer.DHUHR])
    }

    @Test
    fun `after isha the next prayer is tomorrow's fajr`() {
        val makkah = fixtures.first { it.city == "Makkah" }
        val schedule = schedule(makkah)
        val zone = schedule.zone
        val evening = ZonedDateTime.of(LocalDate.parse("2026-11-11"), LocalTime.of(21, 0), zone).toInstant()
        val next = schedule.next(evening)
        assertEquals(Prayer.FAJR, next.prayer)
        assertTrue(next.tomorrow)
        assertEquals(LocalDate.parse("2026-11-12"), next.time.toLocalDate())
        assertEquals(Prayer.ISHA, schedule.current(evening)?.prayer)

        val noon = ZonedDateTime.of(LocalDate.parse("2026-11-11"), LocalTime.of(13, 0), zone).toInstant()
        assertEquals(Prayer.ASR, schedule.next(noon).prayer)
        assertEquals(Prayer.DHUHR, schedule.current(noon)?.prayer)
        val morning = ZonedDateTime.of(LocalDate.parse("2026-11-11"), LocalTime.of(9, 0), zone).toInstant()
        assertNull(schedule.current(morning))
        val night = ZonedDateTime.of(LocalDate.parse("2026-11-11"), LocalTime.of(2, 0), zone).toInstant()
        assertEquals(Prayer.ISHA, schedule.current(night)?.prayer)
        assertEquals(Prayer.FAJR, schedule.next(night).prayer)
    }

    @Test
    fun `a place abroad shows its own local times`() {
        // Cairo's times are read in Cairo's zone, whatever zone the phone is in.
        val cairo = fixtures.first { it.city == "Cairo" }
        assertEquals("Africa/Cairo", schedule(cairo).zone.id)
        val dhuhr = schedule(cairo).day(LocalDate.parse("2026-11-11"))[Prayer.DHUHR]
        assertEquals(LocalDate.parse("2026-11-11"), dhuhr.toLocalDate())
        assertTrue(dhuhr.toLocalTime() in LocalTime.parse("11:38")..LocalTime.parse("11:41"))
    }

    @Test
    fun `qibla bearing and distance`() {
        val riyadh = fixtures.first { it.city == "Riyadh" }
        val schedule = schedule(riyadh)
        // Makkah lies a little south of west from Riyadh, about 793 km away in a straight line.
        assertEquals(244.15, schedule.qiblaBearing, 0.05)
        assertEquals(793.3, schedule.distanceToKaabaKm, 0.5)
        val london = schedule(fixtures.first { it.city == "London" })
        assertEquals(118.99, london.qiblaBearing, 0.05)
        assertEquals(4794.0, london.distanceToKaabaKm, 1.0)
    }

    @Test
    fun `countries choose their own method`() {
        assertEquals(PrayerMethod.UMM_AL_QURA, PrayerMethod.forCountry("sa"))
        assertEquals(PrayerMethod.EGYPT, PrayerMethod.forCountry("EG"))
        assertEquals(PrayerMethod.MUSLIM_WORLD_LEAGUE, PrayerMethod.forCountry("GB"))
        assertTrue(PrayerMethod.hanafiByDefault("PK"))
        assertEquals(false, PrayerMethod.hanafiByDefault("SA"))
    }
}
