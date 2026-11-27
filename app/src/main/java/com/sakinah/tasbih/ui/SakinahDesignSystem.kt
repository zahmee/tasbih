package com.sakinah.tasbih.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors

internal object SakinahSpacing {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val Page = 20.dp
    val Section = 24.dp
}

internal val SakinahContentMaxWidth = 840.dp
internal val SakinahReadingMaxWidth = 920.dp

private val FocusExpandBlueLight = Color(0xFF1565C0)
private val FocusExpandBlueDark = Color(0xFF90CAF9)
private val FocusExitRedLight = Color(0xFFB3261E)
private val FocusExitRedDark = Color(0xFFFFB4AB)

/**
 * Shared focus-mode control used by both reading and tasbih flows.
 *
 * The colored affordance belongs to the icon, not the button's surface. This keeps the
 * control calm in both themes while preserving the familiar blue-to-enter and red-to-exit cue.
 */
@Composable
internal fun FocusModeIconButton(
    isExit: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val iconColor = when {
        isExit && isDarkSurface -> FocusExitRedDark
        isExit -> FocusExitRedLight
        isDarkSurface -> FocusExpandBlueDark
        else -> FocusExpandBlueLight
    }
    val neutralContainer = MaterialTheme.colorScheme.surfaceContainerHigh

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = neutralContainer,
            contentColor = iconColor,
            disabledContainerColor = neutralContainer,
            disabledContentColor = iconColor.copy(alpha = 0.38f),
        ),
    ) {
        Icon(
            imageVector = if (isExit) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
            contentDescription = contentDescription,
        )
    }
}

/** A quiet parchment field shared by every destination in the app. */
@Composable
internal fun SakinahScreenBackground(
    modifier: Modifier = Modifier,
    showOrnament: Boolean = false,
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
                // Two stepped Kufic corners, very faint, at the foot's two ends, since November 28,
                // 2026 on every screen that has this background; the head is the title's.
                val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                val scale = 2.dp.toPx()
                val inset = 6.dp.toPx()
                val faint = brand.ornament.copy(alpha = 0.10f).toArgb()
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    com.sakinah.tasbih.widget.IslamicFrames.corner(
                        com.sakinah.tasbih.widget.WidgetOrnament.STEPPED_CORNERS, native,
                        if (rtl) size.width - inset else inset, size.height - inset, flipX = rtl, flipY = true, scale = scale, color = faint,
                    )
                    com.sakinah.tasbih.widget.IslamicFrames.corner(
                        com.sakinah.tasbih.widget.WidgetOrnament.STEPPED_CORNERS, native,
                        if (rtl) inset else size.width - inset, size.height - inset, flipX = !rtl, flipY = true, scale = scale, color = faint,
                    )
                }
                if (!showOrnament) return@drawBehind

                val ornament = brand.ornament
                val largeRadius = size.minDimension * 0.17f
                drawEightPointRosette(
                    center = Offset(size.width * 0.08f, size.height * 0.075f),
                    outerRadius = largeRadius,
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
                    color = ornament.copy(alpha = 0.035f),
                    strokeWidth = 0.8.dp.toPx(),
                )
            },
        content = content,
    )
}

/**
 * The bar at the top of every tab and of the pages opened from them, the same everywhere since
 * the user asked on November 23, 2026 that the five tabs share one head: the title on the right,
 * a way back before it when there is somewhere to go back to, the page's own buttons on the left
 * with the settings gear always last, and the rule with its star under it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun AnaaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    /** Set on the day's page, where the weekday opens the calendar. */
    onTitleClick: (() -> Unit)? = null,
    titleClickLabel: String? = null,
    /** Off where the page sets its own rule lower down, under the day's dates. */
    showRule: Boolean = true,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        androidx.compose.material3.TopAppBar(
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {
                Text(
                    title,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = if (onTitleClick != null) {
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = titleClickLabel, onClick = onTitleClick)
                            .padding(horizontal = 4.dp)
                    } else {
                        Modifier
                    }.semantics { heading() },
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            backLabel ?: androidx.compose.ui.res.stringResource(com.sakinah.tasbih.R.string.back),
                        )
                    }
                }
            },
            actions = actions,
        )
        if (showRule) KhatamDivider(Modifier.padding(horizontal = 20.dp))
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
    color: Color,
    strokeWidth: Float,
) {
    // The khatam of the home screen's ornament, so every screen draws the one star.
    drawPath(
        path = khatamPath(center, outerRadius),
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}
