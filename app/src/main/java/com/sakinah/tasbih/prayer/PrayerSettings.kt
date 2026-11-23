package com.sakinah.tasbih.prayer

/** The day's times in order. [SUNRISE] ends the time of Fajr; it is not a prayer of its own. */
enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

/** The five prayers, without sunrise. */
val ObligatoryPrayers: List<Prayer> = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

/** Where the times are worked out for: a place found by search, or the phone's own position. */
data class PrayerPlace(
    val name: String,
    val region: String,
    val country: String,
    /** Two letters, such as "SA". Chooses the method when the user has not chosen one. */
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    /** An IANA zone such as "Asia/Riyadh", so a place abroad shows its own local times. */
    val timeZone: String,
    val fromDevice: Boolean = false,
) {
    /**
     * True when the phone's clock no longer keeps this place's time, which after a time zone
     * change most likely means the user has travelled and the times are for where they left.
     */
    fun keepsOtherTimeThan(deviceZone: java.time.ZoneId, now: java.time.Instant): Boolean {
        val own = runCatching { java.time.ZoneId.of(timeZone) }.getOrNull() ?: return false
        return own.rules.getOffset(now) != deviceZone.rules.getOffset(now)
    }
}

enum class AsrChoice { AUTO, SHAFI, HANAFI }

enum class HighLatitudeChoice { AUTO, MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }

data class PrayerSettings(
    val place: PrayerPlace? = null,
    /** Null follows the place's country. */
    val method: PrayerMethod? = null,
    val asr: AsrChoice = AsrChoice.AUTO,
    val highLatitude: HighLatitudeChoice = HighLatitudeChoice.AUTO,
    /** Minutes added to each time, from -30 to +30, to match a local mosque or official table. */
    val adjustments: Map<Prayer, Int> = emptyMap(),
    /** With a place read from the phone, read it again each time the app opens, to follow a journey. */
    val followDevice: Boolean = true,
    /** The Qibla over satellite pictures rather than as a compass, where the phone can show them. */
    val qiblaSatellite: Boolean = false,
    /** Minutes from each prayer's adhan to its iqama, zero for none; the five only. */
    val iqama: Map<Prayer, Int> = DEFAULT_IQAMA,
) {
    fun iqamaFor(prayer: Prayer): Int =
        if (prayer == Prayer.SUNRISE) 0 else (iqama[prayer] ?: DEFAULT_IQAMA[prayer] ?: 0).coerceIn(0, MAX_IQAMA)

    companion object {
        const val MAX_ADJUSTMENT = 30
        const val MAX_IQAMA = 60

        /** As most mosques in Saudi Arabia keep them, which the user chose on November 23, 2026. */
        val DEFAULT_IQAMA: Map<Prayer, Int> = mapOf(
            Prayer.FAJR to 25,
            Prayer.DHUHR to 20,
            Prayer.ASR to 20,
            Prayer.MAGHRIB to 10,
            Prayer.ISHA to 20,
        )

        /** How long the clock counts up after the iqama, about the length of the prayer, before it turns to the next. */
        const val AFTER_IQAMA_MINUTES = 15L
    }
}
