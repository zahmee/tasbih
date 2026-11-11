package com.sakinah.tasbih.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalOccasionsTest {
    private val calendar = HijriCalendar()

    @Test
    fun `occasions survive saving, names with the separator included`() {
        val list = listOf(
            PersonalOccasion("a", "ميلاد سارة", 3, 14),
            PersonalOccasion("b", "ذكرى | الزواج", 11, 2),
        )
        assertEquals(list.sortedBy { it.month }, PersonalOccasions.decode(PersonalOccasions.encode(list)))
    }

    @Test
    fun `broken lines are dropped instead of breaking the list`() {
        val decoded = PersonalOccasions.decode(setOf("13|1|x|bad month", "2|31|y|bad day", "2|3||no id", "2|3|z|", "4|5|ok|حسن"))
        assertEquals(listOf(PersonalOccasion("ok", "حسن", 4, 5)), decoded)
    }

    @Test
    fun `the next date comes this year or next, counted from today`() {
        // 11 November 2026 is the 1st of Jumada al-Akhirah 1448.
        val today = LocalDate.parse("2026-11-11")
        val soon = PersonalOccasion("s", "قريبة", 6, 3)
        val past = PersonalOccasion("p", "مضت", 1, 1)
        val upcoming = PersonalOccasions.upcoming(calendar, today, listOf(past, soon))
        assertEquals(listOf("s", "p"), upcoming.map { it.occasion.id })
        assertEquals(2L, upcoming.first().daysAway)
        assertEquals(calendar.toGregorian(HijriDate(1449, 1, 1)), upcoming.last().date)
    }

    @Test
    fun `a thirtieth falls on the last day of a shorter month`() {
        val thirtieth = PersonalOccasion("t", "آخر الشهر", 6, 30)
        val year = 1448
        val length = calendar.lengthOf(HijriMonth(year, 6))!!
        val date = PersonalOccasions.dateIn(calendar, year, thirtieth)!!
        assertEquals(HijriDate(year, 6, length), calendar.fromGregorian(date))
        assertTrue(PersonalOccasions.on(calendar, date, listOf(thirtieth)).isNotEmpty())
    }
}
