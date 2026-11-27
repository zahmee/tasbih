package com.sakinah.tasbih.widget

/**
 * How far in from each side the lines over the day's prayers start, so their two ends line up
 * with the outer edges of Fajr's words on the right and Isha's on the left. Until November 27,
 * 2026 the lines ran to the widget's edges while the prayers sat in the middle of their cells,
 * and the two never met.
 *
 * All in one unit (pixels or dp alike): [row] is the width the lines and the prayers share,
 * [box] the prayers' box padding when there is one, [right] and [left] the widths of the words
 * in the first and last cells.
 */
data class LineInsets(val right: Float, val left: Float) {
    companion object {
        /** Each prayer's cell keeps this margin all round, as the widget's cell style does. */
        const val CELL_MARGIN = 2f

        fun of(row: Float, box: Float, right: Float, left: Float): LineInsets {
            if (row <= 0f) return LineInsets(0f, 0f)
            val inner = (row - 2 * box) / 5f - 2 * CELL_MARGIN
            fun inset(words: Float) = (box + CELL_MARGIN + (inner - words) / 2f).coerceIn(0f, row / 4f)
            return LineInsets(inset(right), inset(left))
        }
    }
}
