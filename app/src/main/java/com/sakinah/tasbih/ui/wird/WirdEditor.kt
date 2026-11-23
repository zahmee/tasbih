package com.sakinah.tasbih.ui.wird

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.prayer.AlertPrayers
import com.sakinah.tasbih.ui.calendar.CalendarChoices
import com.sakinah.tasbih.ui.prayer.nameRes
import com.sakinah.tasbih.wird.WirdRepeat
import com.sakinah.tasbih.wird.WirdTask
import com.sakinah.tasbih.wird.WirdText
import com.sakinah.tasbih.wird.WirdTimeKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs

/** Where a task sits by its prayer. */
private enum class Side { BEFORE, AT, AFTER }

/**
 * A task's name, how often it comes, when in its day, and whether it reminds. A task of adhkar
 * says that reading them ticks it. Every choice has a sensible start, so a name is enough.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun WirdEditorSheet(
    initial: WirdTask,
    today: LocalDate,
    hasPlace: Boolean,
    text: WirdText,
    onSave: (WirdTask) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val key = initial.id
    var title by rememberSaveable(key) { mutableStateOf(initial.title) }
    var repeat by rememberSaveable(key) { mutableStateOf(initial.repeat) }
    var weekdays by rememberSaveable(key) { mutableStateOf(initial.weekdays.ifEmpty { setOf(today.dayOfWeek) }.map { it.value }.toSet()) }
    var hijriDays by rememberSaveable(key) { mutableStateOf(initial.hijriDays.ifEmpty { setOf(13, 14, 15) }) }
    var date by rememberSaveable(key) { mutableStateOf((initial.date ?: today).toEpochDay()) }
    var timeKind by rememberSaveable(key) { mutableStateOf(initial.timeKind) }
    var minute by rememberSaveable(key) { mutableStateOf(if (initial.timeKind == WirdTimeKind.CLOCK) initial.minuteOfDay else 21 * 60) }
    var prayer by rememberSaveable(key) { mutableStateOf(initial.prayer ?: com.sakinah.tasbih.prayer.Prayer.FAJR) }
    var side by rememberSaveable(key) {
        mutableStateOf(when { initial.offsetMinutes < 0 -> Side.BEFORE; initial.offsetMinutes > 0 -> Side.AFTER; else -> if (initial.id == 0L) Side.AFTER else Side.AT })
    }
    var offset by rememberSaveable(key) { mutableStateOf(abs(initial.offsetMinutes).takeIf { it > 0 } ?: 15) }
    var remind by rememberSaveable(key) { mutableStateOf(initial.remind) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val hasTime = timeKind != WirdTimeKind.NONE
    val valid = title.isNotBlank() && when (repeat) {
        WirdRepeat.WEEKLY -> weekdays.isNotEmpty()
        WirdRepeat.HIJRI_MONTHLY -> hijriDays.isNotEmpty()
        else -> true
    }
    fun result() = initial.copy(
        title = title.trim().take(WirdTask.MAX_TITLE),
        repeat = repeat,
        weekdays = if (repeat == WirdRepeat.WEEKLY) weekdays.map(DayOfWeek::of).toSet() else emptySet(),
        hijriDays = if (repeat == WirdRepeat.HIJRI_MONTHLY) hijriDays else emptySet(),
        date = if (repeat == WirdRepeat.ONCE) LocalDate.ofEpochDay(date) else null,
        timeKind = timeKind,
        minuteOfDay = if (timeKind == WirdTimeKind.CLOCK) minute else 0,
        prayer = if (timeKind == WirdTimeKind.PRAYER) prayer else null,
        offsetMinutes = if (timeKind == WirdTimeKind.PRAYER) when (side) { Side.BEFORE -> -offset; Side.AT -> 0; Side.AFTER -> offset } else 0,
        remind = remind && hasTime,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .testTag("wird_editor"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (initial.id == 0L) R.string.wird_new else R.string.wird_edit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(WirdTask.MAX_TITLE) },
                label = { Text(stringResource(R.string.wird_name)) },
                placeholder = { Text(stringResource(R.string.wird_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("wird_title"),
            )
            if (initial.collectionId != null) {
                Text(stringResource(R.string.wird_linked_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Label(stringResource(R.string.wird_repeat))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("wird_repeat")) {
                WirdRepeat.entries.forEach { choice ->
                    FilterChip(
                        selected = repeat == choice,
                        onClick = { repeat = choice },
                        label = { Text(stringResource(repeatLabel(choice))) },
                        modifier = Modifier.testTag("wird_repeat_${choice.name.lowercase()}"),
                    )
                }
            }
            when (repeat) {
                WirdRepeat.ONCE -> OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("wird_date")) {
                    Icon(Icons.Outlined.CalendarMonth, null)
                    Text(text.day(LocalDate.ofEpochDay(date), today), modifier = Modifier.padding(start = 8.dp))
                }
                WirdRepeat.WEEKLY -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WirdText.WeekOrder.forEach { day ->
                        FilterChip(
                            selected = day.value in weekdays,
                            onClick = { weekdays = if (day.value in weekdays) weekdays - day.value else weekdays + day.value },
                            label = { Text(text.weekday(day)) },
                        )
                    }
                }
                WirdRepeat.HIJRI_MONTHLY -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.wird_hijri_days_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..WirdTask.LAST_HIJRI_DAY).forEach { day ->
                            FilterChip(
                                selected = day in hijriDays,
                                onClick = { hijriDays = if (day in hijriDays) hijriDays - day else hijriDays + day },
                                label = { Text(arabicNumber(day)) },
                            )
                        }
                    }
                }
                WirdRepeat.DAILY -> Unit
            }

            HorizontalDivider()
            Label(stringResource(R.string.wird_time))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("wird_time_kind")) {
                WirdTimeKind.entries.forEach { choice ->
                    FilterChip(
                        selected = timeKind == choice,
                        onClick = { timeKind = choice },
                        label = { Text(stringResource(timeKindLabel(choice))) },
                        modifier = Modifier.testTag("wird_time_${choice.name.lowercase()}"),
                    )
                }
            }
            when (timeKind) {
                WirdTimeKind.CLOCK -> OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("wird_clock")) {
                    Icon(Icons.Outlined.Schedule, null)
                    Text(text.timeRule(WirdTask(title = "", timeKind = WirdTimeKind.CLOCK, minuteOfDay = minute)), modifier = Modifier.padding(start = 8.dp))
                }
                WirdTimeKind.PRAYER -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlertPrayers.forEach { choice ->
                            FilterChip(selected = prayer == choice, onClick = { prayer = choice }, label = { Text(stringResource(choice.nameRes)) })
                        }
                    }
                    CalendarChoices(
                        options = Side.entries,
                        selected = side,
                        label = { stringResource(when (it) { Side.BEFORE -> R.string.wird_side_before; Side.AT -> R.string.wird_side_at; Side.AFTER -> R.string.wird_side_after }) },
                        onSelect = { side = it },
                    )
                    if (side != Side.AT) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WirdTask.OFFSET_CHOICES.filter { it > 0 }.forEach { minutes ->
                                FilterChip(selected = offset == minutes, onClick = { offset = minutes }, label = { Text(offsetLabel(minutes)) })
                            }
                        }
                    }
                    Text(
                        text.timeRule(result()) + if (!hasPlace) " — " + stringResource(R.string.wird_needs_place_short) else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WirdTimeKind.NONE -> Unit
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.wird_remind), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(if (hasTime) R.string.wird_remind_hint else R.string.wird_remind_needs_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = remind && hasTime, onCheckedChange = { remind = it }, enabled = hasTime, modifier = Modifier.testTag("wird_remind"))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { onSave(result()) }, enabled = valid, modifier = Modifier.heightIn(min = 48.dp).testTag("wird_save")) {
                    Text(stringResource(R.string.save))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                if (onDelete != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("wird_delete")) {
                        Text(stringResource(R.string.wird_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Math.floorDiv(it, 86_400_000L) }
                    pickingDate = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(R.string.wird_pick_time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    minute = state.hour * 60 + state.minute
                    pickingTime = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.wird_delete_title, initial.title)) },
            text = { Text(stringResource(R.string.wird_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }, modifier = Modifier.testTag("wird_delete_confirm")) { Text(stringResource(R.string.wird_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
}

private fun repeatLabel(repeat: WirdRepeat): Int = when (repeat) {
    WirdRepeat.ONCE -> R.string.wird_repeat_once
    WirdRepeat.DAILY -> R.string.wird_repeat_daily
    WirdRepeat.WEEKLY -> R.string.wird_repeat_weekly
    WirdRepeat.HIJRI_MONTHLY -> R.string.wird_repeat_hijri
}

private fun timeKindLabel(kind: WirdTimeKind): Int = when (kind) {
    WirdTimeKind.NONE -> R.string.wird_time_kind_none
    WirdTimeKind.CLOCK -> R.string.wird_time_kind_clock
    WirdTimeKind.PRAYER -> R.string.wird_time_kind_prayer
}

/** «٥ د»، «ساعة», short enough for a row of chips. */
private fun offsetLabel(minutes: Int): String = when (minutes) {
    60 -> "ساعة"
    90 -> "ساعة ونصف"
    120 -> "ساعتان"
    else -> "${arabicNumber(minutes)} د"
}
