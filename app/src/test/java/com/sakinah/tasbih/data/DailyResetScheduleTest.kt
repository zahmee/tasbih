package com.sakinah.tasbih.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class DailyResetScheduleTest {
    private val settings = DailyResetSettings(minuteOfDay = 4 * 60 + 30)

    @Test fun `custom boundary keeps the previous session after midnight and starts exactly on time`() {
        val before = ZonedDateTime.parse("2026-09-12T04:29:59+03:00[Asia/Riyadh]")
        assertEquals(LocalDate.parse("2026-09-11"), dailyResetDay(before, settings))
        assertEquals(before.plusSeconds(1), nextDailyReset(before, settings))
        assertEquals(LocalDate.parse("2026-09-12"), dailyResetDay(before.plusSeconds(1), settings))
        assertEquals(before.plusSeconds(1).plusDays(1), nextDailyReset(before.plusSeconds(1), settings))
    }

    @Test fun `default remains local midnight including non whole hour time zones`() {
        val now = ZonedDateTime.parse("2026-09-11T23:59:00+05:45[Asia/Kathmandu]")
        assertEquals(LocalDate.parse("2026-09-11"), dailyResetDay(now, DailyResetSettings()))
        assertEquals(now.plusMinutes(1), nextDailyReset(now, DailyResetSettings()))
    }

    @Test fun `spring daylight saving gap moves the missing time forward`() {
        val before = ZonedDateTime.parse("2026-03-29T01:59:00+01:00[Europe/Berlin]")
        val gapSettings = DailyResetSettings(minuteOfDay = 150)
        val boundary = ZonedDateTime.parse("2026-03-29T03:30:00+02:00[Europe/Berlin]")
        assertEquals(boundary, nextDailyReset(before, gapSettings))
        assertEquals(LocalDate.parse("2026-03-28"), dailyResetDay(boundary.minusMinutes(1), gapSettings))
        assertEquals(LocalDate.parse("2026-03-29"), dailyResetDay(boundary, gapSettings))
    }

    @Test fun `autumn repeated hour resets at its first occurrence only`() {
        val repeatedSettings = DailyResetSettings(minuteOfDay = 150)
        val first = ZonedDateTime.of(2026, 10, 25, 2, 30, 0, 0, ZoneId.of("Europe/Berlin")).withEarlierOffsetAtOverlap()
        val second = first.withLaterOffsetAtOverlap()
        assertEquals(dailyResetDay(first, repeatedSettings), dailyResetDay(second, repeatedSettings))
        assertEquals(first.toLocalDate().plusDays(1), nextDailyReset(first, repeatedSettings).toLocalDate())
        assertEquals(nextDailyReset(first, repeatedSettings), nextDailyReset(second, repeatedSettings))
    }

    @Test fun `independent switches retain the other counters and non daily progress`() {
        val progress = mapOf("hisn_027" to ReadingProgress(entryIndex = 1), "hisn_035" to ReadingProgress(entryIndex = 2))
        val tasbihOnly = dailyStateForDate("2026-09-11", "2026-09-12", 12, progress, DailyResetSettings(adhkarEnabled = false))
        assertEquals(0, tasbihOnly.tasbihCount)
        assertEquals(progress, tasbihOnly.readingProgress)
        val adhkarOnly = dailyStateForDate("2026-09-11", "2026-09-12", 12, progress, DailyResetSettings(tasbihEnabled = false))
        assertEquals(12, adhkarOnly.tasbihCount)
        assertEquals(mapOf("hisn_035" to progress.getValue("hisn_035")), adhkarOnly.readingProgress)
        val neither = dailyStateForDate("2026-09-11", "2026-09-12", 12, progress, DailyResetSettings(false, false))
        assertEquals(12, neither.tasbihCount)
        assertEquals(progress, neither.readingProgress)
    }
}
