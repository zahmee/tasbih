package com.sakinah.tasbih.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors

/** Shares the approved launcher artwork and its original colors. Never mirrors in RTL. */
@Composable
internal fun AnaaAppIcon(modifier: Modifier = Modifier) {
    // Recreate the vector painter when the app theme changes so its cached layer stays visible.
    key(LocalSakinahBrandColors.current.readingPaper) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = modifier.background(
                color = colorResource(R.color.launcher_background),
                shape = RoundedCornerShape(percent = 24),
            ),
        )
    }
}

/** Native vector artwork from the approved first identity concept. Never mirrors in RTL. */
@Composable
internal fun AnaaMark(modifier: Modifier = Modifier, tint: Color? = null) {
    val dark = LocalSakinahBrandColors.current.readingPaper.luminance() < 0.5f
    Image(
        painter = painterResource(if (dark) R.drawable.anaa_mark_dark else R.drawable.anaa_mark),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier,
    )
}

@Composable
internal fun AnaaWordmark(modifier: Modifier = Modifier, tint: Color? = null) {
    val dark = LocalSakinahBrandColors.current.readingPaper.luminance() < 0.5f
    Image(
        painter = painterResource(if (dark) R.drawable.anaa_wordmark_dark else R.drawable.anaa_wordmark),
        contentDescription = stringResource(R.string.app_name),
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier.semantics { heading() },
    )
}

@Composable
internal fun AnaaBrandLockup(modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnaaWordmark(Modifier.width(56.dp).height(37.dp))
        Text(
            text = stringResource(R.string.about_app_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = LocalSakinahBrandColors.current.antiqueGold,
        )
    }
}
