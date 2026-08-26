package com.sakinah.tasbih.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.ui.theme.LocalDhikrFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhraseCarousel(
    state: SakinahUiState,
    onSelectPhrase: (String) -> Unit,
    onLongPress: () -> Unit,
) {
    val phrases = state.tasbihPhrases
    val currentIndex = phrases.indexOfFirst { it.id == state.selectedPhrase.id }.coerceAtLeast(0)
    val shape = MaterialTheme.shapes.extraLarge
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 138.dp)
            .testTag("tasbih_phrase_card")
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onLongPress,
                onLongClickLabel = stringResource(R.string.edit_dhikr),
                onLongClick = onLongPress,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = sakinahCardBorder(0.32f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
                    text = displayArabic(state.selectedPhrase.text, state.showDiacritics),
                    style = TextStyle(
                        fontFamily = LocalDhikrFontFamily.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = (21f * state.tasbihTextScale).sp,
                        lineHeight = (30f * state.tasbihTextScale).sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
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
                text = "$position  •  $goalLabel  •  ${stringResource(R.string.tap_to_edit)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.delete_custom_dhikr))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = canSave,
                        modifier = Modifier.testTag("custom_save"),
                        onClick = { onSave(text.trim(), goal ?: 33) },
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
            }
        }
    }
}
