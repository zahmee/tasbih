package com.sakinah.tasbih.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriCalendarTest {
    private val plain = HijriCalendar()

    private fun date(text: String) = LocalDate.parse(text)

    @Test
    fun `known Umm al-Qura days of 1447`() {
        // Published 2026 dates: Dhu al-Hijjah began on 18 May and Eid al-Adha was on 27 May.
        assertEquals(HijriDate(1447, 12, 1), plain.fromGregorian(date("2026-05-18")))
        assertEquals(HijriDate(1447, 12, 10), plain.fromGregorian(date("2026-05-27")))
        // The computed calendar gives Dhu al-Qa'dah 30 days; the announced month had 29.
        assertEquals(HijriDate(1447, 11, 30), plain.fromGregorian(date("2026-05-17")))
        // The evening the new moon of Ramadan was looked for, and the first day of Ramadan.
        assertEquals(HijriDate(1447, 8, 29), plain.fromGregorian(date("2026-02-17")))
        assertEquals(HijriDate(1447, 9, 1), plain.fromGregorian(date("2026-02-18")))
    }

    @Test
    fun `every day converts back to itself, with and without corrections`() {
        val calendars = listOf(
            plain,
            HijriCalendar(offsetDays = 1),
            HijriCalendar(offsetDays = -2),
            HijriCalendar(corrections = mapOf(HijriMonth(1447, 11) to 1, HijriMonth(1448, 9) to -1)),
        )
        calendars.forEach { calendar ->
            var day = date("2024-01-01")
            while (day <= date("2030-12-31")) {
                val hijri = requireNotNull(calendar.fromGregorian(day)) { "No Hijri date for $day" }
                assertEquals("Round trip of $day", day, calendar.toGregorian(hijri))
                day = day.plusDays(1)
            }
        }
    }

    @Test
    fun `every month has 29 or 30 days`() {
        var month = HijriMonth(1440, 1)
        while (month <= HijriMonth(1460, 12)) {
            assertTrue("Length of $month", plain.lengthOf(month) in 29..30)
            month = month.next()
        }
    }

    @Test
    fun `an offset moves every day`() {
        val ahead = HijriCalendar(offsetDays = 1)
        assertEquals(HijriDate(1447, 12, 1), ahead.fromGregorian(date("2026-05-17")))
        val behind = HijriCalendar(offsetDays = -1)
        assertEquals(HijriDate(1447, 11, 30), behind.fromGregorian(date("2026-05-18")))
    }

    @Test
    fun `a month that began a day later moves only its own start`() {
        val dhuAlQadah = HijriMonth(1447, 11)
        val corrected = HijriCalendar(corrections = mapOf(dhuAlQadah to 1))
        assertEquals(HijriDate(1447, 11, 29), corrected.fromGregorian(date("2026-05-17")))
        assertEquals(29, corrected.lengthOf(dhuAlQadah))
        // Shawwal gains the day, and the months on either side keep their computed start.
        assertEquals(30, corrected.lengthOf(dhuAlQadah.previous()))
        assertEquals(HijriDate(1447, 10, 30), corrected.fromGregorian(date("2026-04-18")))
        assertEquals(HijriDate(1447, 12, 1), corrected.fromGregorian(date("2026-05-18")))
        assertEquals(plain.monthStart(HijriMonth(1447, 10)), corrected.monthStart(HijriMonth(1447, 10)))
    }

    @Test
    fun `a correction that would make a month too short or too long is refused`() {
        val dhuAlQadah = HijriMonth(1447, 11)
        // Shawwal 1447 has 29 days, so starting Dhu al-Qa'dah earlier would leave it 28.
        assertEquals(29, plain.lengthOf(dhuAlQadah.previous()))
        assertFalse(plain.canCorrect(dhuAlQadah, -1))
        assertTrue(plain.canCorrect(dhuAlQadah, 1))
        assertFalse(plain.canCorrect(dhuAlQadah, 2))
    }

    @Test
    fun `a day the month does not have has no Gregorian date`() {
        assertEquals(29, plain.lengthOf(HijriMonth(1447, 12)))
        assertNull(plain.toGregorian(HijriDate(1447, 12, 30)))
        assertNull(plain.toGregorian(HijriDate(1447, 12, 0)))
    }

    @Test
    fun `days outside the Hijri data have no Hijri date`() {
        assertNull(plain.fromGregorian(plain.firstDay.minusDays(1)))
        assertNull(plain.fromGregorian(plain.lastDay.plusDays(1)))
        assertEquals(HijriDate(plain.firstMonth.year, 1, 1), plain.fromGregorian(plain.firstDay))
    }

    @Test
    fun `agrees with Java's own Umm al-Qura calendar when nothing is corrected`() {
        var day = date("2000-01-01")
        while (day <= date("2040-12-31")) {
            val reference = HijrahDate.from(day)
            val expected = HijriDate(
                reference.get(ChronoField.YEAR),
                reference.get(ChronoField.MONTH_OF_YEAR),
                reference.get(ChronoField.DAY_OF_MONTH),
            )
            assertEquals(day.toString(), expected, plain.fromGregorian(day))
            day = day.plusDays(7)
        }
    }

    @Test
    fun `months step across years and keep their saved key`() {
        assertEquals(HijriMonth(1448, 1), HijriMonth(1447, 12).next())
        assertEquals(HijriMonth(1447, 12), HijriMonth(1448, 1).previous())
        assertEquals(HijriMonth(1446, 11), HijriMonth(1448, 1).plus(-14))
        assertEquals(HijriMonth(1447, 11), HijriMonth.fromKey(HijriMonth(1447, 11).key))
        assertNull(HijriMonth.fromKey("1447-13"))
        assertNull(HijriMonth.fromKey("rubbish"))
    }

    @Test
    fun `settings are read with limits`() {
        val calendar = HijriCalendar.from(
            CalendarSettings(hijriOffsetDays = 9, monthCorrections = mapOf("1447-11" to 5, "bad" to 1)),
        )
        // The offset is held to two days and the correction to one.
        assertEquals(HijriDate(1447, 12, 2), calendar.fromGregorian(date("2026-05-17")))
        assertEquals(1, calendar.correctionOf(HijriMonth(1447, 11)))
    }
}
