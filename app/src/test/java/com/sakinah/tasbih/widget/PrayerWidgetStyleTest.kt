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

    @Test
    fun aLineKeepsItsColourAndLinesSavedBeforeColoursReadAsAutomatic() {
        val lines = listOf(WidgetLine(LineItem.PLACE, LineItem.HIJRI_DATE, LineColor.GOLD), WidgetLine(LineItem.DHIKR))
        assertEquals(lines, WidgetLines.decode(WidgetLines.encode(lines)))
        assertEquals(listOf(WidgetLine(LineItem.PLACE, LineItem.HIJRI_DATE, LineColor.AUTO)), WidgetLines.decode("PLACE|HIJRI_DATE"))
        assertEquals(LineColor.AUTO, WidgetLines.decode("PLACE|NONE|NO_SUCH_COLOUR")!!.single().color)
    }

    @Test
    fun anAutomaticLineHasNoColourOfItsOwnAndNamedColoursAreLiftedOnDarkPaper() {
        val light = WidgetPalette.of(PrayerWidgetStyle(surface = WidgetSurface.IVORY), night = false)
        val dark = WidgetPalette.of(PrayerWidgetStyle(surface = WidgetSurface.NAVY), night = false)
        assertNull(light.line(LineColor.AUTO))
        assertEquals(light.accent, light.line(LineColor.ACCENT))
        assertNotEquals(light.line(LineColor.BLUE), dark.line(LineColor.BLUE))
        assertEquals(dark.text, dark.line(LineColor.INK))
    }

    @Test
    fun theSavedWeightsStillReadAndTimesStayBold() {
        // Saved by earlier versions under these names.
        assertEquals(WidgetWeight.REGULAR, WidgetWeight.valueOf("REGULAR"))
        assertEquals(WidgetWeight.BOLD, WidgetWeight.valueOf("BOLD"))
        assertEquals(WidgetWeight.BOLD, PrayerWidgetStyle().timeWeight)
        assertEquals(WidgetWeight.REGULAR, PrayerWidgetStyle().weight)
    }

    @Test
    fun theBarTurnsFromBlueToRedAsTheAdhanNears() {
        val palette = WidgetPalette.of(PrayerWidgetStyle(surface = WidgetSurface.IVORY), night = false)
        val gradient = PrayerWidgetStyle(progressGradient = true)
        assertEquals(palette.line(LineColor.BLUE), palette.progress(gradient, 0f))
        assertEquals(palette.line(LineColor.RED), palette.progress(gradient, 1f))
        val half = palette.progress(gradient, 0.5f)
        assertNotEquals(palette.line(LineColor.BLUE), half)
        assertNotEquals(palette.line(LineColor.RED), half)
        // Without the gradient, the bar takes its own colour, or the accent.
        assertEquals(palette.accent, palette.progress(PrayerWidgetStyle(), 0.5f))
        assertEquals(palette.line(LineColor.GOLD), palette.progress(PrayerWidgetStyle(progressColor = LineColor.GOLD), 0.9f))
    }

    @Test
    fun aStrongBoxSetsTheWordsOnItAndAFaintOneLeavesThemAsOnThePaper() {
        val ivory = PrayerWidgetStyle(surface = WidgetSurface.IVORY)
        val paper = WidgetPalette.of(ivory, night = false)
        // A black box at three quarters: light words on it.
        val strong = WidgetPalette.inner(ivory.copy(prayersBox = true, boxSurface = WidgetSurface.BLACK, boxOpacity = 75), night = false)
        assertNotEquals(paper.text, strong.text)
        assertEquals(WidgetPalette.of(PrayerWidgetStyle(surface = WidgetSurface.BLACK), night = false).text, strong.text)
        // A faint one: the words stay as on the ivory paper.
        val faint = WidgetPalette.inner(ivory.copy(prayersBox = true, boxSurface = WidgetSurface.BLACK, boxOpacity = 20), night = false)
        assertEquals(paper.text, faint.text)
        // No box at all: nothing changes.
        assertEquals(paper, WidgetPalette.inner(ivory, night = false))
    }

    @Test
    fun theMarkTakesItsOwnColourWithWordsThatReadOnIt() {
        val ivory = PrayerWidgetStyle(surface = WidgetSurface.IVORY)
        val paper = WidgetPalette.of(ivory, night = false)
        val gold = WidgetPalette.inner(ivory.copy(markColor = LineColor.GOLD), night = false)
        assertEquals(paper.line(LineColor.GOLD), gold.accent)
        val white = WidgetPalette.inner(ivory.copy(markColor = LineColor.WHITE), night = false)
        // Dark words on a white mark, light ones on a dark mark.
        assertEquals(0xFF0E1A16.toInt(), white.onAccent)
        val black = WidgetPalette.inner(ivory.copy(markColor = LineColor.BLACK), night = false)
        assertEquals(0xFFFFFBF3.toInt(), black.onAccent)
    }

    @Test
    fun blendingGoesChannelByChannel() {
        assertEquals(0xFF7F7F7F.toInt(), blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0.5f))
        assertEquals(0xFF000000.toInt(), blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), -1f))
        assertEquals(0xFFFFFFFF.toInt(), blend(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 2f))
    }
}
