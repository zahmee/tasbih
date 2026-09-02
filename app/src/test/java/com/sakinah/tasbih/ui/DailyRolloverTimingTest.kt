package com.sakinah.tasbih.ui

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyRolloverTimingTest {
    @Test
    fun `rollover is scheduled for local midnight`() {
        val now = ZonedDateTime.of(
            2026,
            9,
            2,
            23,
            59,
            0,
            0,
            ZoneId.of("Asia/Riyadh"),
        )

        assertEquals(60_000L, millisUntilNextDailyRollover(now))
    }
}
