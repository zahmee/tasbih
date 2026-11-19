package com.sakinah.tasbih.prayer

import android.content.Context
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Names a position from a list kept inside the app, with no internet: the towns, villages and
 * districts of the Arab countries that GeoNames knows by an Arabic name. It stands in where the
 * phone has no place naming service, as on Huawei phones without Google services, or no
 * connection. The list is built by `tools/build_places.py`.
 */
object NearbyPlaces {
    private const val ASSET = "places_ar.tsv"

    /** Farther than this from every listed place, the position is left unnamed. */
    private const val REACH_KM = 15.0

    /** A town this near is the place's name, rather than a hamlet that happens to be nearer. */
    private const val TOWN_REACH_KM = 3.0

    /** A degree of latitude is about 111 km, so nothing farther than this many degrees can be in reach. */
    private const val REACH_DEGREES = 0.15

    private class Place(val name: String, val region: String, val countryCode: String, val district: Boolean, val town: Boolean, val km: Double)

    suspend fun name(context: Context, latitude: Double, longitude: Double): PlaceName? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).useLines { pick(it, latitude, longitude) }
        } catch (_: java.io.IOException) {
            null
        }
    }

    /**
     * The nearest listed place. A district is joined to its town, as «الحي، المدينة», and a
     * town close by is preferred to a nearer hamlet, which is how people name where they are.
     */
    fun pick(lines: Sequence<String>, latitude: Double, longitude: Double): PlaceName? {
        val near = lines.mapNotNull { parse(it, latitude, longitude) }.filter { it.km <= REACH_KM }.sortedBy { it.km }.toList()
        val nearest = near.firstOrNull() ?: return null
        val town = near.firstOrNull { it.town && !it.district }
        val name = when {
            nearest.district -> listOfNotNull(nearest.name, (town ?: near.firstOrNull { !it.district })?.name).distinct().joinToString("، ")
            town != null && town.km <= TOWN_REACH_KM -> town.name
            else -> nearest.name
        }
        val region = (if (nearest.district) nearest else near.first { name.endsWith(it.name) }).region
            .ifEmpty { near.firstOrNull { it.region.isNotEmpty() && it.countryCode == nearest.countryCode }?.region.orEmpty() }
        return PlaceName(
            name = name,
            region = region.takeIf { it != name }.orEmpty(),
            country = COUNTRIES[nearest.countryCode].orEmpty(),
            countryCode = nearest.countryCode,
        )
    }

    /** Null for a line out of reach or out of shape. The latitude is checked first, to skip most lines cheaply. */
    private fun parse(line: String, latitude: Double, longitude: Double): Place? {
        val firstTab = line.indexOf('\t')
        if (firstTab <= 0) return null
        val placeLatitude = line.substring(0, firstTab).toDoubleOrNull() ?: return null
        if (abs(placeLatitude - latitude) > REACH_DEGREES) return null
        val fields = line.split('\t')
        if (fields.size < 6) return null
        val placeLongitude = fields[1].toDoubleOrNull() ?: return null
        return Place(
            name = fields[2],
            region = fields[3],
            countryCode = fields[4],
            district = 'x' in fields[5],
            town = 't' in fields[5],
            km = PrayerSchedule.distanceKm(latitude, longitude, placeLatitude, placeLongitude),
        )
    }

    private val COUNTRIES = mapOf(
        "SA" to "السعودية", "AE" to "الإمارات", "KW" to "الكويت", "QA" to "قطر", "BH" to "البحرين", "OM" to "عُمان",
        "YE" to "اليمن", "IQ" to "العراق", "SY" to "سوريا", "JO" to "الأردن", "LB" to "لبنان", "PS" to "فلسطين",
        "EG" to "مصر", "SD" to "السودان", "LY" to "ليبيا", "TN" to "تونس", "DZ" to "الجزائر", "MA" to "المغرب",
        "MR" to "موريتانيا", "SO" to "الصومال", "DJ" to "جيبوتي", "KM" to "جزر القمر",
    )
}
