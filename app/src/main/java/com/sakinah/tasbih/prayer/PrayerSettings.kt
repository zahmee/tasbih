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
) {
    companion object {
        const val MAX_ADJUSTMENT = 30
    }
}
