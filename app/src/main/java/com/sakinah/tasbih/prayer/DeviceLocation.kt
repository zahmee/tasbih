package com.sakinah.tasbih.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import android.os.SystemClock
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The phone's position, read when the user asks and, if they leave it on, each time the app is
 * opened. Reading it needs no internet and no Google services, so it works on Huawei phones
 * too. The coordinates then go to the phone's place naming service for a name, see [PlaceNamer].
 */
object DeviceLocation {
    /** How long to wait for a first position when the phone remembers none at all. */
    private const val TIMEOUT_MS = 20_000L

    /** A remembered position this recent is the answer, with no waiting. */
    private const val FRESH_MS = 10 * 60_000L

    /** With an older position in hand, a newer one is given only this long to arrive. */
    private const val REFRESH_MS = 3_000L

    /** Asked for together: the user chooses in the system's dialog between precise and approximate. */
    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    /** The approximate position is enough for the times and the town; the district needs the precise one. */
    fun hasPermission(context: Context): Boolean =
        PERMISSIONS.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Whether the user allowed the precise position. With the approximate one only, Android moves
     * every position by up to a couple of kilometres on purpose, whatever the phone itself knows.
     */
    fun hasPrecisePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Null when the permission is missing, location is off, or no position came in time.
     * [fallbackName] is the label kept when the position could not be named.
     */
    suspend fun currentPlace(context: Context, fallbackName: String, timeoutMs: Long = TIMEOUT_MS): PrayerPlace? {
        if (!hasPermission(context)) return null
        val location = position(context, timeoutMs) ?: return null
        val named = PlaceNamer.name(context, location.latitude, location.longitude)
        return PrayerPlace(
            name = named?.name ?: fallbackName,
            region = named?.region.orEmpty(),
            country = named?.country.orEmpty(),
            countryCode = named?.countryCode?.takeIf { it.isNotEmpty() } ?: countryCode(context),
            latitude = location.latitude,
            longitude = location.longitude,
            // The phone is where its owner is, so its own zone is the place's.
            timeZone = ZoneId.systemDefault().id,
            fromDevice = true,
        )
    }

    /**
     * The position the phone already remembers comes at once, where waiting for a new one can
     * take half a minute indoors. So a recent one is used as it is, and an older one only gives
     * a newer one a moment to arrive. Opening the app later reads the position again anyway.
     */
    private suspend fun position(context: Context, timeoutMs: Long): Location? {
        val remembered = lastKnown(context)
        if (remembered != null && ageMs(remembered) <= FRESH_MS) return remembered
        val wait = if (remembered == null) timeoutMs else minOf(timeoutMs, REFRESH_MS)
        return withTimeoutOrNull(wait) { current(context) } ?: remembered
    }

    private fun ageMs(location: Location): Long =
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000

    @SuppressLint("MissingPermission")
    private suspend fun current(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = provider(manager)
        if (provider == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        } catch (_: SecurityException) {
            // Before Android 12 the satellites refuse an app that holds the approximate permission only.
            if (continuation.isActive) continuation.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        return manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /** The phone's own blend of sources where it has one, then the network, then satellites. */
    private fun provider(manager: LocationManager): String? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
            LocationManager.FUSED_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> null
    }

    /**
     * The country, to choose the calculation method, without sending the position anywhere: the
     * mobile network's country, then the SIM's, then the phone's region setting.
     */
    private fun countryCode(context: Context): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        return listOfNotNull(
            telephony?.networkCountryIso,
            telephony?.simCountryIso,
            Resources.getSystem().configuration.locales[0].country,
        ).firstOrNull { it.length == 2 }?.uppercase().orEmpty()
    }
}
