@file:OptIn(ExperimentalMaterial3Api::class)

package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.CalendarMonth
import com.sakinah.tasbih.calendar.DayCell
import com.sakinah.tasbih.calendar.HijriMonth
import com.sakinah.tasbih.calendar.Occasions
import com.sakinah.tasbih.calendar.PrimaryCalendar
import com.sakinah.tasbih.calendar.buildMonthGrid
import com.sakinah.tasbih.calendar.weekDays
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    text: CalendarText,
    today: LocalDate,
    startDate: LocalDate,
    jump: CalendarJump?,
    onSetPrimary: (PrimaryCalendar) -> Unit,
    onCorrect: (HijriMonth, Int) -> Unit,
    onOpenOccasions: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val calendar = text.calendar
    val primary = text.settings.primary
    // The day the pages stay on, so changing the leading calendar opens the month that holds it.
    var anchor by rememberSaveable(startDate) { mutableStateOf(startDate.toEpochDay()) }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }

    val (first, last) = remember(primary, calendar) { CalendarMonth.range(primary, calendar) }
    val pageCount = last.minus(first) + 1
    fun pageOf(date: LocalDate): Int =
        (CalendarMonth.containing(date, primary, calendar)?.minus(first) ?: 0).coerceIn(0, pageCount - 1)

    // A new pager for each leading calendar, opened on the month that holds the anchor.
    key(primary, startDate) {
        val pagerState = rememberPagerState(initialPage = pageOf(LocalDate.ofEpochDay(anchor))) { pageCount }
        val scope = rememberCoroutineScope()
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                first.plus(page).days(calendar).getOrNull(MIDDLE_OF_MONTH)?.let { anchor = it.toEpochDay() }
            }
        }
        LaunchedEffect(jump?.serial) {
            jump?.let { pagerState.animateScrollToPage(pageOf(it.date)) }
        }

        CalendarPage(
            title = stringResource(R.string.cal_title),
            tag = "calendar_screen",
            onBack = onBack,
            actions = {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pageOf(today)) } }) {
                    Icon(Icons.Outlined.Today, stringResource(R.string.cal_go_today))
                }
                IconButton(onClick = onOpenOccasions, modifier = Modifier.testTag("calendar_open_occasions")) {
                    Icon(Icons.Outlined.Celebration, stringResource(R.string.cal_occasions))
                }
                IconButton(onClick = onOpenConverter, modifier = Modifier.testTag("calendar_open_converter")) {
                    Icon(Icons.Outlined.SwapHoriz, stringResource(R.string.cal_converter))
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("calendar_open_settings")) {
                    Icon(Icons.Outlined.Tune, stringResource(R.string.cal_settings))
                }
            },
        ) {
            MonthContent(text, today, first, pagerState, onSetPrimary, onSelect = { selected = it }, onOpenOccasions)
        }
    }

    selected?.let { epochDay ->
        DaySheet(
            text = text,
            date = LocalDate.ofEpochDay(epochDay),
            onCorrect = onCorrect,
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun MonthContent(
    text: CalendarText,
    today: LocalDate,
    first: CalendarMonth,
    pagerState: PagerState,
    onSetPrimary: (PrimaryCalendar) -> Unit,
    onSelect: (Long) -> Unit,
    onOpenOccasions: () -> Unit,
) {
    val calendar = text.calendar
    val primary = text.settings.primary
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        CalendarChoices(
            options = PrimaryCalendar.entries,
            selected = primary,
            label = { stringResource(if (it == PrimaryCalendar.HIJRI) R.string.cal_hijri else R.string.cal_gregorian) },
            onSelect = onSetPrimary,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        val month = first.plus(pagerState.currentPage)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, stringResource(R.string.cal_previous_month), Modifier.size(18.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text.monthTitle(month),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text.monthSubtitle(month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            IconButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                enabled = pagerState.currentPage < pagerState.pageCount - 1,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, stringResource(R.string.cal_next_month), Modifier.size(18.dp))
            }
        }

        val firstDay = text.firstDayOfWeek()
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
            weekDays(firstDay).forEach { day ->
                Text(
                    text = text.weekday(day).removePrefix("ال"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Follows the reading direction, so the next month comes in from where reading goes.
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.testTag("calendar_pager")) { page ->
            val grid = remember(page, primary, calendar, firstDay) { buildMonthGrid(first.plus(page), calendar, firstDay) }
            Column {
                grid.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { cell ->
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(60.dp)) {
                                if (cell != null) {
                                    DayBox(cell, text, cell.date == today, onClick = { onSelect(cell.date.toEpochDay()) })
                                }
                            }
                        }
                    }
                }
            }
        }

        TodaySummary(text, today, onOpenOccasions)
    }
}

@Composable
private fun DayBox(cell: DayCell, text: CalendarText, isToday: Boolean, onClick: () -> Unit) {
    val hijriFirst = text.settings.primary == PrimaryCalendar.HIJRI
    val hijriDay = cell.hijri?.let { text.number(it.day) }.orEmpty()
    val gregorianDay = text.number(cell.date.dayOfMonth)
    val hasOccasion = cell.hijri?.let { Occasions.on(it, text.settings.showOtherOccasions).isNotEmpty() } == true
    val isWhiteDay = text.settings.showWhiteDays && cell.hijri?.let(Occasions::isWhiteDay) == true
    val colors = MaterialTheme.colorScheme
    val copper = LocalSakinahBrandColors.current.antiqueGold
    val description = listOfNotNull(
        text.weekday(cell.date.dayOfWeek),
        cell.hijri?.let(text::hijriDate),
        text.gregorianDate(cell.date),
    ).joinToString("، ")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isToday) colors.primary else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = if (hijriFirst) hijriDay else gregorianDay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isToday -> colors.onPrimary
                hasOccasion -> copper
                else -> colors.onSurface
            },
        )
        Text(
            text = if (hijriFirst) gregorianDay else hijriDay,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) colors.onPrimary.copy(alpha = 0.8f) else colors.onSurfaceVariant,
        )
        // Short lines, never dots: a dot beside Arabic digits reads as the digit zero.
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp).padding(top = 3.dp)) {
            if (hasOccasion) Marker(if (isToday) colors.onPrimary else copper)
            if (isWhiteDay) Marker(if (isToday) colors.onPrimary.copy(alpha = 0.7f) else colors.outline)
        }
    }
}

@Composable
private fun Marker(color: Color) {
    Box(
        Modifier
            .width(10.dp)
            .height(3.dp)
            .background(color, RoundedCornerShape(2.dp)),
    )
}

/** Today in both calendars, and how far the next occasion is. */
@Composable
private fun TodaySummary(text: CalendarText, today: LocalDate, onOpenOccasions: () -> Unit) {
    val hijri = text.calendar.fromGregorian(today)
    val todays = hijri?.let { Occasions.on(it, text.settings.showOtherOccasions) }.orEmpty()
    val next = remember(text, today) {
        Occasions.upcoming(text.calendar, today, text.settings.showOtherOccasions).firstOrNull { it.daysAway > 0 }
    }
    Surface(
        onClick = onOpenOccasions,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = sakinahCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("calendar_today_summary"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text.weekday(today.dayOfWeek), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            hijri?.let { Text(text.hijriDate(it), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            Text(text.gregorianDate(today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            todays.forEach { Text(text.occasionName(it), color = LocalSakinahBrandColors.current.antiqueGold) }
            if (next != null) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text(
                    stringResource(R.string.cal_next_occasion, text.occasionName(next.occasion), text.countdown(next.daysAway)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DaySheet(
    text: CalendarText,
    date: LocalDate,
    onCorrect: (HijriMonth, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val calendar = text.calendar
    val hijri = calendar.fromGregorian(date)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .testTag("calendar_day_sheet"),
        ) {
            Text(text.weekday(date.dayOfWeek), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            hijri?.let { Text(text.hijriDate(it), style = MaterialTheme.typography.headlineSmall) }
            Text(text.gregorianDate(date), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val occasions = hijri?.let { Occasions.on(it, text.settings.showOtherOccasions) }.orEmpty()
            occasions.forEach { occasion ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Celebration, null, tint = LocalSakinahBrandColors.current.antiqueGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text.occasionName(occasion), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (hijri != null && text.settings.showWhiteDays && Occasions.isWhiteDay(hijri)) {
                Text(stringResource(R.string.cal_white_day), style = MaterialTheme.typography.bodyMedium)
            }

            if (hijri != null) {
                val month = hijri.yearMonth
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text(stringResource(R.string.cal_correct_title, text.hijriMonthYear(month)), style = MaterialTheme.typography.titleSmall)
                CalendarChoices(
                    options = listOf(-1, 0, 1),
                    selected = calendar.correctionOf(month),
                    label = { days ->
                        stringResource(
                            when (days) {
                                -1 -> R.string.cal_correct_earlier
                                1 -> R.string.cal_correct_later
                                else -> R.string.cal_correct_computed
                            },
                        )
                    },
                    onSelect = { onCorrect(month, it) },
                    enabled = { calendar.canCorrect(month, it) },
                    modifier = Modifier.testTag("calendar_correction"),
                )
                CalendarHint(stringResource(R.string.cal_correct_hint))
            }
        }
    }
}

/** A day near the middle of a month stands for it, safely away from both of its edges. */
private const val MIDDLE_OF_MONTH = 14
