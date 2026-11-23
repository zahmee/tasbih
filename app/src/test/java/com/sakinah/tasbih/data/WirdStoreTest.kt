package com.sakinah.tasbih.data

import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.wird.WirdRepeat
import com.sakinah.tasbih.wird.WirdTask
import com.sakinah.tasbih.wird.WirdTimeKind
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WirdStoreTest {
    @Test
    fun `a task comes back from storage as it went in`() {
        val task = WirdTask(
            id = 7,
            title = "صيام",
            presetId = "monday_thursday",
            repeat = WirdRepeat.WEEKLY,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY),
            timeKind = WirdTimeKind.PRAYER,
            prayer = Prayer.FAJR,
            offsetMinutes = -45,
            remind = true,
            sortOrder = 3,
            createdAtMillis = 99,
        )
        assertEquals(task, task.toRow().toTask())
        val once = WirdTask(id = 8, title = "زيارة", repeat = WirdRepeat.ONCE, date = LocalDate.of(2026, 12, 1), timeKind = WirdTimeKind.CLOCK, minuteOfDay = 600)
        assertEquals(once, once.toRow().toTask())
        val hijri = WirdTask(id = 9, title = "البيض", repeat = WirdRepeat.HIJRI_MONTHLY, hijriDays = setOf(15, 13, 14))
        assertEquals("13,14,15", hijri.toRow().hijriDays)
        assertEquals(hijri, hijri.toRow().toTask())
    }

    @Test
    fun `an unknown name read back falls to a safe choice`() {
        val row = WirdTask(id = 1, title = "x").toRow().copy(repeat = "YEARLY", timeKind = "SOON", prayer = "TAHAJJUD", hijriDays = "0,31,x,5")
        val task = row.toTask()
        assertEquals(WirdRepeat.DAILY, task.repeat)
        assertEquals(WirdTimeKind.NONE, task.timeKind)
        assertEquals(null, task.prayer)
        assertEquals(setOf(5), task.hijriDays)
    }
}
