package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.Occasions
import com.sakinah.tasbih.calendar.PersonalOccasion
import com.sakinah.tasbih.calendar.PersonalOccasions
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The occasions of the coming year, soonest first, each with how many days are left. The
 * user's own occasions come first, each on a Hijri day that returns every year.
 */
@Composable
fun OccasionsScreen(
    text: CalendarText,
    today: LocalDate,
    onShowDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
    onAddPersonal: (name: String, month: Int, day: Int) -> Unit = { _, _, _ -> },
    onRemovePersonal: (id: String) -> Unit = {},
) {
    val calendar = text.calendar
    val upcoming = remember(text, today) { Occasions.upcoming(calendar, today, text.settings.showOtherOccasions) }
    val personal = remember(text, today) { PersonalOccasions.upcoming(calendar, today, text.settings.personalOccasions) }
    val nextWhiteDay = remember(text, today) {
        if (!text.settings.showWhiteDays) {
            null
        } else {
            generateSequence(today) { it.plusDays(1) }
                .take(DAYS_TO_SEARCH)
                .firstOrNull { day -> calendar.fromGregorian(day)?.let(Occasions::isWhiteDay) == true }
        }
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<PersonalOccasion?>(null) }

    if (adding) {
        val start = calendar.fromGregorian(today)
        PersonalDialog(
            text = text,
            startMonth = start?.month ?: 1,
            startDay = start?.day ?: 1,
            onDismiss = { adding = false },
            onSave = { name, month, day ->
                onAddPersonal(name, month, day)
                adding = false
            },
        )
    }
    removing?.let { occasion ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.cal_personal_remove_title, occasion.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onRemovePersonal(occasion.id)
                    removing = null
                }) { Text(stringResource(R.string.cal_personal_remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    CalendarPage(title = stringResource(R.string.cal_occasions), tag = "calendar_occasions", onBack = onBack) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "personal-header") {
                SectionTitle(stringResource(R.string.cal_personal_title)) {
                    TextButton(onClick = { adding = true }, modifier = Modifier.testTag("calendar_add_personal")) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Text(stringResource(R.string.cal_personal_add), Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (personal.isEmpty()) {
                item(key = "personal-empty") {
                    Text(
                        stringResource(R.string.cal_personal_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            items(personal, key = { "personal-${it.occasion.id}" }) { item ->
                OccasionRow(
                    icon = Icons.Outlined.EventNote,
                    name = item.occasion.name,
                    dates = listOf(text.hijriDate(item.date), text.gregorianDate(item.date)),
                    countdown = text.countdown(item.daysAway),
                    onClick = { onShowDate(item.date) },
                    trailing = {
                        IconButton(onClick = { removing = item.occasion }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.cal_personal_remove_title, item.occasion.name))
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            item(key = "public-header") { SectionTitle(stringResource(R.string.cal_public_title)) }
            items(upcoming, key = { it.occasion.name }) { item ->
                OccasionRow(
                    icon = Icons.Outlined.Celebration,
                    name = text.occasionName(item.occasion),
                    dates = listOf(text.hijriDate(item.date), text.gregorianDate(item.date)),
                    countdown = text.countdown(item.daysAway),
                    onClick = { onShowDate(item.date) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            if (nextWhiteDay != null) {
                item(key = "white-days") {
                    OccasionRow(
                        icon = Icons.Outlined.NightsStay,
                        name = stringResource(R.string.cal_next_white_days),
                        dates = listOf(text.hijriDate(nextWhiteDay), text.gregorianDate(nextWhiteDay)),
                        countdown = text.countdown(ChronoUnit.DAYS.between(today, nextWhiteDay)),
                        onClick = { onShowDate(nextWhiteDay) },
                    )
                }
            }
            item(key = "note") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = sakinahCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cal_occasions_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, action: @Composable () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action()
    }
}

/** A name, then the Hijri month and day by steps, so no keyboard is needed beyond the name. */
@Composable
private fun PersonalDialog(
    text: CalendarText,
    startMonth: Int,
    startDay: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableIntStateOf(startMonth) }
    var day by rememberSaveable { mutableIntStateOf(startDay) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cal_personal_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(PersonalOccasions.MAX_NAME) },
                    label = { Text(stringResource(R.string.cal_personal_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("calendar_personal_name"),
                )
                Stepper(
                    label = stringResource(R.string.cal_personal_month),
                    value = text.hijriMonthName(month),
                    onLess = { month = if (month == 1) 12 else month - 1 },
                    onMore = { month = if (month == 12) 1 else month + 1 },
                )
                Stepper(
                    label = stringResource(R.string.cal_personal_day),
                    value = text.number(day),
                    onLess = { day = if (day == 1) 30 else day - 1 },
                    onMore = { day = if (day == 30) 1 else day + 1 },
                )
                if (day == 30) {
                    Text(
                        stringResource(R.string.cal_personal_thirtieth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), month, day) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Stepper(label: String, value: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onLess) { Icon(Icons.Outlined.Remove, stringResource(R.string.cal_personal_less, label)) }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(112.dp),
        )
        IconButton(onClick = onMore) { Icon(Icons.Outlined.Add, stringResource(R.string.cal_personal_more, label)) }
    }
}

@Composable
private fun OccasionRow(
    icon: ImageVector,
    name: String,
    dates: List<String>,
    countdown: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(vertical = 12.dp)
            .testTag("calendar_occasion_row"),
    ) {
        Icon(icon, null, tint = LocalSakinahBrandColors.current.antiqueGold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            dates.filter { it.isNotEmpty() }.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(countdown, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        trailing?.invoke()
    }
}

/** A white day falls in every month but Ramadan, so ten weeks are always enough to find one. */
private const val DAYS_TO_SEARCH = 70
