package com.sakinah.tasbih.ui.prayer

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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sakinah.tasbih.prayer.PrayerSchedule
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
    onBack: () -> Unit,
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
            IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("prayer_open_settings")) {
                Icon(Icons.Outlined.Tune, stringResource(R.string.prayer_settings))
            }
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
                calendarText?.let {
                    val hijriDay = com.sakinah.tasbih.calendar.hijriDayFor(date, now, day[Prayer.MAGHRIB].toInstant(), it.settings.hijriDayFromMaghrib)
                    Text(
                        listOf(it.hijriDate(hijriDay), it.gregorianDate(date)).filter(String::isNotEmpty).joinToString(" – "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onChoosePlace) { Text(stringResource(R.string.prayer_change_place)) }
        }

        NextPrayerCard(
            name = text.name(next.prayer, next.time),
            time = text.time(next.time),
            remaining = text.remaining(Duration.between(now, next.time.toInstant())),
            tomorrow = next.tomorrow,
        )

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

        if (calendarText != null) {
            OutlinedButton(
                onClick = onOpenMonth,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("prayer_open_month"),
            ) {
                Text(stringResource(R.string.prayer_month_open))
            }
        }

        val methodLine = if (state.settings.method == null) {
            stringResource(R.string.prayer_method_auto_line, text.methodName(schedule.method))
        } else {
            stringResource(R.string.prayer_method_line, text.methodName(schedule.method))
        }
        Text(
            listOfNotNull(
                methodLine,
                stringResource(R.string.prayer_asr_hanafi).takeIf { schedule.hanafi && state.settings.asr != AsrChoice.SHAFI },
            ).joinToString("، "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
    }
}

/** The approved home card's shape: rounded top-right and bottom-left corners only. */
private val NextPrayerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 24.dp, bottomStart = 0.dp)

@Composable
private fun NextPrayerCard(name: String, time: String, remaining: String, tomorrow: Boolean) {
    Surface(
        shape = NextPrayerShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().testTag("prayer_next"),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(if (tomorrow) R.string.prayer_next_tomorrow else R.string.prayer_next),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(remaining, style = MaterialTheme.typography.bodyLarge)
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
