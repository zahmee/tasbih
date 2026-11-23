package com.sakinah.tasbih.ui.wird

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sakinah.tasbih.R
import com.sakinah.tasbih.alerts.AlertHealth
import com.sakinah.tasbih.alerts.AlertNotifier
import com.sakinah.tasbih.alerts.PrayerAlarms
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.ui.ProgressLine
import com.sakinah.tasbih.ui.SettingsTab
import com.sakinah.tasbih.ui.TabbedSettingsPage
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.wird.WirdDay
import com.sakinah.tasbih.wird.WirdItem
import com.sakinah.tasbih.wird.WirdNotifier
import com.sakinah.tasbih.wird.WirdPreset
import com.sakinah.tasbih.wird.WirdRepeat
import com.sakinah.tasbih.wird.WirdTask
import com.sakinah.tasbih.wird.WirdText
import com.sakinah.tasbih.wird.WirdTimeKind
import java.time.LocalDate

private enum class WirdTab { TODAY, UPCOMING, FINISHED }

/**
 * The wird: the day's acts of worship and the user's own tasks, in three tabs: today's, those to
 * come, and those done with the days just gone by. Asked for on November 23, 2026. Nothing here
 * counts days in a row or marks a day in red: a task not done is said so quietly.
 */
@Composable
fun WirdScreen(
    state: WirdState?,
    viewModel: WirdViewModel,
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onChoosePlace: () -> Unit,
    /** Set when the calendar asked to add a task on its day. */
    addOn: LocalDate? = null,
) {
    val context = LocalContext.current
    val text = remember(context) { WirdText(context) }
    var tab by rememberSaveable { mutableStateOf(WirdTab.TODAY) }
    // The task being edited: a new one, or one of the list's. Kept by id across a rotation.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newOn by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    // Once only: coming back to the page from the adhkar must not open it again.
    var addHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(addOn) {
        if (addOn != null && !addHandled) {
            addHandled = true
            newOn = addOn.toEpochDay()
            creating = true
        }
    }

    var health by remember { mutableStateOf(AlertHealth.read(context, com.sakinah.tasbih.prayer.AlertSettings.DEFAULT_TONE)) }
    var askNotifications by rememberSaveable { mutableStateOf(false) }
    var askExact by rememberSaveable { mutableStateOf(false) }
    var exactAsked by rememberSaveable { mutableStateOf(false) }
    fun askExactIfMissing() {
        if (!exactAsked && !PrayerAlarms.canScheduleExact(context) && AlertHealth.exactAlarmSettings(context) != null) {
            exactAsked = true
            askExact = true
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        health = AlertHealth.read(context, com.sakinah.tasbih.prayer.AlertSettings.DEFAULT_TONE)
        askExactIfMissing()
    }
    LifecycleResumeEffect(Unit) {
        health = AlertHealth.read(context, com.sakinah.tasbih.prayer.AlertSettings.DEFAULT_TONE)
        onPauseOrDispose { }
    }
    fun save(task: WirdTask) {
        viewModel.save(task)
        if (task.reminds) {
            if (!AlertNotifier.canPost(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) askNotifications = true
            else askExactIfMissing()
        }
    }

    if (askNotifications) {
        AlertDialog(
            onDismissRequest = { askNotifications = false },
            title = { Text(stringResource(R.string.alert_permission_title)) },
            text = { Text(stringResource(R.string.wird_permission_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    askNotifications = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.prayer_location_continue)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askNotifications = false
                    askExactIfMissing()
                }) { Text(stringResource(R.string.prayer_not_now)) }
            },
        )
    }
    if (askExact) {
        AlertDialog(
            onDismissRequest = { askExact = false },
            title = { Text(stringResource(R.string.alert_exact_title)) },
            text = { Text(stringResource(R.string.wird_exact_disclosure)) },
            confirmButton = {
                TextButton(onClick = {
                    askExact = false
                    AlertHealth.exactAlarmSettings(context)?.let { runCatching { context.startActivity(it) } }
                }) { Text(stringResource(R.string.prayer_location_continue)) }
            },
            dismissButton = { TextButton(onClick = { askExact = false }) { Text(stringResource(R.string.prayer_not_now)) } },
        )
    }

    val board = state?.board
    val editing = editingId?.let { id -> state?.tasks?.firstOrNull { it.id == id } }
    if (creating || editing != null) {
        val initial = editing ?: WirdTask(
            title = "",
            repeat = if (newOn != null) WirdRepeat.ONCE else WirdRepeat.DAILY,
            date = newOn?.let(LocalDate::ofEpochDay) ?: board?.today,
        )
        WirdEditorSheet(
            initial = initial,
            today = board?.today ?: LocalDate.now(),
            hasPlace = state?.hasPlace ?: false,
            text = text,
            onSave = { task ->
                save(task)
                creating = false
                editingId = null
                newOn = null
            },
            onDelete = editing?.let { task ->
                {
                    viewModel.delete(task)
                    editingId = null
                }
            },
            onDismiss = {
                creating = false
                editingId = null
                newOn = null
            },
        )
    }

    TabbedSettingsPage(
        title = stringResource(R.string.wird_title),
        onBack = onBack,
        tag = "wird",
        tabs = WirdTab.entries.map { SettingsTab(tabLabel(it), tabIcon(it), "wird_tab_${it.name.lowercase()}") },
        selected = tab.ordinal,
        onSelect = { tab = WirdTab.entries[it] },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    newOn = null
                    creating = true
                },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.wird_add)) },
                modifier = Modifier.testTag("wird_add"),
            )
        },
    ) {
        if (state == null || board == null) return@TabbedSettingsPage
        val onEdit: (WirdItem) -> Unit = { editingId = it.task.id }
        when (tab) {
            WirdTab.TODAY -> todayItems(
                state, text, health.notifications && !WirdNotifier.muted(context),
                onToggle = viewModel::setDone, onEdit = onEdit, onRead = onOpenCollection,
                onAddPreset = viewModel::addPreset, onChoosePlace = onChoosePlace,
                onAskNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !AlertNotifier.canPost(context)) askNotifications = true
                    else runCatching { context.startActivity(AlertHealth.notificationSettings(context)) }
                },
            )
            WirdTab.UPCOMING -> upcomingItems(state, text, onEdit)
            WirdTab.FINISHED -> finishedItems(state, text, onToggle = viewModel::setDone, onEdit = onEdit)
        }
    }
}

@Composable
private fun tabLabel(tab: WirdTab): String = stringResource(
    when (tab) {
        WirdTab.TODAY -> R.string.wird_tab_today
        WirdTab.UPCOMING -> R.string.wird_tab_upcoming
        WirdTab.FINISHED -> R.string.wird_tab_finished
    },
)

private fun tabIcon(tab: WirdTab) = when (tab) {
    WirdTab.TODAY -> Icons.Outlined.Today
    WirdTab.UPCOMING -> Icons.Outlined.Schedule
    WirdTab.FINISHED -> Icons.Outlined.History
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.todayItems(
    state: WirdState,
    text: WirdText,
    canNotify: Boolean,
    onToggle: (WirdItem, Boolean) -> Unit,
    onEdit: (WirdItem) -> Unit,
    onRead: (String) -> Unit,
    onAddPreset: (WirdPreset) -> Unit,
    onChoosePlace: () -> Unit,
    onAskNotifications: () -> Unit,
) {
    val board = state.board
    item(key = "summary") {
        Column(Modifier.fillMaxWidth().testTag("wird_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text.date(board.today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (board.totalToday == 0) stringResource(R.string.wird_nothing_today)
                else stringResource(R.string.wird_done_of, arabicNumber(board.doneToday), arabicNumber(board.totalToday)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (board.totalToday > 0) ProgressLine(board.doneToday.toFloat() / board.totalToday, height = 6.dp)
        }
    }
    if (!state.hasPlace && state.tasks.any { it.timeKind == WirdTimeKind.PRAYER }) {
        item(key = "no_place") { Notice(stringResource(R.string.wird_needs_place), stringResource(R.string.prayer_choose_place), onChoosePlace) }
    }
    if (!canNotify && state.tasks.any(WirdTask::reminds)) {
        item(key = "no_notifications") { Notice(stringResource(R.string.wird_needs_notifications), stringResource(R.string.wird_allow), onAskNotifications) }
    }
    board.todayItems.forEach { item ->
        item(key = "today_${item.task.id}_${item.day}") {
            WirdRow(item, text, board.today, onToggle = { onToggle(item, it) }, onEdit = { onEdit(item) }, onRead = item.task.collectionId?.let { id -> { onRead(id) } })
        }
    }
    if (state.presets.isNotEmpty()) {
        item(key = "presets") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.wird_suggestions), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.wird_suggestions_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("wird_presets")) {
                    state.presets.forEach { preset ->
                        SuggestionChip(
                            onClick = { onAddPreset(preset) },
                            label = { Text(stringResource(preset.title)) },
                            icon = { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("wird_preset_${preset.id}"),
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.upcomingItems(state: WirdState, text: WirdText, onEdit: (WirdItem) -> Unit) {
    val board = state.board
    if (board.upcoming.isEmpty()) {
        item(key = "upcoming_empty") { Hint(stringResource(R.string.wird_upcoming_empty)) }
        return
    }
    board.upcoming.groupBy { it.day }.forEach { (day, items) ->
        item(key = "day_$day") {
            Text(
                text.day(day, board.today),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
        }
        items.forEach { item ->
            item(key = "upcoming_${item.task.id}") {
                WirdRow(item, text, board.today, onToggle = null, onEdit = { onEdit(item) }, onRead = null, showRepeat = true)
            }
        }
    }
}

private fun LazyListScope.finishedItems(
    state: WirdState,
    text: WirdText,
    onToggle: (WirdItem, Boolean) -> Unit,
    onEdit: (WirdItem) -> Unit,
) {
    val board = state.board
    if (board.finished.isEmpty() && board.history.isEmpty()) {
        item(key = "finished_empty") { Hint(stringResource(R.string.wird_finished_empty)) }
        return
    }
    if (board.finished.isNotEmpty()) {
        item(key = "finished_title") { SectionTitle(stringResource(R.string.wird_finished_once)) }
        board.finished.forEach { item ->
            item(key = "finished_${item.task.id}") {
                WirdRow(item, text, board.today, onToggle = { onToggle(item, it) }, onEdit = { onEdit(item) }, onRead = null, showRepeat = true)
            }
        }
    }
    if (board.history.isNotEmpty()) {
        item(key = "history_title") { SectionTitle(stringResource(R.string.wird_history)) }
        board.history.forEach { day ->
            item(key = "history_${day.day}") { HistoryDay(day, text, board.today, onToggle, onEdit) }
        }
    }
}

/** A day gone by, folded to its count; opened, each task with its tick, which may still be given. */
@Composable
private fun HistoryDay(day: WirdDay, text: WirdText, today: LocalDate, onToggle: (WirdItem, Boolean) -> Unit, onEdit: (WirdItem) -> Unit) {
    var open by rememberSaveable(day.day) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = { open = !open },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = sakinahCardBorder(),
            modifier = Modifier.fillMaxWidth().testTag("wird_history_day"),
        ) {
            Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.day(day.day, today), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.wird_done_of, arabicNumber(day.doneCount), arabicNumber(day.items.size)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, stringResource(if (open) R.string.wird_fold else R.string.wird_unfold))
            }
        }
        if (open) {
            day.items.forEach { item ->
                WirdRow(item, text, today, onToggle = { onToggle(item, it) }, onEdit = { onEdit(item) }, onRead = null)
            }
        }
    }
}

/**
 * One task on one day: its tick, its name, and when it falls. A tap elsewhere on the row opens
 * it for editing; a task of adhkar not yet read has «read» beside it.
 */
@Composable
private fun WirdRow(
    item: WirdItem,
    text: WirdText,
    today: LocalDate,
    onToggle: ((Boolean) -> Unit)?,
    onEdit: () -> Unit,
    onRead: (() -> Unit)?,
    showRepeat: Boolean = false,
) {
    val task = item.task
    val details = listOfNotNull(
        text.clock(item),
        if (task.timeKind == WirdTimeKind.PRAYER || item.time == null) text.timeRule(task) else null,
        if (showRepeat) text.repeatRule(task, today) else null,
        if (item.late) stringResource(R.string.wird_late_since, text.day(item.day, today)) else null,
        if (item.auto) stringResource(R.string.wird_done_by_reading) else null,
        if (!item.done && onToggle != null && item.day.isBefore(today) && task.recurring) stringResource(R.string.wird_not_done) else null,
    ).distinct().joinToString(" · ")
    val tickLabel = stringResource(if (item.done) R.string.wird_untick else R.string.wird_tick, task.title)
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.medium,
        color = if (item.done) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("wird_item"),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onToggle != null) {
                Checkbox(
                    checked = item.done,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("wird_tick").semantics { contentDescription = tickLabel },
                )
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (task.reminds) {
                Icon(
                    Icons.Outlined.NotificationsActive,
                    stringResource(R.string.wird_reminds),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (onRead != null && !item.done) {
                TextButton(onClick = onRead, modifier = Modifier.heightIn(min = 48.dp).testTag("wird_read")) {
                    Text(stringResource(R.string.wird_read))
                }
            } else if (item.done && onToggle == null) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Notice(message: String, action: String, onAction: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, null, Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
}

@Composable
private fun Hint(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
}
