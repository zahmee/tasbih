package com.sakinah.tasbih.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerWidgetStyleTest {
    @Test
    fun linesReadBackAsWritten() {
        val lines = listOf(WidgetLine(LineItem.HIJRI_DATE, LineItem.GREGORIAN_DATE), WidgetLine(LineItem.COUNTDOWN))
        assertEquals(lines, WidgetLines.decode(WidgetLines.encode(lines)))
    }

    @Test
    fun unreadableLinesAreDroppedAndNothingSavedMeansDefault() {
        assertNull(WidgetLines.decode(null))
        assertEquals(emptyList<WidgetLine>(), WidgetLines.decode(""))
        assertEquals(listOf(WidgetLine(LineItem.PLACE)), WidgetLines.decode("BROKEN|X;PLACE|MISSING"))
    }

    @Test
    fun noMoreThanThreeLines() {
        val many = List(5) { WidgetLine(LineItem.DHIKR) }
        assertEquals(PrayerWidgetStyle.MAX_LINES, WidgetLines.decode(WidgetLines.encode(many))!!.size)
    }

    @Test
    fun theBarFillsWithTheTimePassedAndStaysWithinItsEnds() {
        val isha = java.time.Instant.parse("2026-11-19T16:03:00Z")
        val fajr = java.time.Instant.parse("2026-11-20T02:00:00Z")
        val halfway = isha.plusMillis(java.time.Duration.between(isha, fajr).toMillis() / 2)
        assertEquals(500, passedShare(isha, fajr, halfway))
        assertEquals(0, passedShare(isha, fajr, isha.minusSeconds(60)))
        assertEquals(1000, passedShare(isha, fajr, fajr.plusSeconds(60)))
        assertEquals(1000, passedShare(fajr, fajr, fajr))
    }

    @Test
    fun theNextWidgetKeepsAtMostTwoLines() {
        assertEquals(listOf(WidgetLine(LineItem.PLACE)), PrayerWidgetStyle().nextLines)
        assertEquals(2, PrayerWidgetStyle.MAX_NEXT_LINES)
    }

    @Test
    fun automaticInkFollowsThePaper() {
        val day = WidgetPalette.of(PrayerWidgetStyle(), night = false)
        val night = WidgetPalette.of(PrayerWidgetStyle(), night = true)
        assertNotEquals(day.text, night.text)
        // Light ink on the green paper, whatever the hour.
        assertEquals(night.text, WidgetPalette.of(PrayerWidgetStyle(surface = WidgetSurface.PINE), night = false).text)
    }

    @Test
    fun aFilledMarkCarriesWordsThatStandOutFromIt() {
        val gold = WidgetPalette.of(PrayerWidgetStyle(accent = WidgetAccent.GOLD, surface = WidgetSurface.PINE), night = false)
        assertNotEquals(gold.accent, gold.nextText(NextMark.FILLED))
        assertEquals(gold.accent, gold.nextText(NextMark.OUTLINE))
    }
}
