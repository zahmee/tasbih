package com.sakinah.tasbih.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class LineInsetsTest {
    @Test
    fun `the lines' ends meet the edges of the outer prayers' words`() {
        // A row of 300 split in five cells of 60, each 56 inside its margins; words 40 wide
        // stand 8 in from their cell's inner edge, 10 from the row's.
        val insets = LineInsets.of(row = 300f, box = 0f, right = 40f, left = 40f)
        assertEquals(10f, insets.right, 0.001f)
        assertEquals(10f, insets.left, 0.001f)
    }

    @Test
    fun `the box's padding and each side's own words count`() {
        val insets = LineInsets.of(row = 306f, box = 3f, right = 30f, left = 50f)
        assertEquals(3f + 2f + (56f - 30f) / 2f, insets.right, 0.001f)
        assertEquals(3f + 2f + (56f - 50f) / 2f, insets.left, 0.001f)
    }

    @Test
    fun `words wider than their cell, or an unknown width, set nothing in`() {
        assertEquals(LineInsets(0f, 0f), LineInsets.of(row = 0f, box = 0f, right = 40f, left = 40f))
        assertEquals(0f, LineInsets.of(row = 300f, box = 0f, right = 90f, left = 40f).right, 0f)
    }
}
