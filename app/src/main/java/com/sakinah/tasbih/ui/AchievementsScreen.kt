package com.sakinah.tasbih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ActivityAnalytics
import com.sakinah.tasbih.data.ActivityEvent
import com.sakinah.tasbih.data.ActivityKinds
import com.sakinah.tasbih.data.DailyActivity
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ArabicLocale = Locale.forLanguageTag("ar-SA")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
) {
    val analytics = state.activityAnalytics
    SakinahScreenBackground {
        Scaffold(
            modifier = Modifier.testTag("achievements_screen"),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
                    ),
                    title = { Text(stringResource(R.string.achievements_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.back),
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
                        .testTag("achievements_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 20.dp,
                        bottom = 28.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { AchievementHero(analytics) }
                    item { SummaryMetrics(analytics) }
                    item { WeeklyActivityCard(analytics) }
                    item { ActivityDistributionCard(analytics) }
                    item { ActivityCalendarCard(analytics) }
                    item { RecentActivityHeader() }
                    if (analytics.recent.isEmpty()) {
                        item { EmptyActivityCard() }
                    } else {
                        val timeline = aggregateTimeline(analytics.recent)
                        items(
                            count = timeline.size,
                            key = { timeline[it].key },
                        ) { index ->
                            TimelineCard(timeline[index])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementHero(analytics: ActivityAnalytics) {
    val brand = LocalSakinahBrandColors.current
    val gradientStart = brand.heroStart
    val gradientEnd = brand.heroEnd
    val onPrimary = brand.onHero
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(92.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val path = Path()
            repeat(16) { index ->
                val angle = -PI / 2 + index * PI / 8
                val radius = if (index % 2 == 0) size.minDimension * 0.43f else size.minDimension * 0.22f
                val point = Offset(
                    center.x + (cos(angle) * radius).toFloat(),
                    center.y + (sin(angle) * radius).toFloat(),
                )
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(path, onPrimary.copy(alpha = 0.08f))
            drawPath(path, onPrimary.copy(alpha = 0.2f), style = Stroke(1.2.dp.toPx()))
        }
        Column(modifier = Modifier.fillMaxWidth(0.78f)) {
            Text(
                stringResource(R.string.achievements_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = onPrimary.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatNumber(analytics.totals.totalCount),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = onPrimary,
            )
            Text(
                stringResource(R.string.lifetime_total),
                style = MaterialTheme.typography.titleMedium,
                color = onPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.achievements_message),
                style = MaterialTheme.typography.bodyMedium,
                color = onPrimary.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun SummaryMetrics(analytics: ActivityAnalytics) {
    val currentStreak = analytics.currentStreak()
    val longestStreak = analytics.longestStreak()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.days_value, currentStreak),
                label = stringResource(R.string.current_streak),
                accent = MaterialTheme.colorScheme.secondaryContainer,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.days_value, analytics.totals.activeDays),
                label = stringResource(R.string.active_days),
                accent = MaterialTheme.colorScheme.primaryContainer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.days_value, longestStreak),
                label = stringResource(R.string.longest_streak),
                accent = MaterialTheme.colorScheme.surfaceVariant,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                value = formatNumber(analytics.totals.completions),
                label = stringResource(R.string.completed_sessions),
                accent = MaterialTheme.colorScheme.secondaryContainer,
            )
        }
    }
}

@Composable
private fun MetricCard(modifier: Modifier, value: String, label: String, accent: Color) {
    val contentColor = MaterialTheme.colorScheme.contentColorFor(accent)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = accent,
        contentColor = contentColor,
        border = sakinahCardBorder(0.12f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = contentColor)
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.74f))
        }
    }
}

@Composable
private fun WeeklyActivityCard(analytics: ActivityAnalytics) {
    val today = LocalDate.now()
    val days = remember(analytics.daily, today) {
        (6L downTo 0L).map { offset ->
            val date = today.minusDays(offset)
            date to (analytics.activityFor(date)?.totalCount ?: 0)
        }
    }
    val maxValue = days.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val weekTotal = days.sumOf { it.second }
    val encouragement = when {
        weekTotal == 0 -> stringResource(R.string.weekly_gentle_start)
        days.count { it.second > 0 } >= 5 -> stringResource(R.string.weekly_strong)
        else -> stringResource(R.string.weekly_keep_going)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.this_week), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.week_total, weekTotal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                BeadMark()
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                days.forEach { (date, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            if (value > 0) formatNumber(value) else "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((12 + 72f * value / maxValue).dp)
                                .clip(CircleShape)
                                .background(
                                    if (date == today) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                                ),
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EE", ArabicLocale)).take(1),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            ) {
                Text(
                    encouragement,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun BeadMark() {
    val gold = LocalSakinahBrandColors.current.antiqueGold
    Canvas(modifier = Modifier.size(42.dp)) {
        repeat(8) { index ->
            val angle = index * PI / 4
            drawCircle(
                color = gold,
                radius = 2.8.dp.toPx(),
                center = Offset(
                    size.width / 2 + (cos(angle) * 13.dp.toPx()).toFloat(),
                    size.height / 2 + (sin(angle) * 13.dp.toPx()).toFloat(),
                ),
            )
        }
    }
}

@Composable
private fun ActivityDistributionCard(analytics: ActivityAnalytics) {
    val tasbih = analytics.totals.tasbihCount
    val reader = analytics.totals.readerCount
    val total = (tasbih + reader).coerceAtLeast(1)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.activity_distribution), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(126.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 15.dp.toPx()
                        drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                        if (tasbih + reader > 0) {
                            val tasbihSweep = 360f * tasbih / total
                            drawArc(primary, -90f, tasbihSweep, false, style = Stroke(stroke, cap = StrokeCap.Round))
                            if (reader > 0) {
                                drawArc(
                                    secondary,
                                    -90f + tasbihSweep + 3f,
                                    360f * reader / total - 3f,
                                    false,
                                    style = Stroke(stroke, cap = StrokeCap.Round),
                                )
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatNumber(tasbih + reader), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.lifetime_total), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.width(22.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DistributionLegend(primary, stringResource(R.string.tasbih_activity), tasbih)
                    DistributionLegend(secondary, stringResource(R.string.hisn_activity), reader)
                }
            }
        }
    }
}

@Composable
private fun DistributionLegend(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(formatNumber(value), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ActivityCalendarCard(analytics: ActivityAnalytics) {
    val currentMonth = YearMonth.now()
    val earliestMonth = analytics.daily.firstOrNull { it.totalCount > 0 }
        ?.let { runCatching { YearMonth.from(LocalDate.parse(it.dayKey)) }.getOrNull() }
        ?: currentMonth
    var monthKey by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    var selectedDayKey by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = runCatching { YearMonth.parse(monthKey) }.getOrDefault(currentMonth)
    val selectedDate = runCatching { LocalDate.parse(selectedDayKey) }.getOrDefault(LocalDate.now())
    val selectedActivity = analytics.activityFor(selectedDate) ?: DailyActivity(selectedDayKey, 0, 0, 0, 0)
    val monthActivities = remember(analytics.daily, month) {
        analytics.daily.filter { it.dayKey.startsWith(month.toString()) }.associateBy { it.dayKey }
    }
    val maxInMonth = monthActivities.values.maxOfOrNull { it.totalCount }?.coerceAtLeast(1) ?: 1
    val firstDayOffset = (month.atDay(1).dayOfWeek.value + 1) % 7
    val slots = (((firstDayOffset + month.lengthOfMonth()) + 6) / 7) * 7

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_calendar"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(stringResource(R.string.activity_calendar), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.calendar_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    enabled = month > earliestMonth,
                    onClick = {
                        monthKey = month.minusMonths(1).toString()
                        selectedDayKey = month.minusMonths(1).atDay(1).toString()
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.previous_month),
                    )
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", ArabicLocale)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(
                    enabled = month < currentMonth,
                    onClick = {
                        monthKey = month.plusMonths(1).toString()
                        selectedDayKey = month.plusMonths(1).atDay(1).toString()
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = stringResource(R.string.next_month),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    R.string.weekday_sat,
                    R.string.weekday_sun,
                    R.string.weekday_mon,
                    R.string.weekday_tue,
                    R.string.weekday_wed,
                    R.string.weekday_thu,
                    R.string.weekday_fri,
                ).forEach { label ->
                    Text(
                        stringResource(label),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            repeat(slots / 7) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val slot = week * 7 + weekday
                        val dayNumber = slot - firstDayOffset + 1
                        if (dayNumber !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).aspectRatio(0.86f))
                        } else {
                            val date = month.atDay(dayNumber)
                            val activity = monthActivities[date.toString()]
                            CalendarDay(
                                modifier = Modifier.weight(1f),
                                date = date,
                                count = activity?.totalCount ?: 0,
                                maxCount = maxInMonth,
                                selected = date == selectedDate,
                                enabled = !date.isAfter(LocalDate.now()),
                                onClick = { selectedDayKey = date.toString() },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SelectedDayDetails(selectedDate, selectedActivity)
        }
    }
}

@Composable
private fun CalendarDay(
    modifier: Modifier,
    date: LocalDate,
    count: Int,
    maxCount: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val intensity = if (count == 0) 0f else (0.2f + 0.8f * count / maxCount).coerceIn(0.2f, 1f)
    val color = when {
        !enabled -> MaterialTheme.colorScheme.surface
        count > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = intensity)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentColor = if (count > 0 && intensity > 0.55f) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val selectionColor = MaterialTheme.colorScheme.secondary
    val spokenDate = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ArabicLocale))
    val spokenCount = stringResource(R.string.count_value, count)
    val selectionState = stringResource(
        if (selected) R.string.calendar_day_selected else R.string.calendar_day_not_selected,
    )
    Box(
        modifier = modifier
            .padding(2.5.dp)
            .aspectRatio(0.86f)
            .sizeIn(minHeight = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .semantics(mergeDescendants = true) {
                contentDescription = "$spokenDate، $spokenCount"
                this.selected = selected
                stateDescription = selectionState
            }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatNumber(date.dayOfMonth), style = MaterialTheme.typography.labelLarge, color = contentColor)
            if (count > 0) {
                Text(
                    formatNumber(count),
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = contentColor.copy(alpha = 0.84f),
                )
            }
        }
        if (selected) {
            Canvas(Modifier.fillMaxSize().padding(2.dp)) {
                drawRoundRect(
                    color = selectionColor,
                    style = Stroke(1.8.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun SelectedDayDetails(date: LocalDate, activity: DailyActivity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", ArabicLocale)),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(
                    R.string.day_details_format,
                    activity.totalCount,
                    activity.tasbihCount,
                    activity.readerCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (activity.completions > 0) {
                Text(
                    stringResource(R.string.day_completions_format, activity.completions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun RecentActivityHeader() {
    Column(modifier = Modifier.padding(top = 4.dp, start = 3.dp, end = 3.dp)) {
        Text(stringResource(R.string.recent_activity), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.recent_activity_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class TimelineItem(
    val key: String,
    val kind: String,
    val title: String,
    val amount: Int,
    val timestampMillis: Long,
)

private fun aggregateTimeline(events: List<ActivityEvent>): List<TimelineItem> {
    val grouped = linkedMapOf<String, TimelineItem>()
    events.forEach { event ->
        val minute = event.timestampMillis / 60_000
        val key = "${event.kind}|${event.sourceId}|$minute"
        val existing = grouped[key]
        grouped[key] = if (existing == null) {
            TimelineItem(key, event.kind, event.title, event.amount.coerceAtLeast(1), event.timestampMillis)
        } else {
            existing.copy(amount = existing.amount + event.amount.coerceAtLeast(1))
        }
    }
    return grouped.values.take(30)
}

@Composable
private fun TimelineCard(item: TimelineItem) {
    val typeTitle = when (item.kind) {
        ActivityKinds.Tasbih -> stringResource(R.string.activity_tasbih)
        ActivityKinds.Reader -> stringResource(R.string.activity_reader)
        else -> stringResource(R.string.activity_completion)
    }
    val timestamp = Instant.ofEpochMilli(item.timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMMM، h:mm a", ArabicLocale))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityGlyph(item.kind)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(typeTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(timestamp, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.kind != ActivityKinds.Completion) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        "+${formatNumber(item.amount)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityGlyph(kind: String) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(44.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Canvas(Modifier.padding(9.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            when (kind) {
                ActivityKinds.Tasbih -> {
                    repeat(8) { index ->
                        val angle = index * PI / 4
                        drawCircle(
                            color,
                            radius = 1.8.dp.toPx(),
                            center = Offset(
                                center.x + (cos(angle) * size.minDimension * 0.36f).toFloat(),
                                center.y + (sin(angle) * size.minDimension * 0.36f).toFloat(),
                            ),
                        )
                    }
                }

                ActivityKinds.Reader -> {
                    val stroke = 1.7.dp.toPx()
                    drawLine(color, Offset(center.x, size.height * 0.25f), Offset(center.x, size.height * 0.83f), stroke)
                    drawLine(color, Offset(center.x, size.height * 0.3f), Offset(size.width * 0.12f, size.height * 0.18f), stroke)
                    drawLine(color, Offset(size.width * 0.12f, size.height * 0.18f), Offset(size.width * 0.12f, size.height * 0.72f), stroke)
                    drawLine(color, Offset(size.width * 0.12f, size.height * 0.72f), Offset(center.x, size.height * 0.83f), stroke)
                    drawLine(color, Offset(center.x, size.height * 0.3f), Offset(size.width * 0.88f, size.height * 0.18f), stroke)
                    drawLine(color, Offset(size.width * 0.88f, size.height * 0.18f), Offset(size.width * 0.88f, size.height * 0.72f), stroke)
                    drawLine(color, Offset(size.width * 0.88f, size.height * 0.72f), Offset(center.x, size.height * 0.83f), stroke)
                }

                else -> {
                    drawCircle(color, size.minDimension * 0.38f, center, style = Stroke(1.8.dp.toPx()))
                    drawLine(color, Offset(size.width * 0.28f, center.y), Offset(size.width * 0.44f, size.height * 0.67f), 1.8.dp.toPx(), StrokeCap.Round)
                    drawLine(color, Offset(size.width * 0.44f, size.height * 0.67f), Offset(size.width * 0.74f, size.height * 0.32f), 1.8.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun EmptyActivityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BeadMark()
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.no_activity_yet), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.no_activity_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatNumber(value: Int): String = value.toString()
