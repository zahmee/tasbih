package com.sakinah.tasbih.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.TasbihPhrase
import com.sakinah.tasbih.data.TasbihPhraseAnalytics
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.ui.theme.LocalDhikrFontFamily
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TASBIH_COMPLETION_TONE_DURATION_MILLIS = 2_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    state: SakinahUiState,
    isFocusMode: Boolean,
    onFocusModeChange: (Boolean) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onOpenPhraseManager: () -> Unit,
    onSelectPhrase: (String) -> Unit,
    onAddCustomPhrase: (String, Int) -> Unit,
    onUpdatePhrase: (String, String, Int) -> Unit,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<TasbihPhrase?>(null) }
    var showPhraseStatistics by rememberSaveable { mutableStateOf(false) }
    var showCompletedCycle by remember { mutableStateOf(false) }
    var completionJob by remember { mutableStateOf<Job?>(null) }
    val haptics = LocalHapticFeedback.current
    val completionScope = rememberCoroutineScope()
    val completionTone = rememberCompletionSoundPlayer()
    var previousCount by remember(state.selectedPhrase.id, state.tasbihTarget) {
        mutableIntStateOf(state.tasbihCount)
    }
    var lastResetMilestone by remember(state.selectedPhrase.id, state.tasbihTarget) {
        mutableIntStateOf(latestTasbihMilestone(state.tasbihCount, state.tasbihTarget))
    }
    val exactUnacknowledgedMilestone = state.tasbihTarget > 0 &&
        state.tasbihCount > 0 &&
        state.tasbihCount % state.tasbihTarget == 0 &&
        state.tasbihCount > lastResetMilestone
    val displayCompletedCycle = showCompletedCycle || exactUnacknowledgedMilestone
    val displayedProgress = tasbihCycleProgress(
        count = state.tasbihCount,
        target = state.tasbihTarget,
        showCompletedCycle = displayCompletedCycle,
    )

    fun count() {
        if (state.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onIncrement()
    }

    val currentCountAction by rememberUpdatedState { count() }

    BackHandler(enabled = isFocusMode) {
        onFocusModeChange(false)
    }

    DisposableEffect(completionTone) {
        onDispose {
            completionJob?.cancel()
            completionTone.stop()
        }
    }

    LaunchedEffect(state.selectedPhrase.id, state.tasbihTarget) {
        completionJob?.cancel()
        completionTone.stop()
        showCompletedCycle = false
        previousCount = state.tasbihCount
        lastResetMilestone = latestTasbihMilestone(state.tasbihCount, state.tasbihTarget)
    }

    LaunchedEffect(state.selectedPhrase.id, state.tasbihTarget, state.tasbihCount) {
        val currentCount = state.tasbihCount
        val countBeforeUpdate = previousCount
        previousCount = currentCount

        if (currentCount < countBeforeUpdate || state.tasbihTarget <= 0) {
            completionJob?.cancel()
            completionTone.stop()
            showCompletedCycle = false
            lastResetMilestone = latestTasbihMilestone(currentCount, state.tasbihTarget)
            return@LaunchedEffect
        }

        if (hasCrossedTasbihCycle(countBeforeUpdate, currentCount, state.tasbihTarget)) {
            val completedMilestone = latestTasbihMilestone(currentCount, state.tasbihTarget)
            completionJob?.cancel()
            completionTone.stop()
            showCompletedCycle = true
            if (state.tasbihCompletionSoundEnabled) {
                completionTone.play(state.completionSound, state.completionSoundVolume)
            }
            completionJob = completionScope.launch {
                delay(TASBIH_COMPLETION_TONE_DURATION_MILLIS.toLong())
                completionTone.stop()
                lastResetMilestone = completedMilestone
                showCompletedCycle = false
            }
        }
    }

    SakinahScreenBackground(showOrnament = !isFocusMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight && maxHeight < 500.dp
            val textMaxHeight = if (landscape) (maxHeight - 132.dp).coerceAtLeast(64.dp)
                else (maxHeight * if (isFocusMode) 0.38f else 0.26f).coerceAtLeast(64.dp)
            // Keep the convenient background gesture without exposing a duplicate
            // accessibility action; the dial below is the single announced counter.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("tasbih_count_surface")
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { currentCountAction() })
                    },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SakinahContentMaxWidth)
                    .fillMaxSize()
                    .testTag(if (isFocusMode) "tasbih_focus_screen" else "tasbih_standard_screen")
                    .padding(
                        horizontal = if (isFocusMode) 16.dp else 20.dp,
                        vertical = if (isFocusMode) 8.dp else 16.dp,
                    ),
            ) {
                if (!isFocusMode) {
                    TasbihTopBar(
                        count = state.tasbihCount, onUndo = onDecrement,
                        onReset = { showResetConfirmation = true },
                        onOpenStatistics = { showPhraseStatistics = true },
                        onEdit = {
                            editingPhrase = state.selectedPhrase.copy(defaultGoal = state.tasbihTarget)
                            showEditor = true
                        },
                        onManagePhrases = onOpenPhraseManager,
                        onAdd = {
                            editingPhrase = null
                            showEditor = true
                        },
                        onEnterFocusMode = { onFocusModeChange(true) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val phraseContent: @Composable () -> Unit = {
                    if (isFocusMode) FocusPhraseHeader(state, textMaxHeight, onSelectPhrase) { onFocusModeChange(false) }
                    else PhraseCarousel(state, textMaxHeight, onSelectPhrase)
                }
                val counterContent: @Composable (Modifier) -> Unit = { modifier ->
                    CounterSection(state, displayedProgress, displayCompletedCycle, isFocusMode, ::count, modifier)
                }
                if (landscape) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(SakinahSpacing.Large)) {
                        Column(Modifier.weight(1.2f)) { phraseContent() }
                        counterContent(Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    phraseContent()
                    Spacer(Modifier.height(8.dp))
                    counterContent(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.reset_counter_title)) },
            text = { Text(stringResource(R.string.reset_counter_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetConfirmation = false
                    },
                ) {
                    Text(stringResource(R.string.confirm_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showEditor) {
        PhraseEditorDialog(
            phrase = editingPhrase,
            onDismiss = { showEditor = false },
            onSave = { text, goal ->
                val phrase = editingPhrase
                if (phrase == null) onAddCustomPhrase(text, goal)
                else onUpdatePhrase(phrase.id, text, goal)
                showEditor = false
            },
            onDelete = null,
        )
    }

    if (showPhraseStatistics) {
        TasbihPhraseStatisticsSheet(
            phrase = state.selectedPhrase,
            analytics = state.selectedTasbihPhraseAnalytics,
            showDiacritics = state.showDiacritics,
            onDismiss = { showPhraseStatistics = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihPhraseManagerScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
    onSelectPhrase: (String) -> Unit,
    onAddCustomPhrase: (String, Int) -> Unit,
    onUpdatePhrase: (String, String, Int) -> Unit,
    onMovePhrase: (String, Int) -> Unit,
    onDeletePhrase: (String) -> Unit,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<TasbihPhrase?>(null) }
    var phrasePendingDeletion by remember { mutableStateOf<TasbihPhrase?>(null) }
    var reorderingPhraseId by rememberSaveable { mutableStateOf<String?>(null) }
    var revealPhraseId by rememberSaveable { mutableStateOf<String?>(null) }
    var revealIndex by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val canDeletePhrase = state.tasbihPhrases.size > 1

    LaunchedEffect(state.tasbihPhrases, revealPhraseId, revealIndex) {
        val id = revealPhraseId ?: return@LaunchedEffect
        if (state.tasbihPhrases.getOrNull(revealIndex)?.id == id) {
            listState.animateScrollToItem(revealIndex + 1) // The introduction precedes the phrase rows.
            revealPhraseId = null
        } else if (state.tasbihPhrases.none { it.id == id }) {
            revealPhraseId = null
        }
    }

    SakinahScreenBackground {
        Scaffold(
            modifier = Modifier.testTag("tasbih_phrase_manager_screen"),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    title = { Text(stringResource(R.string.manage_tasbih_phrases)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                editingPhrase = null
                                showEditor = true
                            },
                            modifier = Modifier.testTag("tasbih_manager_add"),
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.add_dhikr),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()).imePadding()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SakinahContentMaxWidth)
                        .fillMaxSize()
                        .testTag("tasbih_phrase_manager_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 8.dp,
                        end = 20.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { TasbihPhraseManagerIntro() }
                    itemsIndexed(
                        items = state.tasbihPhrases,
                        key = { _, phrase -> phrase.id },
                    ) { index, phrase ->
                        TasbihPhraseTableRow(
                            phrase = phrase,
                            showDiacritics = state.showDiacritics,
                            selected = phrase.id == state.selectedPhrase.id,
                            canDelete = canDeletePhrase,
                            position = index + 1,
                            total = state.tasbihPhrases.size,
                            isReordering = reorderingPhraseId == phrase.id,
                            onSelect = { onSelectPhrase(phrase.id) },
                            onEdit = {
                                editingPhrase = phrase
                                showEditor = true
                            },
                            onDelete = { phrasePendingDeletion = phrase },
                            onBeginReorder = { reorderingPhraseId = phrase.id },
                            onCancelReorder = { reorderingPhraseId = null },
                            onSaveOrder = { position ->
                                revealPhraseId = phrase.id
                                revealIndex = position - 1
                                onMovePhrase(phrase.id, position - 1)
                                reorderingPhraseId = null
                            },
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        key(editingPhrase?.id ?: "new_phrase") {
            PhraseEditorDialog(
                phrase = editingPhrase,
                onDismiss = { showEditor = false },
                onSave = { text, goal ->
                    val phrase = editingPhrase
                    if (phrase == null) {
                        onAddCustomPhrase(text, goal)
                    } else {
                        onUpdatePhrase(phrase.id, text, goal)
                    }
                    showEditor = false
                },
                onDelete = editingPhrase
                    ?.takeIf(TasbihPhrase::isCustom)
                    ?.let { phrase ->
                        {
                            showEditor = false
                            phrasePendingDeletion = phrase
                        }
                    },
            )
        }
    }

    phrasePendingDeletion?.let { phrase ->
        AlertDialog(
            onDismissRequest = { phrasePendingDeletion = null },
            title = { Text(stringResource(R.string.delete_dhikr_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Small)) {
                    Text(stringResource(R.string.delete_dhikr_message))
                    Text(
                        text = displayArabic(phrase.text, state.showDiacritics),
                        style = TextStyle(
                            fontFamily = LocalDhikrFontFamily.current,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePhrase(phrase.id)
                        phrasePendingDeletion = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete_dhikr))
                }
            },
            dismissButton = {
                TextButton(onClick = { phrasePendingDeletion = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TasbihPhraseManagerIntro() {
    Text(stringResource(R.string.phrase_manager_hint), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun TasbihPhraseTableRow(
    phrase: TasbihPhrase, showDiacritics: Boolean, selected: Boolean, canDelete: Boolean,
    position: Int, total: Int, isReordering: Boolean,
    onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onBeginReorder: () -> Unit, onCancelReorder: () -> Unit, onSaveOrder: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val phraseKind = stringResource(if (phrase.isCustom) R.string.tasbih_phrase_personal else R.string.tasbih_phrase_ready)
    val status = if (selected) "$phraseKind • ${stringResource(R.string.tasbih_phrase_selected)}" else phraseKind
    val positionLabel = stringResource(R.string.phrase_order_position, arabicNumber(position), arabicNumber(total))
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("tasbih_phrase_row_${phrase.id}")
            .semantics { this.selected = selected }
            .then(if (isReordering) Modifier else Modifier.clickable(onClick = onSelect)),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Small)) {
            Text(displayArabic(phrase.text, showDiacritics), style = TextStyle(
                fontFamily = LocalDhikrFontFamily.current, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 28.sp))
            Text("$status • $positionLabel", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (phrase.defaultGoal == 0) stringResource(R.string.unlimited)
                    else "${stringResource(R.string.goal)}: ${phrase.defaultGoal}",
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                )
                Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag(
                        if (selected) "tasbih_phrase_actions_selected" else "tasbih_phrase_actions_${phrase.id}")) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.manage_phrase_actions))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.change_phrase_order)) }, enabled = total > 1,
                            leadingIcon = { Icon(Icons.Outlined.SwapVert, null) },
                            modifier = Modifier.testTag(if (selected) "tasbih_phrase_reorder_selected" else "tasbih_phrase_reorder_${phrase.id}"),
                            onClick = { expanded = false; onBeginReorder() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit_dhikr)) },
                            modifier = Modifier.testTag(if (selected) "tasbih_phrase_edit_selected" else "tasbih_phrase_edit_${phrase.id}"),
                            onClick = { expanded = false; onEdit() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_dhikr)) }, enabled = canDelete,
                            modifier = Modifier.testTag(if (selected) "tasbih_phrase_delete_selected" else "tasbih_phrase_delete_${phrase.id}"),
                            onClick = { expanded = false; onDelete() })
                    }
                }
            }
            if (isReordering) {
                PhraseOrderEditor(phrase.id, position, total, onCancelReorder, onSaveOrder)
            }
        }
    }
}

@Composable
private fun PhraseOrderEditor(id: String, currentPosition: Int, total: Int, onCancel: () -> Unit, onSave: (Int) -> Unit) {
    var positionText by rememberSaveable(id, currentPosition) { mutableStateOf(arabicNumber(currentPosition)) }
    val position = positionText.toIntOrNull()
    val validPosition = position != null && position in 1..total
    Column(verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Small), modifier = Modifier.testTag("phrase_order_editor")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { positionText = arabicNumber(requireNotNull(position) - 1) },
                enabled = validPosition && requireNotNull(position) > 1, modifier = Modifier.testTag("phrase_order_up")) {
                Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.phrase_order_up))
            }
            OutlinedTextField(
                value = positionText, onValueChange = { positionText = it },
                modifier = Modifier.weight(1f).testTag("phrase_order_input"),
                label = { Text(stringResource(R.string.phrase_order_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, isError = !validPosition,
            )
            IconButton(onClick = { positionText = arabicNumber(requireNotNull(position) + 1) },
                enabled = validPosition && requireNotNull(position) < total, modifier = Modifier.testTag("phrase_order_down")) {
                Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.phrase_order_down))
            }
        }
        Text(stringResource(R.string.phrase_order_range, arabicNumber(total)),
            style = MaterialTheme.typography.bodySmall,
            color = if (validPosition) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SakinahSpacing.Small)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { onSave(requireNotNull(position)) }, enabled = validPosition,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("phrase_order_save")) {
                Text(stringResource(R.string.save_phrase_order))
            }
        }
    }
}

@Composable
private fun TasbihTopBar(
    count: Int,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onOpenStatistics: () -> Unit,
    onEdit: () -> Unit,
    onManagePhrases: () -> Unit,
    onAdd: () -> Unit,
    onEnterFocusMode: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().testTag("tasbih_top_bar"), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.my_tasbih), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        FocusModeIconButton(false, stringResource(R.string.tasbih_focus_mode), onEnterFocusMode, Modifier.testTag("tasbih_focus_mode"))
        Box {
            IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("tasbih_actions")) {
                Icon(Icons.Outlined.MoreVert, stringResource(R.string.manage_phrase_actions))
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.edit_dhikr)) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    modifier = Modifier.testTag("tasbih_edit"), onClick = { expanded = false; onEdit() })
                DropdownMenuItem(text = { Text(stringResource(R.string.manage_tasbih_phrases)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) },
                    modifier = Modifier.testTag("tasbih_manage_phrases"), onClick = { expanded = false; onManagePhrases() })
                DropdownMenuItem(text = { Text(stringResource(R.string.undo)) }, enabled = count > 0,
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null) },
                    modifier = Modifier.testTag("tasbih_undo"), onClick = { expanded = false; onUndo() })
                DropdownMenuItem(text = { Text(stringResource(R.string.add_dhikr)) },
                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    modifier = Modifier.testTag("tasbih_add"), onClick = { expanded = false; onAdd() })
                DropdownMenuItem(text = { Text(stringResource(R.string.tasbih_phrase_statistics)) },
                    leadingIcon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                    modifier = Modifier.testTag("tasbih_phrase_statistics"), onClick = { expanded = false; onOpenStatistics() })
                DropdownMenuItem(text = { Text(stringResource(R.string.reset)) }, enabled = count > 0,
                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    onClick = { expanded = false; onReset() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasbihPhraseStatisticsSheet(
    phrase: TasbihPhrase,
    analytics: TasbihPhraseAnalytics,
    showDiacritics: Boolean,
    onDismiss: () -> Unit,
) {
    val stats = if (analytics.sourceId == phrase.id) {
        analytics
    } else {
        TasbihPhraseAnalytics(sourceId = phrase.id)
    }
    val today = LocalDate.now()
    val weekStart = today.minusDays(6)
    val monthStart = today.withDayOfMonth(1)
    val weekDays = remember(stats.daily, today) {
        (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            date to stats.countFor(date)
        }
    }
    val todayCount = stats.countFor(today)
    val weekCount = stats.countBetween(weekStart, today)
    val monthCount = stats.countBetween(monthStart, today)
    val bestDay = stats.bestDay()
    val bestDate = remember(bestDay?.dayKey) {
        bestDay?.dayKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("tasbih_phrase_statistics_sheet"),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .testTag("tasbih_phrase_statistics_content"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            Icons.Outlined.BarChart,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tasbih_phrase_statistics),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.tasbih_phrase_statistics_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("tasbih_phrase_statistics_close"),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                }
            }

            item {
                PhraseStatisticsHero(
                    phraseText = displayArabic(phrase.text, showDiacritics),
                    totalCount = stats.totalCount,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PhraseStatisticMetric(
                            modifier = Modifier.weight(1f),
                            value = todayCount,
                            label = stringResource(R.string.tasbih_phrase_today),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                        PhraseStatisticMetric(
                            modifier = Modifier.weight(1f),
                            value = weekCount,
                            label = stringResource(R.string.tasbih_phrase_last_seven_days),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PhraseStatisticMetric(
                            modifier = Modifier.weight(1f),
                            value = monthCount,
                            label = stringResource(R.string.tasbih_phrase_this_month),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                        PhraseStatisticMetric(
                            modifier = Modifier.weight(1f),
                            value = stats.activeDays,
                            label = stringResource(R.string.tasbih_phrase_active_days),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }
            }

            if (stats.totalCount == 0) {
                item { PhraseStatisticsEmptyState() }
            } else {
                item {
                    PhraseBestDayCard(
                        count = bestDay?.count ?: 0,
                        date = bestDate,
                    )
                }
            }

            item {
                SakinahSectionHeader(text = stringResource(R.string.tasbih_phrase_weekly_activity))
            }

            item {
                PhraseWeeklyActivityCard(days = weekDays, today = today)
            }
        }
    }
}

@Composable
private fun PhraseStatisticsHero(
    phraseText: String,
    totalCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(
                text = stringResource(R.string.tasbih_phrase_only),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = phraseText,
                style = TextStyle(
                    fontFamily = LocalDhikrFontFamily.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 32.sp,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = totalCount.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("tasbih_phrase_total_value"),
            )
            Text(
                text = stringResource(R.string.tasbih_phrase_total),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun PhraseStatisticMetric(
    modifier: Modifier,
    value: Int,
    label: String,
    color: Color,
) {
    val contentColor = MaterialTheme.colorScheme.contentColorFor(color)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = contentColor,
        border = sakinahCardBorder(0.12f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.74f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhraseStatisticsEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = sakinahCardBorder(0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SakinahRosette(modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tasbih_phrase_no_activity),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.tasbih_phrase_no_activity_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhraseBestDayCard(
    count: Int,
    date: LocalDate?,
) {
    val arabicLocale = remember { Locale.forLanguageTag("ar") }
    val dateLabel = remember(date) {
        date?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", arabicLocale)).orEmpty()
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = sakinahCardBorder(0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tasbih_phrase_best_day),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PhraseWeeklyActivityCard(
    days: List<Pair<LocalDate, Int>>,
    today: LocalDate,
) {
    val maxValue = days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val arabicLocale = remember { Locale.forLanguageTag("ar") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = sakinahCardBorder(0.14f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEach { (date, count) ->
                PhraseDayBar(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    date = date,
                    count = count,
                    maxValue = maxValue,
                    isToday = date == today,
                    arabicLocale = arabicLocale,
                )
            }
        }
    }
}

@Composable
private fun PhraseDayBar(
    modifier: Modifier,
    date: LocalDate,
    count: Int,
    maxValue: Int,
    isToday: Boolean,
    arabicLocale: Locale,
) {
    val targetRatio = count.toFloat() / maxValue.coerceAtLeast(1)
    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "tasbih phrase day ${date.dayOfYear}",
    )
    val accent = if (isToday) {
        LocalSakinahBrandColors.current.antiqueGold
    } else {
        MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = if (count > 0) count.toString() else "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height((8f + 64f * animatedRatio).dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (isToday) 1f else 0.78f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EE", arabicLocale)).take(1),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PhraseCarousel(
    state: SakinahUiState,
    textMaxHeight: Dp,
    onSelectPhrase: (String) -> Unit,
) {
    val textScroll = key(state.selectedPhrase.id) { rememberScrollState() }
    val phrases = state.tasbihPhrases
    val currentIndex = phrases.indexOfFirst { it.id == state.selectedPhrase.id }.coerceAtLeast(0)
    val position = stringResource(
        R.string.tasbih_phrase_position,
        currentIndex + 1,
        phrases.size.coerceAtLeast(1),
    )
    val goalLabel = if (state.tasbihTarget == 0) {
        stringResource(R.string.unlimited)
    } else {
        "${stringResource(R.string.goal)} ${state.tasbihTarget}"
    }

    fun moveBy(change: Int) {
        if (phrases.isEmpty()) return
        val nextIndex = (currentIndex + change).coerceIn(0, phrases.lastIndex)
        if (nextIndex == currentIndex) return
        onSelectPhrase(phrases[nextIndex].id)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasbih_phrase_card")
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = currentIndex > 0,
                onClick = { moveBy(-1) },
                modifier = Modifier.testTag("tasbih_previous_phrase"),
            ) {
                CarouselArrow(
                    pointsRight = true,
                    contentDescription = stringResource(R.string.previous_dhikr),
                )
            }
            Text(
                text = stringResource(R.string.tasbih_phrase_summary, position, goalLabel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = currentIndex < phrases.lastIndex,
                onClick = { moveBy(1) },
                modifier = Modifier.testTag("tasbih_next_phrase"),
            ) {
                CarouselArrow(
                    pointsRight = false,
                    contentDescription = stringResource(R.string.next_tasbih_dhikr),
                )
            }
        }
        Text(
            text = displayArabic(state.selectedPhrase.text, state.showDiacritics),
            style = TextStyle(
                fontFamily = LocalDhikrFontFamily.current,
                fontWeight = FontWeight.Bold,
                fontSize = (22f * state.tasbihTextScale).sp,
                lineHeight = (32f * state.tasbihTextScale).sp,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = textMaxHeight)
                .verticalScroll(textScroll)
                .padding(horizontal = 2.dp, vertical = 6.dp)
                .testTag("tasbih_phrase_text"),
        )
        if (textScroll.maxValue > 0) Text(stringResource(R.string.scroll_dhikr_hint),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun FocusPhraseHeader(
    state: SakinahUiState,
    textMaxHeight: Dp,
    onSelectPhrase: (String) -> Unit,
    onExitFocusMode: () -> Unit,
) {
    val textScroll = key(state.selectedPhrase.id) { rememberScrollState() }
    val phrases = state.tasbihPhrases
    val currentIndex = phrases.indexOfFirst { it.id == state.selectedPhrase.id }.coerceAtLeast(0)

    fun moveBy(change: Int) {
        if (phrases.isEmpty()) return
        val nextIndex = (currentIndex + change).coerceIn(0, phrases.lastIndex)
        if (nextIndex != currentIndex) onSelectPhrase(phrases[nextIndex].id)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasbih_focus_phrase_header"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusNavigationBar(
            canGoPrevious = currentIndex > 0,
            canGoNext = currentIndex < phrases.lastIndex,
            onPrevious = { moveBy(-1) },
            onNext = { moveBy(1) },
            onExit = onExitFocusMode,
            previousTag = "tasbih_previous_phrase",
            nextTag = "tasbih_next_phrase",
            exitTag = "tasbih_exit_focus_mode",
        )
        Text(
            text = displayArabic(state.selectedPhrase.text, state.showDiacritics),
            style = TextStyle(
                fontFamily = LocalDhikrFontFamily.current,
                fontWeight = FontWeight.Bold,
                fontSize = (24f * state.tasbihTextScale).sp,
                lineHeight = (36f * state.tasbihTextScale).sp,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = textMaxHeight)
                .verticalScroll(textScroll)
                .padding(vertical = 6.dp)
                .testTag("tasbih_phrase_text"),
        )
        if (textScroll.maxValue > 0) Text(stringResource(R.string.scroll_dhikr_hint),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)

    }
}

@Composable
internal fun CarouselArrow(
    pointsRight: Boolean,
    contentDescription: String,
) {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(31.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val startX = if (pointsRight) size.width * 0.37f else size.width * 0.63f
        val endX = if (pointsRight) size.width * 0.67f else size.width * 0.33f
        val path = Path().apply {
            moveTo(startX, size.height * 0.24f)
            lineTo(endX, size.height * 0.5f)
            lineTo(startX, size.height * 0.76f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun CounterSection(
    state: SakinahUiState,
    displayedProgress: Float,
    showCompletionFeedback: Boolean,
    isFocusMode: Boolean,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxHeight < 260.dp || (LocalDensity.current.fontScale > 1.2f && maxHeight < 400.dp)) {
            val cycleCount = tasbihCycleCount(state.tasbihCount, state.tasbihTarget, showCompletionFeedback)
            val cycleLabel = if (state.tasbihTarget == 0) stringResource(R.string.unlimited)
                else stringResource(R.string.tasbih_cycle_count, cycleCount, state.tasbihTarget)
            val spoken = stringResource(if (state.dailyReset.tasbihEnabled && state.dailyReset.minuteOfDay == 0) R.string.tasbih_today_count else R.string.tasbih_current_count, state.tasbihCount) + "، " + cycleLabel
            val label = stringResource(R.string.tasbih_button)
            Surface(Modifier.align(Alignment.Center).fillMaxWidth().testTag("tasbih_counter")
                .clip(MaterialTheme.shapes.large).clickable(role = Role.Button, onClick = onIncrement)
                .semantics { contentDescription = label; stateDescription = spoken },
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.tasbihCount.toString(), style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.testTag("tasbih_total_count"))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(if (state.dailyReset.tasbihEnabled && state.dailyReset.minuteOfDay == 0) R.string.tasbih_daily_total_label else R.string.tasbih_current_total_label), style = MaterialTheme.typography.labelLarge)
                            Text(cycleLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tasbih_cycle_count"))
                        }
                    }
                    ProgressLine(displayedProgress, height = 4.dp)
                }
            }
            return@BoxWithConstraints
        }
        val feedbackSpace = if (isFocusMode) 0.dp else 52.dp
        val availableHeight = (maxHeight - feedbackSpace).coerceAtLeast(80.dp)
        val maximumDialSize = if (isFocusMode) 360.dp else 286.dp
        val dialSize = minOf(maxWidth, availableHeight, maximumDialSize)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CounterDial(
                state = state,
                displayedProgress = displayedProgress,
                showCompletedCycle = showCompletionFeedback,
                onIncrement = onIncrement,
                diameter = dialSize,
            )
            if (!isFocusMode) {
                Spacer(Modifier.height(10.dp))
                if (showCompletionFeedback) {
                    Surface(
                        modifier = Modifier.testTag("tasbih_completion_feedback"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.completed),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.tap_anywhere_to_count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterDial(
    state: SakinahUiState,
    displayedProgress: Float,
    showCompletedCycle: Boolean,
    onIncrement: () -> Unit,
    diameter: androidx.compose.ui.unit.Dp,
) {
    val tapPulse = remember { Animatable(0f) }
    val feedbackScope = rememberCoroutineScope()
    val animatedProgress by animateFloatAsState(
        targetValue = displayedProgress,
        label = "tasbih progress",
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val progressColor = MaterialTheme.colorScheme.primary
    val cycleCount = tasbihCycleCount(
        count = state.tasbihCount,
        target = state.tasbihTarget,
        showCompletedCycle = showCompletedCycle,
    )
    val targetLabel = if (state.tasbihTarget == 0) {
        stringResource(R.string.unlimited)
    } else {
        stringResource(R.string.tasbih_cycle_count, cycleCount, state.tasbihTarget)
    }
    val counterStateDescription = if (state.tasbihTarget == 0) {
        stringResource(R.string.counter_free_value, state.tasbihCount)
    } else {
        stringResource(if (state.dailyReset.tasbihEnabled && state.dailyReset.minuteOfDay == 0) R.string.tasbih_today_count else R.string.tasbih_current_count, state.tasbihCount) + "، " + targetLabel
    }
    val buttonLabel = stringResource(R.string.tasbih_button)
    val tapPulseColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = if (state.tasbihTarget == 0) 360f else 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                style = Stroke(
                    width = if (state.tasbihTarget == 0) 4.dp.toPx() else stroke,
                    cap = StrokeCap.Round,
                ),
                alpha = if (state.tasbihTarget == 0) 0.42f else 1f,
            )
        }

        Surface(
            modifier = Modifier
                .size(diameter * 0.82f)
                .testTag("tasbih_counter")
                .semantics {
                    contentDescription = buttonLabel
                    stateDescription = counterStateDescription
                    progressBarRangeInfo = if (state.tasbihTarget == 0) {
                        ProgressBarRangeInfo.Indeterminate
                    } else {
                        ProgressBarRangeInfo(
                            current = cycleCount.toFloat(),
                            range = 0f..state.tasbihTarget.toFloat().coerceAtLeast(1f),
                        )
                    }
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        feedbackScope.launch {
                            tapPulse.snapTo(1f)
                            tapPulse.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 360,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        }
                        onIncrement()
                    },
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = tapPulseColor,
                        alpha = tapPulse.value * 0.16f,
                    )
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(if (state.dailyReset.tasbihEnabled && state.dailyReset.minuteOfDay == 0) R.string.tasbih_daily_total_label else R.string.tasbih_current_total_label), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = state.tasbihCount.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = (diameter.value * 0.23f).sp,
                            lineHeight = (diameter.value * 0.26f).sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("tasbih_total_count"),
                    )
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        modifier = Modifier.testTag("tasbih_cycle_count"),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhraseEditorDialog(
    phrase: TasbihPhrase?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit, onDelete: (() -> Unit)?,
) {
    var text by rememberSaveable(phrase?.id) { mutableStateOf(phrase?.text.orEmpty()) }
    var goalText by rememberSaveable(phrase?.id) { mutableStateOf((phrase?.defaultGoal ?: 33).toString()) }
    var textTouched by rememberSaveable { mutableStateOf(false) }
    val goal = goalText.toIntOrNull()
    val invalidGoal = goal == null || goal !in 0..9_999
    // Imported book entries can exceed the custom-entry limit. Preserve their full text when editing the goal.
    val textLimit = maxOf(500, phrase?.text?.length ?: 0)
    val invalidText = text.isBlank() || text.length > textLimit
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().imePadding().testTag("tasbih_phrase_editor"), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                BoxWithConstraints(Modifier.widthIn(max = SakinahContentMaxWidth).fillMaxSize()) {
                    // Keep the form in the same composition slot when the keyboard opens, so focus survives.
                    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE && maxWidth > 480.dp
                    val form: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier.verticalScroll(rememberScrollState()).testTag("phrase_editor_form"),
                        verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Medium)) {
                        if (compact) Text(stringResource(if (phrase == null) R.string.new_dhikr else R.string.edit_dhikr),
                            style = MaterialTheme.typography.titleMedium)
                        if (phrase != null && !phrase.isCustom) {
                            Text(stringResource(R.string.edit_built_in_dhikr_note), style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 12.dp))
                        }
                        OutlinedTextField(
                            value = text, onValueChange = { text = it; textTouched = true },
                            modifier = Modifier.fillMaxWidth().testTag("custom_text"),
                            label = { Text(stringResource(R.string.dhikr_text)) },
                            isError = invalidText && (textTouched || text.isNotEmpty()),
                            supportingText = {
                                Column {
                                    Text(stringResource(R.string.phrase_character_count, text.length, textLimit), modifier = Modifier.testTag("phrase_character_count"))
                                    if (text.length > textLimit) Text(stringResource(R.string.phrase_too_long, textLimit))
                                    else if (textTouched && text.isBlank()) Text(stringResource(R.string.phrase_empty_error))
                                }
                            },
                            textStyle = TextStyle(fontFamily = LocalDhikrFontFamily.current,
                                fontSize = if (compact) 18.sp else 22.sp, lineHeight = if (compact) 26.sp else 34.sp),
                            minLines = if (compact) 1 else 3, maxLines = if (compact) 2 else 6,
                        )
                        OutlinedTextField(
                            value = goalText, onValueChange = { goalText = it }, modifier = Modifier.fillMaxWidth().testTag("custom_goal"),
                            label = { Text(stringResource(R.string.default_goal)) }, isError = invalidGoal,
                            supportingText = { Text(stringResource(if (invalidGoal) R.string.goal_invalid_error else R.string.goal_optional_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        )
                        if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text(stringResource(R.string.delete_custom_dhikr))
                        }
                    }
                    }
                    val actions: @Composable (Modifier) -> Unit = { modifier ->
                    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SakinahSpacing.Medium)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onSave(text.trim(), requireNotNull(goal)) }, enabled = !invalidText && !invalidGoal,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("custom_save")) { Text(stringResource(R.string.save)) }
                    }
                    }
                    if (compact) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            form(Modifier.weight(1f).fillMaxHeight())
                            actions(Modifier.width(176.dp))
                        }
                    } else {
                        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text(stringResource(if (phrase == null) R.string.new_dhikr else R.string.edit_dhikr), style = MaterialTheme.typography.titleLarge)
                            form(Modifier.weight(1f))
                            actions(Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
