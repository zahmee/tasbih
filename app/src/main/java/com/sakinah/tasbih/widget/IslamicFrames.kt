package com.sakinah.tasbih.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser

/**
 * The widgets' corner and frame ornaments, added on November 28, 2026 at the user's wish: Kufic
 * square corners, arabesque corners, a frame crowned top and bottom, two wings at the sides, a
 * flourished divider, and stepped corners. Drawn for Anaa, after the kinds of ornament the user
 * showed, not copied from them. Every piece is laid out in dp, as on a widget 130 dp high, and
 * scaled to the widget it is drawn on. The widget, its preview and the app's screens all draw
 * them with this code, so they cannot differ.
 */
object IslamicFrames {
    /** One stroke or filled shape of an ornament, as SVG path data in dp. */
    private class Part(data: String, val filled: Boolean = false, val stroke: Float = 1.6f) {
        val path: Path by lazy { PathParser.createPathFromPathData(data) }
    }

    private val KUFIC = listOf(Part("M0 30V0H30M5 25V5H25M5 14H14V5M10 10H18V18H10Z", stroke = 2.2f))

    private val ARABESQUE = listOf(
        Part("M2 38C2 16 12 4 36 2"),
        Part("M36 2c7 0 9 7 4 9c-4 1-6-3-3-5"),
        Part("M2 38c0 7 7 9 9 4c1-4-3-6-5-3"),
        Part("M6 6c9 0 13 4 12 12c-8 1-12-3-12-12z", filled = true),
        Part("M22.4 16a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z", filled = true),
    )

    private val STEPPED = listOf(Part("M3 26V3H26M3 12H12V3", stroke = 2f))

    /** The crown over the middle of the frame's top, centred on the line. */
    private val CROWN = listOf(
        Part("M0 -9c4 5 4 11 0 16c-4-5-4-11 0-16z", filled = true),
        Part("M-3 4c-8-6-20-5-26 1c-3 4 1 8 5 6c3-2 1-5-2-4", stroke = 1.5f),
        Part("M3 4c8-6 20-5 26 1c3 4-1 8-5 6c-3-2-1-5 2-4", stroke = 1.5f),
        Part("M-30 6c-8 3-14 3-20 0M30 6c8 3 14 3 20 0", stroke = 1.5f),
    )

    /** Half the crown's width, where the frame's line breaks for it. */
    private const val CROWN_HALF = 50f

    /** A wing at the left side, its diamond on the side's middle. */
    private val WING = listOf(
        Part("M0 0l7-7l7 7l-7 7z", filled = true),
        Part("M7 -7c0-10 6-18 14-22c4-2 7 2 4 5c-3 2-5-1-3-3", stroke = 1.5f),
        Part("M7 7c0 10 6 18 14 22c4 2 7-2 4-5c-3-2-5 1-3 3", stroke = 1.5f),
        Part("M14 0c8 0 12-3 12-8M14 0c8 0 12 3 12 8", stroke = 1.5f),
    )

    /**
     * The flourish at a divider's middle, without its lines: an upright leaf, and to each side
     * a stem that curls up and back on itself, with a tail below it and a dot at its end.
     */
    private val FLOURISH = listOf(
        Part("M0-8c3.5 4 3.5 12 0 16c-3.5-4-3.5-12 0-16z", filled = true),
        Part("M5 0c4-7 13-8 17-3c3 4-1 8-4 6c-2-1.5-1-4.5 1.5-3.5", stroke = 1.4f),
        Part("M-5 0c-4-7-13-8-17-3c-3 4 1 8 4 6c2-1.5 1-4.5-1.5-3.5", stroke = 1.4f),
        Part("M5 0c7 3 14 4 21 2M-5 0c-7 3-14 4-21 2", stroke = 1.2f),
        Part("M27 2a1.4 1.4 0 1 0 2.8 0a1.4 1.4 0 1 0-2.8 0zM-27 2a1.4 1.4 0 1 1-2.8 0a1.4 1.4 0 1 1 2.8 0z", filled = true),
    )

    /** Whether [ornament] draws over the whole widget, rather than only between its lines. */
    fun framesWidget(ornament: WidgetOrnament): Boolean = when (ornament) {
        WidgetOrnament.KUFIC_CORNERS, WidgetOrnament.ARABESQUE_CORNERS, WidgetOrnament.CROWNED_FRAME,
        WidgetOrnament.WINGS, WidgetOrnament.STEPPED_CORNERS -> true
        else -> false
    }

    /**
     * The widget's padding in dp, across and down, so the words keep clear of the ornament:
     * the wings take the sides, the crowns the top and bottom, the corners a little of each.
     */
    fun padding(ornament: WidgetOrnament): Pair<Int, Int> = when (ornament) {
        WidgetOrnament.KUFIC_CORNERS, WidgetOrnament.ARABESQUE_CORNERS, WidgetOrnament.STEPPED_CORNERS -> 30 to 8
        WidgetOrnament.CROWNED_FRAME -> 18 to 20
        WidgetOrnament.WINGS -> 34 to 6
        else -> 10 to 6
    }

    /**
     * Draws [ornament] over [width] by [height] on [canvas] in [color], alpha included, [unit]
     * being one dp in pixels. Draws nothing for the ornaments that are not frames.
     */
    fun draw(ornament: WidgetOrnament, canvas: Canvas, width: Float, height: Float, unit: Float, color: Int) {
        // Pieces drawn for a widget 130 dp high; smaller on a lower one, never larger. The
        // corners are kept smaller still, so the words and the bar keep clear of them.
        val k = unit * (height / unit / 130f).coerceIn(0.7f, 1f)
        val corner = k * 0.8f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        when (ornament) {
            WidgetOrnament.KUFIC_CORNERS -> corners(canvas, KUFIC, width, height, 8 * unit, corner, paint, Paint.Cap.SQUARE)
            WidgetOrnament.ARABESQUE_CORNERS -> corners(canvas, ARABESQUE, width, height, 7 * unit, corner, paint, Paint.Cap.ROUND)
            WidgetOrnament.STEPPED_CORNERS -> corners(canvas, STEPPED, width, height, 6 * unit, corner, paint, Paint.Cap.SQUARE)
            WidgetOrnament.CROWNED_FRAME -> crownedFrame(canvas, width, height, unit, k, paint)
            WidgetOrnament.WINGS -> wings(canvas, width, height, unit, k, paint)
            else -> Unit
        }
    }

    /**
     * One corner of [ornament], one of the three kinds of corners, its angle at [x], [y] and
     * turned toward the right or down by [flipX] and [flipY]. [scale] is one design dp in pixels.
     */
    fun corner(ornament: WidgetOrnament, canvas: Canvas, x: Float, y: Float, flipX: Boolean, flipY: Boolean, scale: Float, color: Int) {
        val (parts, cap) = when (ornament) {
            WidgetOrnament.KUFIC_CORNERS -> KUFIC to Paint.Cap.SQUARE
            WidgetOrnament.ARABESQUE_CORNERS -> ARABESQUE to Paint.Cap.ROUND
            WidgetOrnament.STEPPED_CORNERS -> STEPPED to Paint.Cap.SQUARE
            else -> return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val matrix = Matrix().apply {
            setScale(if (flipX) -scale else scale, if (flipY) -scale else scale)
            postTranslate(x, y)
        }
        place(canvas, parts, matrix, scale, paint, cap)
    }

    /**
     * The flourished divider centred at [cx], [cy]: the flourish and a fine line to each side,
     * [reach] from the middle. [scale] is one design dp in pixels.
     */
    fun flourish(canvas: Canvas, cx: Float, cy: Float, reach: Float, scale: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = scale.coerceAtLeast(1f)
        paint.strokeCap = Paint.Cap.ROUND
        val gap = 34 * scale
        if (reach > gap) {
            canvas.drawLine(cx - reach, cy, cx - gap, cy, paint)
            canvas.drawLine(cx + gap, cy, cx + reach, cy, paint)
        }
        place(canvas, FLOURISH, Matrix().apply { setScale(scale, scale); postTranslate(cx, cy) }, scale, paint, Paint.Cap.ROUND)
    }

    /**
     * A widget-sized picture of [ornament], clipped to the paper's rounded corners, at most
     * twice the screen's density, as a widget's pictures share a small allowance of memory.
     */
    fun bitmap(ornament: WidgetOrnament, widthDp: Int, heightDp: Int, density: Float, radiusDp: Float, color: Int): Bitmap {
        val scale = density.coerceAtMost(2f)
        val width = (widthDp * scale).toInt().coerceIn(1, 1200)
        val height = (heightDp * scale).toInt().coerceIn(1, 1200)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(
            Path().apply {
                addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radiusDp * scale, radiusDp * scale, Path.Direction.CW)
            },
        )
        draw(ornament, canvas, width.toFloat(), height.toFloat(), scale, color)
        return bitmap
    }

    /** The flourished divider as a picture [widthDp] by [heightDp], in white for the widget to tint. */
    fun dividerBitmap(widthDp: Int, heightDp: Int, density: Float): Bitmap {
        val scale = density.coerceAtMost(2f)
        val width = (widthDp * scale).toInt().coerceAtLeast(1)
        val height = (heightDp * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // The flourish is 20 dp high; it is set to fill the height, its lines the width.
        val design = height / 20f
        flourish(Canvas(bitmap), width / 2f, height / 2f, width / 2f - scale, design, 0xFFFFFFFF.toInt())
        return bitmap
    }

    private fun corners(canvas: Canvas, parts: List<Part>, width: Float, height: Float, inset: Float, k: Float, paint: Paint, cap: Paint.Cap) {
        listOf(
            Triple(inset, inset, 1f to 1f),
            Triple(width - inset, inset, -1f to 1f),
            Triple(inset, height - inset, 1f to -1f),
            Triple(width - inset, height - inset, -1f to -1f),
        ).forEach { (x, y, flip) ->
            place(canvas, parts, Matrix().apply { setScale(flip.first * k, flip.second * k); postTranslate(x, y) }, k, paint, cap)
        }
    }

    /** A fine frame with small rings at its corners, broken top and bottom for a crown. */
    private fun crownedFrame(canvas: Canvas, width: Float, height: Float, unit: Float, k: Float, paint: Paint) {
        // Far enough in that the rings clear the paper's rounded corners.
        val left = 14 * unit
        val right = width - 14 * unit
        val top = 12 * unit
        val bottom = height - 12 * unit
        val ring = 3 * unit
        // The crowns smaller than the corners' scale, so the words and the bar keep clear of them.
        val crown = k * 0.7f
        val half = CROWN_HALF * crown
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit
        val middle = width / 2
        for (y in listOf(top, bottom)) {
            canvas.drawLine(left + ring, y, middle - half, y, paint)
            canvas.drawLine(middle + half, y, right - ring, y, paint)
        }
        for (x in listOf(left, right)) canvas.drawLine(x, top + ring, x, bottom - ring, paint)
        for (x in listOf(left, right)) for (y in listOf(top, bottom)) canvas.drawCircle(x, y, ring, paint)
        place(canvas, CROWN, Matrix().apply { setScale(crown, crown); postTranslate(middle, top) }, crown, paint, Paint.Cap.ROUND)
        place(canvas, CROWN, Matrix().apply { setScale(crown, -crown); postTranslate(middle, bottom) }, crown, paint, Paint.Cap.ROUND)
    }

    /** A wing at each side's middle, and a fine rule along the top and the bottom between them. */
    private fun wings(canvas: Canvas, width: Float, height: Float, unit: Float, k: Float, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit
        val inset = 5 * unit
        canvas.drawLine(48 * unit, inset, width - 48 * unit, inset, paint)
        canvas.drawLine(48 * unit, height - inset, width - 48 * unit, height - inset, paint)
        place(canvas, WING, Matrix().apply { setScale(k, k); postTranslate(8 * unit, height / 2) }, k, paint, Paint.Cap.ROUND)
        place(canvas, WING, Matrix().apply { setScale(-k, k); postTranslate(width - 8 * unit, height / 2) }, k, paint, Paint.Cap.ROUND)
    }

    /** Draws [parts] through [matrix], their strokes [k] pixels to the design dp. */
    private fun place(canvas: Canvas, parts: List<Part>, matrix: Matrix, k: Float, paint: Paint, cap: Paint.Cap) {
        val path = Path()
        for (part in parts) {
            path.reset()
            part.path.transform(matrix, path)
            if (part.filled) {
                paint.style = Paint.Style.FILL
            } else {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (part.stroke * k).coerceAtLeast(1f)
                paint.strokeCap = cap
                paint.strokeJoin = Paint.Join.MITER
            }
            canvas.drawPath(path, paint)
        }
    }
}
