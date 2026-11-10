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
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The phone's approximate position, read once when the user asks. It needs no internet and no
 * Google services, so it works on Huawei phones too, and the position never leaves the phone.
 */
object DeviceLocation {
    private const val TIMEOUT_MS = 30_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Null when the permission is missing, location is off, or no position came in time. */
    suspend fun currentPlace(context: Context, name: String): PrayerPlace? {
        if (!hasPermission(context)) return null
        val location = withTimeoutOrNull(TIMEOUT_MS) { current(context) } ?: lastKnown(context) ?: return null
        return PrayerPlace(
            name = name,
            region = "",
            country = "",
            countryCode = countryCode(context),
            latitude = location.latitude,
            longitude = location.longitude,
            // The phone is where its owner is, so its own zone is the place's.
            timeZone = ZoneId.systemDefault().id,
            fromDevice = true,
        )
    }

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
        LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { location ->
            if (continuation.isActive) continuation.resume(location)
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
