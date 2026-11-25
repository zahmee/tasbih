package com.sakinah.tasbih.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeLogTest {
    private val minute = 60_000L
    private val noon = 1_795_600_000_000L

    @Test
    fun `an alarm that rings is written with how late it rang`() {
        val log = WakeLog.update(emptyList(), planned = listOf(noon, noon + 20 * minute), rangFor = noon, now = noon + 40, judgeMissed = true)
        assertEquals(listOf(WakeRecord(noon, noon + 40, rang = true)), log)
        assertEquals(0, log.single().lateMinutes)
        val late = WakeLog.update(emptyList(), listOf(noon), rangFor = noon, now = noon + 12 * minute, judgeMissed = true)
        assertEquals(12, late.single().lateMinutes)
    }

    @Test
    fun `a planned wake that passed without its alarm is written as never delivered`() {
        // The alarm for noon never came; the one twenty minutes later did.
        val later = noon + 20 * minute
        val log = WakeLog.update(emptyList(), planned = listOf(noon, later, later + 15 * minute), rangFor = later, now = later + 50, judgeMissed = true)
        assertEquals(listOf(WakeRecord(noon, later + 50, rang = false), WakeRecord(later, later + 50, rang = true)), log)
    }

    @Test
    fun `an alarm still on its way is not yet called dropped`() {
        // The app was opened half a minute after noon, with noon's alarm not yet handled.
        val log = WakeLog.update(emptyList(), planned = listOf(noon), rangFor = null, now = noon + minute / 2, judgeMissed = true)
        assertEquals(emptyList<WakeRecord>(), log)
    }

    @Test
    fun `a late alarm corrects the record that called it dropped`() {
        val later = noon + 5 * minute
        val first = WakeLog.update(emptyList(), planned = listOf(noon, later), rangFor = later, now = later + 10, judgeMissed = true)
        assertEquals(false, first.first().rang)
        // Noon's own alarm arrives right behind the other.
        val second = WakeLog.update(first, planned = emptyList(), rangFor = noon, now = later + 900, judgeMissed = true)
        assertEquals(listOf(true, true), second.map { it.rang })
        assertEquals(5, second.first().lateMinutes)
    }

    @Test
    fun `the first of two alarms for one wake is the one kept`() {
        val first = WakeLog.update(emptyList(), listOf(noon), rangFor = noon, now = noon + 3 * minute, judgeMissed = true)
        val second = WakeLog.update(first, emptyList(), rangFor = noon, now = noon + 9 * minute, judgeMissed = true)
        assertEquals(3, second.single().lateMinutes)
    }

    @Test
    fun `nothing is called dropped after a restart or a change of the clock`() {
        val log = WakeLog.update(emptyList(), planned = listOf(noon), rangFor = null, now = noon + 90 * minute, judgeMissed = false)
        assertEquals(emptyList<WakeRecord>(), log)
    }

    @Test
    fun `only the last wakes are kept, and they survive being saved`() {
        var log = emptyList<WakeRecord>()
        repeat(WakeLog.KEEP + 5) { index ->
            val at = noon + index * 20 * minute
            log = WakeLog.update(log, listOf(at), rangFor = at, now = at + 5, judgeMissed = true)
        }
        assertEquals(WakeLog.KEEP, log.size)
        assertEquals(noon + 5 * 20 * minute, log.first().plannedAt)
        assertEquals(log, WakeLog.decode(WakeLog.encode(log)))
        assertEquals(emptyList<WakeRecord>(), WakeLog.decode(null))
        assertEquals(emptyList<WakeRecord>(), WakeLog.decode("not a record"))
    }
}
