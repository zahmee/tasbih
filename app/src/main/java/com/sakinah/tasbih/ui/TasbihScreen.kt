package com.sakinah.tasbih.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.TasbihPhrase
import com.sakinah.tasbih.data.TasbihPhraseAnalytics
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.ui.theme.LocalDhikrFontFamily
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    state: SakinahUiState,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onOpenPhraseManager: () -> Unit,
    onSelectPhrase: (String) -> Unit,
    onAddCustomPhrase: (String, Int) -> Unit,
    onUpdatePhrase: (String, String, Int) -> Unit,
    onDeleteCustomPhrase: (String) -> Unit,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var showPhraseStatistics by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<TasbihPhrase?>(null) }
    val haptics = LocalHapticFeedback.current
    fun count() {
        if (state.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onIncrement()
    }

    SakinahScreenBackground {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SakinahContentMaxWidth)
                .fillMaxSize(),
        ) {
            // Keep the convenient background gesture without exposing a duplicate
            // accessibility action; the dial below is the single announced counter.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("tasbih_count_surface")
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { count() })
                    },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                TasbihTopBar(
                    count = state.tasbihCount,
                    onUndo = onDecrement,
                    onReset = { showResetConfirmation = true },
                    onOpenStatistics = { showPhraseStatistics = true },
                    onManagePhrases = onOpenPhraseManager,
                    onAdd = {
                        editingPhrase = null
                        showEditor = true
                    },
                )
                Spacer(Modifier.height(12.dp))
                PhraseCarousel(
                    state = state,
                    onSelectPhrase = onSelectPhrase,
                    onLongPress = {
                        if (state.hapticsEnabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        editingPhrase = state.selectedPhrase
                        showEditor = true
                    },
                )
                Spacer(Modifier.height(8.dp))
                CounterSection(
                    state = state,
                    onIncrement = ::count,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
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
                            onDeleteCustomPhrase(phrase.id)
                            showEditor = false
                        }
                    },
            )
        }
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
    onDeletePhrase: (String) -> Unit,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingPhrase by remember { mutableStateOf<TasbihPhrase?>(null) }
    var phrasePendingDeletion by remember { mutableStateOf<TasbihPhrase?>(null) }
    val canDeletePhrase = state.tasbihPhrases.size > 1

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
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SakinahContentMaxWidth)
                        .fillMaxSize()
                        .testTag("tasbih_phrase_manager_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 20.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { TasbihPhraseManagerIntro() }
                    item { TasbihPhraseTableHeader() }
                    items(
                        items = state.tasbihPhrases,
                        key = TasbihPhrase::id,
                    ) { phrase ->
                        TasbihPhraseTableRow(
                            phrase = phrase,
                            showDiacritics = state.showDiacritics,
                            selected = phrase.id == state.selectedPhrase.id,
                            canDelete = canDeletePhrase,
                            onSelect = { onSelectPhrase(phrase.id) },
                            onEdit = {
                                editingPhrase = phrase
                                showEditor = true
                            },
                            onDelete = { phrasePendingDeletion = phrase },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        border = sakinahCardBorder(0.18f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.manage_tasbih_phrases),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.manage_tasbih_phrases_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun TasbihPhraseTableHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tasbih_table_dhikr),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.tasbih_table_target),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(62.dp),
            )
            Text(
                text = stringResource(R.string.tasbih_table_actions),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(96.dp),
            )
        }
    }
}

@Composable
private fun TasbihPhraseTableRow(
    phrase: TasbihPhrase,
    showDiacritics: Boolean,
    selected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val phraseKind = stringResource(
        if (phrase.isCustom) R.string.tasbih_phrase_personal else R.string.tasbih_phrase_ready,
    )
    val goal = if (phrase.defaultGoal == 0) {
        stringResource(R.string.unlimited)
    } else {
        phrase.defaultGoal.toString()
    }
    val status = if (selected) {
        "$phraseKind • ${stringResource(R.string.tasbih_phrase_selected)}"
    } else {
        phraseKind
    }
    val editTag = if (selected) {
        "tasbih_phrase_edit_selected"
    } else {
        "tasbih_phrase_edit_${phrase.id}"
    }
    val deleteTag = if (selected) {
        "tasbih_phrase_delete_selected"
    } else {
        "tasbih_phrase_delete_${phrase.id}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tasbih_phrase_row_${phrase.id}")
            .semantics { this.selected = selected }
            .clickable(onClick = onSelect),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = sakinahCardBorder(if (selected) 0.38f else 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayArabic(phrase.text, showDiacritics),
                    style = TextStyle(
                        fontFamily = LocalDhikrFontFamily.current,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = goal,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(62.dp),
            )
            Row(
                modifier = Modifier.width(96.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag(editTag),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit_dhikr),
                    )
                }
                IconButton(
                    enabled = canDelete,
                    onClick = onDelete,
                    modifier = Modifier.testTag(deleteTag),
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_dhikr),
                    )
                }
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
    onManagePhrases: () -> Unit,
    onAdd: () -> Unit,
) {
    SakinahScreenHeader(
        title = stringResource(R.string.my_tasbih),
        subtitle = stringResource(R.string.my_tasbih_subtitle),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    enabled = count > 0,
                    onClick = onUndo,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.undo),
                    )
                }
                FilledTonalIconButton(
                    enabled = count > 0,
                    onClick = onReset,
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.reset),
                    )
                }
                FilledTonalIconButton(
                    onClick = onOpenStatistics,
                    modifier = Modifier.testTag("tasbih_phrase_statistics"),
                ) {
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = stringResource(R.string.tasbih_phrase_statistics),
                    )
                }
                FilledTonalIconButton(
                    onClick = onManagePhrases,
                    modifier = Modifier.testTag("tasbih_manage_phrases"),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ListAlt,
                        contentDescription = stringResource(R.string.manage_tasbih_phrases),
                    )
                }
                FilledTonalIconButton(
                    onClick = onAdd,
                    modifier = Modifier.testTag("tasbih_add"),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_dhikr),
                    )
                }
            }
        },
    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseCarousel(
    state: SakinahUiState,
    onSelectPhrase: (String) -> Unit,
    onLongPress: () -> Unit,
) {
    val phrases = state.tasbihPhrases
    val currentIndex = phrases.indexOfFirst { it.id == state.selectedPhrase.id }.coerceAtLeast(0)
    val interactionSource = remember { MutableInteractionSource() }
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
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onLongPress,
                onLongClickLabel = stringResource(R.string.edit_dhikr),
                onLongClick = onLongPress,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = displayArabic(state.selectedPhrase.text, state.showDiacritics),
            style = TextStyle(
                fontFamily = LocalDhikrFontFamily.current,
                fontWeight = FontWeight.Bold,
                fontSize = (22f * state.tasbihTextScale).sp,
                lineHeight = (32f * state.tasbihTextScale).sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp)
                .testTag("tasbih_phrase_text"),
        )
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
            text = stringResource(R.string.tap_to_edit),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CarouselArrow(
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
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val availableHeight = (maxHeight - 52.dp).coerceAtLeast(164.dp)
        val dialSize = minOf(maxWidth, availableHeight, 286.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CounterDial(
                state = state,
                onIncrement = onIncrement,
                diameter = dialSize,
            )
            Spacer(Modifier.height(10.dp))
            if (state.isTasbihGoalComplete) {
                Surface(
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

@Composable
private fun CounterDial(
    state: SakinahUiState,
    onIncrement: () -> Unit,
    diameter: androidx.compose.ui.unit.Dp,
) {
    val tapPulse = remember { Animatable(0f) }
    val feedbackScope = rememberCoroutineScope()
    val animatedProgress by animateFloatAsState(
        targetValue = state.tasbihProgress,
        label = "tasbih progress",
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val progressColor = MaterialTheme.colorScheme.primary
    val targetLabel = if (state.tasbihTarget == 0) {
        stringResource(R.string.unlimited)
    } else {
        stringResource(R.string.counter_value, state.tasbihCount, state.tasbihTarget)
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
                    stateDescription = targetLabel
                    progressBarRangeInfo = if (state.tasbihTarget == 0) {
                        ProgressBarRangeInfo.Indeterminate
                    } else {
                        ProgressBarRangeInfo(
                            current = state.tasbihCount.toFloat()
                                .coerceAtMost(state.tasbihTarget.toFloat()),
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
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.tasbihCount.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = (diameter.value * 0.23f).sp,
                            lineHeight = (diameter.value * 0.26f).sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = targetLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhraseEditorDialog(
    phrase: TasbihPhrase?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var text by rememberSaveable(phrase?.id) { mutableStateOf(phrase?.text.orEmpty()) }
    var goalText by rememberSaveable(phrase?.id) {
        mutableStateOf((phrase?.defaultGoal ?: 33).toString())
    }
    val goal = goalText.toIntOrNull()
    val canSave = text.isNotBlank() && goal != null && goal in 0..9_999

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tasbih_phrase_editor")
                .imePadding(),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 0.dp,
        ) {
            SakinahScreenBackground {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    SakinahScreenHeader(
                        title = stringResource(
                            if (phrase == null) R.string.new_dhikr else R.string.edit_dhikr,
                        ),
                    )
                if (phrase != null && !phrase.isCustom) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.edit_built_in_dhikr_note),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("custom_text"),
                    label = { Text(stringResource(R.string.dhikr_text)) },
                    placeholder = { Text(stringResource(R.string.dhikr_text_hint)) },
                    textStyle = TextStyle(
                        fontFamily = LocalDhikrFontFamily.current,
                        fontSize = 22.sp,
                        lineHeight = 34.sp,
                    ),
                    minLines = 3,
                    maxLines = 12,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all(Char::isDigit)) goalText = value
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_goal"),
                    label = { Text(stringResource(R.string.default_goal)) },
                    supportingText = { Text(stringResource(R.string.goal_optional_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.delete_custom_dhikr),
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            maxLines = 1,
                        )
                    }
                    Button(
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("custom_save"),
                        onClick = { onSave(text.trim(), goal ?: 33) },
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            maxLines = 1,
                        )
                    }
                }
            }
            }
        }
    }
}
