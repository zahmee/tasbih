package com.sakinah.tasbih

import com.sakinah.tasbih.MainActivity.Companion.ACTION_OPEN
import com.sakinah.tasbih.MainActivity.Companion.externalRequest
import com.sakinah.tasbih.ui.AppOpenRequest
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The contract Natlu relies on: these strings must keep opening these screens. */
class ExternalOpenRequestTest {
    private val today = LocalDate.of(2026, 11, 28)

    @Test
    fun `each published destination opens its screen`() {
        assertEquals("com.sakinah.tasbih.action.OPEN", ACTION_OPEN)
        assertEquals("com.sakinah.tasbih.extra.DESTINATION", MainActivity.EXTRA_DESTINATION)
        assertEquals(AppOpenRequest.Today, externalRequest(ACTION_OPEN, "today", today))
        assertEquals(AppOpenRequest.Prayer, externalRequest(ACTION_OPEN, "prayer", today))
        assertEquals(AppOpenRequest.Calendar(today), externalRequest(ACTION_OPEN, "calendar", today))
        assertEquals(AppOpenRequest.Adhkar, externalRequest(ACTION_OPEN, "adhkar", today))
        assertEquals(AppOpenRequest.Tasbih, externalRequest(ACTION_OPEN, "tasbih", today))
        assertEquals(AppOpenRequest.Qibla, externalRequest(ACTION_OPEN, "qibla", today))
    }

    @Test
    fun `an unknown or missing destination opens the day`() {
        assertEquals(AppOpenRequest.Today, externalRequest(ACTION_OPEN, "wrong", today))
        assertEquals(AppOpenRequest.Today, externalRequest(ACTION_OPEN, "", today))
        assertEquals(AppOpenRequest.Today, externalRequest(ACTION_OPEN, null, today))
    }

    @Test
    fun `another action is left to the app's own requests`() {
        assertNull(externalRequest("android.intent.action.MAIN", "tasbih", today))
        assertNull(externalRequest(null, "tasbih", today))
    }
}
