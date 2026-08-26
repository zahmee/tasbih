package com.sakinah.tasbih.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.DhikrGroup
import com.sakinah.tasbih.data.displayArabic
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LibraryScreen(
    state: SakinahUiState,
    onOpenCollection: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf<DhikrGroup?>(null) }
    val searchResults = remember(state.catalog, query) { state.catalog.search(query) }
    val visibleCollections = remember(searchResults, selectedGroup) {
        selectedGroup?.let { group ->
            searchResults.filter { DhikrGroup.forOrder(it.order) == group }
        } ?: searchResults
    }
    val showingSections = selectedGroup == null && query.isBlank()

    BackHandler(enabled = !showingSections) {
        query = ""
        selectedGroup = null
    }

    SakinahScreenBackground {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SakinahContentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LibraryHeader(
                    selectedGroup = selectedGroup,
                    onBackToSections = {
                        query = ""
                        selectedGroup = null
                    },
                )
            }

            if (state.isLoading || state.contentLoadFailed) {
                item { ContentStatusCard(state = state, onRetry = onRetry) }
            } else {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.clear_search),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }

                if (showingSections) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            SakinahSectionHeader(stringResource(R.string.library_sections))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.library_sections_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 3.dp),
                            )
                        }
                    }
                    item {
                        ResponsiveLibraryGroups(
                            state = state,
                            onSelect = { selectedGroup = it },
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(R.string.search_results_count, visibleCollections.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    if (visibleCollections.isEmpty()) {
                        item { EmptySearchCard() }
                    } else {
                        items(
                            count = visibleCollections.size,
                            key = { index -> visibleCollections[index].id },
                        ) { index ->
                            val collection = visibleCollections[index]
                            CollectionCard(
                                collection = collection,
                                state = state,
                                onClick = { onOpenCollection(collection.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(selectedGroup: DhikrGroup?, onBackToSections: () -> Unit) {
    if (selectedGroup == null) {
        SakinahScreenHeader(
            title = stringResource(R.string.hisn_library),
            subtitle = stringResource(R.string.library_subtitle),
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackToSections) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_sections),
                )
            }
            Spacer(Modifier.width(7.dp))
            LibraryGroupIcon(group = selectedGroup, modifier = Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(selectedGroup.titleRes()), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.back_to_sections),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResponsiveLibraryGroups(
    state: SakinahUiState,
    onSelect: (DhikrGroup) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DhikrGroup.entries.chunked(columns).forEach { rowGroups ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowGroups.forEach { group ->
                        val collections = state.catalog.collections.filter {
                            DhikrGroup.forOrder(it.order) == group
                        }
                        LibraryGroupCard(
                            modifier = Modifier.weight(1f),
                            group = group,
                            collectionCount = collections.size,
                            dhikrCount = collections.sumOf { it.entries.size },
                            onClick = { onSelect(group) },
                        )
                    }
                    repeat(columns - rowGroups.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentStatusCard(state: SakinahUiState, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (state.isLoading) R.string.loading_book else R.string.content_load_error),
                modifier = Modifier.weight(1f),
            )
            if (state.contentLoadFailed) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Composable
private fun LibraryGroupCard(
    modifier: Modifier = Modifier,
    group: DhikrGroup,
    collectionCount: Int,
    dhikrCount: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("library_group_${group.name}")
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryGroupIcon(group = group, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(group.titleRes()), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.library_group_summary, collectionCount, dhikrCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun LibraryGroupIcon(group: DhikrGroup, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = color,
    ) {
        Canvas(Modifier.padding(11.dp)) {
            val stroke = 2.dp.toPx()
            when (group) {
                DhikrGroup.DailyLife -> drawDailyLifeIcon(color, accent, stroke)
                DhikrGroup.Prayer -> drawPrayerIcon(color, accent, stroke)
                DhikrGroup.MorningEvening -> drawMorningEveningIcon(color, accent, stroke)
                DhikrGroup.ReliefAndWellbeing -> drawReliefIcon(color, accent, stroke)
                DhikrGroup.Occasions -> drawLanternIcon(color, accent, stroke)
                DhikrGroup.Travel -> drawCompassIcon(color, accent, stroke)
                DhikrGroup.SocialAndVirtues -> drawVirtuesIcon(color, accent, stroke)
                DhikrGroup.HajjAndUmrah -> drawKaabaIcon(color, accent, stroke)
                DhikrGroup.GeneralGood -> drawGeneralGoodIcon(color, accent, stroke)
            }
        }
    }
}

private fun DrawScope.drawDailyLifeIcon(color: Color, accent: Color, stroke: Float) {
    val arch = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.82f)
        lineTo(size.width * 0.2f, size.height * 0.46f)
        cubicTo(size.width * 0.2f, size.height * 0.28f, size.width * 0.4f, size.height * 0.2f, size.width * 0.5f, size.height * 0.12f)
        cubicTo(size.width * 0.6f, size.height * 0.2f, size.width * 0.8f, size.height * 0.28f, size.width * 0.8f, size.height * 0.46f)
        lineTo(size.width * 0.8f, size.height * 0.82f)
    }
    drawPath(arch, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(accent, size.minDimension * 0.09f, Offset(size.width * 0.5f, size.height * 0.53f))
    drawLine(color, Offset(size.width * 0.12f, size.height * 0.82f), Offset(size.width * 0.88f, size.height * 0.82f), stroke, StrokeCap.Round)
}

private fun DrawScope.drawPrayerIcon(color: Color, accent: Color, stroke: Float) {
    val rug = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.82f)
        lineTo(size.width * 0.22f, size.height * 0.33f)
        lineTo(size.width * 0.5f, size.height * 0.13f)
        lineTo(size.width * 0.78f, size.height * 0.33f)
        lineTo(size.width * 0.78f, size.height * 0.82f)
        close()
    }
    drawPath(rug, color.copy(alpha = 0.08f))
    drawPath(rug, color, style = Stroke(stroke, join = StrokeJoin.Round))
    drawEightPointIcon(Offset(size.width * 0.5f, size.height * 0.53f), size.minDimension * 0.14f, accent, stroke * 0.65f)
    repeat(4) { index ->
        val x = size.width * (0.3f + index * 0.13f)
        drawLine(color, Offset(x, size.height * 0.84f), Offset(x, size.height * 0.93f), stroke * 0.6f)
    }
}

private fun DrawScope.drawMorningEveningIcon(color: Color, accent: Color, stroke: Float) {
    drawCircle(accent.copy(alpha = 0.25f), size.minDimension * 0.18f, Offset(size.width * 0.68f, size.height * 0.34f))
    repeat(8) { index ->
        val angle = index * PI / 4
        val start = Offset(
            size.width * 0.68f + (cos(angle) * size.minDimension * 0.24f).toFloat(),
            size.height * 0.34f + (sin(angle) * size.minDimension * 0.24f).toFloat(),
        )
        val end = Offset(
            size.width * 0.68f + (cos(angle) * size.minDimension * 0.3f).toFloat(),
            size.height * 0.34f + (sin(angle) * size.minDimension * 0.3f).toFloat(),
        )
        drawLine(accent, start, end, stroke * 0.7f, StrokeCap.Round)
    }
    val crescent = Path().apply {
        moveTo(size.width * 0.45f, size.height * 0.36f)
        cubicTo(size.width * 0.08f, size.height * 0.4f, size.width * 0.12f, size.height * 0.86f, size.width * 0.52f, size.height * 0.79f)
        cubicTo(size.width * 0.3f, size.height * 0.66f, size.width * 0.31f, size.height * 0.49f, size.width * 0.45f, size.height * 0.36f)
        close()
    }
    drawPath(crescent, color)
}

private fun DrawScope.drawReliefIcon(color: Color, accent: Color, stroke: Float) {
    val heart = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.82f)
        cubicTo(size.width * 0.12f, size.height * 0.6f, size.width * 0.2f, size.height * 0.2f, size.width * 0.5f, size.height * 0.4f)
        cubicTo(size.width * 0.8f, size.height * 0.2f, size.width * 0.88f, size.height * 0.6f, size.width * 0.5f, size.height * 0.82f)
        close()
    }
    drawPath(heart, color.copy(alpha = 0.09f))
    drawPath(heart, color, style = Stroke(stroke, join = StrokeJoin.Round))
    drawEightPointIcon(Offset(size.width * 0.5f, size.height * 0.52f), size.minDimension * 0.11f, accent, stroke * 0.62f)
}

private fun DrawScope.drawLanternIcon(color: Color, accent: Color, stroke: Float) {
    drawLine(color, Offset(size.width * 0.5f, size.height * 0.08f), Offset(size.width * 0.5f, size.height * 0.22f), stroke)
    drawLine(color, Offset(size.width * 0.34f, size.height * 0.23f), Offset(size.width * 0.66f, size.height * 0.23f), stroke, StrokeCap.Round)
    val lantern = Path().apply {
        moveTo(size.width * 0.34f, size.height * 0.23f)
        lineTo(size.width * 0.22f, size.height * 0.73f)
        lineTo(size.width * 0.36f, size.height * 0.88f)
        lineTo(size.width * 0.64f, size.height * 0.88f)
        lineTo(size.width * 0.78f, size.height * 0.73f)
        lineTo(size.width * 0.66f, size.height * 0.23f)
        close()
    }
    drawPath(lantern, color.copy(alpha = 0.08f))
    drawPath(lantern, color, style = Stroke(stroke, join = StrokeJoin.Round))
    drawCircle(accent, size.minDimension * 0.1f, Offset(size.width * 0.5f, size.height * 0.58f))
}

private fun DrawScope.drawCompassIcon(color: Color, accent: Color, stroke: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(color, size.minDimension * 0.38f, center, style = Stroke(stroke))
    drawCircle(accent, size.minDimension * 0.05f, center)
    val needle = Path().apply {
        moveTo(center.x, size.height * 0.14f)
        lineTo(size.width * 0.58f, center.y)
        lineTo(center.x, size.height * 0.86f)
        lineTo(size.width * 0.42f, center.y)
        close()
    }
    drawPath(needle, color.copy(alpha = 0.12f))
    drawPath(needle, color, style = Stroke(stroke * 0.8f, join = StrokeJoin.Round))
}

private fun DrawScope.drawVirtuesIcon(color: Color, accent: Color, stroke: Float) {
    drawCircle(color, size.minDimension * 0.13f, Offset(size.width * 0.34f, size.height * 0.34f), style = Stroke(stroke))
    drawCircle(accent, size.minDimension * 0.13f, Offset(size.width * 0.66f, size.height * 0.34f), style = Stroke(stroke))
    val bridge = Path().apply {
        moveTo(size.width * 0.13f, size.height * 0.82f)
        cubicTo(size.width * 0.18f, size.height * 0.55f, size.width * 0.4f, size.height * 0.54f, size.width * 0.5f, size.height * 0.72f)
        cubicTo(size.width * 0.6f, size.height * 0.54f, size.width * 0.82f, size.height * 0.55f, size.width * 0.87f, size.height * 0.82f)
    }
    drawPath(bridge, color, style = Stroke(stroke, cap = StrokeCap.Round))
    drawEightPointIcon(Offset(size.width * 0.5f, size.height * 0.53f), size.minDimension * 0.08f, accent, stroke * 0.6f)
}

private fun DrawScope.drawKaabaIcon(color: Color, accent: Color, stroke: Float) {
    val building = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.3f)
        lineTo(size.width * 0.7f, size.height * 0.2f)
        lineTo(size.width * 0.84f, size.height * 0.34f)
        lineTo(size.width * 0.84f, size.height * 0.82f)
        lineTo(size.width * 0.2f, size.height * 0.82f)
        close()
    }
    drawPath(building, color.copy(alpha = 0.11f))
    drawPath(building, color, style = Stroke(stroke, join = StrokeJoin.Round))
    drawLine(accent, Offset(size.width * 0.2f, size.height * 0.45f), Offset(size.width * 0.84f, size.height * 0.45f), stroke * 1.35f)
    drawRect(color, Offset(size.width * 0.55f, size.height * 0.58f), androidx.compose.ui.geometry.Size(size.width * 0.13f, size.height * 0.24f))
}

private fun DrawScope.drawGeneralGoodIcon(color: Color, accent: Color, stroke: Float) {
    drawEightPointIcon(Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.4f, color, stroke)
    drawCircle(accent, size.minDimension * 0.105f, Offset(size.width / 2f, size.height / 2f))
    repeat(4) { index ->
        rotate(index * 45f, Offset(size.width / 2f, size.height / 2f)) {
            drawLine(
                color,
                Offset(size.width * 0.5f, size.height * 0.18f),
                Offset(size.width * 0.5f, size.height * 0.34f),
                stroke * 0.7f,
                StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawEightPointIcon(center: Offset, outer: Float, color: Color, stroke: Float) {
    val path = Path()
    repeat(16) { index ->
        val angle = -PI / 2 + index * PI / 8
        val radius = if (index % 2 == 0) outer else outer * 0.46f
        val point = Offset(
            center.x + (cos(angle) * radius).toFloat(),
            center.y + (sin(angle) * radius).toFloat(),
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = 0.1f))
    drawPath(path, color, style = Stroke(stroke, join = StrokeJoin.Round))
}

@Composable
private fun CollectionCard(collection: DhikrCollection, state: SakinahUiState, onClick: () -> Unit) {
    val progress = state.progressFor(collection.id)
    val progressValue = progress.fraction(collection)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("collection_${collection.id}")
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.13f),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (progress.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (progress.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (progress.completed) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                    } else {
                        Text(collection.order.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayArabic(collection.title, state.showDiacritics),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dhikr_items_count, collection.entries.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progressValue > 0f) {
                    Spacer(Modifier.height(9.dp))
                    ProgressLine(progressValue)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptySearchCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.no_search_results), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.no_search_results_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun DhikrGroup.titleRes(): Int = when (this) {
    DhikrGroup.DailyLife -> R.string.group_daily_life
    DhikrGroup.Prayer -> R.string.group_prayer
    DhikrGroup.MorningEvening -> R.string.group_morning_evening
    DhikrGroup.ReliefAndWellbeing -> R.string.group_relief
    DhikrGroup.Occasions -> R.string.group_occasions
    DhikrGroup.Travel -> R.string.group_travel
    DhikrGroup.SocialAndVirtues -> R.string.group_social
    DhikrGroup.HajjAndUmrah -> R.string.group_hajj
    DhikrGroup.GeneralGood -> R.string.group_general_good
}
