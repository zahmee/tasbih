package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.TextDecrease
import androidx.compose.material.icons.outlined.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import kotlin.math.roundToInt

/** One 10% step, rounded so repeated taps land exactly on the range bounds. */
internal fun Float.textScaleStep(larger: Boolean, range: ClosedFloatingPointRange<Float>): Float =
    ((this + if (larger) 0.1f else -0.1f) * 10).roundToInt().div(10f).coerceIn(range)

@Composable
internal fun FocusTextSizeButton(
    larger: Boolean,
    scale: Float,
    range: ClosedFloatingPointRange<Float>,
    onSetScale: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onSetScale(scale.textScaleStep(larger, range)) },
        enabled = if (larger) scale < range.endInclusive else scale > range.start,
        modifier = modifier,
    ) {
        Icon(
            if (larger) Icons.Outlined.TextIncrease else Icons.Outlined.TextDecrease,
            contentDescription = stringResource(if (larger) R.string.increase_reader_font else R.string.decrease_reader_font),
        )
    }
}

@Composable
internal fun FocusNavigationBar(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    previousTag: String,
    nextTag: String,
    exitTag: String,
    beforeExit: @Composable () -> Unit = {},
    afterExit: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious, modifier = Modifier.testTag(previousTag)) {
            CarouselArrow(pointsRight = true, contentDescription = stringResource(R.string.previous_dhikr))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            beforeExit()
            OutlinedButton(
                onClick = onExit,
                // Measured after the optional side controls so they always keep their touch targets.
                modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp).testTag(exitTag),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.FullscreenExit, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.tasbih_exit_focus_mode),
                    modifier = Modifier.width(IntrinsicSize.Max),
                    style = MaterialTheme.typography.labelLarge.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = true),
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            afterExit()
        }
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.testTag(nextTag)) {
            CarouselArrow(pointsRight = false, contentDescription = stringResource(R.string.next_dhikr))
        }
    }
}
