@file:OptIn(ExperimentalMaterial3Api::class)

package com.sakinah.tasbih.ui.calendar

import com.sakinah.tasbih.ui.KhatamMark
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.sakinah.tasbih.calendar.AgendaEvent
import com.sakinah.tasbih.calendar.DeviceAgenda
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Today
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
import androidx.compose.ui.platform.LocalDensity
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
import com.sakinah.tasbih.calendar.GridStyle
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
    onJumpHandled: (CalendarJump) -> Unit,
    onSetPrimary: (PrimaryCalendar) -> Unit,
    onCorrect: (HijriMonth, Int) -> Unit,
    onOpenOccasions: () -> Unit,
    onOpenConverter: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: (() -> Unit)?,
    hijriToday: LocalDate = today,
    agenda: AgendaState = AgendaState(),
    onVisibleMonth: (LocalDate) -> Unit = {},
    onSetAgenda: (Boolean) -> Unit = {},
    /** The wird's tasks that fall on a day, by name. */
    wirdOn: (LocalDate) -> List<String> = { emptyList() },
    onAddWird: ((LocalDate) -> Unit)? = null,
) {
    val calendar = text.calendar
    val primary = text.settings.primary
    // The day the pages stay on, so changing the leading calendar opens the month that holds it.
    var anchor by rememberSaveable(startDate) { mutableStateOf(startDate.toEpochDay()) }
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    // Outside the key below, so changing the leading calendar keeps the page where it was
    // instead of building it anew and throwing the reader back to the top.
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onSetAgenda(true)
    }
    val showAgenda = {
        if (DeviceAgenda.granted(context)) onSetAgenda(true) else askPermission.launch(DeviceAgenda.PERMISSION)
    }
    val agendaOn = text.settings.agendaEnabled && agenda.granted
    val hidden = text.settings.agendaHidden
    val eventsOn: (LocalDate) -> List<AgendaEvent> = { day ->
        if (agendaOn) agenda.events[day].orEmpty().filter { it.calendarId !in hidden } else emptyList()
    }

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
                first.plus(page).days(calendar).getOrNull(MIDDLE_OF_MONTH)?.let {
                    anchor = it.toEpochDay()
                    onVisibleMonth(it)
                }
            }
        }
        LaunchedEffect(jump?.serial) {
            jump?.let {
                pagerState.animateScrollToPage(pageOf(it.date))
                onJumpHandled(it)
            }
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
                com.sakinah.tasbih.ui.SettingsGear(onOpenSettings, "calendar_open_settings")
            },
        ) {
            MonthContent(text, today, hijriToday, first, pagerState, scroll, eventsOn, onSetPrimary, onSelect = { selected = it }, onOpenOccasions)
        }
    }

    selected?.let { epochDay ->
        val date = LocalDate.ofEpochDay(epochDay)
        DaySheet(
            text = text,
            date = date,
            events = eventsOn(date),
            agendaOn = agendaOn,
            calendars = agenda.calendars,
            onShowAgenda = showAgenda,
            onCorrect = onCorrect,
            onDismiss = { selected = null },
            wird = wirdOn(date),
            onAddWird = onAddWird?.let { add -> { selected = null; add(date) } },
        )
    }
}

/** The colour an event is drawn in: the one the user gave its calendar, or the phone's own. */
@Composable
internal fun agendaInk(event: AgendaEvent, settings: com.sakinah.tasbih.calendar.CalendarSettings): Color =
    settings.agendaColors[event.calendarId]?.ink() ?: Color(event.color).copy(alpha = 1f)

@Composable
private fun MonthContent(
    text: CalendarText,
    today: LocalDate,
    hijriToday: LocalDate,
    first: CalendarMonth,
    pagerState: PagerState,
    scroll: ScrollState,
    eventsOn: (LocalDate) -> List<AgendaEvent>,
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
            .verticalScroll(scroll)
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
        val weekend = text.settings.weekendDays
        // Days off stand in a faint band down the whole column, their names in copper.
        val band = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        val copper = LocalSakinahBrandColors.current.antiqueGold
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            weekDays(firstDay).forEach { day ->
                val off = day in weekend
                Text(
                    text = text.weekday(day).removePrefix("ال"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (off) FontWeight.SemiBold else null,
                    color = if (off) copper else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (off) band else Color.Transparent, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .padding(top = 6.dp, bottom = 6.dp),
                )
            }
        }

        // Follows the reading direction, so the next month comes in from where reading goes.
        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.testTag("calendar_pager")) { page ->
            val grid = remember(page, primary, calendar, firstDay) { buildMonthGrid(first.plus(page), calendar, firstDay) }
            // The cell grows with the system's text size, so two dates and the markers still fit.
            // A little taller while the phone's events are shown, for their dots under the dates.
            val base = if (text.settings.agendaEnabled) 66.dp else 60.dp
            val cellHeight = base * LocalDensity.current.fontScale.coerceIn(1f, 1.8f)
            Column {
                val columns = weekDays(firstDay)
                // The band ends with the month's last week, not in an empty row kept for the height.
                val lastWeek = grid.indexOfLast { week -> week.any { it != null } }
                grid.forEachIndexed { row, week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEachIndexed { column, cell ->
                            val off = columns[column] in weekend && row <= lastWeek
                            val last = row == lastWeek
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                                    .background(
                                        if (off) band else Color.Transparent,
                                        if (last) RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp) else RectangleShape,
                                    ),
                            ) {
                                if (cell != null) {
                                    val events = eventsOn(cell.date)
                                    val dots = events.map { agendaInk(it, text.settings) }.distinct().take(3)
                                    DayBox(cell, text, cell.date == today, off, dots, events.size, onClick = { onSelect(cell.date.toEpochDay()) })
                                }
                            }
                        }
                    }
                }
            }
        }

        TodaySummary(text, hijriToday, onOpenOccasions)
    }
}

/**
 * The week that holds [today], drawn by the calendar's own cells: the preview on top of the
 * calendar's settings, so the leading calendar, the week's first day, the days off, the colours
 * and the white days show as they will in the calendar.
 */
@Composable
internal fun CalendarWeekPreview(text: CalendarText, today: LocalDate, modifier: Modifier = Modifier) {
    val firstDay = text.firstDayOfWeek()
    val week = remember(today, text.settings.primary, text.calendar, firstDay) {
        CalendarMonth.containing(today, text.settings.primary, text.calendar)
            ?.let { buildMonthGrid(it, text.calendar, firstDay) }
            ?.firstOrNull { row -> row.any { it?.date == today } }
    }
    val weekend = text.settings.weekendDays
    val band = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
    val copper = LocalSakinahBrandColors.current.antiqueGold
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("calendar_settings_preview"),
    ) {
        Row(Modifier.fillMaxWidth()) {
            weekDays(firstDay).forEach { day ->
                val off = day in weekend
                Text(
                    text = text.weekday(day).removePrefix("ال"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (off) FontWeight.SemiBold else null,
                    color = if (off) copper else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (off) band else Color.Transparent, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .padding(vertical = 6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            val columns = weekDays(firstDay)
            columns.forEachIndexed { column, day ->
                val off = day in weekend
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(if (off) band else Color.Transparent, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)),
                ) {
                    week?.getOrNull(column)?.let { cell ->
                        DayBox(cell, text, cell.date == today, off, emptyList(), 0, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBox(
    cell: DayCell,
    text: CalendarText,
    isToday: Boolean,
    isDayOff: Boolean,
    dots: List<Color>,
    eventCount: Int,
    onClick: () -> Unit,
) {
    val hijriFirst = text.settings.primary == PrimaryCalendar.HIJRI
    val hijriDay = cell.hijri?.let { text.number(it.day) }.orEmpty()
    val gregorianDay = text.number(cell.date.dayOfMonth)
    val hasOccasion = cell.hijri?.let { Occasions.on(it).isNotEmpty() } == true ||
        text.personalOn(cell.date).isNotEmpty()
    val isWhiteDay = text.settings.showWhiteDays && cell.hijri?.let(Occasions::isWhiteDay) == true
    val colors = MaterialTheme.colorScheme
    val copper = LocalSakinahBrandColors.current.antiqueGold
    val dayOff = stringResource(R.string.cal_day_off)
    val todayLabel = stringResource(R.string.cal_today)
    val description = listOfNotNull(
        todayLabel.takeIf { isToday },
        text.weekday(cell.date.dayOfWeek),
        cell.hijri?.let(text::hijriDate),
        text.gregorianDate(cell.date),
        dayOff.takeIf { isDayOff },
        if (eventCount > 0) stringResource(R.string.cal_agenda_count, text.number(eventCount)) else null,
    ).joinToString("، ")
    val todayShape = RoundedCornerShape(12.dp)
    val table = text.settings.gridStyle == GridStyle.TABLE
    // The user's own colours for each calendar's number, whichever of the two leads.
    val hijriInk = text.settings.hijriColor.ink()
    val gregorianInk = text.settings.gregorianColor.ink()
    val largeInk = if (hijriFirst) hijriInk else gregorianInk
    val smallInk = if (hijriFirst) gregorianInk else hijriInk

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(3.dp)
            .fillMaxSize()
            // Today sits in a faint square, like a pressed button: a soft tint and a thin rule.
            // In the table every other day has a square of its own, drawn fainter.
            .then(
                when {
                    isToday -> Modifier
                        .background(colors.primary.copy(alpha = 0.12f), todayShape)
                        .border(if (table) 1.5.dp else 1.dp, colors.primary.copy(alpha = 0.5f), todayShape)
                    table -> Modifier
                        .background(colors.surface.copy(alpha = 0.6f), todayShape)
                        .border(1.dp, colors.outlineVariant, todayShape)
                    else -> Modifier
                },
            )
            .clip(todayShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = if (hijriFirst) hijriDay else gregorianDay,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                largeInk != null -> largeInk
                isToday -> colors.primary
                hasOccasion -> copper
                else -> colors.onSurface
            },
        )
        Text(
            text = if (hijriFirst) gregorianDay else hijriDay,
            style = MaterialTheme.typography.labelSmall,
            color = smallInk ?: if (isToday) colors.primary.copy(alpha = 0.8f) else colors.onSurfaceVariant,
        )
        // Short lines for occasions, never dots: a dot beside Arabic digits reads as the digit zero.
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp).padding(top = 3.dp)) {
            if (hasOccasion) Marker(copper)
            if (isWhiteDay) Marker(colors.outline)
        }
        // The phone's events are the dots the user asked for, one per colour, on a line of their
        // own below the digits and the occasion lines so they cannot be read as a zero.
        if (dots.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 3.dp)) {
                dots.forEach { Box(Modifier.size(6.dp).background(it, CircleShape)) }
            }
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
    val todays = hijri?.let { Occasions.on(it) }.orEmpty()
    val next = remember(text, today) {
        Occasions.upcoming(text.calendar, today).firstOrNull { it.daysAway > 0 }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                KhatamMark()
                Text(text.weekday(today.dayOfWeek), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
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
    events: List<AgendaEvent>,
    agendaOn: Boolean,
    calendars: List<com.sakinah.tasbih.calendar.DeviceCalendar>,
    onShowAgenda: () -> Unit,
    onCorrect: (HijriMonth, Int) -> Unit,
    onDismiss: () -> Unit,
    wird: List<String> = emptyList(),
    onAddWird: (() -> Unit)? = null,
) {
    val calendar = text.calendar
    val hijri = calendar.fromGregorian(date)
    // The month's correction is asked for only on request, so a tap on a day shows the day.
    var correcting by rememberSaveable(date) { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                .testTag("calendar_day_sheet"),
        ) {
            Text(text.weekday(date.dayOfWeek), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            hijri?.let { Text(text.hijriDate(it), style = MaterialTheme.typography.headlineSmall) }
            Text(text.gregorianDate(date), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val occasions = hijri?.let { Occasions.on(it) }.orEmpty()
            occasions.forEach { occasion ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Celebration, null, tint = LocalSakinahBrandColors.current.antiqueGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text.occasionName(occasion), style = MaterialTheme.typography.bodyLarge)
                }
            }
            text.personalOn(date).forEach { personal ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Celebration, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(personal.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (hijri != null && text.settings.showWhiteDays && Occasions.isWhiteDay(hijri)) {
                Text(stringResource(R.string.cal_white_day), style = MaterialTheme.typography.bodyMedium)
            }

            if (wird.isNotEmpty() || onAddWird != null) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                if (wird.isNotEmpty()) Text(stringResource(R.string.cal_wird_title), style = MaterialTheme.typography.titleSmall)
                wird.forEach { title ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp).testTag("calendar_wird_task")) {
                        Icon(Icons.Outlined.TaskAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (onAddWird != null) {
                    TextButton(onClick = onAddWird, modifier = Modifier.testTag("calendar_add_wird")) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.cal_wird_add))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            if (agendaOn) {
                Text(stringResource(R.string.cal_agenda_title), style = MaterialTheme.typography.titleSmall)
                if (events.isEmpty()) {
                    CalendarHint(stringResource(R.string.cal_agenda_empty))
                }
                events.forEach { event ->
                    AgendaRow(text, date, event, calendars.firstOrNull { it.id == event.calendarId }?.name.orEmpty())
                }
            } else {
                TextButton(onClick = onShowAgenda, modifier = Modifier.testTag("calendar_show_agenda")) {
                    Icon(Icons.AutoMirrored.Outlined.EventNote, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cal_agenda_show))
                }
            }

            if (hijri != null && !correcting) {
                TextButton(onClick = { correcting = true }, modifier = Modifier.padding(top = 8.dp).testTag("calendar_open_correction")) {
                    Text(stringResource(R.string.cal_correct_open, text.hijriMonthYear(hijri.yearMonth)))
                }
            }
            if (hijri != null && correcting) {
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

/** One event of the phone's calendar: its colour, title, hours and calendar. Opens in the phone's calendar app. */
@Composable
private fun AgendaRow(text: CalendarText, day: LocalDate, event: AgendaEvent, calendarName: String) {
    val context = LocalContext.current
    val zone = java.time.ZoneId.systemDefault()
    val use24 = android.text.format.DateFormat.is24HourFormat(context)
    val am = stringResource(R.string.prayer_am)
    val pm = stringResource(R.string.prayer_pm)
    fun clock(instant: java.time.Instant): String {
        val time = instant.atZone(zone)
        val minute = com.sakinah.tasbih.data.twoDigits(time.minute)
        if (use24) return "${text.number(time.hour)}:$minute"
        val hour = (time.hour % 12).let { if (it == 0) 12 else it }
        return "${text.number(hour)}:$minute ${if (time.hour < 12) am else pm}"
    }
    val starts = event.begin.atZone(zone).toLocalDate()
    val ends = event.end.minusMillis(1).atZone(zone).toLocalDate()
    val hours = when {
        event.allDay -> stringResource(R.string.cal_agenda_all_day)
        starts == day && ends == day -> "${clock(event.begin)} – ${clock(event.end)}"
        starts == day -> stringResource(R.string.cal_agenda_from, clock(event.begin))
        ends == day -> stringResource(R.string.cal_agenda_until, clock(event.end))
        else -> stringResource(R.string.cal_agenda_all_day)
    }
    val title = event.title.ifBlank { stringResource(R.string.cal_agenda_untitled) }
    // Names and places may be Latin, so each is isolated to keep the line in reading order.
    val details = listOf(hours, event.location, calendarName).filter { it.isNotBlank() }
        .joinToString(" · ") { "\u2068$it\u2069" }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.content.ContentUris.withAppendedId(android.provider.CalendarContract.Events.CONTENT_URI, event.eventId),
                ).putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin.toEpochMilli())
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli())
                runCatching { context.startActivity(intent) }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("calendar_agenda_event"),
    ) {
        Box(Modifier.size(12.dp).background(agendaInk(event, text.settings), CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A day near the middle of a month stands for it, safely away from both of its edges. */
private const val MIDDLE_OF_MONTH = 14
