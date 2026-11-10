package com.sakinah.tasbih.prayer

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.PrayerAdjustments

/**
 * The ways of working out the times that countries and bodies publish. The angles follow the
 * table the Aladhan service publishes, so the same place gives the same times here and there.
 */
enum class PrayerMethod {
    UMM_AL_QURA,
    KUWAIT,
    QATAR,
    GULF,
    DUBAI,
    EGYPT,
    JORDAN,
    MOROCCO,
    ALGERIA,
    TUNISIA,
    TURKEY,
    KARACHI,
    INDONESIA,
    MALAYSIA,
    SINGAPORE,
    MUSLIM_WORLD_LEAGUE,
    NORTH_AMERICA,
    MOONSIGHTING,
    FRANCE,
    RUSSIA,
    ;

    /** A fresh set each time, as the library's parameters are changed in place by their user. */
    fun parameters(): CalculationParameters = when (this) {
        UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
        KUWAIT -> CalculationMethod.KUWAIT.parameters
        QATAR -> CalculationMethod.QATAR.parameters
        DUBAI -> CalculationMethod.DUBAI.parameters
        EGYPT -> CalculationMethod.EGYPTIAN.parameters
        KARACHI -> CalculationMethod.KARACHI.parameters
        SINGAPORE -> CalculationMethod.SINGAPORE.parameters
        MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA.parameters
        MOONSIGHTING -> CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
        GULF -> CalculationParameters(19.5, 90, CalculationMethod.OTHER)
        // The Jordanian ministry adds five minutes to sunset for Maghrib.
        JORDAN -> angles(18.0, 18.0).withMethodAdjustments(PrayerAdjustments(0, 0, 0, 0, 5, 0))
        // The Moroccan ministry adds five minutes of caution to Dhuhr and to Maghrib.
        MOROCCO -> angles(19.0, 17.0).withMethodAdjustments(PrayerAdjustments(0, 0, 5, 0, 5, 0))
        ALGERIA -> angles(18.0, 17.0)
        TUNISIA -> angles(18.0, 18.0)
        // Diyanet's fixed margins, its "temkin", on sunrise, Dhuhr, Asr and Maghrib.
        TURKEY -> angles(18.0, 17.0).withMethodAdjustments(PrayerAdjustments(0, -7, 5, 4, 7, 0))
        INDONESIA -> angles(20.0, 18.0)
        MALAYSIA -> angles(20.0, 18.0)
        FRANCE -> angles(12.0, 12.0)
        RUSSIA -> angles(16.0, 15.0)
    }

    private fun angles(fajr: Double, isha: Double) = CalculationParameters(fajr, isha, CalculationMethod.OTHER)

    companion object {
        /**
         * The method a country's own authority uses, as best known. Countries not listed use the
         * Muslim World League's. This is a default only: the user can always choose another.
         */
        fun forCountry(countryCode: String): PrayerMethod = when (countryCode.uppercase()) {
            "SA" -> UMM_AL_QURA
            "KW" -> KUWAIT
            "QA" -> QATAR
            "AE" -> DUBAI
            "BH", "OM" -> GULF
            "EG", "SD", "LY", "SY", "LB", "IQ", "PS" -> EGYPT
            "JO" -> JORDAN
            "MA" -> MOROCCO
            "DZ" -> ALGERIA
            "TN" -> TUNISIA
            "TR" -> TURKEY
            "PK", "IN", "BD", "AF" -> KARACHI
            "ID" -> INDONESIA
            "MY", "BN" -> MALAYSIA
            "SG" -> SINGAPORE
            "US", "CA" -> NORTH_AMERICA
            "FR" -> FRANCE
            "RU" -> RUSSIA
            else -> MUSLIM_WORLD_LEAGUE
        }

        /** Where most Muslims follow the Hanafi school, Asr is later. */
        fun hanafiByDefault(countryCode: String): Boolean = countryCode.uppercase() in setOf("PK", "IN", "BD", "AF")
    }
}
