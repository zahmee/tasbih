package com.sakinah.tasbih.ui.prayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Qibla
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.sakinah.tasbih.BuildConfig
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.prayer.BetterFix
import com.sakinah.tasbih.prayer.DeviceLocation
import com.sakinah.tasbih.prayer.PrayerSchedule
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Qibla over satellite pictures, from Google Maps. It is offered only where it can work: in
 * a build that was given a Maps key, on a phone with Google services. The map's pictures are
 * fetched from Google, which so learns the area on show; nothing else about the user is sent.
 */
object QiblaMaps {
    fun available(context: Context): Boolean = BuildConfig.MAPS_ENABLED &&
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}

/** The compass, or the same direction drawn over satellite pictures of the place. */
@Composable
internal fun QiblaViewToggle(satellite: Boolean, onSatellite: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth().testTag("qibla_view_toggle")) {
        SegmentedButton(
            selected = !satellite,
            onClick = { onSatellite(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("qibla_view_compass"),
        ) { Text(stringResource(R.string.qibla_view_compass)) }
        SegmentedButton(
            selected = satellite,
            onClick = { onSatellite(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("qibla_view_satellite"),
        ) { Text(stringResource(R.string.qibla_view_satellite)) }
    }
}

/** The phone's own position while the page is on screen, or null until the first fix arrives. */
internal class LivePosition {
    var location by mutableStateOf<Location?>(null)
}

/**
 * Follows the phone while the page is in front, and stops when the user leaves it: nothing is
 * read in the background. It listens to every source the phone has switched on, and keeps the
 * better fix by [BetterFix], so a rough network position gives way to the satellites' once they
 * answer, and does not take their place again while theirs is still current.
 */
@SuppressLint("MissingPermission")
@Composable
internal fun rememberLivePosition(granted: Boolean, precise: Boolean): LivePosition {
    val context = LocalContext.current
    val state = remember { LivePosition() }
    // Listeners made under the approximate permission keep their blurred positions after the
    // user allows the precise one, so a change of either starts the listening afresh.
    LifecycleStartEffect(context, granted, precise) {
        val manager = context.getSystemService(LocationManager::class.java)
        if (!granted || manager == null) return@LifecycleStartEffect onStopOrDispose { }
        val wanted = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        val providers = wanted.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        fun offer(fix: Location) {
            val held = state.location
            if (BetterFix.replaces(fix.accuracyOrNull(), fix.atMs(), held?.accuracyOrNull(), held?.atMs())) state.location = fix
        }
        state.location = null
        providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.forEach(::offer)
        val listener = LocationListenerCompat { fix -> offer(fix) }
        val request = LocationRequestCompat.Builder(FIX_EVERY_MS).setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY).build()
        providers.forEach { provider ->
            // Before Android 12 the satellites refuse an app that holds the approximate permission only.
            runCatching {
                LocationManagerCompat.requestLocationUpdates(manager, provider, request, ContextCompat.getMainExecutor(context), listener)
            }
        }
        onStopOrDispose { runCatching { LocationManagerCompat.removeUpdates(manager, listener) } }
    }
    return state
}

private fun Location.accuracyOrNull(): Float? = if (hasAccuracy()) accuracy else null

/** On the phone's running clock, which every source shares, unlike the satellites' own time. */
private fun Location.atMs(): Long = elapsedRealtimeNanos / 1_000_000

/**
 * The page in its satellite form: the user stands at the middle of the pictures, the pictures
 * turn with the phone, and a line runs from the user to the Kaaba. Turning until the line runs
 * straight up, under the arrow that shows where the phone points, is facing the Qibla.
 */
@Composable
internal fun SatelliteQibla(
    schedule: PrayerSchedule,
    text: PrayerText,
    onChoosePlace: () -> Unit,
    onSatellite: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val place = schedule.place
    var granted by remember { mutableStateOf(DeviceLocation.hasPermission(context)) }
    var precise by remember { mutableStateOf(DeviceLocation.hasPrecisePermission(context)) }
    // Once Android has been asked and still gives the approximate position, it shows no dialog
    // again, so the next tap opens the app's own page in the phone's settings.
    var askedPrecise by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        granted = DeviceLocation.hasPermission(context)
        precise = DeviceLocation.hasPrecisePermission(context)
    }
    // The user may change it in the settings and come back.
    LifecycleResumeEffect(context) {
        granted = DeviceLocation.hasPermission(context)
        precise = DeviceLocation.hasPrecisePermission(context)
        onPauseOrDispose { }
    }
    val askPrecise = {
        if (askedPrecise) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            askedPrecise = true
            permission.launch(DeviceLocation.PERMISSIONS)
        }
    }
    val fix = rememberLivePosition(granted, precise).location
    // Until the phone knows where it is, the saved place stands in, and the page says so.
    val here = fix?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(place.latitude, place.longitude)
    val qibla = remember(here) { Qibla(Coordinates(here.latitude, here.longitude)).direction.toFloat() }
    // The field's correction barely changes within a town, so the saved place serves for it.
    val compass = rememberCompass(place.latitude, place.longitude)
    val heading = compass.heading
    val turn = heading?.let { normalize(qibla - it) }
    val facing = turn != null && abs(turn) <= FACING_DEGREES

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(facing) {
        if (facing) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.qibla_bearing, text.degrees(qibla.toDouble())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("qibla_map_bearing"),
                )
            }
            TextButton(onClick = onChoosePlace) { Text(stringResource(R.string.prayer_change_place)) }
        }
        QiblaViewToggle(satellite = true, onSatellite = onSatellite)
        QiblaMap(here = here, heading = heading, facing = facing, modifier = Modifier.weight(1f).fillMaxWidth())

        val guidance = when {
            !compass.available -> stringResource(R.string.qibla_map_no_compass)
            turn == null -> stringResource(R.string.qibla_waiting)
            facing -> stringResource(R.string.qibla_facing)
            turn > 0 -> stringResource(R.string.qibla_turn_right, arabicNumber(roundToFive(turn)))
            else -> stringResource(R.string.qibla_turn_left, arabicNumber(roundToFive(-turn)))
        }
        Text(
            guidance,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (facing) FontWeight.Bold else FontWeight.Normal,
            color = if (facing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.testTag("qibla_guidance"),
        )

        when {
            !granted -> {
                Text(stringResource(R.string.qibla_map_saved_place), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = { permission.launch(DeviceLocation.PERMISSIONS) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("qibla_map_allow"),
                ) { Text(stringResource(R.string.qibla_map_allow)) }
            }
            // Going outside cannot help here: Android moves the position on purpose.
            !precise -> {
                Text(stringResource(R.string.qibla_map_approximate), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(
                    onClick = askPrecise,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("qibla_map_allow_precise"),
                ) { Text(stringResource(R.string.qibla_map_allow_precise)) }
            }
            fix == null -> Text(stringResource(R.string.qibla_map_locating), style = MaterialTheme.typography.bodyMedium)
            fix.hasAccuracy() && fix.accuracy > ROUGH_METRES -> Text(
                stringResource(R.string.qibla_map_rough, arabicNumber(fix.accuracy.roundToInt())),
                style = MaterialTheme.typography.bodyMedium,
            )
            compass.available -> Text(stringResource(R.string.qibla_map_hint), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            stringResource(R.string.qibla_map_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Satellite pictures centred on the user and turned as the phone turns, so what is ahead of the
 * user in the street is ahead of them on the screen. The user's mark stays in the middle with an
 * arrow for where the phone points, and the line to the Kaaba swings round it as they turn; it
 * goes green when the two agree. Without a compass the pictures keep north up.
 */
@Composable
private fun QiblaMap(here: LatLng, heading: Float?, facing: Boolean, modifier: Modifier = Modifier) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.Builder().target(here).zoom(START_ZOOM).build()
    }
    val kaaba = remember { LatLng(PrayerSchedule.KAABA_LATITUDE, PrayerSchedule.KAABA_LONGITUDE) }
    val description = stringResource(R.string.qibla_map_description)
    val (lineWidth, edgeWidth) = with(LocalDensity.current) { 5.dp.toPx() to 8.dp.toPx() }
    // In whole degrees, so the pictures are not redrawn for every tremor of the hand.
    val bearing = heading?.roundToInt()?.toFloat() ?: 0f

    LaunchedEffect(here, bearing) {
        // The user's own pinch to zoom is left alone until it ends.
        val pinching = camera.isMoving && camera.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        if (!pinching) {
            camera.position = CameraPosition.Builder().target(here).zoom(camera.position.zoom).bearing(bearing).build()
        }
    }

    Box(modifier.clip(MaterialTheme.shapes.large).testTag("qibla_map").semantics { contentDescription = description }) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = camera,
            // Pictures with the names of streets and districts over them, to know the place by.
            properties = MapProperties(mapType = MapType.HYBRID),
            uiSettings = MapUiSettings(
                compassEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                // The map follows the user and the phone; only its closeness is the user's to change.
                rotationGesturesEnabled = false,
                scrollGesturesEnabled = false,
                tiltGesturesEnabled = false,
                zoomControlsEnabled = false,
            ),
        ) {
            // A dark edge under the line, to stand out on sand and on roofs alike.
            Polyline(points = listOf(here, kaaba), geodesic = true, color = LINE_EDGE, width = edgeWidth)
            Polyline(points = listOf(here, kaaba), geodesic = true, color = if (facing) LINE_FACING else LINE_GOLD, width = lineWidth)
        }
        UserMark(showsDirection = heading != null, modifier = Modifier.align(Alignment.Center))
    }
}

/** The user, in the middle of the map: a dot, and above it an arrow for where the phone points. */
@Composable
private fun UserMark(showsDirection: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(64.dp)) {
        val dot = 9.dp.toPx()
        if (showsDirection) {
            val tip = center.y - 30.dp.toPx()
            val base = center.y - 12.dp.toPx()
            val half = 9.dp.toPx()
            val arrow = Path().apply {
                moveTo(center.x, tip)
                lineTo(center.x + half, base)
                lineTo(center.x, base - 4.dp.toPx())
                lineTo(center.x - half, base)
                close()
            }
            drawPath(arrow, LINE_EDGE, style = Stroke(4.dp.toPx()))
            drawPath(arrow, Color.White)
        }
        drawCircle(LINE_EDGE, dot + 3.dp.toPx(), center)
        drawCircle(Color.White, dot, center)
        drawCircle(USER_BLUE, dot - 3.dp.toPx(), Offset(center.x, center.y))
    }
}

/** Near enough to tell one house from the next. */
private const val START_ZOOM = 18f
private const val FIX_EVERY_MS = 2_000L

/** Beyond this the mark may not be on the user's own building, and the page says how rough it is. */
private const val ROUGH_METRES = 50f
private val LINE_GOLD = Color(0xFFFFC857)
private val LINE_FACING = Color(0xFF3DDC84)
private val LINE_EDGE = Color(0xCC0B1A16)
private val USER_BLUE = Color(0xFF1A73E8)
