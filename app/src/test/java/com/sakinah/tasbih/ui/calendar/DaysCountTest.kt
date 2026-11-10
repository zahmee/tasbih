package com.sakinah.tasbih.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class DaysCountTest {
    @Test
    fun `days are counted as they follow a preposition`() {
        assertEquals("يوم واحد", CalendarText.daysCount(1))
        assertEquals("يومين", CalendarText.daysCount(2))
        assertEquals("٣ أيام", CalendarText.daysCount(3))
        assertEquals("١٠ أيام", CalendarText.daysCount(10))
        assertEquals("١١ يومًا", CalendarText.daysCount(11))
        assertEquals("٩٠ يومًا", CalendarText.daysCount(90))
        assertEquals("١٠٠ يوم", CalendarText.daysCount(100))
        assertEquals("١٠٣ أيام", CalendarText.daysCount(103))
        assertEquals("٣٥٤ يومًا", CalendarText.daysCount(354))
    }
}
