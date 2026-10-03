package com.sakinah.tasbih.ui

import android.text.BidiFormatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.DhikrEntry
import com.sakinah.tasbih.data.ReadingProgress
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.ui.theme.LocalDhikrFontFamily
import kotlin.math.roundToInt

private fun Modifier.countUnconsumedTap(
    enabled: Boolean,
    onTap: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    this
        .pointerInput(onTap) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Final,
                )
                if (down.isConsumed) return@awaitEachGesture

                val pointerId = down.id
                val downPosition = down.position
                var isTap = true
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change == null) {
                        isTap = false
                        break
                    }
                    if (
                        change.isConsumed ||
                        (change.position - downPosition).getDistance() > viewConfiguration.touchSlop
                    ) {
                        isTap = false
                    }
                    if (!change.pressed) break
                }
                if (isTap) onTap()
            }
        }
        .semantics {
            role = Role.Button
            onClick {
                onTap()
                true
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: SakinahUiState,
    collectionId: String,
    reviewCompleted: Boolean = false,
    onUndo: () -> Unit = {},
    onEntryVisible: (String) -> Unit = {},
    isFocusMode: Boolean,
    onFocusModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onIncrement: () -> Unit,
    onAdvance: () -> Unit,
    onNavigateDhikr: (Int) -> Unit,
    onRestart: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToTasbih: (DhikrEntry) -> Unit,
    onSetShowReferenceByDefault: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetDhikrCompletionSoundEnabled: (Boolean) -> Unit,
    onSetAutoAdvanceDhikrEnabled: (Boolean) -> Unit,
) {
    val collection = state.catalog.collection(collectionId)
    if (collection == null || collection.entries.isEmpty()) {
        MissingCollectionScreen(onBack = onBack)
        return
    }

    val progress = state.progressFor(collectionId)
    val safeIndex = progress.entryIndex.coerceIn(0, collection.entries.lastIndex)
    val entry = collection.entries[safeIndex]
    var reviewingCompleted by rememberSaveable(collectionId) { mutableStateOf(reviewCompleted) }
    val showCompletion = progress.completed && !reviewingCompleted
    val presentationProgress = if (progress.completed && reviewingCompleted) progress.copy(
        completed = false,
        repetitionCounts = progress.repetitionCounts + (safeIndex to entry.repetitions),
        completedEntryIndices = collection.entries.indices.toSet(),
    ) else progress
    val canUndo = collectionId in state.readerUndo
    LaunchedEffect(entry.id) { onEntryVisible(entry.id) }
    LaunchedEffect(progress.completed) { if (!progress.completed) reviewingCompleted = false }
    var showActions by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val canIncrement = !progress.completed &&
        !progress.isEntryCompleted(safeIndex, collection)
    val isFavorite = entry.id in state.favoriteEntryIds
    val haptics = LocalHapticFeedback.current
    val countActionLabel = stringResource(R.string.tap_to_count)
    var pendingAdvanceEntry by rememberSaveable(collectionId) { mutableStateOf<String?>(null) }
    val isAdvancingAutomatically = pendingAdvanceEntry == entry.id &&
        progress.isEntryCompleted(safeIndex, collection) && !progress.completed &&
        (state.autoAdvanceDhikrEnabled || isFocusMode)
    val incrementDhikr = {
        if (canIncrement) {
            if (state.hapticsEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            pendingAdvanceEntry = entry.id
            onIncrement()
        }
    }
    val undoLastCount = { pendingAdvanceEntry = null; onUndo() }
    val navigateEntry: (Int) -> Unit = { direction -> pendingAdvanceEntry = null; onNavigateDhikr(direction) }
    var showAddDialog by remember(entry.id) { mutableStateOf(false) }
    var showReaderSettings by rememberSaveable { mutableStateOf(false) }
    var showReference by rememberSaveable(entry.id) {
        mutableStateOf(state.showReferenceByDefault)
    }
    var previousRepetitionCount by remember(entry.id) {
        mutableIntStateOf(progress.repetitionCount)
    }
    val completionTone = rememberCompletionSoundPlayer()

    LaunchedEffect(entry.id, state.showReferenceByDefault) {
        showReference = state.showReferenceByDefault
    }

    LaunchedEffect(entry.id, entry.repetitions, progress.repetitionCount) {
        val justCompleted = previousRepetitionCount < entry.repetitions &&
            progress.repetitionCount >= entry.repetitions
        previousRepetitionCount = progress.repetitionCount

        if (justCompleted) {
            if (state.dhikrCompletionSoundEnabled) {
                completionTone.play(state.completionSound, state.completionSoundVolume)
            }
        }
        if (isAdvancingAutomatically) {
            // Keep the counter in place until the next entry arrives from storage.
            onAdvance()
        }
    }

    BackHandler(enabled = isFocusMode) {
        onFocusModeChange(false)
    }

    SakinahScreenBackground(showOrnament = !isFocusMode) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 480.dp
        if (isFocusMode) {
            ReaderFocusLayout(
                state = state,
                progress = presentationProgress,
                collection = collection,
                entry = entry,
                isAdvancingAutomatically = isAdvancingAutomatically,
                canUndo = canUndo,
                onUndo = undoLastCount,
                onReview = { reviewingCompleted = true },
                onAdvance = onAdvance,
                currentIndex = safeIndex,
                canIncrement = canIncrement,
                onIncrement = incrementDhikr,
                onRestart = onRestart,
                onNavigateDhikr = navigateEntry,
                onExitFocusMode = { onFocusModeChange(false) },
            )
        } else {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val titleStyle = MaterialTheme.typography.titleMedium
                val headerHeight = maxOf(
                    if (compactHeight) 48.dp else 64.dp,
                    with(LocalDensity.current) { titleStyle.lineHeight.toDp() * 2 } + 16.dp,
                )
                TopAppBar(
                    modifier = Modifier.testTag("reader_top_bar"),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    expandedHeight = headerHeight,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    title = {
                        Text(
                            displayArabic(collection.title, state.showDiacritics),
                            modifier = Modifier.testTag("reader_title"),
                            style = titleStyle.copy(platformStyle = PlatformTextStyle(includeFontPadding = true)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back")) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (!showCompletion) {
                            FocusModeIconButton(false, stringResource(R.string.reader_focus_mode), { onFocusModeChange(true) }, Modifier.testTag("reader_focus_mode"))
                        }
                        Box {
                            IconButton(onClick = { showActions = true }, modifier = Modifier.testTag("reader_actions")) {
                                Icon(Icons.Outlined.MoreVert, stringResource(R.string.reader_actions))
                            }
                            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (isFavorite) R.string.remove_favorite else R.string.favorite)) },
                                    leadingIcon = { Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null) },
                                    onClick = { showActions = false; onToggleFavorite(entry.id) }, modifier = Modifier.testTag("reader_favorite"),
                                )
                                DropdownMenuItem(text = { Text(stringResource(R.string.add_to_tasbih)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, null) },
                                    onClick = { showActions = false; showAddDialog = true })
                                DropdownMenuItem(text = { Text(stringResource(R.string.reader_options)) },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                    onClick = { showActions = false; showReaderSettings = true }, modifier = Modifier.testTag("reader_settings"))
                                if (compactHeight && entry.reference.isNotBlank()) DropdownMenuItem(
                                    text = { Text(stringResource(if (showReference) R.string.hide_source_details else R.string.show_source_details)) },
                                    onClick = { showActions = false; showReference = !showReference })
                                DropdownMenuItem(text = { Text(stringResource(R.string.restart_session)) },
                                    onClick = { showActions = false; showRestartDialog = true })
                            }
                        }
                    },
                )
            },
            bottomBar = {
                ReaderBottomAction(
                    state = state,
                    progress = presentationProgress,
                    collection = collection,
                    entry = entry,
                    isAdvancingAutomatically = isAdvancingAutomatically,
                    onIncrement = incrementDhikr,
                    onAdvance = onAdvance,
                    onRestart = onRestart,
                    canUndo = canUndo,
                    onUndo = undoLastCount,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (canIncrement) {
                            Modifier
                                .testTag("reader_count_area")
                                .pointerInput(incrementDhikr) {
                                    detectTapGestures(onTap = { incrementDhikr() })
                                }
                                .semantics {
                                    role = Role.Button
                                    onClick(label = countActionLabel) {
                                        incrementDhikr()
                                        true
                                    }
                                }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SakinahReadingMaxWidth)
                        .fillMaxSize()
                        .padding(
                            start = 20.dp,
                            top = innerPadding.calculateTopPadding(),
                            end = 20.dp,
                            bottom = innerPadding.calculateBottomPadding() + if (compactHeight) 4.dp else 22.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compactHeight) 4.dp else 16.dp),
                ) {
                    if (compactHeight && !showCompletion) {
                        Row(Modifier.fillMaxWidth().testTag("reader_session_header"), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigateEntry(-1) }, enabled = safeIndex > 0, modifier = Modifier.testTag("reader_previous_dhikr")) {
                                CarouselArrow(true, stringResource(R.string.previous_dhikr))
                            }
                            Text(stringResource(R.string.dhikr_position, safeIndex + 1, collection.entries.size),
                                style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            IconButton(onClick = { navigateEntry(1) }, enabled = safeIndex < collection.entries.lastIndex, modifier = Modifier.testTag("reader_next_dhikr")) {
                                CarouselArrow(false, stringResource(R.string.next_dhikr))
                            }
                        }
                    } else SessionHeader(
                        collection = collection,
                        progress = progress,
                        currentIndex = safeIndex,
                    )

                    if (showCompletion) {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            SessionCompleteCard(onRestart = onRestart, onReview = { reviewingCompleted = true })
                        }
                    } else {
                        DhikrReadingCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            entry = entry,
                            state = state,
                            showReference = showReference,
                            onToggleReference = entry.reference.takeIf { it.isNotBlank() && !compactHeight }?.let {
                                { showReference = !showReference }
                            },
                            canNavigatePrevious = safeIndex > 0,
                            canNavigateNext = safeIndex < collection.entries.lastIndex,
                            onNavigateDhikr = navigateEntry,
                            isFocusMode = compactHeight,
                        )
                    }
                }
            }
        }
    }
    }
    }

    if (showRestartDialog) {
        AlertDialog(onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_collection_title)) },
            text = { Text(stringResource(R.string.restart_collection_message)) },
            confirmButton = { TextButton(onClick = { showRestartDialog = false; onRestart() }) { Text(stringResource(R.string.restart_session)) } },
            dismissButton = { TextButton(onClick = { showRestartDialog = false }) { Text(stringResource(R.string.cancel)) } })
    }
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_to_tasbih_title)) },
            text = { Text(stringResource(R.string.add_to_tasbih_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddToTasbih(entry)
                        showAddDialog = false
                    },
                ) {
                    Text(stringResource(R.string.confirm_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showReaderSettings) {
        ReaderSettingsSheet(
            state = state,
            onDismiss = { showReaderSettings = false },
            onSetShowReferenceByDefault = onSetShowReferenceByDefault,
            onSetTextScale = onSetTextScale,
            onSetCompletionSoundEnabled = onSetDhikrCompletionSoundEnabled,
            onSetAutoAdvanceDhikrEnabled = onSetAutoAdvanceDhikrEnabled,
        )
    }
}

@Composable
private fun SessionHeader(
    collection: DhikrCollection,
    progress: ReadingProgress,
    currentIndex: Int,
) {
    Column(
        modifier = Modifier
            .testTag("reader_session_header")
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress.fraction(collection),
                    range = 0f..1f,
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (progress.completed) {
                    stringResource(R.string.completed_label)
                } else {
                    stringResource(R.string.dhikr_position, currentIndex + 1, collection.entries.size)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(progress.fraction(collection) * 100).toInt()}٪",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        BeadTrail(
            collection = collection,
            currentIndex = currentIndex,
            progress = progress,
        )
    }
}

@Composable
private fun BeadTrail(
    collection: DhikrCollection,
    currentIndex: Int,
    progress: ReadingProgress,
) {
    val total = collection.entries.size
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, total) {
        if (total > 0) listState.animateScrollToItem(currentIndex.coerceIn(0, total - 1))
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(total) { index ->
            val isDone = progress.isEntryCompleted(index, collection)
            val isCurrent = !progress.completed && index == currentIndex
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 13.dp else 9.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun DhikrReadingCard(
    modifier: Modifier,
    entry: DhikrEntry,
    state: SakinahUiState,
    showReference: Boolean,
    onToggleReference: (() -> Unit)?,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onNavigateDhikr: (Int) -> Unit,
    isFocusMode: Boolean = false,
) {
    val readingScrollState = key(entry.id) { rememberScrollState() }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reader_dhikr_card"),
    ) {
        val readingCanvasHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader_dhikr_scroll")
                .verticalScroll(readingScrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = readingCanvasHeight),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = if (isFocusMode) 4.dp else 22.dp, vertical = if (isFocusMode) 12.dp else 28.dp)
                        .padding(bottom = if (onToggleReference != null) 40.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (!isFocusMode) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(
                            text = entry.repetitions.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    }
                    Text(
                        text = displayArabic(entry.text, state.showDiacritics),
                        style = TextStyle(
                            fontFamily = LocalDhikrFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = (27f * state.textScale).sp,
                            lineHeight = (49f * state.textScale).sp,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag("reader_dhikr_text"),
                    )
                }

                if (!isFocusMode) {
                DhikrNavigationArrow(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.previous_dhikr),
                    enabled = canNavigatePrevious,
                    onClick = { onNavigateDhikr(-1) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 4.dp, top = 18.dp)
                        .testTag("reader_previous_dhikr"),
                )

                DhikrNavigationArrow(
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.next_dhikr),
                    enabled = canNavigateNext,
                    onClick = { onNavigateDhikr(1) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 18.dp)
                        .testTag("reader_next_dhikr"),
                )
                }

                if (onToggleReference != null) {
                    IconButton(
                        onClick = onToggleReference,
                        modifier = Modifier
                            // In RTL, Start resolves to the physical right edge requested for the reading canvas.
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .testTag("reader_reference_toggle"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showReference) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (showReference) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(
                                if (showReference) {
                                    R.string.hide_source_details
                                } else {
                                    R.string.show_source_details
                                },
                            ),
                        )
                    }
                }
            }

            if (showReference) {
                Spacer(Modifier.height(16.dp))
                ReferenceCard(entry.reference)
            }
        }
    }
}

@Composable
private fun DhikrNavigationArrow(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.48f)
        },
        contentColor = MaterialTheme.colorScheme.primary,
        border = sakinahCardBorder(if (enabled) 0.26f else 0.1f),
        shadowElevation = if (enabled) 2.dp else 0.dp,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(27.dp),
            )
        }
    }
}

@Composable
private fun ReferenceCard(reference: String) {
    if (reference.isBlank()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = sakinahCardBorder(0.16f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SakinahRosette(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.source_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = BidiFormatter.getInstance(true).unicodeWrap(reference),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    state: SakinahUiState,
    onDismiss: () -> Unit,
    onSetShowReferenceByDefault: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetCompletionSoundEnabled: (Boolean) -> Unit,
    onSetAutoAdvanceDhikrEnabled: (Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_options),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(14.dp))
            ReaderOptionSwitch(
                title = stringResource(R.string.show_reference_by_default),
                description = stringResource(R.string.show_reference_by_default_description),
                checked = state.showReferenceByDefault,
                onCheckedChange = onSetShowReferenceByDefault,
            )
            Spacer(Modifier.height(8.dp))
            ReaderTextScaleControl(
                scale = state.textScale,
                onDecrease = {
                    onSetTextScale((state.textScale - 0.1f).coerceAtLeast(0.5f))
                },
                onIncrease = {
                    onSetTextScale((state.textScale + 0.1f).coerceAtMost(1.4f))
                },
            )
            Spacer(Modifier.height(8.dp))
            ReaderOptionSwitch(
                title = stringResource(R.string.dhikr_completion_sound),
                description = stringResource(R.string.dhikr_completion_sound_description),
                checked = state.dhikrCompletionSoundEnabled,
                onCheckedChange = onSetCompletionSoundEnabled,
            )
            Spacer(Modifier.height(8.dp))
            ReaderOptionSwitch(
                title = stringResource(R.string.auto_advance_dhikr),
                description = stringResource(R.string.auto_advance_dhikr_description),
                checked = state.autoAdvanceDhikrEnabled,
                onCheckedChange = onSetAutoAdvanceDhikrEnabled,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReaderOptionSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ReaderTextScaleControl(
    scale: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.reader_font_size), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${(scale * 100).roundToInt()}٪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                )
            }
            IconButton(
                enabled = scale > 0.5f,
                onClick = onDecrease,
                modifier = Modifier.testTag("reader_font_decrease"),
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = stringResource(R.string.decrease_reader_font),
                )
            }
            IconButton(
                enabled = scale < 1.4f,
                onClick = onIncrease,
                modifier = Modifier.testTag("reader_font_increase"),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.increase_reader_font),
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomAction(
    state: SakinahUiState,
    progress: ReadingProgress,
    collection: DhikrCollection,
    entry: DhikrEntry,
    isAdvancingAutomatically: Boolean,
    onIncrement: () -> Unit,
    onAdvance: () -> Unit,
    onRestart: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 5.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                progress.completed -> {
                    ReaderUndoButton(canUndo, onUndo)
                }

                state.progressFor(collection.id).completed -> {
                    ReaderCounterDock(progress, entry, onIncrement, canUndo, onUndo)
                }

                !isAdvancingAutomatically && progress.isEntryCompleted(progress.entryIndex, collection) -> {
                    ReaderAdvanceAction(progress, collection, onAdvance, canUndo, onUndo)
                }

                else -> {
                    ReaderCounterDock(
                        progress = progress,
                        entry = entry,
                        onIncrement = onIncrement,
                        canUndo = canUndo,
                        onUndo = onUndo,
                        showCompletionFeedback = !isAdvancingAutomatically,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderFocusLayout(
    state: SakinahUiState,
    progress: ReadingProgress,
    collection: DhikrCollection,
    entry: DhikrEntry,
    isAdvancingAutomatically: Boolean,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onReview: () -> Unit,
    onAdvance: () -> Unit,
    currentIndex: Int,
    canIncrement: Boolean,
    onIncrement: () -> Unit,
    onRestart: () -> Unit,
    onNavigateDhikr: (Int) -> Unit,
    onExitFocusMode: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_focus_screen")
            .countUnconsumedTap(
                enabled = canIncrement,
                onTap = onIncrement,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SakinahReadingMaxWidth)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusNavigationBar(
                canGoPrevious = !progress.completed && currentIndex > 0,
                canGoNext = !progress.completed && currentIndex < collection.entries.lastIndex,
                onPrevious = { onNavigateDhikr(-1) },
                onNext = { onNavigateDhikr(1) },
                onExit = onExitFocusMode,
                previousTag = "reader_previous_dhikr",
                nextTag = "reader_next_dhikr",
                exitTag = "reader_exit_focus_mode",
            )
            if (progress.completed) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SessionCompleteCard(onRestart = onRestart, onReview = onReview)
                }
            } else {
                DhikrReadingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    entry = entry,
                    state = state,
                    showReference = false,
                    onToggleReference = null,
                    canNavigatePrevious = currentIndex > 0,
                    canNavigateNext = currentIndex < collection.entries.lastIndex,
                    onNavigateDhikr = onNavigateDhikr,
                    isFocusMode = true,
                )
            }

            if (progress.completed) {
                ReaderUndoButton(canUndo, onUndo)
            } else if (!isAdvancingAutomatically && !state.progressFor(collection.id).completed && progress.isEntryCompleted(currentIndex, collection)) {
                ReaderAdvanceAction(progress, collection, onAdvance, canUndo, onUndo)
            } else {
                ReaderCounterDock(
                    progress = progress,
                    entry = entry,
                    onIncrement = onIncrement,
                    canUndo = canUndo,
                    onUndo = onUndo,
                    showCompletionFeedback = !isAdvancingAutomatically,
                )
            }
        }

    }
}

@Composable
private fun ReaderAdvanceAction(
    progress: ReadingProgress, collection: DhikrCollection, onAdvance: () -> Unit, canUndo: Boolean, onUndo: () -> Unit,
) {
    Column {
        Button(onClick = onAdvance, modifier = Modifier.fillMaxWidth().testTag("reader_advance")) {
            Text(stringResource(if (progress.completedEntries(collection) == collection.entries.size)
                R.string.finish_session else R.string.next_dhikr))
        }
        ReaderUndoButton(canUndo, onUndo)
    }
}

@Composable
private fun ReaderCounterDock(
    progress: ReadingProgress,
    entry: DhikrEntry,
    onIncrement: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    showCompletionFeedback: Boolean = true,
) {
    val repetitionFraction = (
        progress.repetitionCount.toFloat() / entry.repetitions.coerceAtLeast(1)
    ).coerceIn(0f, 1f)
    val remaining = (entry.repetitions - progress.repetitionCount).coerceAtLeast(0)
    val completed = remaining == 0
    val showCompleted = completed && showCompletionFeedback
    val countDescription = stringResource(R.string.reader_count_state, progress.repetitionCount, entry.repetitions, remaining)

    if (LocalConfiguration.current.screenHeightDp < 480) {
        Surface(Modifier.fillMaxWidth().testTag("reader_count_button").clip(MaterialTheme.shapes.medium)
            .semantics { stateDescription = countDescription }
            .clickable(enabled = !completed, role = Role.Button, onClick = onIncrement),
            shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(progress.repetitionCount.toString(), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("reader_repetition_count"))
                Text(stringResource(R.string.repetition_target, entry.repetitions), style = MaterialTheme.typography.labelMedium)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (showCompleted) R.string.reader_dhikr_done else R.string.reader_count_action), style = MaterialTheme.typography.labelLarge)
                    ProgressLine(repetitionFraction, height = 4.dp)
                }
                Text(stringResource(R.string.reader_remaining, remaining), style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("reader_undo")) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo_reader_count))
                }
            }
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_count_button")
            .clip(MaterialTheme.shapes.large)
            .semantics { stateDescription = countDescription }
            .clickable(
                enabled = !completed,
                role = Role.Button,
                onClickLabel = stringResource(R.string.reader_count_action),
                onClick = onIncrement,
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = progress.repetitionCount.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .testTag("reader_repetition_count"),
                    )
                    Text(stringResource(R.string.repetition_target, entry.repetitions), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (showCompleted) R.string.reader_dhikr_done else R.string.reader_count_action),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.reader_remaining, remaining),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        if (showCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ProgressLine(progress = repetitionFraction, height = 4.dp) }
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("reader_undo")) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo_reader_count))
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteCard(onRestart: () -> Unit, onReview: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.session_complete), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.session_complete_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            TextButton(onClick = onReview, modifier = Modifier.testTag("reader_review_completed")) { Text(stringResource(R.string.review_collection)) }
            Button(onClick = onRestart) { Text(stringResource(R.string.restart_session)) }
        }
    }
}

@Composable
private fun MissingCollectionScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.content_load_error), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun ReaderUndoButton(enabled: Boolean, onUndo: () -> Unit) {
    TextButton(onClick = onUndo, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("reader_undo")) {
        Icon(Icons.AutoMirrored.Outlined.Undo, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.undo_reader_count))
    }
}
