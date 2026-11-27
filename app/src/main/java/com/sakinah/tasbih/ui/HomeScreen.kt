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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Explore
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
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
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

/**
 * The day at a glance. On November 11, 2026 the user chose to put prayer and its services first:
 * the next prayer and the day's times lead, the Qibla and the calendar sit beside them,
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
    onOpenTasbih: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    /** Today's wird, for its line: how much is done and what comes next. */
    wird: com.sakinah.tasbih.wird.WirdBoard? = null,
    onOpenWird: () -> Unit = {},
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
    val phase = remember(schedule, minute) { schedule?.phase(now) }

    val arabic = remember { Locale.forLanguageTag("ar") }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", arabic).withDecimalStyle(DecimalStyle.of(arabic)) }
    val weekday = calendarText?.weekday(today.dayOfWeek)
        ?: remember(today) { DateTimeFormatter.ofPattern("EEEE", arabic).format(today) }
    val hijriDay = calendarText?.let { text ->
        hijriDayFor(today, now, day?.get(Prayer.MAGHRIB)?.toInstant(), text.settings.hijriDayFromMaghrib)
    }
    val hijri = calendarText?.let { text -> hijriDay?.let { text.hijriDate(it, numbered = true) } }
        ?.takeIf { it.isNotEmpty() } ?: remember(today) { formatter.format(HijrahDate.from(today)) }
    // What makes today itself a day apart: its occasions, and Friday's sura.
    val todayNotes = buildList {
        if (calendarText != null && hijriDay != null) {
            calendarText.calendar.fromGregorian(hijriDay)?.let { date -> Occasions.on(date).forEach { add(calendarText.occasionName(it)) } }
        }
        if (today.dayOfWeek == java.time.DayOfWeek.FRIDAY) add(stringResource(R.string.home_friday_kahf))
    }
    val gregorian = calendarText?.gregorianDate(today, numbered = true)?.takeIf { it.isNotEmpty() } ?: remember(today) { formatter.format(today) }

    val featuredOrder = if (LocalTime.now().hour in 4..15) 27 else 28
    val featured = state.catalog.collections.firstOrNull { it.order == featuredOrder }

    val header: @Composable () -> Unit = {
        HomeDates(hijri, gregorian, todayNotes, onOpenCalendar)
    }
    val prayer: @Composable () -> Unit = {
        val words = phase?.let { com.sakinah.tasbih.ui.prayer.prayerClockWords(it, now, prayerText) }
        PrayerHero(words, onOpenPrayer, onChoosePlace)
        if (day != null) DayTimes(day, words?.marked, prayerText, onOpenPrayer)
    }
    val services: @Composable () -> Unit = {
        Services(schedule, calendarText, today, prayerText, onOpenQibla, onOpenCalendar)
    }
    val remembrance: @Composable () -> Unit = {
        if (wird != null) WirdTodayRow(wird, prayerText, onOpenWird)
        when {
            state.isLoading -> ContentStatusCard(stringResource(R.string.loading_book), false, onRetry)
            state.contentLoadFailed -> ContentStatusCard(stringResource(R.string.content_load_error), true, onRetry)
            featured != null -> WirdRow(featured, state) { onOpenCollection(featured.id) }
        }
        TasbihEntryRow(state, onOpenTasbih, tag = "home_open_tasbih")
    }

    SakinahScreenBackground(showOrnament = false) {
      Column(Modifier.fillMaxSize()) {
        // The head every tab shares: the weekday, which opens the calendar, the clock and the gear.
        AnaaTopBar(
            title = weekday,
            onTitleClick = onOpenCalendar,
            titleClickLabel = stringResource(R.string.cal_open),
            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 920.dp),
            actions = {
                HomeClock(prayerText)
                // What belongs to the whole app: its look, and about it.
                SettingsGear(onOpenSettings, "home_open_settings")
            },
        )
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
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
}

/**
 * The day, the head of the home: the Hijri date large, the Gregorian under it, and what makes
 * today a day apart. It opens the calendar. The place the times are for left it on November 27,
 * 2026, at the user's wish, for the prayer tab alone, so the two no longer open alike.
 */
@Composable
private fun HomeDates(
    hijri: String,
    gregorian: String,
    notes: List<String>,
    onOpenCalendar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_open_calendar")
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cal_open), onClick = onOpenCalendar)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(hijri, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(gregorian, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        notes.forEach { note ->
            Text(note, style = MaterialTheme.typography.bodyMedium, color = LocalSakinahBrandColors.current.antiqueGold)
        }
        FlourishDivider(Modifier.padding(top = 6.dp))
    }
}

/** The time now, turning over on the minute itself rather than up to half a minute late. */
@Composable
private fun HomeClock(prayerText: PrayerText) {
    var time by remember { mutableStateOf(java.time.ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.ZonedDateTime.now()
            time = now
            delay(60_000L - (now.second * 1000L + now.nano / 1_000_000))
        }
    }
    LifecycleResumeEffect(Unit) {
        time = java.time.ZonedDateTime.now()
        onPauseOrDispose { }
    }
    Text(
        prayerText.time(time),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(4.dp).testTag("home_clock"),
    )
}

/** The next prayer and how long until it, or a way to choose a place before there are times. */
@Composable
private fun PrayerHero(
    words: com.sakinah.tasbih.ui.prayer.PrayerClockWords?,
    onOpenPrayer: () -> Unit,
    onChoosePlace: () -> Unit,
) {
    if (words == null) {
        Surface(
            shape = FeatureShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth().testTag("home_no_place"),
        ) {
            OrnamentedFeature(MaterialTheme.colorScheme.primary) {
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
    // The next adhan, or the iqama of the prayer whose adhan has come, or the quarter hour after it.
    val label = words.label
    val name = words.name
    val time = words.time
    val remaining = words.span
    val spoken = stringResource(R.string.home_next_prayer_spoken, label, name, time, remaining)
    Surface(
        onClick = onOpenPrayer,
        shape = FeatureShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_next_prayer")
            .semantics { contentDescription = spoken },
    ) {
        // A slim strip since November 27, 2026: the prayer tab holds the full card, for the prayer whose time it is.
        OrnamentedFeature(MaterialTheme.colorScheme.onPrimary) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(10.dp))
                        Text(time, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(remaining, style = MaterialTheme.typography.titleSmall)
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

/**
 * The Qibla and the calendar, each with what it would show first, as two small tiles on one
 * line. The month's table left the home on November 23, 2026, at the user's wish, to keep this
 * short; it stays a tap away under the day's times in the prayer tab.
 */
@Composable
private fun Services(
    schedule: PrayerSchedule?,
    calendarText: CalendarText?,
    today: LocalDate,
    prayerText: PrayerText,
    onOpenQibla: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val occasion = calendarText?.let { text ->
        Occasions.nextMajor(text.calendar, today)?.let { "${text.occasionName(it.occasion)} ${text.countdown(it.daysAway)}" }
    }
    val tiles = listOf(
        ServiceTile(Icons.Outlined.Explore, stringResource(R.string.qibla_title), schedule?.let { prayerText.degrees(it.qiblaBearing) }, "home_open_qibla", onOpenQibla),
        ServiceTile(Icons.Outlined.CalendarMonth, stringResource(R.string.cal_title), occasion, "home_open_calendar_tile", onOpenCalendar),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        // Side by side while each keeps room for its word and value; under each other on a narrow
        // screen or with a large font.
        if (maxWidth >= 240.dp * fontScale.coerceAtLeast(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            ) {
                tiles.forEach { tile -> ServiceChip(tile, Modifier.weight(1f).fillMaxHeight()) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.forEach { tile -> ServiceChip(tile, Modifier.fillMaxWidth()) }
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

/** A small tile: the medallion beside the word, and what it would show first under the word. */
@Composable
private fun ServiceChip(tile: ServiceTile, modifier: Modifier) {
    Surface(
        onClick = tile.onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = modifier.heightIn(min = 56.dp).testTag(tile.tag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            KhatamMedallion(tile.icon, Modifier.size(32.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(tile.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                tile.value?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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

/**
 * Today's wird in one line: how much of it is done, and the next task with its time. It opens
 * the wird, where each task is ticked; added on November 23, 2026 with the wird itself.
 */
@Composable
private fun WirdTodayRow(board: com.sakinah.tasbih.wird.WirdBoard, prayerText: PrayerText, onClick: () -> Unit) {
    val next = board.nextToDo
    val headline = when {
        board.totalToday == 0 -> stringResource(R.string.home_wird_empty)
        next == null -> stringResource(R.string.home_wird_all_done)
        else -> stringResource(R.string.wird_done_of, arabicNumber(board.doneToday), arabicNumber(board.totalToday))
    }
    val nextLine = next?.let { item ->
        stringResource(R.string.home_wird_next, listOfNotNull(item.task.title, item.time?.let(prayerText::time)).joinToString(" · "))
    }
    val openLabel = stringResource(R.string.home_wird_open)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = Modifier.fillMaxWidth().testTag("home_wird").semantics { contentDescription = listOfNotNull(openLabel, headline, nextLine).joinToString("، ") },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_wird), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(headline, style = MaterialTheme.typography.titleSmall)
                if (nextLine != null) {
                    Text(nextLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (board.totalToday > 0) ProgressLine(board.doneToday.toFloat() / board.totalToday, height = 4.dp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
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
