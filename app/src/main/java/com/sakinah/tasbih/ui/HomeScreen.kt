package com.sakinah.tasbih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.Occasions
import com.sakinah.tasbih.calendar.hijriDayFor
import com.sakinah.tasbih.data.DhikrCollection
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.data.displayArabic
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerDay
import com.sakinah.tasbih.prayer.PrayerMoment
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.prayer.PrayerText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale
import kotlinx.coroutines.delay

/** Ramadan, when the month's prayer table becomes the fasting timetable. */
private const val RAMADAN = 9

/**
 * The day at a glance. On November 11, 2026 the user chose to put prayer and its services first:
 * the next prayer and the day's times lead, the Qibla, calendar and month table sit beside them,
 * and the day's wird and the tasbih follow as one tap each.
 */
@Composable
fun HomeScreen(
    state: SakinahUiState,
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    onOpenCollection: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenPrayer: () -> Unit = {},
    onChoosePlace: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenQibla: () -> Unit = {},
    onOpenMonth: () -> Unit = {},
    onOpenTasbih: () -> Unit = {},
) {
    val context = LocalContext.current
    val prayerText = remember(context) { PrayerText(context) }
    var now by remember { mutableStateOf(Instant.now()) }
    // The next prayer changes as its time passes while the screen stays open, and the day
    // can turn while the app waits in the background.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }
    LifecycleResumeEffect(Unit) {
        now = Instant.now()
        onPauseOrDispose { }
    }
    val minute = now.epochSecond / 60
    val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
    val prayerDate = schedule?.today(now)
    val day = remember(schedule, prayerDate) { schedule?.let { s -> prayerDate?.let(s::day) } }
    val next = remember(schedule, minute) { schedule?.next(now) }

    val arabic = remember { Locale.forLanguageTag("ar") }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", arabic).withDecimalStyle(DecimalStyle.of(arabic)) }
    val weekday = calendarText?.weekday(today.dayOfWeek)
        ?: remember(today) { DateTimeFormatter.ofPattern("EEEE", arabic).format(today) }
    val hijri = calendarText?.let { text ->
        val maghrib = day?.get(Prayer.MAGHRIB)?.toInstant()
        text.hijriDate(hijriDayFor(today, now, maghrib, text.settings.hijriDayFromMaghrib))
    }?.takeIf { it.isNotEmpty() } ?: remember(today) { formatter.format(HijrahDate.from(today)) }
    val gregorian = calendarText?.gregorianDate(today)?.takeIf { it.isNotEmpty() } ?: remember(today) { formatter.format(today) }

    val featuredOrder = if (LocalTime.now().hour in 4..15) 27 else 28
    val featured = state.catalog.collections.firstOrNull { it.order == featuredOrder }

    val header: @Composable () -> Unit = {
        HomeHeader(weekday, hijri, gregorian, schedule?.place?.name, onOpenCalendar, onChoosePlace)
    }
    val prayer: @Composable () -> Unit = {
        PrayerHero(next, next?.let { prayerText.remaining(Duration.between(now, it.time.toInstant())) }, prayerText, onOpenPrayer, onChoosePlace)
        if (day != null) DayTimes(day, next, prayerText, onOpenPrayer)
    }
    val services: @Composable () -> Unit = {
        Services(schedule, calendarText, today, prayerText, onOpenQibla, onOpenCalendar, onOpenMonth, onChoosePlace)
    }
    val remembrance: @Composable () -> Unit = {
        when {
            state.isLoading -> ContentStatusCard(stringResource(R.string.loading_book), false, onRetry)
            state.contentLoadFailed -> ContentStatusCard(stringResource(R.string.content_load_error), true, onRetry)
            featured != null -> WirdRow(featured, state) { onOpenCollection(featured.id) }
        }
        TasbihEntryRow(state, onOpenTasbih, tag = "home_open_tasbih")
    }

    SakinahScreenBackground(showOrnament = false) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // A short, wide window, as a phone on its side, puts prayer beside the rest.
            val twoColumns = maxWidth >= 560.dp && maxHeight < 480.dp
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = if (twoColumns) 920.dp else 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("home_list")
                    .padding(horizontal = 20.dp, vertical = if (twoColumns) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                header()
                KhatamDivider()
                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { prayer() }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            services()
                            remembrance()
                        }
                    }
                } else {
                    prayer()
                    services()
                    remembrance()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** The day by both calendars, which opens the calendar, and the place the times are for. */
@Composable
private fun HomeHeader(
    weekday: String,
    hijri: String,
    gregorian: String,
    place: String?,
    onOpenCalendar: () -> Unit,
    onChoosePlace: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag("home_open_calendar")
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cal_open), onClick = onOpenCalendar)
                .padding(4.dp),
        ) {
            Text(
                weekday,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(hijri, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(gregorian, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (place != null) {
            TextButton(
                onClick = onChoosePlace,
                modifier = Modifier.widthIn(max = 168.dp).testTag("home_place"),
            ) {
                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(place, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The approved feature shape: rounded top-right and bottom-left corners, sharp opposite ones. */
private val HomeFeatureShape = RoundedCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 24.dp, bottomStart = 0.dp)

/** The same shape for the ruled frame inside it, its corners turned on the same centres. */
private val HomeFeatureFrameShape = RoundedCornerShape(topStart = 18.dp, topEnd = 0.dp, bottomEnd = 18.dp, bottomStart = 0.dp)

/** The lead card's ornament: a tile lattice fading in from the end edge, inside a ruled frame. */
@Composable
private fun FeatureOrnament(color: Color, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().ornamentFrame(HomeFeatureFrameShape, color.copy(alpha = 0.22f))) {
        KhatamLattice(color.copy(alpha = 0.11f), Modifier.matchParentSize())
        content()
    }
}

/** The next prayer and how long until it, or a way to choose a place before there are times. */
@Composable
private fun PrayerHero(
    next: PrayerMoment?,
    remaining: String?,
    prayerText: PrayerText,
    onOpenPrayer: () -> Unit,
    onChoosePlace: () -> Unit,
) {
    if (next == null || remaining == null) {
        Surface(
            shape = HomeFeatureShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("home_no_place"),
        ) {
            FeatureOrnament(MaterialTheme.colorScheme.primary) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.prayer_no_place_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_no_place_body), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onChoosePlace,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_choose_place"),
                    ) {
                        Text(stringResource(R.string.prayer_choose_place))
                    }
                }
            }
        }
        return
    }
    val label = stringResource(if (next.tomorrow) R.string.prayer_next_tomorrow else R.string.prayer_next)
    val name = prayerText.name(next.prayer, next.time)
    val time = prayerText.time(next.time)
    val spoken = stringResource(R.string.home_next_prayer_spoken, label, name, time, remaining)
    Surface(
        onClick = onOpenPrayer,
        shape = HomeFeatureShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_next_prayer")
            .semantics { contentDescription = spoken },
    ) {
        FeatureOrnament(MaterialTheme.colorScheme.onPrimary) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .clearAndSetSemantics { },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        modifier = Modifier.weight(1f),
                    )
                    Text(time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(remaining, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * The day's six times in one row, the next one marked. With large text or a narrow screen
 * they fold into two rows of three rather than shrink.
 */
@Composable
private fun DayTimes(day: PrayerDay, next: PrayerMoment?, prayerText: PrayerText, onOpenPrayer: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    Surface(
        onClick = onOpenPrayer,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("home_day_times"),
    ) {
        BoxWithConstraints(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            val perRow = if (maxWidth >= 300.dp * fontScale.coerceAtLeast(1f)) 6 else 3
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Prayer.entries.chunked(perRow).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { prayer ->
                            val time = day[prayer]
                            val isNext = next != null && next.prayer == prayer && next.time == time
                            TimeCell(
                                name = prayerText.name(prayer, time),
                                clock = prayerText.clock(time),
                                spoken = "${prayerText.name(prayer, time)} ${prayerText.time(time)}",
                                isNext = isNext,
                                muted = prayer == Prayer.SUNRISE,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeCell(name: String, clock: String, spoken: String, isNext: Boolean, muted: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val color = when {
        isNext -> colors.onSecondaryContainer
        muted -> colors.onSurfaceVariant
        else -> colors.onSurface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 2.dp)
            .background(if (isNext) colors.secondaryContainer else Color.Transparent, MihrabArchShape)
            // Room above the name for the arch's point, kept on every cell so the row stays level.
            .padding(top = 13.dp, bottom = 7.dp)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = if (isNext) FontWeight.SemiBold else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            clock,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (isNext) FontWeight.SemiBold else null,
            maxLines = 1,
        )
    }
}

/** The Qibla, the calendar and the month's table, each with what it would show first. */
@Composable
private fun Services(
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    today: LocalDate,
    prayerText: PrayerText,
    onOpenQibla: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenMonth: () -> Unit,
    onChoosePlace: () -> Unit,
) {
    val ramadan = calendarText?.calendar?.fromGregorian(today)?.month == RAMADAN
    val occasion = calendarText?.let { text ->
        Occasions.nextMajor(text.calendar, today)?.let { "${text.occasionName(it.occasion)} ${text.countdown(it.daysAway)}" }
    }
    val tiles = listOf(
        ServiceTile(Icons.Outlined.Explore, stringResource(R.string.qibla_title), schedule?.let { prayerText.degrees(it.qiblaBearing) }, "home_open_qibla", onOpenQibla),
        ServiceTile(Icons.Outlined.CalendarMonth, stringResource(R.string.cal_title), occasion, "home_open_calendar_tile", onOpenCalendar),
        ServiceTile(
            Icons.AutoMirrored.Outlined.ListAlt,
            stringResource(if (ramadan) R.string.prayer_month_ramadan_title else R.string.prayer_month_title),
            null,
            "home_open_month",
            if (schedule != null) onOpenMonth else onChoosePlace,
        ),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        if (maxWidth >= 330.dp * fontScale.coerceAtLeast(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                tiles.forEach { tile -> ServiceBox(tile, Modifier.weight(1f).fillMaxHeight()) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEach { tile -> ServiceRow(tile) }
            }
        }
    }
}

private data class ServiceTile(
    val icon: ImageVector,
    val title: String,
    val value: String?,
    val tag: String,
    val onClick: () -> Unit,
)

@Composable
private fun ServiceBox(tile: ServiceTile, modifier: Modifier) {
    Surface(
        onClick = tile.onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = modifier.heightIn(min = 92.dp).testTag(tile.tag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Top),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
        ) {
            KhatamMedallion(tile.icon)
            Spacer(Modifier.height(4.dp))
            Text(tile.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            tile.value?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(tile: ServiceTile) {
    Surface(
        onClick = tile.onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag(tile.tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 60.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            KhatamMedallion(tile.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tile.title, style = MaterialTheme.typography.titleMedium)
                tile.value?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The morning or evening adhkar with their progress, now one quiet row under the services. */
@Composable
private fun WirdRow(collection: DhikrCollection, state: SakinahUiState, onClick: () -> Unit) {
    val progress = state.progressFor(collection.id)
    val progressValue = progress.fraction(collection)
    val actionLabel = stringResource(when {
        progress.completed -> R.string.review_collection
        progressValue > 0f -> R.string.home_resume_wird
        else -> R.string.home_start_wird
    })
    val progressLabel = stringResource(
        R.string.home_wird_progress,
        arabicNumber(progress.completedEntries(collection)),
        arabicNumber(collection.entries.size),
    )
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().testTag("home_featured"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KhatamMark()
                    Text(stringResource(R.string.today), style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    displayArabic(collection.title, state.showDiacritics),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(progressLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("home_featured_progress"))
                    ProgressLine(progressValue, height = 4.dp, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 48.dp).testTag("home_resume_wird"),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** The tasbih with its current dhikr and total, on the home screen and atop the adhkar. */
@Composable
internal fun TasbihEntryRow(state: SakinahUiState, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    val totalLabel = stringResource(
        if (state.dailyReset.tasbihEnabled && state.dailyReset.minuteOfDay == 0) R.string.tasbih_daily_total_label
        else R.string.tasbih_current_total_label,
    )
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = modifier.fillMaxWidth().testTag(tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) {
                IslamicNavigationIcon(IslamicNavSymbol.Tasbih, selected = false)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.my_tasbih), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    displayArabic(state.selectedPhrase.text, state.showDiacritics),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    arabicNumber(state.tasbihCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("home_tasbih_total"),
                )
                Text(totalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
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
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safeProgress = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
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
