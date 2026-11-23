package com.sakinah.tasbih.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * The widgets' ornament since November 23, 2026, when the user found the interlaced stars too
 * busy and asked for a mihrab, and a light one: the paper's edge drawn as a niche whose top
 * rises to a pointed arch, in a fine line with a fainter one inside it, and a small star at the
 * arch's point. In one colour, so it lies under the words as a watermark. The widget and the
 * preview in the app draw it with this same code, so the two cannot differ.
 */
object MihrabOrnament {
    const val STROKE_DP = 1f

    /** The outline's distance from the paper's edge. */
    private const val INSET_DP = 6f

    /** The inner line, this far inside the outer. */
    private const val GAP_DP = 4f

    /**
     * Draws the mihrab over [width] by [height] on [canvas], in [color] (alpha included), [unit]
     * being one dp in pixels.
     */
    fun draw(canvas: Canvas, width: Float, height: Float, unit: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = STROKE_DP * unit
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }
        val outer = INSET_DP * unit
        // The arch rises by a share of the height, and never so far that a wide widget looks flat.
        val rise = minOf(height * 0.34f, (width - 2 * outer) * 0.2f, 46 * unit)
        canvas.drawPath(niche(width, height, outer, rise), paint)
        paint.alpha = paint.alpha / 2
        canvas.drawPath(niche(width, height, outer + GAP_DP * unit, rise - GAP_DP * unit * 0.6f), paint)
        // The finial: a small eight-pointed star at the arch's point.
        paint.alpha = Color.alpha(color)
        paint.style = Paint.Style.FILL
        canvas.drawPath(star(width / 2, outer, 3.2f * unit), paint)
    }

    /**
     * A widget-sized picture of the mihrab, clipped to the paper's rounded corners. Kept to at
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
        draw(canvas, width.toFloat(), height.toFloat(), scale, color)
        return bitmap
    }

    /** The niche: straight sides and base, and a pointed arch of [rise] meeting at the middle of the top. */
    private fun niche(width: Float, height: Float, inset: Float, rise: Float): Path {
        val left = inset
        val right = width - inset
        val top = inset
        val bottom = height - inset
        val middle = width / 2
        val spring = top + rise
        val corner = minOf(rise * 0.3f, (bottom - spring) * 0.5f).coerceAtLeast(0f)
        return Path().apply {
            moveTo(left, spring)
            // Each half of the arch bows out from its side and narrows to the point, as a mihrab's does.
            cubicTo(left, spring - rise * 0.62f, middle - (middle - left) * 0.36f, top + rise * 0.1f, middle, top)
            cubicTo(middle + (right - middle) * 0.36f, top + rise * 0.1f, right, spring - rise * 0.62f, right, spring)
            lineTo(right, bottom - corner)
            quadTo(right, bottom, right - corner, bottom)
            lineTo(left + corner, bottom)
            quadTo(left, bottom, left, bottom - corner)
            close()
        }
    }

    private fun star(cx: Float, cy: Float, radius: Float): Path = Path().apply {
        val inner = radius * 0.55f
        for (i in 0 until 16) {
            val angle = Math.PI / 8 * i - Math.PI / 2
            val r = if (i % 2 == 0) radius else inner
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private object Color {
        fun alpha(color: Int): Int = color ushr 24
    }
}
