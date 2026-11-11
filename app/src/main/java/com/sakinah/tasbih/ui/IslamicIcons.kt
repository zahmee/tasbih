package com.sakinah.tasbih.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class IslamicNavSymbol {
    Mihrab,
    Dome,
    Calendar,
    Manuscript,
    Tasbih,
    More,
}

/**
 * Compact navigation glyphs for the Anaa identity, with a shared rounded stroke.
 */
@Composable
internal fun IslamicNavigationIcon(
    symbol: IslamicNavSymbol,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconSize by animateDpAsState(
        targetValue = if (selected) 29.dp else 26.dp,
        label = "Islamic navigation icon size",
    )
    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        label = "Islamic navigation icon selection",
    )
    val color = LocalContentColor.current
    val density = LocalDensity.current

    Canvas(modifier = modifier.size(iconSize)) {
        val unit = size.minDimension / 100f
        val stroke = with(density) { (if (selected) 2.1.dp else 1.75.dp).toPx() }
        when (symbol) {
            IslamicNavSymbol.Mihrab -> drawMihrab(color, unit, stroke, selection)
            IslamicNavSymbol.Dome -> drawDome(color, unit, stroke, selection)
            IslamicNavSymbol.Calendar -> drawCalendarPage(color, unit, stroke, selection)
            IslamicNavSymbol.Manuscript -> drawManuscript(color, unit, stroke, selection)
            IslamicNavSymbol.Tasbih -> drawTasbih(color, unit, stroke, selection)
            IslamicNavSymbol.More -> {
                listOf(24f, 50f, 76f).forEach { x ->
                    drawCircle(color, radius = 6f * unit, center = Offset(x * unit, 50f * unit))
                }
            }
        }
    }
}

private fun DrawScope.drawCrescent(
    color: Color,
    center: Offset,
    unit: Float,
    scale: Float,
) {
    val crescent = Path().apply {
        moveTo(center.x + 12f * unit * scale, center.y - 22f * unit * scale)
        cubicTo(
            center.x - 16f * unit * scale,
            center.y - 18f * unit * scale,
            center.x - 18f * unit * scale,
            center.y + 18f * unit * scale,
            center.x + 11f * unit * scale,
            center.y + 22f * unit * scale,
        )
        cubicTo(
            center.x - 3f * unit * scale,
            center.y + 9f * unit * scale,
            center.x - 1f * unit * scale,
            center.y - 10f * unit * scale,
            center.x + 12f * unit * scale,
            center.y - 22f * unit * scale,
        )
        close()
    }
    drawPath(crescent, color)
}

private fun DrawScope.drawMihrab(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    val house = Path().apply {
        moveTo(18f * unit, 88f * unit)
        lineTo(18f * unit, 42f * unit)
        lineTo(50f * unit, 13f * unit)
        lineTo(82f * unit, 42f * unit)
        lineTo(82f * unit, 88f * unit)
        lineTo(60f * unit, 88f * unit)
        lineTo(60f * unit, 61f * unit)
        quadraticTo(50f * unit, 50f * unit, 40f * unit, 61f * unit)
        lineTo(40f * unit, 88f * unit)
        close()
    }
    drawPath(house, color.copy(alpha = selection))
    drawPath(house, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A domed prayer hall with a crescent finial, for the prayer times. */
private fun DrawScope.drawDome(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    val hall = Path().apply {
        moveTo(16f * unit, 88f * unit)
        lineTo(16f * unit, 58f * unit)
        lineTo(22f * unit, 58f * unit)
        cubicTo(22f * unit, 40f * unit, 36f * unit, 30f * unit, 50f * unit, 26f * unit)
        cubicTo(64f * unit, 30f * unit, 78f * unit, 40f * unit, 78f * unit, 58f * unit)
        lineTo(84f * unit, 58f * unit)
        lineTo(84f * unit, 88f * unit)
        // The doorway is cut from the outline, so it stays open when the hall is filled.
        lineTo(58f * unit, 88f * unit)
        lineTo(58f * unit, 75f * unit)
        quadraticTo(50f * unit, 64f * unit, 42f * unit, 75f * unit)
        lineTo(42f * unit, 88f * unit)
        close()
    }
    drawPath(hall, color.copy(alpha = selection))
    drawPath(hall, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color, Offset(50f * unit, 26f * unit), Offset(50f * unit, 16f * unit), stroke, StrokeCap.Round)
    drawCrescent(color, Offset(50f * unit, 10f * unit), unit, scale = 0.3f)
}

/** A calendar page with a crescent, for the Hijri and Gregorian calendar. */
private fun DrawScope.drawCalendarPage(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    val page = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 14f * unit,
                top = 22f * unit,
                right = 86f * unit,
                bottom = 88f * unit,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * unit),
            ),
        )
    }
    drawPath(page, color.copy(alpha = 0.14f * selection))
    drawPath(page, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color, Offset(14f * unit, 40f * unit), Offset(86f * unit, 40f * unit), stroke, StrokeCap.Round)
    listOf(33f, 67f).forEach { x ->
        drawLine(color, Offset(x * unit, 12f * unit), Offset(x * unit, 28f * unit), stroke, StrokeCap.Round)
    }
    drawCrescent(color, Offset(50f * unit, 64f * unit), unit, scale = 0.72f)
}

private fun DrawScope.drawManuscript(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    val rightPage = Path().apply {
        moveTo(50f * unit, 82f * unit)
        cubicTo(41f * unit, 72f * unit, 28f * unit, 70f * unit, 14f * unit, 74f * unit)
        lineTo(14f * unit, 25f * unit)
        cubicTo(29f * unit, 20f * unit, 42f * unit, 25f * unit, 50f * unit, 35f * unit)
        close()
    }
    val leftPage = Path().apply {
        moveTo(50f * unit, 82f * unit)
        cubicTo(59f * unit, 72f * unit, 72f * unit, 70f * unit, 86f * unit, 74f * unit)
        lineTo(86f * unit, 25f * unit)
        cubicTo(71f * unit, 20f * unit, 58f * unit, 25f * unit, 50f * unit, 35f * unit)
        close()
    }
    drawPath(rightPage, color.copy(alpha = 0.09f * selection))
    drawPath(leftPage, color.copy(alpha = 0.09f * selection))
    listOf(rightPage, leftPage).forEach { page ->
        drawPath(
            page,
            color,
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    drawLine(
        color,
        Offset(50f * unit, 35f * unit),
        Offset(50f * unit, 82f * unit),
        stroke,
        StrokeCap.Round,
    )
    drawEightPointStar(
        center = Offset(50f * unit, 18f * unit),
        outerRadius = 8f * unit,
        innerRadius = 3.4f * unit,
        color = color,
        stroke = stroke * 0.72f,
        fillAlpha = selection * 0.18f,
    )
}

private fun DrawScope.drawTasbih(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    val thread = Path().apply {
        moveTo(27f * unit, 70f * unit)
        cubicTo(8f * unit, 48f * unit, 22f * unit, 15f * unit, 51f * unit, 16f * unit)
        cubicTo(80f * unit, 17f * unit, 91f * unit, 48f * unit, 72f * unit, 69f * unit)
        cubicTo(61f * unit, 81f * unit, 39f * unit, 82f * unit, 27f * unit, 70f * unit)
    }
    drawPath(
        thread,
        color,
        style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    val beads = listOf(
        Offset(20f, 58f), Offset(19f, 39f), Offset(29f, 23f), Offset(48f, 17f),
        Offset(67f, 23f), Offset(79f, 39f), Offset(79f, 58f), Offset(65f, 73f),
        Offset(46f, 79f), Offset(29f, 70f),
    )
    beads.forEachIndexed { index, point ->
        val center = Offset(point.x * unit, point.y * unit)
        val radius = (4.4f + if (index == 8) 1.2f else 0f) * unit
        if (selection > 0.5f) {
            drawCircle(color, radius, center)
        } else {
            drawCircle(color, radius, center, style = Stroke(stroke * 0.82f))
        }
    }
    drawLine(
        color,
        Offset(46f * unit, 84f * unit),
        Offset(46f * unit, 94f * unit),
        stroke,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(46f * unit, 94f * unit),
        Offset(39f * unit, 88f * unit),
        stroke,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(46f * unit, 94f * unit),
        Offset(53f * unit, 88f * unit),
        stroke,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawRosette(
    color: Color,
    unit: Float,
    stroke: Float,
    selection: Float,
) {
    drawEightPointStar(
        center = Offset(50f * unit, 50f * unit),
        outerRadius = 39f * unit,
        innerRadius = 22f * unit,
        color = color,
        stroke = stroke,
        fillAlpha = selection * 0.12f,
    )
    drawCircle(
        color,
        radius = 10f * unit,
        center = Offset(50f * unit, 50f * unit),
        style = if (selection > 0.5f) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke),
    )
    repeat(4) { index ->
        rotate(index * 45f, Offset(50f * unit, 50f * unit)) {
            drawLine(
                color.copy(alpha = 0.78f),
                Offset(50f * unit, 22f * unit),
                Offset(50f * unit, 38f * unit),
                stroke * 0.72f,
                StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawEightPointStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    stroke: Float,
    fillAlpha: Float,
) {
    val star = Path()
    repeat(16) { index ->
        val angle = -PI / 2 + index * PI / 8
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(
            x = center.x + (cos(angle) * radius).toFloat(),
            y = center.y + (sin(angle) * radius).toFloat(),
        )
        if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
    }
    star.close()
    if (fillAlpha > 0f) drawPath(star, color.copy(alpha = fillAlpha))
    drawPath(
        star,
        color,
        style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
