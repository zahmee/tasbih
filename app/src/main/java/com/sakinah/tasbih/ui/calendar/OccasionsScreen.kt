package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.Occasions
import com.sakinah.tasbih.ui.sakinahCardBorder
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The occasions of the coming year, soonest first, each with how many days are left. */
@Composable
fun OccasionsScreen(
    text: CalendarText,
    today: LocalDate,
    onShowDate: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    val calendar = text.calendar
    val upcoming = remember(text, today) { Occasions.upcoming(calendar, today, text.settings.showOtherOccasions) }
    val nextWhiteDay = remember(text, today) {
        if (!text.settings.showWhiteDays) {
            null
        } else {
            generateSequence(today) { it.plusDays(1) }
                .take(DAYS_TO_SEARCH)
                .firstOrNull { day -> calendar.fromGregorian(day)?.let(Occasions::isWhiteDay) == true }
        }
    }

    CalendarPage(title = stringResource(R.string.cal_occasions), tag = "calendar_occasions", onBack = onBack) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(upcoming, key = { it.occasion.name }) { item ->
                OccasionRow(
                    icon = Icons.Outlined.Celebration,
                    name = text.occasionName(item.occasion),
                    dates = listOf(text.hijriDate(item.date), text.gregorianDate(item.date)),
                    countdown = text.countdown(item.daysAway),
                    onClick = { onShowDate(item.date) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            if (nextWhiteDay != null) {
                item(key = "white-days") {
                    OccasionRow(
                        icon = Icons.Outlined.NightsStay,
                        name = stringResource(R.string.cal_next_white_days),
                        dates = listOf(text.hijriDate(nextWhiteDay), text.gregorianDate(nextWhiteDay)),
                        countdown = text.countdown(ChronoUnit.DAYS.between(today, nextWhiteDay)),
                        onClick = { onShowDate(nextWhiteDay) },
                    )
                }
            }
            item(key = "note") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = sakinahCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.cal_occasions_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OccasionRow(icon: ImageVector, name: String, dates: List<String>, countdown: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(vertical = 12.dp)
            .testTag("calendar_occasion_row"),
    ) {
        Icon(icon, null, tint = LocalSakinahBrandColors.current.antiqueGold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            dates.filter { it.isNotEmpty() }.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(countdown, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** A white day falls in every month but Ramadan, so ten weeks are always enough to find one. */
private const val DAYS_TO_SEARCH = 70
