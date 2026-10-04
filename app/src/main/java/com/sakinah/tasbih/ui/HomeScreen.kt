package com.sakinah.tasbih.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.data.dhikrQuantity
import com.sakinah.tasbih.data.displayArabic
import java.time.LocalDate
import java.time.LocalTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

@Composable
fun HomeScreen(
    state: SakinahUiState,
    onOpenCollection: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val today = LocalDate.now()
    val arabicLocale = remember { Locale.forLanguageTag("ar") }
    val weekdayLabel = remember(today) {
        DateTimeFormatter.ofPattern("EEEE", arabicLocale).format(today)
    }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy", arabicLocale)
            .withDecimalStyle(DecimalStyle.of(arabicLocale))
    }
    val gregorianDateLabel = remember(today) { dateFormatter.format(today) }
    val hijriDateLabel = remember(today) { dateFormatter.format(HijrahDate.from(today)) }
    val featuredOrder = if (LocalTime.now().hour in 4..15) 27 else 28
    val featured = state.catalog.collections.firstOrNull { it.order == featuredOrder }

    SakinahScreenBackground(showOrnament = false) {
      BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 420.dp && maxWidth >= 480.dp
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .testTag("home_list"),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp),
        ) {
            item(key = "identity") {
              if (compact) {
                HomeCompactHeader(weekdayLabel, hijriDateLabel, gregorianDateLabel)
              } else {
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("home_brand"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnaaBrandLockup()
                    Spacer(Modifier.weight(1f))
                    AnaaAppIcon(Modifier.size(72.dp).testTag("home_app_icon"))
                }
              }
            }
            if (!compact) {
                item(key = "greeting") {
                    HomeDateAndGreeting(weekdayLabel, hijriDateLabel, gregorianDateLabel)
                }
            }
            when {
                state.isLoading -> item {
                    ContentStatusCard(stringResource(R.string.loading_book), false, onRetry)
                }
                state.contentLoadFailed -> item {
                    ContentStatusCard(stringResource(R.string.content_load_error), true, onRetry)
                }
                featured != null -> {
                    item(key = "featured") {
                        FeaturedDhikrCard(
                            collection = featured,
                            state = state,
                            compact = compact,
                            onClick = { onOpenCollection(featured.id) },
                        )
                    }
                    item(key = "quick-access") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.quick_access),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .semantics { heading() },
                            )
                            QuickAccessList(state, onOpenCollection)
                        }
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun HomeCompactHeader(weekday: String, hijri: String, gregorian: String) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("home_brand"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AnaaWordmark(Modifier.width(76.dp).height(54.dp))
        if (LocalDensity.current.fontScale > 1.3f) {
            Column(modifier = Modifier.weight(1f)) {
                HomeGreeting(Modifier.testTag("home_greeting"))
                Text("$weekday · $hijri", style = MaterialTheme.typography.bodySmall)
                Text(gregorian, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            HomeGreeting(Modifier.weight(1f).testTag("home_greeting"))
            HomeDate(weekday, hijri, gregorian)
        }
        AnaaAppIcon(Modifier.size(42.dp).testTag("home_app_icon"))
    }
}

@Composable
private fun HomeDateAndGreeting(weekday: String, hijri: String, gregorian: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home_greeting")) {
        if (maxWidth < 330.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeGreeting()
                HomeDate(weekday, hijri, gregorian, Modifier.fillMaxWidth())
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeGreeting(Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                HomeDate(weekday, hijri, gregorian)
            }
        }
    }
}

@Composable
private fun HomeGreeting(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.greeting),
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun HomeDate(
    weekday: String,
    hijri: String,
    gregorian: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(weekday, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(hijri, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            gregorian,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeaturedDhikrCard(
    collection: DhikrCollection,
    state: SakinahUiState,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val progress = state.progressFor(collection.id)
    val progressValue = progress.fraction(collection)
    val completedEntries = progress.completedEntries(collection)
    val actionLabel = stringResource(when {
        progress.completed -> R.string.review_collection
        progressValue > 0f -> R.string.home_resume_wird
        else -> R.string.home_start_wird
    })
    val progressLabel = stringResource(
        R.string.home_wird_progress,
        arabicNumber(completedEntries),
        arabicNumber(collection.entries.size),
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_featured")
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
      if (compact) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.today), style = MaterialTheme.typography.titleSmall)
                Text(
                    displayArabic(collection.title, state.showDiacritics),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(progressLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("home_featured_progress"))
                ProgressLine(progressValue)
            }
            HomeWirdAction(actionLabel, onClick, Modifier.width(180.dp))
        }
      } else {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(stringResource(R.string.today), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            Text(
                text = displayArabic(collection.title, state.showDiacritics),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 44.sp),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().testTag("home_featured_progress"),
            )
            Spacer(Modifier.height(8.dp))
            ProgressLine(progress = progressValue, height = 6.dp)
            Spacer(Modifier.height(18.dp))
            HomeWirdAction(actionLabel, onClick, Modifier.fillMaxWidth())
        }
      }
    }
}

@Composable
private fun HomeWirdAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp).testTag("home_resume_wird"),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun QuickAccessList(
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickItems.forEach { (collection, titleRes, symbol) ->
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("home_quick_${collection.id}"),
                onClick = { onOpenCollection(collection.id) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Row(
                    modifier = Modifier.heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IslamicHomeActionIcon(symbol, Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                        Text(
                            dhikrQuantity(collection.entries.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentStatusCard(title: String, showAction: Boolean, onAction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (showAction) {
                TextButton(onClick = onAction) { Text(stringResource(R.string.retry)) }
            }
        }
    }
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
        if (safeProgress == 0f) return@Canvas
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
