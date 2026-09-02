package com.sakinah.tasbih.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val SakinahContentMaxWidth = 840.dp
internal val SakinahReadingMaxWidth = 920.dp

/** A quiet parchment field shared by every destination in the app. */
@Composable
internal fun SakinahScreenBackground(
    modifier: Modifier = Modifier,
    showOrnament: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val brand = LocalSakinahBrandColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(brand.backdropTop, brand.backdropBottom),
                ),
            )
            .drawBehind {
                if (!showOrnament) return@drawBehind

                val ornament = brand.ornament
                val largeRadius = size.minDimension * 0.17f
                drawEightPointRosette(
                    center = Offset(size.width * 0.08f, size.height * 0.075f),
                    outerRadius = largeRadius,
                    innerRadius = largeRadius * 0.47f,
                    color = ornament.copy(alpha = 0.045f),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(
                    color = ornament.copy(alpha = 0.032f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.08f, size.height * 0.075f),
                    style = Stroke(1.dp.toPx()),
                )

                val smallRadius = size.minDimension * 0.09f
                drawEightPointRosette(
                    center = Offset(size.width * 0.94f, size.height * 0.7f),
                    outerRadius = smallRadius,
                    innerRadius = smallRadius * 0.47f,
                    color = ornament.copy(alpha = 0.035f),
                    strokeWidth = 0.8.dp.toPx(),
                )
            },
        content = content,
    )
}

/** Branded heading used at the start of top-level destinations. */
@Composable
internal fun SakinahScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useStackedActions = trailing != null && maxWidth < 520.dp

        if (useStackedActions) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SakinahHeaderCopy(
                    title = title,
                    subtitle = subtitle,
                    eyebrow = eyebrow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    trailing()
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                SakinahHeaderCopy(
                    title = title,
                    subtitle = subtitle,
                    eyebrow = eyebrow,
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                } else {
                    SakinahRosette(modifier = Modifier.padding(top = 2.dp).size(42.dp))
                }
            }
        }
    }
}

@Composable
private fun SakinahHeaderCopy(
    title: String,
    subtitle: String?,
    eyebrow: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = LocalSakinahBrandColors.current.antiqueGold,
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                lineHeight = 38.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Section label with a restrained gold rule and eight-point terminal. */
@Composable
internal fun SakinahSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val gold = LocalSakinahBrandColors.current.antiqueGold
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                lineHeight = 30.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.width(12.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(18.dp),
        ) {
            val centerY = size.height / 2f
            val starRadius = 4.5.dp.toPx()
            drawLine(
                color = gold.copy(alpha = 0.36f),
                start = Offset(0f, centerY),
                end = Offset((size.width - starRadius * 3f).coerceAtLeast(0f), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawEightPointRosette(
                center = Offset(size.width - starRadius, centerY),
                outerRadius = starRadius,
                innerRadius = starRadius * 0.46f,
                color = gold,
                strokeWidth = 0.8.dp.toPx(),
            )
        }
    }
}

@Composable
internal fun SakinahRosette(
    modifier: Modifier = Modifier,
    color: Color = LocalSakinahBrandColors.current.antiqueGold,
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.42f
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = size.minDimension / 2f,
        )
        drawEightPointRosette(
            center = center,
            outerRadius = radius,
            innerRadius = radius * 0.46f,
            color = color,
            strokeWidth = 1.2.dp.toPx(),
        )
        drawCircle(color = color, radius = size.minDimension * 0.065f)
    }
}

@Composable
internal fun sakinahCardBorder(alpha: Float = 0.18f): BorderStroke = BorderStroke(
    width = 1.dp,
    color = LocalSakinahBrandColors.current.antiqueGold.copy(alpha = alpha),
)

private fun DrawScope.drawEightPointRosette(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    strokeWidth: Float,
) {
    val path = Path()
    repeat(16) { index ->
        val angle = -PI / 2 + index * PI / 8
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(
            x = center.x + (cos(angle) * radius).toFloat(),
            y = center.y + (sin(angle) * radius).toFloat(),
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}
