package com.sakinah.tasbih.ui

import androidx.compose.foundation.layout.Box
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
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious, modifier = Modifier.testTag(previousTag)) {
            CarouselArrow(pointsRight = true, contentDescription = stringResource(R.string.previous_dhikr))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.heightIn(min = 48.dp).testTag(exitTag),
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
        }
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.testTag(nextTag)) {
            CarouselArrow(pointsRight = false, contentDescription = stringResource(R.string.next_dhikr))
        }
    }
}
