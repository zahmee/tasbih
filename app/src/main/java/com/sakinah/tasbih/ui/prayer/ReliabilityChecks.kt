package com.sakinah.tasbih.ui.prayer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.AlertHealth
import com.sakinah.tasbih.alerts.PhoneMaker
import com.sakinah.tasbih.alerts.WakeRecord
import com.sakinah.tasbih.ui.SakinahSectionHeader
import com.sakinah.tasbih.ui.calendar.CalendarHint
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Everything a prayer alert needs from the phone, each with its state and a way to fix it, the
 * last alarms and whether they rang, and a test alert. The reliability tab of the alert settings,
 * since its page and the test tab became one on November 28, 2026. It asks for nothing on its
 * own, so it never nags.
 */
internal fun LazyListScope.reliabilityItems(
    health: AlertHealth,
    wakes: List<WakeRecord>,
    testScheduled: Boolean,
    onAllowNotifications: () -> Unit,
    onTest: () -> Unit,
) {
    item { CalendarHint(stringResource(R.string.alert_reliability_intro)) }
    item {
        Check(
            ok = health.notifications,
            title = stringResource(R.string.alert_check_notifications),
            detail = stringResource(if (health.notifications) R.string.alert_check_notifications_ok else R.string.alert_check_notifications_off),
            action = stringResource(R.string.alert_allow),
            onFix = onAllowNotifications,
        )
    }
    item {
        val context = LocalContext.current
        Check(
            ok = health.exactAlarms,
            title = stringResource(R.string.alert_check_exact),
            detail = stringResource(if (health.exactAlarms) R.string.alert_check_exact_ok else R.string.alert_check_exact_off),
            action = stringResource(R.string.alert_allow),
        ) { AlertHealth.exactAlarmSettings(context)?.let { open(context, it) } }
    }
    item {
        val context = LocalContext.current
        Check(
            ok = health.batteryUnrestricted,
            title = stringResource(R.string.alert_check_battery),
            detail = stringResource(if (health.batteryUnrestricted) R.string.alert_check_battery_ok else R.string.alert_check_battery_off),
            action = stringResource(R.string.alert_open_settings),
        ) { open(context, AlertHealth.batterySettings()) }
    }
    item {
        val context = LocalContext.current
        Check(
            ok = health.channelsOn,
            title = stringResource(R.string.alert_check_channels),
            detail = stringResource(if (health.channelsOn) R.string.alert_check_channels_ok else R.string.alert_check_channels_off),
            action = stringResource(R.string.alert_open_settings),
        ) { open(context, AlertHealth.notificationSettings(context)) }
    }

    item { SakinahSectionHeader(stringResource(R.string.alert_maker_title)) }
    item {
        Text(
            stringResource(
                when (PhoneMaker.current()) {
                    PhoneMaker.HUAWEI -> R.string.alert_maker_huawei
                    PhoneMaker.SAMSUNG -> R.string.alert_maker_samsung
                    PhoneMaker.XIAOMI -> R.string.alert_maker_xiaomi
                    PhoneMaker.OPPO -> R.string.alert_maker_oppo
                    PhoneMaker.OTHER -> R.string.alert_maker_other
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    item { SakinahSectionHeader(stringResource(R.string.alert_log_title)) }
    item { CalendarHint(stringResource(if (wakes.isEmpty()) R.string.alert_log_empty else R.string.alert_log_intro)) }
    if (wakes.isNotEmpty()) item { WakeRows(wakes) }

    item { SakinahSectionHeader(stringResource(R.string.alert_section_test)) }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onTest,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("alert_test"),
            ) { Text(stringResource(R.string.alert_test)) }
            CalendarHint(stringResource(if (testScheduled) R.string.alert_test_scheduled else R.string.alert_test_hint))
        }
    }
}

/** Asks for notifications, or, once refused, opens the system's own page where only they can be turned on. */
@Composable
internal fun rememberAllowNotifications(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult()
        if (!granted) open(context, AlertHealth.notificationSettings(context))
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            open(context, AlertHealth.notificationSettings(context))
        }
    }
}

@Composable
private fun Check(ok: Boolean, title: String, detail: String, action: String, onFix: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (ok) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                null,
                tint = if (ok) LocalSakinahBrandColors.current.antiqueGold else MaterialTheme.colorScheme.error,
            )
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            if (!ok) TextButton(onClick = onFix) { Text(action) }
        }
    }
}

/** How many of the last wakes the screen lists: about half a day of them. */
internal const val LOG_ROWS = 12

/** The last wakes, each with its day and time, and whether its alarm rang on time, late or never. */
@Composable
private fun WakeRows(wakes: List<WakeRecord>) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            wakes.forEach { wake ->
                val planned = Instant.ofEpochMilli(wake.plannedAt).atZone(ZoneId.systemDefault())
                val weekday = planned.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ar"))
                val state = when {
                    !wake.rang -> stringResource(R.string.alert_log_missed)
                    wake.lateMinutes < 1 -> stringResource(R.string.alert_log_on_time)
                    else -> stringResource(R.string.alert_log_late, text.span(wake.lateMinutes))
                }
                val troubled = !wake.rang || wake.lateMinutes >= 1
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$weekday ${text.time(planned)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        state,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (troubled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun open(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // Some phones hide this screen; the app's own details page always exists.
        context.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
