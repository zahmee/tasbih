package com.sakinah.tasbih.wird

import com.sakinah.tasbih.data.DailyResetSettings
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WirdBoardTest {
    private val monday = LocalDate.of(2026, 11, 23)
    private val created = at(monday.minusDays(10), 12).toEpochMilli()
    private val morning = WirdPreset.MORNING.task("أذكار الصباح", 1, created).copy(id = 1)
    private val evening = WirdPreset.EVENING.task("أذكار المساء", 2, created).copy(id = 2)
    private val friday = WirdPreset.KAHF.task("سورة الكهف", 3, created).copy(id = 3)
    private val visit = WirdTask(id = 4, title = "زيارة", repeat = WirdRepeat.ONCE, date = monday.minusDays(2), createdAtMillis = created)

    @Test
    fun `today holds the day's tasks and a task of one time gone by undone`() {
        val board = WirdBoard.build(listOf(morning, evening, friday, visit), emptyMap(), emptyList(), at(monday, 9), calendar())
        assertEquals(listOf(4L, 1L, 2L).toSet(), board.todayItems.map { it.task.id }.toSet())
        val late = board.todayItems.single { it.task.id == 4L }
        assertTrue(late.late)
        assertEquals(monday.minusDays(2), late.day)
        assertEquals(0, board.doneToday)
        assertEquals(3, board.totalToday)
    }

    @Test
    fun `reading the adhkar to their end ticks their task, but only on that day`() {
        val readings = listOf(WirdReading(MORNING_ADHKAR, at(monday, 6)), WirdReading(EVENING_ADHKAR, at(monday.minusDays(1), 16)))
        val board = WirdBoard.build(listOf(morning, evening), emptyMap(), readings, at(monday, 9), calendar())
        val today = board.todayItems.associateBy { it.task.id }
        assertTrue(today.getValue(1).done)
        assertTrue(today.getValue(1).auto)
        assertFalse(today.getValue(2).done)
        // Yesterday's evening was read, so the history shows it done.
        val yesterday = board.history.first { it.day == monday.minusDays(1) }
        assertTrue(yesterday.items.single { it.task.id == 2L }.done)
        assertEquals(evening, board.nextToDo?.task)
    }

    @Test
    fun `before the hour the day turns, it is still the day before`() {
        val calendar = calendar(DailyResetSettings(minuteOfDay = 3 * 60))
        val readings = listOf(WirdReading(EVENING_ADHKAR, at(monday, 1)))
        val board = WirdBoard.build(listOf(evening), emptyMap(), readings, at(monday, 2), calendar)
        assertEquals(monday.minusDays(1), board.today)
        assertTrue(board.todayItems.single().done)
    }

    @Test
    fun `a task of one time ticked done leaves today for the finished`() {
        val ticked = mapOf(4L to setOf(monday.minusDays(2)))
        val board = WirdBoard.build(listOf(visit), ticked, emptyList(), at(monday, 9), calendar())
        assertTrue(board.todayItems.isEmpty())
        assertEquals(listOf(4L), board.finished.map { it.task.id })
        assertTrue(board.upcoming.isEmpty())
    }

    @Test
    fun `done tasks go after those still to do, and the timed ones by their time`() {
        val ticked = mapOf(1L to setOf(monday))
        val untimed = WirdTask(id = 5, title = "صدقة", createdAtMillis = created)
        val board = WirdBoard.build(listOf(morning, evening, untimed), ticked, emptyList(), at(monday, 9), calendar())
        assertEquals(listOf(2L, 5L, 1L), board.todayItems.map { it.task.id })
    }

    @Test
    fun `the coming tasks are each at their next day`() {
        val board = WirdBoard.build(listOf(morning, friday), emptyMap(), emptyList(), at(monday, 9), calendar())
        val next = board.upcoming.associate { it.task.id to it.day }
        assertEquals(monday.plusDays(1), next[1L])
        assertEquals(monday.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.FRIDAY)), next[3L])
    }

    @Test
    fun `the history starts from the day a task was added`() {
        val added = WirdTask(id = 6, title = "جديدة", createdAtMillis = at(monday.minusDays(1), 20).toEpochMilli())
        val board = WirdBoard.build(listOf(added), emptyMap(), emptyList(), at(monday, 9), calendar())
        assertEquals(listOf(monday.minusDays(1)), board.history.map { it.day })
        assertFalse(board.history.single().items.single().done)
    }
}
