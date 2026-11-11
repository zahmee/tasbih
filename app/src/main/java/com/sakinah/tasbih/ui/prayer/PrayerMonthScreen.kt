package com.sakinah.tasbih.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.CalendarMonth
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerDay
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.calendar.CalendarPage
import com.sakinah.tasbih.ui.calendar.CalendarText
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** Ramadan is the ninth Hijri month, where the table becomes the month's fasting timetable. */
private const val RAMADAN = 9

/**
 * The month's prayer times in one table, by whichever calendar the user reads first. Each row
 * is one day; Friday is marked for the Jumu'ah and today stands out. In Ramadan the page is
 * named for it and says when the fast begins and ends.
 */
@Composable
fun PrayerMonthScreen(schedule: PrayerSchedule, calendarText: CalendarText, onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    val today = remember(schedule) { schedule.today(Instant.now()) }
    val first = remember(schedule, calendarText) {
        CalendarMonth.containing(today, calendarText.settings.primary, calendarText.calendar)
    }
    var shift by rememberSaveable { mutableIntStateOf(0) }
    val month = first?.plus(shift)
    val days = remember(month, calendarText, schedule) {
        month?.days(calendarText.calendar).orEmpty().map(schedule::day)
    }
    val ramadan = month is CalendarMonth.Hijri && month.month.month == RAMADAN

    CalendarPage(
        title = stringResource(if (ramadan) R.string.prayer_month_ramadan_title else R.string.prayer_month_title),
        tag = "prayer_month",
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            if (month != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { shift-- }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, stringResource(R.string.cal_previous_month), Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            calendarText.monthTitle(month),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            listOf(calendarText.monthSubtitle(month), schedule.place.name).filter(String::isNotEmpty).joinToString(" – "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { shift++ }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, stringResource(R.string.cal_next_month), Modifier.size(18.dp))
                    }
                }
            }
            if (ramadan) {
                Text(
                    stringResource(R.string.prayer_month_ramadan_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                )
            }
            HeaderRow(text)
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize()) {
                items(days, key = { it.date.toEpochDay() }) { day ->
                    DayRow(day, today, month, calendarText, text)
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(text: PrayerText) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Cell(stringResource(R.string.prayer_month_day), header = true, width = DAY_WIDTH)
        Prayer.entries.forEach { Cell(text.name(it), header = true) }
    }
}

@Composable
private fun DayRow(day: PrayerDay, today: LocalDate, month: CalendarMonth?, calendarText: CalendarText, text: PrayerText) {
    val colors = MaterialTheme.colorScheme
    val isToday = day.date == today
    val isFriday = day.date.dayOfWeek == DayOfWeek.FRIDAY
    val (main, other) = dayNumbers(day.date, month, calendarText)
    val spoken = listOf(calendarText.weekday(day.date.dayOfWeek), calendarText.hijriDate(day.date), calendarText.gregorianDate(day.date))
        .filter(String::isNotEmpty).joinToString("، ") + ": " +
        Prayer.entries.joinToString("، ") { "${text.name(it, day[it])} ${text.time(day[it])}" }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(
                when {
                    isToday -> colors.secondaryContainer
                    isFriday -> colors.surfaceContainerHigh
                    else -> Color.Transparent
                },
                MaterialTheme.shapes.small,
            )
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Column(Modifier.width(DAY_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(main, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(other, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Prayer.entries.forEach { prayer ->
            Cell(text.clock(day[prayer]), muted = prayer == Prayer.SUNRISE)
        }
    }
}

/** The day in the month's own calendar first, and in the other calendar beneath it. */
private fun dayNumbers(date: LocalDate, month: CalendarMonth?, calendarText: CalendarText): Pair<String, String> {
    val hijri = calendarText.calendar.fromGregorian(date)?.day?.let(calendarText::number).orEmpty()
    val gregorian = calendarText.number(date.dayOfMonth)
    return if (month is CalendarMonth.Gregorian) gregorian to hijri else hijri to gregorian
}

@Composable
private fun RowScope.Cell(value: String, header: Boolean = false, muted: Boolean = false, width: androidx.compose.ui.unit.Dp? = null) {
    Text(
        value,
        style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
        color = if (header || muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = if (width != null) Modifier.width(width) else Modifier.weight(1f),
    )
}

private val DAY_WIDTH = 44.dp
