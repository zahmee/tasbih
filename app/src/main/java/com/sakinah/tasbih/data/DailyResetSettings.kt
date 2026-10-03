package com.sakinah.tasbih.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

data class DailyResetSettings(
    val tasbihEnabled: Boolean = true,
    val adhkarEnabled: Boolean = true,
    val minuteOfDay: Int = 0,
) {
    val time: LocalTime get() = LocalTime.ofSecondOfDay(minuteOfDay.coerceIn(0, 1439) * 60L)
}

/** The first occurrence of the chosen local time is the boundary on a DST overlap day. */
internal fun dailyResetDay(now: ZonedDateTime, settings: DailyResetSettings): LocalDate {
    val date = now.toLocalDate()
    val boundary = date.atTime(settings.time).atZone(now.zone)
    return if (now.isBefore(boundary)) date.minusDays(1) else date
}

internal fun nextDailyReset(now: ZonedDateTime, settings: DailyResetSettings): ZonedDateTime {
    val boundary = now.toLocalDate().atTime(settings.time).atZone(now.zone)
    return if (boundary.isAfter(now)) boundary else now.toLocalDate().plusDays(1).atTime(settings.time).atZone(now.zone)
}

internal fun dailyStateToken(day: String, revision: Long): String =
    if (revision == 0L) day else "$day@$revision"

internal fun validDailyStateToken(value: String): String {
    val parts = value.split('@')
    require(parts.size in 1..2)
    LocalDate.parse(parts[0])
    if (parts.size == 2) require(parts[1].toLong() > 0)
    return value
}
