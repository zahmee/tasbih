package com.sakinah.tasbih.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors

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
    Column(modifier = modifier.width(124.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnaaWordmark(Modifier.width(112.dp).height(74.dp))
        Text(
            text = stringResource(R.string.about_app_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = LocalSakinahBrandColors.current.antiqueGold,
        )
    }
}
