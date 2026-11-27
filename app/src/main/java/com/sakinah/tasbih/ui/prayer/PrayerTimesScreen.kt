package com.sakinah.tasbih.ui.prayer

import com.sakinah.tasbih.ui.KhatamMark
import com.sakinah.tasbih.ui.FeatureShape
import com.sakinah.tasbih.ui.kuficCorners
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.ui.graphics.vector.ImageVector
import com.sakinah.tasbih.prayer.AlertSettings
import com.sakinah.tasbih.prayer.AlertStyle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.AsrChoice
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerPhase
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.prayer.PrayerWindow
import com.sakinah.tasbih.ui.calendar.CalendarHint
import com.sakinah.tasbih.ui.calendar.CalendarPage
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

@Composable
fun PrayerTimesScreen(
    state: PrayerState,
    calendarText: CalendarText?,
    onChoosePlace: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)?,
    alerts: AlertSettings? = null,
    onOpenAlerts: () -> Unit = {},
    onOpenMonth: () -> Unit = {},
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    var now by remember { mutableStateOf(Instant.now()) }
    // The countdown moves inside the app only; widgets show a fixed time.
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            now = Instant.now()
        }
    }

    CalendarPage(
        title = stringResource(R.string.prayer_title),
        tag = "prayer_screen",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenAlerts, modifier = Modifier.testTag("prayer_open_alerts")) {
                Icon(Icons.Outlined.NotificationsActive, stringResource(R.string.alert_title))
            }
            IconButton(onClick = onOpenQibla, modifier = Modifier.testTag("prayer_open_qibla")) {
                Icon(Icons.Outlined.Explore, stringResource(R.string.qibla_title))
            }
            com.sakinah.tasbih.ui.SettingsGear(onOpenSettings, "prayer_open_settings")
        },
    ) {
        val schedule = state.schedule
        if (schedule == null) {
            NoPlace(onChoosePlace)
        } else {
            Times(schedule, state, text, calendarText, now, onChoosePlace, alerts, onOpenMonth)
        }
    }
}

@Composable
private fun NoPlace(onChoosePlace: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            Icons.Outlined.LocationOn,
            null,
            tint = LocalSakinahBrandColors.current.antiqueGold,
            modifier = Modifier.size(48.dp).padding(top = 32.dp),
        )
        Text(stringResource(R.string.prayer_no_place_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.prayer_no_place_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onChoosePlace, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("prayer_choose_place")) {
            Text(stringResource(R.string.prayer_choose_place))
        }
    }
}

@Composable
private fun Times(
    schedule: PrayerSchedule,
    state: PrayerState,
    text: PrayerText,
    calendarText: CalendarText?,
    now: Instant,
    onChoosePlace: () -> Unit,
    alerts: AlertSettings?,
    onOpenMonth: () -> Unit,
) {
    val date = schedule.today(now)
    val day = remember(schedule, date) { schedule.day(date) }
    val next = remember(schedule, now.epochSecond / 60) { schedule.next(now) }
    val current = remember(schedule, now.epochSecond / 60) { schedule.current(now) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    schedule.place.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("prayer_place"),
                )
                text.placeDetail(schedule.place).takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // The date left this head on November 27, 2026 for the home's alone; the method came up in its place.
                Text(
                    methodLine(schedule, state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onChoosePlace) { Text(stringResource(R.string.prayer_change_place)) }
        }

        val phase = remember(schedule, now.epochSecond / 60) { schedule.phase(now) }
        val window = remember(schedule, now.epochSecond / 60) { schedule.window(now) }
        // The home leads with the next prayer; this tab with the one whose time it is.
        CurrentPrayerCard(window, phase, now, text)

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = sakinahCardBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Prayer.entries.forEach { prayer ->
                    PrayerRow(
                        name = text.name(prayer, day[prayer]),
                        time = text.time(day[prayer]),
                        isCurrent = current?.prayer == prayer && current.time.toLocalDate() == date,
                        isSunrise = prayer == Prayer.SUNRISE,
                        alert = alerts?.alertFor(prayer)?.style ?: AlertStyle.OFF,
                    )
                }
            }
        }

        if (day.estimated) {
            CalendarHint(stringResource(R.string.prayer_estimated_hint), Modifier.testTag("prayer_estimated_hint"))
        }

        if (calendarText != null) {
            OutlinedButton(
                onClick = onOpenMonth,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("prayer_open_month"),
            ) {
                Text(stringResource(R.string.prayer_month_open))
            }
        }

        Spacer(Modifier.size(8.dp))
    }
}

/** How the times are worked out: the method, and the Hanafi Asr where it is used. */
@Composable
private fun methodLine(schedule: PrayerSchedule, state: PrayerState): String = listOfNotNull(
    if (state.settings.method == null) {
        stringResource(R.string.prayer_method_auto_line, methodName(schedule))
    } else {
        stringResource(R.string.prayer_method_line, methodName(schedule))
    },
    stringResource(R.string.prayer_asr_hanafi).takeIf { schedule.hanafi && state.settings.asr != AsrChoice.SHAFI },
).joinToString("، ")

@Composable
private fun methodName(schedule: PrayerSchedule): String = PrayerText(LocalContext.current).methodName(schedule.method)

/**
 * The prayer whose time it is: its name and adhan, how much of its time has passed, what comes
 * after it and when, and its iqama while that is still near. Between sunrise and Dhuhr, the
 * stretch with no prayer due.
 */
@Composable
private fun CurrentPrayerCard(window: PrayerWindow, phase: PrayerPhase, now: Instant, text: PrayerText) {
    val colors = MaterialTheme.colorScheme
    val name = window.prayer?.let { text.name(it.prayer, it.time) } ?: stringResource(R.string.prayer_between_sunrise_dhuhr)
    val until = stringResource(
        R.string.prayer_window_until,
        text.name(window.end.prayer, window.end.time),
        text.time(window.end.time),
        text.remaining(java.time.Duration.between(now, window.end.time.toInstant())),
    )
    // The iqama, from its adhan until a quarter hour after it, as the clock counts it.
    val iqama = if (phase is PrayerPhase.ToAdhan) null else prayerClockWords(phase, now, text).let { "${it.label} ${it.time}، ${it.span}" }
    Surface(
        shape = FeatureShape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().testTag("prayer_current"),
    ) {
        Column(Modifier.kuficCorners(colors.onSecondaryContainer.copy(alpha = 0.22f)).padding(horizontal = 26.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.prayer_now_label), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (window.prayer == null) 24.sp else 30.sp),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                window.prayer?.let { Text(text.time(it.time), style = MaterialTheme.typography.titleMedium) }
            }
            LinearProgressIndicator(
                progress = { window.passed(now) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = colors.primary,
                trackColor = colors.onSecondaryContainer.copy(alpha = 0.15f),
                drawStopIndicator = {},
            )
            Text(until, style = MaterialTheme.typography.bodyLarge)
            iqama?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun PrayerRow(name: String, time: String, isCurrent: Boolean, isSunrise: Boolean, alert: AlertStyle) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(if (isCurrent) colors.secondaryContainer else colors.surfaceContainerLow, MaterialTheme.shapes.medium)
            .heightIn(min = 52.dp)
            .padding(horizontal = 12.dp),
    ) {
        // A khatam marks the prayer whose time it is, so the mark is not the row's colour alone.
        if (isCurrent) {
            KhatamMark()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            name,
            style = MaterialTheme.typography.titleMedium,
            color = if (isSunrise) colors.onSurfaceVariant else colors.onSurface,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        alertIcon(alert)?.let { icon ->
            Icon(icon, stringResource(alert.labelRes), tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        if (isCurrent) {
            Text(
                stringResource(R.string.prayer_current),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            time,
            style = MaterialTheme.typography.titleMedium,
            color = if (isSunrise) colors.onSurfaceVariant else colors.onSurface,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** The mark beside a prayer for how it is announced, as the best prayer apps show it. */
private fun alertIcon(style: AlertStyle): ImageVector? = when (style) {
    AlertStyle.OFF -> null
    AlertStyle.SILENT -> Icons.Outlined.Notifications
    AlertStyle.SOUND -> Icons.Outlined.NotificationsActive
    AlertStyle.ADHAN -> Icons.AutoMirrored.Outlined.VolumeUp
}

private const val TICK_MS = 15_000L
