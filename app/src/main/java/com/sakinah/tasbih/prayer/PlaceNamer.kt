package com.sakinah.tasbih.prayer

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** What a position is called: the district and town, the region around them, and the country. */
data class PlaceName(val name: String, val region: String, val country: String, val countryCode: String)

/** The parts of one address the phone's place naming service gave back, any of which may be missing. */
data class AddressParts(
    val district: String? = null,
    val town: String? = null,
    val county: String? = null,
    val region: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
)

/**
 * Names the phone's position, as «الحي، المدينة», with the phone's own place naming service.
 * On most phones that service is Google's, so the coordinates are sent to it, and nothing else.
 * A phone without the service, or without a connection, is named from the list inside the app,
 * see [NearbyPlaces], which sends nothing. Outside that list's countries it keeps a plain label.
 */
object PlaceNamer {
    private const val TIMEOUT_MS = 10_000L
    private const val MAX_ADDRESSES = 5

    /** Null when neither the phone's naming service nor the app's own list knows a name there. */
    suspend fun name(context: Context, latitude: Double, longitude: Double): PlaceName? =
        fromService(context, latitude, longitude) ?: NearbyPlaces.name(context, latitude, longitude)

    private suspend fun fromService(context: Context, latitude: Double, longitude: Double): PlaceName? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("ar"))
        val addresses = withTimeoutOrNull(TIMEOUT_MS) {
            try {
                fetch(geocoder, latitude, longitude)
            } catch (_: IOException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }.orEmpty()
        return compose(
            addresses.map {
                AddressParts(it.subLocality, it.locality, it.subAdminArea, it.adminArea, it.countryName, it.countryCode)
            },
        )
    }

    /**
     * The service answers with several addresses, from the nearest building outwards, and each
     * may lack a part, so every part is taken from the first address that has it.
     */
    fun compose(addresses: List<AddressParts>): PlaceName? {
        fun first(part: (AddressParts) -> String?): String? =
            addresses.firstNotNullOfOrNull { part(it)?.trim()?.takeIf(String::isNotEmpty) }

        val region = first { it.region }
        val town = first { it.town } ?: first { it.county }
        val district = first { it.district }?.takeIf { it != town }
        val name = listOfNotNull(district, town).joinToString("، ").ifEmpty { region ?: return null }
        return PlaceName(
            name = name,
            region = region?.takeIf { it != name && it != town }.orEmpty(),
            country = first { it.country }.orEmpty(),
            countryCode = first { it.countryCode }?.takeIf { it.length == 2 }?.uppercase().orEmpty(),
        )
    }

    private suspend fun fetch(geocoder: Geocoder, latitude: Double, longitude: Double): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    MAX_ADDRESSES,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, MAX_ADDRESSES).orEmpty()
            }
        }
}
