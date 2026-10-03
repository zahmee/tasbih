package com.sakinah.tasbih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.data.dhikrQuantity
import com.sakinah.tasbih.data.dayQuantity
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ActivityAnalytics
import com.sakinah.tasbih.data.ActivityEvent
import com.sakinah.tasbih.data.ActivityKinds
import com.sakinah.tasbih.data.DailyActivity
import com.sakinah.tasbih.data.HourlyActivityTotal
import com.sakinah.tasbih.data.PeriodActivity
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ArabicLocale = Locale.forLanguageTag("ar-SA")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    state: SakinahUiState,
    onBack: () -> Unit,
) {
    val analytics = state.activityAnalytics
    var expandedSections by rememberSaveable { mutableStateOf(emptyList<String>()) }
    SakinahScreenBackground {
        Scaffold(
            modifier = Modifier.testTag("achievements_screen"),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
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
                    item { ActivityOverview(analytics) }
                    item { WeeklyActivityCard(analytics) }
                    item { SakinahSectionHeader(stringResource(R.string.activity_more_details)) }
                    listOf(
                        "sources" to R.string.activity_sources, "hours" to R.string.activity_hours,
                        "trends" to R.string.activity_trends, "calendar" to R.string.activity_calendar_details,
                        "history" to R.string.activity_history,
                    ).forEach { (id, label) ->
                        item(key = "section_$id") {
                            ActivitySectionToggle(stringResource(label), id in expandedSections, id) {
                                expandedSections = if (id in expandedSections) expandedSections - id else expandedSections + id
                            }
                        }
                        if (id in expandedSections) {
                            when (id) {
                                "sources" -> { item { SummaryMetrics(analytics) }; item { SourceStatisticsSection(analytics) } }
                                "hours" -> item { HourlyActivityCard(analytics) }
                                "trends" -> item { LongTermActivityCard(analytics) }
                                "calendar" -> item { ActivityCalendarCard(analytics) }
                                "history" -> {
                                    if (analytics.recent.isEmpty()) item { EmptyActivityCard() }
                                    else {
                                        val timeline = aggregateTimeline(analytics.recent)
                                        items(count = timeline.size, key = { timeline[it].key }) { index -> TimelineCard(timeline[index]) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityOverview(analytics: ActivityAnalytics) {
    val today = LocalDate.now()
    val todayCount = analytics.activityFor(today)?.totalCount ?: 0
    val weekCount = (0L..6L).sumOf { analytics.activityFor(today.minusDays(it))?.totalCount ?: 0 }
    Column(Modifier.fillMaxWidth().testTag("activity_overview"), verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Medium)) {
        Text(stringResource(R.string.activity_overview), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(SakinahSpacing.Section)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.activity_today), style = MaterialTheme.typography.bodyMedium)
                Text(formatNumber(todayCount), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.activity_week), style = MaterialTheme.typography.bodyMedium)
                Text(formatNumber(weekCount), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (analytics.totals.totalCount == 0) Text(stringResource(R.string.empty_activity_overview),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActivitySectionToggle(title: String, expanded: Boolean, id: String, onClick: () -> Unit) {
    Column {
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("activity_expand_$id")
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = if (expanded) "مفتوح" else "مغلق" }
            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
    }
}

@Composable
private fun ChartValues(tag: String, values: List<String>) {
    var visible by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { visible = !visible }, modifier = Modifier.testTag("${tag}_values")) {
        Text(stringResource(if (visible) R.string.hide_chart_values else R.string.show_chart_values))
    }
    if (visible) Column(verticalArrangement = Arrangement.spacedBy(SakinahSpacing.Small), modifier = Modifier.testTag("${tag}_value_list")) {
        values.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun SummaryMetrics(analytics: ActivityAnalytics) {
    val currentStreak = analytics.currentStreak()
    val longestStreak = analytics.longestStreak()
    Column(
        modifier = Modifier.testTag("source_statistics"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                value = dayQuantity(currentStreak),
                label = stringResource(R.string.current_streak),
                accent = MaterialTheme.colorScheme.secondaryContainer,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                value = dayQuantity(analytics.totals.activeDays),
                label = stringResource(R.string.active_days),
                accent = MaterialTheme.colorScheme.primaryContainer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                value = dayQuantity(longestStreak),
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
    Column(modifier.padding(vertical = 8.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                }
                BeadMark()
            }
            Spacer(Modifier.height(18.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            val barWidth = (maxWidth / 7).coerceAtLeast((38 * LocalDensity.current.fontScale).dp)
            val weekScroll = rememberScrollState()
            androidx.compose.runtime.LaunchedEffect(weekScroll.maxValue) { weekScroll.scrollTo(weekScroll.maxValue) }
            Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(weekScroll)
                    .heightIn(min = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                days.forEach { (date, value) ->
                    val spoken = stringResource(R.string.weekly_day_description,
                        date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ArabicLocale)), value)
                    Column(
                        modifier = Modifier
                            .width(barWidth)
                            .height(160.dp)
                            .clearAndSetSemantics { contentDescription = spoken },
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
                            shortArabicWeekday(date.dayOfWeek),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            if (weekScroll.maxValue > 0) Text(stringResource(R.string.scroll_chart_hint),
                style = MaterialTheme.typography.labelSmall)
            }
            }
            Spacer(Modifier.height(12.dp))
            Text(encouragement, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChartValues("weekly", days.map { (date, count) ->
                stringResource(R.string.weekly_day_description,
                    date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ArabicLocale)), count)
            })
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
private fun SourceStatisticsSection(analytics: ActivityAnalytics) {
    val today = LocalDate.now()
    val todaySummary = remember(analytics.daily, today) {
        analytics.summaryBetween(today, today)
    }
    val monthSummary = remember(analytics.daily, today) {
        analytics.summaryBetween(today.withDayOfMonth(1), today)
    }
    val tasbihActiveDays = remember(analytics.daily) {
        analytics.daily.count { it.tasbihCount > 0 }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 3.dp)) {
            Text(stringResource(R.string.source_statistics_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.source_statistics_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SourceSummaryCard(
            kind = ActivityKinds.Reader,
            title = stringResource(R.string.dhikr_statistics_title),
            caption = stringResource(R.string.dhikr_statistics_caption),
            total = analytics.totals.readerCount,
            today = todaySummary.readerCount,
            month = monthSummary.readerCount,
            thirdValue = analytics.totals.completions,
            thirdLabel = stringResource(R.string.completed_sessions),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        )
        SourceSummaryCard(
            kind = ActivityKinds.Tasbih,
            title = stringResource(R.string.tasbih_activity),
            caption = stringResource(R.string.tasbih_statistics_caption),
            total = analytics.totals.tasbihCount,
            today = todaySummary.tasbihCount,
            month = monthSummary.tasbihCount,
            thirdValue = tasbihActiveDays,
            thirdLabel = stringResource(R.string.source_active_days),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    }
}

@Composable
private fun SourceSummaryCard(
    kind: String,
    title: String,
    caption: String,
    total: Int,
    today: Int,
    month: Int,
    thirdValue: Int,
    thirdLabel: String,
    containerColor: Color,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            stringResource(R.string.since_beginning) to total,
            stringResource(R.string.today_short) to today,
            stringResource(R.string.this_month_short) to month,
            thirdLabel to thirdValue,
        ).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(formatNumber(value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private enum class ActivityRange {
    Today,
    Month,
    Year,
    All,
}

@Composable
private fun ActivityRange.label(): String = stringResource(
    when (this) {
        ActivityRange.Today -> R.string.range_today
        ActivityRange.Month -> R.string.range_month
        ActivityRange.Year -> R.string.range_year
        ActivityRange.All -> R.string.range_all
    },
)

private fun ActivityRange.bounds(today: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
    ActivityRange.Today -> today to today
    ActivityRange.Month -> today.withDayOfMonth(1) to today
    ActivityRange.Year -> today.withDayOfYear(1) to today
    ActivityRange.All -> null to null
}

@Composable
private fun HourlyActivityCard(analytics: ActivityAnalytics) {
    var selectedRangeKey by rememberSaveable { mutableStateOf(ActivityRange.Today.name) }
    val selectedRange = runCatching { ActivityRange.valueOf(selectedRangeKey) }
        .getOrDefault(ActivityRange.Today)
    val today = LocalDate.now()
    val bounds = selectedRange.bounds(today)
    val hours = remember(analytics.hourly, selectedRange, today) {
        analytics.hourlyTotalsBetween(bounds.first, bounds.second)
    }
    val total = hours.sumOf(HourlyActivityTotal::totalCount)
    val dayCount = hours.filter { it.hourOfDay in 6..17 }.sumOf(HourlyActivityTotal::totalCount)
    val nightCount = total - dayCount
    val peak = hours.maxByOrNull(HourlyActivityTotal::totalCount)
        ?.takeIf { it.totalCount > 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hourly_activity"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.hourly_activity_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.hourly_activity_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DayNightOrb()
            }
            Spacer(Modifier.height(15.dp))
            ActivityRangeSelector(
                selected = selectedRange,
                onSelected = { selectedRangeKey = it.name },
            )
            Spacer(Modifier.height(18.dp))
            HourlyBarsChart(hours)
            HourAxisLabels()
            ChartValues("hourly", hours.map { stringResource(R.string.hour_value_description, formatHourLabel(it.hourOfDay), it.totalCount, it.tasbihCount, it.readerCount) })
            Spacer(Modifier.height(12.dp))
            Text(
                peak?.let { stringResource(R.string.busiest_hour, formatHourLabel(it.hourOfDay)) }
                    ?: stringResource(R.string.no_hourly_activity),
                style = MaterialTheme.typography.bodyMedium,
                color = if (peak == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DayNightMetric(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.day_period),
                    hours = stringResource(R.string.day_period_hours),
                    count = dayCount,
                    total = total,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                DayNightMetric(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.night_period),
                    hours = stringResource(R.string.night_period_hours),
                    count = nightCount,
                    total = total,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ActivityRangeSelector(selected: ActivityRange, onSelected: (ActivityRange) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SakinahSpacing.Small), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActivityRange.entries.forEach { range ->
            FilterChip(selected = range == selected, onClick = { onSelected(range) },
                label = { Text(range.label()) }, modifier = Modifier.testTag("hour_range_${range.name}"))
        }
    }
}

@Composable
private fun HourlyBarsChart(hours: List<HourlyActivityTotal>) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val spoken = hours.joinToString("، ") { "${formatHourLabel(it.hourOfDay)}: ${formatNumber(it.totalCount)}" }
    val nightColor = MaterialTheme.colorScheme.primary
    val dayColor = LocalSakinahBrandColors.current.antiqueGold
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = hours.maxOfOrNull(HourlyActivityTotal::totalCount)?.coerceAtLeast(1) ?: 1
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp).semantics { contentDescription = spoken },
    ) {
        val baseline = size.height - 8.dp.toPx()
        val topPadding = 10.dp.toPx()
        val availableHeight = baseline - topPadding
        val slotWidth = size.width / 24f
        drawRoundRect(
            color = dayColor.copy(alpha = 0.045f),
            topLeft = Offset(slotWidth * 6f, topPadding / 2f),
            size = Size(slotWidth * 12f, baseline - topPadding / 2f),
            cornerRadius = CornerRadius(12.dp.toPx()),
        )
        repeat(3) { index ->
            val y = topPadding + availableHeight * (index + 1) / 4f
            drawLine(
                color = trackColor.copy(alpha = 0.22f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawLine(
            color = trackColor.copy(alpha = 0.7f),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        hours.forEach { hour ->
            val x = size.width * hourPositionFraction(hour.hourOfDay, rtl)
            val isDay = hour.hourOfDay in 6..17
            val color = if (isDay) dayColor else nightColor
            val ratio = hour.totalCount.toFloat() / maxValue
            val barHeight = if (hour.totalCount == 0) 2.dp.toPx() else {
                (availableHeight * ratio).coerceAtLeast(7.dp.toPx())
            }
            drawLine(
                color = color.copy(alpha = if (hour.totalCount == 0) 0.2f else 0.92f),
                start = Offset(x, baseline),
                end = Offset(x, baseline - barHeight),
                strokeWidth = (slotWidth * 0.48f).coerceIn(3.dp.toPx(), 9.dp.toPx()),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun HourAxisLabels() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(0, 6, 12, 18, 23).forEach { hour ->
            Text(formatNumber(hour), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayNightMetric(
    modifier: Modifier,
    title: String,
    hours: String,
    count: Int,
    total: Int,
    containerColor: Color,
) {
    val contentColor = MaterialTheme.colorScheme.contentColorFor(containerColor)
    val percentage = if (total == 0) 0 else (count * 100f / total).roundToInt()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                hours,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                stringResource(R.string.period_count_percentage, count, percentage),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun DayNightOrb() {
    val gold = LocalSakinahBrandColors.current.antiqueGold
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.size(48.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(primary.copy(alpha = 0.13f), radius = size.minDimension * 0.48f, center = center)
        drawCircle(gold, radius = size.minDimension * 0.2f, center = Offset(center.x - 5.dp.toPx(), center.y))
        drawCircle(
            surface,
            radius = size.minDimension * 0.18f,
            center = Offset(center.x + 1.dp.toPx(), center.y - 2.dp.toPx()),
        )
        repeat(6) { index ->
            val angle = index * PI / 3
            drawCircle(
                color = gold,
                radius = 1.5.dp.toPx(),
                center = Offset(
                    center.x + (cos(angle) * size.minDimension * 0.38f).toFloat(),
                    center.y + (sin(angle) * size.minDimension * 0.38f).toFloat(),
                ),
            )
        }
    }
}

private enum class TrendMode {
    Monthly,
    Yearly,
}

@Composable
private fun LongTermActivityCard(analytics: ActivityAnalytics) {
    val currentYear = Year.now().value
    val currentMonth = YearMonth.now()
    val earliestYear = analytics.daily.firstOrNull { it.totalCount > 0 }
        ?.dayKey
        ?.take(4)
        ?.toIntOrNull()
        ?: currentYear
    var selectedYear by rememberSaveable { mutableStateOf(currentYear) }
    var modeKey by rememberSaveable { mutableStateOf(TrendMode.Monthly.name) }
    val mode = runCatching { TrendMode.valueOf(modeKey) }.getOrDefault(TrendMode.Monthly)
    val monthlyByKey = remember(analytics.daily) {
        analytics.monthlyActivity().associateBy(PeriodActivity::periodKey)
    }
    val yearlyPeriods = remember(analytics.daily, currentYear) {
        analytics.yearlyActivity().ifEmpty {
            listOf(PeriodActivity(currentYear.toString(), 0, 0, 0, 0, 0))
        }
    }
    val monthPeriods = remember(monthlyByKey, selectedYear) {
        (1..12).map { month ->
            val key = YearMonth.of(selectedYear, month).toString()
            monthlyByKey[key] ?: PeriodActivity(key, 0, 0, 0, 0, 0)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("long_term_activity"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.long_term_activity_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.long_term_activity_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            TrendModeSelector(
                selected = mode,
                onSelected = { modeKey = it.name },
            )
            Spacer(Modifier.height(15.dp))
            if (mode == TrendMode.Monthly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        enabled = selectedYear > earliestYear,
                        onClick = { selectedYear -= 1 },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.previous_year),
                        )
                    }
                    Text(
                        stringResource(
                            R.string.year_activity_total,
                            monthPeriods.sumOf(PeriodActivity::totalCount),
                            selectedYear,
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(
                        enabled = selectedYear < currentYear,
                        onClick = { selectedYear += 1 },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = stringResource(R.string.next_year),
                        )
                    }
                }
                PeriodBarChart(
                    periods = monthPeriods,
                    labels = monthPeriods.map { YearMonth.parse(it.periodKey).monthValue.toString() },
                    highlightIndex = if (selectedYear == currentYear) currentMonth.monthValue - 1 else -1,
                )
                Text(
                    stringResource(R.string.months_axis_hint),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(
                        R.string.years_activity_total,
                        yearlyPeriods.sumOf(PeriodActivity::totalCount),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                PeriodBarChart(
                    periods = yearlyPeriods,
                    labels = yearlyPeriods.map(PeriodActivity::periodKey),
                    highlightIndex = yearlyPeriods.indexOfFirst { it.periodKey == currentYear.toString() },
                )
            }
        }
    }
}

@Composable
private fun TrendModeSelector(
    selected: TrendMode,
    onSelected: (TrendMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TrendMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trend_mode_${mode.name}")
                        .clip(MaterialTheme.shapes.large)
                        .semantics { this.selected = isSelected }
                        .clickable { onSelected(mode) },
                    shape = MaterialTheme.shapes.large,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Text(
                        stringResource(
                            if (mode == TrendMode.Monthly) R.string.monthly_view else R.string.yearly_view,
                        ),
                        modifier = Modifier.padding(vertical = 9.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodBarChart(
    periods: List<PeriodActivity>,
    labels: List<String>,
    highlightIndex: Int,
) {
    val maxValue = periods.maxOfOrNull(PeriodActivity::totalCount)?.coerceAtLeast(1) ?: 1
    Column {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .height(180.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        periods.forEachIndexed { index, period ->
            val highlighted = index == highlightIndex
            val barColor = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    if (period.totalCount > 0) formatNumber(period.totalCount) else "·",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(if (periods.size > 8) 12.dp else 22.dp)
                        .height(
                            if (period.totalCount == 0) 7.dp else {
                                (12 + 78f * period.totalCount / maxValue).dp
                            },
                        )
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = if (period.totalCount == 0) 0.2f else 0.82f)),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    periodLabel(period.periodKey),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
    ChartValues("period", periods.map { "${periodLabel(it.periodKey)}: ${formatNumber(it.totalCount)}" })
    }
}

private fun formatHourLabel(hour: Int): String = LocalTime.of(hour.coerceIn(0, 23), 0)
    .format(DateTimeFormatter.ofPattern("h a", ArabicLocale))

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
    var listViewOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }

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
            BoxWithConstraints {
            val listView = listViewOverride ?: (maxWidth < 320.dp && LocalDensity.current.fontScale > 1.3f)
            Column {
            TextButton(onClick = { listViewOverride = !listView }, modifier = Modifier.testTag("calendar_view_toggle")) {
                Text(stringResource(if (listView) R.string.calendar_show_grid else R.string.calendar_show_list))
            }
            Column(Modifier.testTag("activity_calendar_grid")) {
            if (listView) {
                for (day in 1..month.lengthOfMonth()) {
                    val date = month.atDay(day)
                    CalendarDay(
                        modifier = Modifier.fillMaxWidth(), date = date,
                        count = monthActivities[date.toString()]?.totalCount ?: 0, maxCount = maxInMonth,
                        selected = date == selectedDate, enabled = !date.isAfter(LocalDate.now()),
                        onClick = { selectedDayKey = date.toString() }, listLayout = true,
                    )
                }
            } else {
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
    listLayout: Boolean = false,
) {
    val (color, contentColor) = calendarCellColors(MaterialTheme.colorScheme, count, maxCount, enabled)
    val selectionColor = MaterialTheme.colorScheme.secondary
    val spokenDate = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ArabicLocale))
    val spokenCount = dhikrQuantity(count)
    val selectionState = stringResource(
        if (selected) R.string.calendar_day_selected else R.string.calendar_day_not_selected,
    )
    Box(
        modifier = modifier
            .padding(2.dp)
            .heightIn(min = 64.dp)
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
        Column(
            modifier = if (listLayout) Modifier.fillMaxWidth().padding(12.dp) else Modifier,
            horizontalAlignment = if (listLayout) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Text(if (listLayout) spokenDate else formatNumber(date.dayOfMonth),
                style = if (listLayout) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.labelLarge, color = contentColor)
            if (count > 0 || listLayout) {
                Text(
                    if (listLayout) spokenCount else formatNumber(count),
                    fontSize = if (listLayout) 12.sp else 11.sp,
                    lineHeight = if (listLayout) 20.sp else 16.sp,
                    color = contentColor,
                )
            }
        }
        if (selected) {
            Canvas(Modifier.matchParentSize().padding(2.dp)) {
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

private fun formatNumber(value: Int): String = com.sakinah.tasbih.data.arabicNumber(value)

private fun periodLabel(key: String): String = if (key.length == 7) {
    YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM", ArabicLocale))
} else key
