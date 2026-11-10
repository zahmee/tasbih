package com.sakinah.tasbih.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPartsTest {
    private val calendar = HijriCalendar()

    @Test
    fun `the next big occasion after 10 November 2026 is Ramadan 1448`() {
        val today = LocalDate.parse("2026-11-10")
        val next = Occasions.nextMajor(calendar, today)
        val ramadan = calendar.toGregorian(HijriDate(1448, 9, 1))
        assertNotNull(next)
        assertEquals(Occasion.RAMADAN, next!!.occasion)
        assertEquals(ramadan, next.date)
        assertEquals(ChronoUnit.DAYS.between(today, ramadan), next.daysAway)
    }

    @Test
    fun `an occasion today counts as zero days away`() {
        val eid = LocalDate.parse("2026-05-27")
        val first = Occasions.upcoming(calendar, eid, showOther = false).first()
        assertEquals(Occasion.EID_AL_ADHA, first.occasion)
        assertEquals(0, first.daysAway)
    }

    @Test
    fun `upcoming occasions are each listed once, soonest first, within a year`() {
        val today = LocalDate.parse("2026-11-10")
        val list = Occasions.upcoming(calendar, today, showOther = true)
        assertEquals(Occasion.entries.size, list.size)
        assertEquals(list.sortedBy { it.date }, list)
        assertTrue(list.all { it.daysAway in 0..355 })
    }

    @Test
    fun `contested occasions are hidden unless asked for`() {
        val mawlid = HijriDate(1448, 3, 12)
        assertTrue(Occasions.on(mawlid, showOther = false).isEmpty())
        assertEquals(listOf(Occasion.MAWLID), Occasions.on(mawlid, showOther = true))
        val today = LocalDate.parse("2026-11-10")
        assertFalse(Occasions.upcoming(calendar, today, showOther = false).any { it.occasion.kind == OccasionKind.CONTESTED })
    }

    @Test
    fun `white days skip Ramadan and the first day of Tashreeq`() {
        assertTrue(Occasions.isWhiteDay(HijriDate(1448, 7, 13)))
        assertTrue(Occasions.isWhiteDay(HijriDate(1448, 7, 15)))
        assertFalse(Occasions.isWhiteDay(HijriDate(1448, 7, 16)))
        assertFalse(Occasions.isWhiteDay(HijriDate(1448, 9, 14)))
        assertFalse(Occasions.isWhiteDay(HijriDate(1447, 12, 13)))
        assertTrue(Occasions.isWhiteDay(HijriDate(1447, 12, 14)))
    }

    @Test
    fun `a Gregorian month fills six weeks from the chosen first day`() {
        // 1 November 2026 is a Sunday.
        val november = CalendarMonth.Gregorian(YearMonth.of(2026, 11))
        val fromSunday = buildMonthGrid(november, calendar, DayOfWeek.SUNDAY)
        assertEquals(WEEKS, fromSunday.size)
        assertTrue(fromSunday.all { it.size == DAYS_IN_WEEK })
        assertEquals(LocalDate.parse("2026-11-01"), fromSunday[0][0]?.date)
        assertEquals(30, fromSunday.flatten().count { it != null })

        val fromSaturday = buildMonthGrid(november, calendar, DayOfWeek.SATURDAY)
        assertNull(fromSaturday[0][0])
        assertEquals(LocalDate.parse("2026-11-01"), fromSaturday[0][1]?.date)
    }

    @Test
    fun `a Hijri month grid carries both dates in each day`() {
        val month = CalendarMonth.Hijri(HijriMonth(1448, 5))
        val days = buildMonthGrid(month, calendar, DayOfWeek.SATURDAY).flatten().filterNotNull()
        assertEquals(calendar.lengthOf(HijriMonth(1448, 5)), days.size)
        assertEquals(HijriDate(1448, 5, 30), days.first { it.date == LocalDate.parse("2026-11-10") }.hijri)
        assertEquals((1..days.size).toList(), days.map { it.hijri?.day })
    }

    @Test
    fun `months count apart in both calendars`() {
        val a = CalendarMonth.Gregorian(YearMonth.of(2026, 11))
        assertEquals(14, a.plus(14).minus(a))
        assertEquals(CalendarMonth.Gregorian(YearMonth.of(2027, 1)), a.plus(2))
        val h = CalendarMonth.Hijri(HijriMonth(1448, 5))
        assertEquals(-17, h.plus(-17).minus(h))
    }

    @Test
    fun `the month range covers only whole months the Hijri data knows`() {
        val (first, last) = CalendarMonth.range(PrimaryCalendar.GREGORIAN, calendar)
        val firstDays = first.days(calendar)
        val lastDays = last.days(calendar)
        assertTrue(calendar.isSupported(firstDays.first()))
        assertTrue(calendar.isSupported(lastDays.last()))
        assertFalse(calendar.isSupported(first.plus(-1).days(calendar).first()))
    }

    @Test
    fun `saved maps read back, and damaged lines are dropped`() {
        val corrections = mapOf("1447-11" to 1, "1448-9" to -1)
        assertEquals(corrections, SavedMaps.decodeStrings(SavedMaps.encode(corrections)))
        val pages = mapOf(42 to 3, 7 to -2)
        assertEquals(pages, SavedMaps.decodeInts(SavedMaps.encode(pages)))
        assertEquals(mapOf("1447-11" to 1), SavedMaps.decodeStrings(setOf("1447-11=1", "broken", "=4", "1448-1=x")))
        assertEquals(emptyMap<String, Int>(), SavedMaps.decodeStrings(null))
        assertEquals(mapOf(5 to 1), SavedMaps.decodeInts(setOf("5=1", "five=1")))
    }

    @Test
    fun `a month widget's page counts only on the day it was turned`() {
        val settings = CalendarSettings(monthWidgetPages = mapOf(7 to 2), monthWidgetPagesDay = 100)
        assertEquals(2, settings.monthWidgetPage(7, today = 100))
        assertEquals(0, settings.monthWidgetPage(7, today = 101))
        assertEquals(0, settings.monthWidgetPage(8, today = 100))
    }
}
