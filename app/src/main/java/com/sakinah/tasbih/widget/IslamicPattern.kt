package com.sakinah.tasbih.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The classic pattern of eight-pointed stars and crosses: a star of two squares at every point of
 * a square grid, whose tips touch those of its neighbours, so the spaces between four stars close
 * into crosses; a small star marks the middle of each cross. Drawn in lines, in one colour, so it
 * lies under the words as a watermark. The widget and the preview in the app draw it with this
 * same code, so the two cannot differ.
 */
object IslamicPattern {
    /** The tip of a star two squares make sits this much further out than the bend between tips. */
    private val INNER = (cos(Math.PI / 4) / cos(Math.PI / 8)).toFloat()

    /**
     * Fills [width] by [height] on [canvas] with the pattern, its grid [cell] wide, in [color]
     * (alpha included) and lines [stroke] thick. The grid is centred, so the pattern sits evenly.
     */
    fun draw(canvas: Canvas, width: Float, height: Float, cell: Float, color: Int, stroke: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeJoin = Paint.Join.MITER
            this.color = color
        }
        val path = Path()
        val startX = width / 2 - cell * (floor(width / 2 / cell) + 1)
        val startY = height / 2 - cell * (floor(height / 2 / cell) + 1)
        var y = startY
        while (y <= height + cell) {
            var x = startX
            while (x <= width + cell) {
                star(path, x, y, cell / 2)
                star(path, x + cell / 2, y + cell / 2, cell * 0.17f)
                x += cell
            }
            y += cell
        }
        canvas.drawPath(path, paint)
    }

    /**
     * A widget-sized picture of the pattern, clipped to the paper's rounded corners. Kept to at
     * most twice the screen's density, as a widget's pictures share a small allowance of memory.
     */
    fun bitmap(widthDp: Int, heightDp: Int, density: Float, radiusDp: Float, color: Int): Bitmap {
        val scale = density.coerceAtMost(2f)
        val width = (widthDp * scale).toInt().coerceIn(1, 1200)
        val height = (heightDp * scale).toInt().coerceIn(1, 1200)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clip = Path().apply {
            addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radiusDp * scale, radiusDp * scale, Path.Direction.CW)
        }
        canvas.clipPath(clip)
        draw(canvas, width.toFloat(), height.toFloat(), CELL_DP * scale, color, STROKE_DP * scale)
        return bitmap
    }

    /** An eight-pointed star of [radius], its tips up, down and to the sides. */
    private fun star(path: Path, cx: Float, cy: Float, radius: Float) {
        for (i in 0 until 16) {
            val angle = Math.PI / 8 * i
            val r = if (i % 2 == 0) radius else radius * INNER
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    /** The grid's step: three stars across the height of a small widget. */
    const val CELL_DP = 26f
    const val STROKE_DP = 1f
}
