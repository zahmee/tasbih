package com.sakinah.tasbih.calendar

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceAgendaTest {
    private val riyadh = ZoneId.of("Asia/Riyadh")
    private val from = LocalDate.of(2026, 11, 1)
    private val to = LocalDate.of(2026, 11, 30)

    private fun event(id: Long, begin: java.time.Instant, end: java.time.Instant, allDay: Boolean = false) =
        AgendaEvent(id, 1, "e$id", begin, end, allDay, "", 0)

    @Test
    fun anAllDayEventKeepsItsOwnDayInAnyZone() {
        // Stored from UTC midnight to UTC midnight; in Riyadh that is 3 a.m. to 3 a.m.
        val day = LocalDate.of(2026, 11, 19)
        val e = event(1, day.atStartOfDay(ZoneOffset.UTC).toInstant(), day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), allDay = true)
        assertEquals(setOf(day), DeviceAgenda.byDay(listOf(e), from, to, riyadh).keys)
    }

    @Test
    fun aTimedEventEndingAtMidnightStaysOnOneDay() {
        val day = LocalDate.of(2026, 11, 19)
        val e = event(1, day.atTime(22, 0).atZone(riyadh).toInstant(), day.plusDays(1).atStartOfDay(riyadh).toInstant())
        assertEquals(setOf(day), DeviceAgenda.byDay(listOf(e), from, to, riyadh).keys)
    }

    @Test
    fun anEventOverNightCoversBothDaysAndAllDayLeads() {
        val day = LocalDate.of(2026, 11, 19)
        val night = event(1, day.atTime(23, 0).atZone(riyadh).toInstant(), day.plusDays(1).atTime(1, 0).atZone(riyadh).toInstant())
        val allDay = event(2, day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), day.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant(), allDay = true)
        val days = DeviceAgenda.byDay(listOf(night, allDay), from, to, riyadh)
        assertEquals(listOf(day, day.plusDays(1)), days.keys.toList())
        assertEquals(listOf(2L, 1L), days.getValue(day.plusDays(1)).map { it.eventId })
    }

    @Test
    fun daysOutsideTheRangeAreLeftOut() {
        val e = event(1, LocalDate.of(2026, 10, 30).atStartOfDay(riyadh).toInstant(), LocalDate.of(2026, 11, 3).atStartOfDay(riyadh).toInstant())
        assertEquals(listOf(from, from.plusDays(1)), DeviceAgenda.byDay(listOf(e), from, to, riyadh).keys.toList())
    }
}
