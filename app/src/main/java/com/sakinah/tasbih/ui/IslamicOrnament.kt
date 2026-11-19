package com.sakinah.tasbih.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The home screen's ornament, all of one family: the khatam, the eight-point star made of two
 * squares, and the pointed arch of a mihrab. Each is drawn as a thin line or a faint tint and
 * marks something, the lead card, the next prayer, a service, so none is decoration alone.
 * They keep the identity's colours when the user turns on the phone's own colours.
 */

/** Where the two squares of a khatam cross, as a share of the radius to its points. */
private const val KHATAM_INNER = 0.7654f

/** The outline of a khatam with a point up, down and to each side. */
internal fun khatamPath(center: Offset, radius: Float): Path = Path().apply { addKhatam(center, radius) }

private fun Path.addKhatam(center: Offset, radius: Float) {
    repeat(16) { index ->
        val angle = -PI / 2 + index * PI / 8
        val reach = if (index % 2 == 0) radius else radius * KHATAM_INNER
        val x = center.x + (cos(angle) * reach).toFloat()
        val y = center.y + (sin(angle) * reach).toFloat()
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/**
 * The star-and-cross lattice of old tilework: khatams touching point to point, which leaves a
 * cross between every four. It is strongest at the end edge and fades out before the middle,
 * so it sits behind the card's quieter side and never behind a line of text at full strength.
 */
@Composable
internal fun KhatamLattice(color: Color, modifier: Modifier = Modifier, cell: Dp = 34.dp) {
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val step = cell.toPx()
                val rtl = layoutDirection == LayoutDirection.Rtl
                val lattice = Path()
                var row = 0
                while (row * step <= size.height + step) {
                    var column = 0
                    while (column * step <= size.width * LATTICE_REACH + step) {
                        val x = if (rtl) column * step else size.width - column * step
                        lattice.addKhatam(Offset(x, row * step), step / 2)
                        column++
                    }
                    row++
                }
                val endEdge = if (rtl) 0f else size.width
                val fadeEnd = if (rtl) size.width * LATTICE_REACH else size.width * (1 - LATTICE_REACH)
                val fade = Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = endEdge, endX = fadeEnd)
                val stroke = Stroke(1.dp.toPx(), join = StrokeJoin.Round)
                onDrawBehind {
                    drawPath(lattice, color, style = stroke)
                    drawRect(fade, blendMode = BlendMode.DstIn)
                }
            },
    )
}

private const val LATTICE_REACH = 0.62f

/** A hairline inside a card's edge, as the ruled frame round a manuscript page. */
internal fun Modifier.ornamentFrame(shape: Shape, color: Color, inset: Dp = 6.dp): Modifier = drawWithCache {
    val gap = inset.toPx()
    val outline = shape.createOutline(Size(size.width - gap * 2, size.height - gap * 2), layoutDirection, this)
    val stroke = Stroke(1.dp.toPx())
    onDrawBehind {
        translate(gap, gap) { drawOutline(outline, color, style = stroke) }
    }
}

/** A service's icon in a khatam: a faint fill and a copper line. */
@Composable
internal fun KhatamMedallion(icon: ImageVector, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val line = LocalSakinahBrandColors.current.antiqueGold.copy(alpha = 0.6f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .drawBehind {
                val star = khatamPath(center, size.minDimension / 2)
                drawPath(star, fill)
                drawPath(star, line, style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
            },
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

/** A rule that fades out to both sides of a small khatam, between the date and the day's prayer. */
@Composable
internal fun KhatamDivider(modifier: Modifier = Modifier) {
    val copper = LocalSakinahBrandColors.current.antiqueGold
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val radius = 5.5.dp.toPx()
        val gap = radius + 6.dp.toPx()
        val line = copper.copy(alpha = 0.5f)
        val width = 1.dp.toPx()
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, line), startX = 0f, endX = center.x - gap),
            start = Offset(0f, center.y),
            end = Offset(center.x - gap, center.y),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
        drawLine(
            Brush.horizontalGradient(listOf(line, Color.Transparent), startX = center.x + gap, endX = size.width),
            start = Offset(center.x + gap, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
        drawPath(khatamPath(center, radius), copper, style = Stroke(width, join = StrokeJoin.Round))
        drawCircle(copper, radius * 0.24f)
    }
}

/** A small filled khatam, set before a label as a manuscript sets one before a heading. */
@Composable
internal fun KhatamMark(modifier: Modifier = Modifier, color: Color = LocalSakinahBrandColors.current.antiqueGold) {
    Canvas(modifier.size(9.dp)) { drawPath(khatamPath(center, size.minDimension / 2), color) }
}

/** The pointed arch of a mihrab, which holds the prayer whose time comes next. */
internal val MihrabArchShape: Shape = GenericShape { size, _ ->
    val rise = minOf(size.width * 0.5f, size.height * 0.42f)
    val corner = size.minDimension * 0.12f
    moveTo(0f, size.height - corner)
    lineTo(0f, rise)
    quadraticTo(0f, rise * 0.2f, size.width / 2, 0f)
    quadraticTo(size.width, rise * 0.2f, size.width, rise)
    lineTo(size.width, size.height - corner)
    quadraticTo(size.width, size.height, size.width - corner, size.height)
    lineTo(corner, size.height)
    quadraticTo(0f, size.height, 0f, size.height - corner)
    close()
}
