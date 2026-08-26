package com.sakinah.tasbih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    state: SakinahUiState,
    onOpenCollection: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTasbih: () -> Unit,
    onRetry: () -> Unit,
) {
    val today = LocalDate.now()
    val arabicLocale = remember { Locale.forLanguageTag("ar") }
    val weekdayLabel = remember(today) {
        DateTimeFormatter.ofPattern("EEEE", arabicLocale).format(today)
    }
    val gregorianDateLabel = remember(today) {
        DateTimeFormatter.ofPattern("d MMMM yyyy", arabicLocale).format(today)
    }
    val hijriDateLabel = remember(today) {
        DateTimeFormatter.ofPattern("d MMMM yyyy", arabicLocale).format(HijrahDate.from(today))
    }
    val isMorning = remember { LocalTime.now().hour in 4..15 }
    val featuredOrder = if (isMorning) 27 else 28
    val featured = state.catalog.collections.firstOrNull { it.order == featuredOrder }

    SakinahScreenBackground {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SakinahContentMaxWidth)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                DualDateHeader(
                    weekdayLabel = weekdayLabel,
                    hijriDateLabel = hijriDateLabel,
                    gregorianDateLabel = gregorianDateLabel,
                )
            }
            item {
                HomeHero(isMorning = isMorning)
            }

            when {
                state.isLoading -> item {
                    ContentStatusCard(
                        title = stringResource(R.string.loading_book),
                        showAction = false,
                        onAction = onRetry,
                    )
                }

                state.contentLoadFailed -> item {
                    ContentStatusCard(
                        title = stringResource(R.string.content_load_error),
                        showAction = true,
                        onAction = onRetry,
                    )
                }

                featured != null -> {
                    item { SakinahSectionHeader(stringResource(R.string.today)) }
                    item {
                        FeaturedDhikrCard(
                            collection = featured,
                            state = state,
                            isMorning = isMorning,
                            onClick = { onOpenCollection(featured.id) },
                        )
                    }
                    item { SakinahSectionHeader(stringResource(R.string.quick_access)) }
                    item {
                        QuickAccessGrid(
                            state = state,
                            onOpenCollection = onOpenCollection,
                        )
                    }
                    item {
                        BookLibraryCard(
                            state = state,
                            onClick = onOpenLibrary,
                        )
                    }
                }
            }

            item { SakinahSectionHeader(stringResource(R.string.daily_tasbih)) }
            item {
                TasbihSnapshot(
                    state = state,
                    onClick = onOpenTasbih,
                )
            }
        }
    }
}

@Composable
private fun DualDateHeader(
    weekdayLabel: String,
    hijriDateLabel: String,
    gregorianDateLabel: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = weekdayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SakinahRosette(modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DatePill(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.hijri_date),
                value = hijriDateLabel,
            )
            DatePill(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.gregorian_date),
                value = gregorianDateLabel,
            )
        }
    }
}

@Composable
private fun DatePill(
    modifier: Modifier,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeHero(isMorning: Boolean) {
    val brand = LocalSakinahBrandColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = brand.heroStart,
        contentColor = brand.onHero,
        shadowElevation = 3.dp,
    ) {
        val gold = brand.antiqueGold
        val foreground = brand.onHero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(brand.heroStart, brand.heroEnd),
                        ),
                    )
                    drawRoundRect(
                        color = gold.copy(alpha = 0.48f),
                        cornerRadius = CornerRadius(30.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                    val starColor = foreground.copy(alpha = 0.105f)
                    repeat(5) { index ->
                        drawIslamicStar(
                            center = Offset(
                                x = size.width * (0.12f + index * 0.19f),
                                y = size.height * if (index % 2 == 0) 0.18f else 0.29f,
                            ),
                            outerRadius = 7.dp.toPx(),
                            innerRadius = 3.dp.toPx(),
                            color = starColor,
                            strokeWidth = 0.8.dp.toPx(),
                        )
                    }
                    val arch = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, size.height * 0.48f)
                        cubicTo(
                            size.width * 0.04f,
                            size.height * 0.27f,
                            size.width * 0.18f,
                            size.height * 0.13f,
                            size.width * 0.28f,
                            0f,
                        )
                        cubicTo(
                            size.width * 0.38f,
                            size.height * 0.13f,
                            size.width * 0.48f,
                            size.height * 0.31f,
                            size.width * 0.49f,
                            size.height * 0.53f,
                        )
                        lineTo(size.width * 0.49f, size.height)
                        close()
                    }
                    drawPath(arch, foreground.copy(alpha = 0.055f))
                }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.greeting),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 27.sp,
                            lineHeight = 32.sp,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            if (isMorning) R.string.home_morning_subtitle else R.string.home_evening_subtitle,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = brand.onHero.copy(alpha = 0.86f),
                    )
                }
                Spacer(Modifier.width(10.dp))
                HomeIslamicSeal(isMorning = isMorning)
            }
        }
    }
}

@Composable
private fun FeaturedDhikrCard(
    collection: DhikrCollection,
    state: SakinahUiState,
    isMorning: Boolean,
    onClick: () -> Unit,
) {
    val progress = state.progressFor(collection.id)
    val progressValue = progress.fraction(collection)
    val completedEntries = progress.completedEntries(collection)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_featured")
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = sakinahCardBorder(0.28f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    IslamicHomeActionIcon(
                        symbol = if (isMorning) {
                            HomeActionSymbol.MorningWird
                        } else {
                            HomeActionSymbol.EveningWird
                        },
                        modifier = Modifier.padding(7.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayArabic(collection.title, state.showDiacritics),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            lineHeight = 27.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.collection_progress,
                            completedEntries,
                            collection.entries.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = stringResource(
                            if (progressValue > 0f && !progress.completed) R.string.resume else R.string.start,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressLine(progress = progressValue, height = 4.dp)
        }
    }
}

@Composable
private fun QuickAccessGrid(
    state: SakinahUiState,
    onOpenCollection: (String) -> Unit,
) {
    val quickItems = listOfNotNull(
        state.catalog.collections.firstOrNull { it.order == 29 }?.let {
            Triple(it, R.string.sleep_adhkar, HomeActionSymbol.Sleep)
        },
        state.catalog.collections.firstOrNull { it.order == 25 }?.let {
            Triple(it, R.string.after_prayer_adhkar, HomeActionSymbol.AfterPrayer)
        },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        quickItems.forEach { (collection, titleRes, symbol) ->
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                title = stringResource(titleRes),
                subtitle = stringResource(R.string.dhikr_items_count, collection.entries.size),
                symbol = symbol,
                onClick = { onOpenCollection(collection.id) },
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    symbol: HomeActionSymbol,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 94.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.16f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                IslamicHomeActionIcon(
                    symbol = symbol,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BookLibraryCard(
    state: SakinahUiState,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IslamicNavigationIcon(
                symbol = IslamicNavSymbol.Manuscript,
                selected = true,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.book_summary),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                    ),
                )
                Text(
                    stringResource(
                        R.string.book_summary_format,
                        state.catalog.collections.size,
                        state.catalog.totalDhikr,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.library))
            }
        }
    }
}

@Composable
private fun TasbihSnapshot(
    state: SakinahUiState,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = sakinahCardBorder(0.14f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                IslamicNavigationIcon(
                    symbol = IslamicNavSymbol.Tasbih,
                    selected = true,
                    modifier = Modifier.padding(10.dp).size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayArabic(state.selectedPhrase.text, state.showDiacritics),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if (state.tasbihTarget == 0) {
                        stringResource(R.string.counter_free_value, state.tasbihCount)
                    } else {
                        stringResource(R.string.counter_value, state.tasbihCount, state.tasbihTarget)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                state.tasbihCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ContentStatusCard(
    title: String,
    showAction: Boolean,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (showAction) {
                TextButton(onClick = onAction) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Composable
private fun HomeIslamicSeal(isMorning: Boolean) {
    val brand = LocalSakinahBrandColors.current
    val foreground = brand.onHero
    val gold = brand.antiqueGold
    val cutout = brand.heroStart
    Canvas(modifier = Modifier.size(52.dp)) {
        drawCircle(foreground.copy(alpha = 0.07f), radius = size.minDimension / 2f)
        drawCircle(
            color = gold.copy(alpha = 0.9f),
            radius = size.minDimension * 0.42f,
            style = Stroke(1.2.dp.toPx()),
        )
        drawIslamicStar(
            center = center,
            outerRadius = size.minDimension * 0.31f,
            innerRadius = size.minDimension * 0.19f,
            color = foreground.copy(alpha = 0.48f),
            strokeWidth = 0.9.dp.toPx(),
        )
        if (isMorning) {
            drawCircle(gold, radius = 7.dp.toPx(), center = center)
            repeat(8) { index ->
                val angle = index * PI / 4
                drawLine(
                    color = gold,
                    start = Offset(
                        center.x + (cos(angle) * 11.dp.toPx()).toFloat(),
                        center.y + (sin(angle) * 11.dp.toPx()).toFloat(),
                    ),
                    end = Offset(
                        center.x + (cos(angle) * 15.dp.toPx()).toFloat(),
                        center.y + (sin(angle) * 15.dp.toPx()).toFloat(),
                    ),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        } else {
            drawCircle(gold, radius = 12.dp.toPx(), center = center)
            drawCircle(
                cutout,
                radius = 11.dp.toPx(),
                center = Offset(center.x - 5.dp.toPx(), center.y - 3.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawIslamicStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    strokeWidth: Float,
) {
    val star = Path()
    repeat(16) { index ->
        val angle = -PI / 2 + index * PI / 8
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(
            center.x + (cos(angle) * radius).toFloat(),
            center.y + (sin(angle) * radius).toFloat(),
        )
        if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
    }
    star.close()
    drawPath(
        path = star,
        color = color,
        style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Composable
internal fun ProgressLine(
    progress: Float,
    height: Dp = 6.dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safeProgress = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(safeProgress, 0f..1f)
            },
    ) {
        val stroke = size.height
        val left = stroke / 2
        val right = size.width - stroke / 2
        drawLine(
            color = trackColor,
            start = Offset(left, size.height / 2),
            end = Offset(right, size.height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val progressWidth = (right - left) * safeProgress
        val progressStart = if (layoutDirection == LayoutDirection.Rtl) right else left
        val progressEnd = if (layoutDirection == LayoutDirection.Rtl) {
            right - progressWidth
        } else {
            left + progressWidth
        }
        drawLine(
            color = progressColor,
            start = Offset(progressStart, size.height / 2),
            end = Offset(progressEnd, size.height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
