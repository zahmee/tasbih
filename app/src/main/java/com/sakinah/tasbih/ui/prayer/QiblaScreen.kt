package com.sakinah.tasbih.ui.prayer

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.calendar.CalendarPage
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Qibla as a compass. The bearing is worked out from the place, and the phone's sensors turn
 * the dial. Magnetic north is moved to true north with the earth's field model, which works
 * offline. The screen moves by nature; it is the home screen widgets that must stay still.
 */
@Composable
fun QiblaScreen(
    schedule: PrayerSchedule?,
    onChoosePlace: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    CalendarPage(title = stringResource(R.string.qibla_title), tag = "qibla_screen", onBack = onBack) {
        if (schedule == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(32.dp),
            ) {
                Text(stringResource(R.string.prayer_no_place_title), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onChoosePlace) { Text(stringResource(R.string.prayer_choose_place)) }
            }
            return@CalendarPage
        }
        val place = schedule.place
        val compass = rememberCompass(place.latitude, place.longitude)
        val qibla = schedule.qiblaBearing.toFloat()
        val heading = compass.heading
        // Positive means the Qibla is to the right, clockwise from where the phone points.
        val turn = heading?.let { normalize(qibla - it) }
        val facing = turn != null && abs(turn) <= FACING_DEGREES

        val haptics = LocalHapticFeedback.current
        LaunchedEffect(facing) {
            if (facing) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    text.placeDetail(place).takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onChoosePlace) { Text(stringResource(R.string.prayer_change_place)) }
            }

            Dial(
                heading = heading ?: 0f,
                qibla = qibla,
                facing = facing,
                showHeading = heading != null,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("qibla_dial"),
            )

            val guidance = when {
                !compass.available -> stringResource(R.string.qibla_no_compass, text.degrees(qibla.toDouble()))
                turn == null -> stringResource(R.string.qibla_waiting)
                facing -> stringResource(R.string.qibla_facing)
                // In steps of five degrees, so a screen reader is not flooded with numbers.
                turn > 0 -> stringResource(R.string.qibla_turn_right, arabicNumber(roundToFive(turn)))
                else -> stringResource(R.string.qibla_turn_left, arabicNumber(roundToFive(-turn)))
            }
            Text(
                guidance,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (facing) FontWeight.Bold else FontWeight.Normal,
                color = if (facing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("qibla_guidance"),
            )

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = sakinahCardBorder(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.qibla_bearing, text.degrees(qibla.toDouble())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.qibla_distance, text.kilometres(schedule.distanceToKaabaKm)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (compass.available && compass.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().testTag("qibla_calibrate"),
                ) {
                    Text(
                        stringResource(R.string.qibla_calibrate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            if (compass.available) {
                Text(
                    stringResource(R.string.qibla_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Dial(heading: Float, qibla: Float, facing: Boolean, showHeading: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val copper = LocalSakinahBrandColors.current.antiqueGold
    val measurer = rememberTextMeasurer()
    val northLabel = stringResource(R.string.qibla_north)
    val description = stringResource(R.string.qibla_dial_description)
    val ring = if (facing) colors.primary else copper
    val labelStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier.semantics { contentDescription = description }) {
        val radius = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        drawCircle(colors.surfaceContainerLow, radius, centre)
        drawCircle(ring, radius - 2.dp.toPx(), centre, style = Stroke(if (facing) 4.dp.toPx() else 2.dp.toPx()))

        // The dial turns against the phone, so north on it stays to the real north.
        rotate(-heading, centre) {
            for (degree in 0 until 360 step 15) {
                val long = degree % 90 == 0
                val angle = Math.toRadians(degree.toDouble())
                val outer = radius - 8.dp.toPx()
                val inner = outer - (if (long) 14.dp else 7.dp).toPx()
                drawLine(
                    colors.outline,
                    Offset(centre.x + (sin(angle) * inner).toFloat(), centre.y - (cos(angle) * inner).toFloat()),
                    Offset(centre.x + (sin(angle) * outer).toFloat(), centre.y - (cos(angle) * outer).toFloat()),
                    strokeWidth = if (long) 2.dp.toPx() else 1.dp.toPx(),
                )
            }
            val north = measurer.measure(northLabel, labelStyle)
            drawText(
                north,
                topLeft = Offset(centre.x - north.size.width / 2f, centre.y - radius + 26.dp.toPx()),
            )
            // The Kaaba: a dark cube with a copper band, on the Qibla's bearing.
            val angle = Math.toRadians(qibla.toDouble())
            val at = radius * 0.62f
            val kaaba = Offset(centre.x + (sin(angle) * at).toFloat(), centre.y - (cos(angle) * at).toFloat())
            drawLine(ring.copy(alpha = 0.55f), centre, kaaba, strokeWidth = 3.dp.toPx())
            val side = 30.dp.toPx()
            rotate(qibla, kaaba) {
                drawRect(Color(0xFF1B1B1B), Offset(kaaba.x - side / 2, kaaba.y - side / 2), Size(side, side))
                drawRect(copper, Offset(kaaba.x - side / 2, kaaba.y - side / 2 + side * 0.22f), Size(side, side * 0.14f))
            }
        }

        // Where the phone points: a fixed mark at the top of the dial.
        if (showHeading) {
            val tip = Offset(centre.x, centre.y - radius + 4.dp.toPx())
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - 10.dp.toPx(), tip.y + 18.dp.toPx())
                lineTo(tip.x + 10.dp.toPx(), tip.y + 18.dp.toPx())
                close()
            }
            drawPath(path, if (facing) colors.primary else colors.onSurface)
        }
        drawCircle(ring, 5.dp.toPx(), centre)
    }
}

@Stable
private class CompassState {
    var heading by mutableStateOf<Float?>(null)
    var accuracy by mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    var available by mutableStateOf(true)
}

/**
 * True heading in degrees, from the rotation vector, or the accelerometer and magnetometer
 * where a phone has no rotation vector. Smoothed on sine and cosine, so 359 to 0 is no jump.
 */
@Composable
private fun rememberCompass(latitude: Double, longitude: Double): CompassState {
    val context = LocalContext.current
    val state = remember { CompassState() }
    val declination = remember(latitude, longitude) {
        GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }
    DisposableEffect(context, declination) {
        val manager = context.getSystemService(SensorManager::class.java)
        val rotation = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magnetic = manager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gravity = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        state.available = manager != null && magnetic != null && (rotation != null || gravity != null)
        if (!state.available) return@DisposableEffect onDispose { }

        val matrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        val lastGravity = FloatArray(3)
        val lastField = FloatArray(3)
        var haveGravity = false
        var haveField = false
        var sine = 0f
        var cosine = 0f
        var started = false

        fun publish() {
            remap(context, matrix, remapped)
            SensorManager.getOrientation(remapped, orientation)
            val trueHeading = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
            val radians = Math.toRadians(trueHeading.toDouble())
            if (!started) {
                sine = sin(radians).toFloat()
                cosine = cos(radians).toFloat()
                started = true
            } else {
                sine += SMOOTHING * (sin(radians).toFloat() - sine)
                cosine += SMOOTHING * (cos(radians).toFloat() - cosine)
            }
            state.heading = ((Math.toDegrees(atan2(sine, cosine).toDouble()) + 360) % 360).toFloat()
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(matrix, event.values)
                        publish()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        event.values.copyInto(lastGravity, endIndex = 3)
                        haveGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        event.values.copyInto(lastField, endIndex = 3)
                        haveField = true
                        if (rotation == null && haveGravity && SensorManager.getRotationMatrix(matrix, null, lastGravity, lastField)) {
                            publish()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // The magnetometer's own accuracy says whether the phone needs calibrating.
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) state.accuracy = accuracy
            }
        }
        if (rotation != null) manager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        if (rotation == null) manager.registerListener(listener, gravity, SensorManager.SENSOR_DELAY_UI)
        manager.registerListener(listener, magnetic, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return state
}

/** The sensors measure against the phone held upright; a turned screen turns their axes. */
@Suppress("DEPRECATION")
private fun remap(context: Context, matrix: FloatArray, out: FloatArray) {
    val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display?.rotation ?: Surface.ROTATION_0
    } else {
        context.getSystemService(WindowManager::class.java)?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    }
    val (x, y) = when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
    SensorManager.remapCoordinateSystem(matrix, x, y, out)
}

/** From -180 to 180, so the shorter way round is always chosen. */
private fun normalize(degrees: Float): Float = ((degrees % 360f) + 540f) % 360f - 180f

private fun roundToFive(degrees: Float): Int = ((degrees / 5f).roundToInt() * 5).coerceAtLeast(5)

/** Within this many degrees the phone is taken to face the Qibla. */
private const val FACING_DEGREES = 5f
private const val SMOOTHING = 0.15f
