package com.sakinah.tasbih.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.HijriDate
import com.sakinah.tasbih.ui.sakinahCardBorder
import java.time.DateTimeException
import java.time.LocalDate

/** Turns a date of one calendar into the other, in either direction. */
@Composable
fun ConverterScreen(text: CalendarText, today: LocalDate, onBack: () -> Unit) {
    val calendar = text.calendar
    val todayHijri = calendar.fromGregorian(today)
    var fromHijri by rememberSaveable { mutableStateOf(false) }
    var day by rememberSaveable(fromHijri) { mutableIntStateOf(if (fromHijri) todayHijri?.day ?: 1 else today.dayOfMonth) }
    var month by rememberSaveable(fromHijri) { mutableIntStateOf(if (fromHijri) todayHijri?.month ?: 1 else today.monthValue) }
    var yearText by rememberSaveable(fromHijri) {
        mutableStateOf(text.number(if (fromHijri) todayHijri?.year ?: calendar.firstMonth.year else today.year))
    }

    val year = yearText.toNumber()
    val result: LocalDate? = year?.let {
        if (fromHijri) {
            calendar.toGregorian(HijriDate(it, month, day))
        } else {
            try {
                LocalDate.of(it, month, day).takeIf(calendar::isSupported)
            } catch (_: DateTimeException) {
                null
            }
        }
    }

    CalendarPage(title = stringResource(R.string.cal_converter), tag = "calendar_converter", onBack = onBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            CalendarChoices(
                options = listOf(false, true),
                selected = fromHijri,
                label = { stringResource(if (it) R.string.cal_from_hijri else R.string.cal_from_gregorian) },
                onSelect = { fromHijri = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Chooser(
                    label = stringResource(R.string.cal_day),
                    value = text.number(day),
                    options = (1..MAX_DAY).map { it to text.number(it) },
                    onSelect = { day = it },
                    modifier = Modifier.weight(0.8f),
                )
                Chooser(
                    label = stringResource(R.string.cal_month),
                    value = if (fromHijri) text.hijriMonthName(month) else text.gregorianMonthName(month),
                    options = (1..12).map { it to if (fromHijri) text.hijriMonthName(it) else text.gregorianMonthName(it) },
                    onSelect = { month = it },
                    modifier = Modifier.weight(1.6f),
                )
                OutlinedTextField(
                    value = yearText,
                    // Shown in Arabic digits whichever digits the keyboard types.
                    onValueChange = { value -> yearText = value.toNumber()?.let(text::number).orEmpty() },
                    label = { Text(stringResource(R.string.cal_year)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("calendar_converter_year"),
                )
            }

            if (result == null) {
                val (firstYear, lastYear) = if (fromHijri) {
                    calendar.firstMonth.year to calendar.lastMonth.year
                } else {
                    calendar.firstDay.year + 1 to calendar.lastDay.year - 1
                }
                Text(
                    stringResource(R.string.cal_invalid_date, text.number(firstYear), text.number(lastYear)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = sakinahCardBorder(),
                    modifier = Modifier.fillMaxWidth().testTag("calendar_converter_result"),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text.weekday(result.dayOfWeek), style = MaterialTheme.typography.labelLarge)
                        Text(text.hijriDate(result), style = MaterialTheme.typography.titleLarge)
                        Text(text.gregorianDate(result), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text.distanceFromToday(result, today),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A field that opens a list to choose from, such as the month's name. */
@Composable
private fun Chooser(
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (option, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** Reads digits typed in any script, Arabic-Indic or Western, as a number. */
private fun String.toNumber(): Int? =
    filter(Char::isDigit).takeIf { it.isNotEmpty() }?.take(4)?.fold(0) { total, digit -> total * 10 + digit.digitToInt() }

private const val MAX_DAY = 31
