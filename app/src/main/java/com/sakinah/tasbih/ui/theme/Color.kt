package com.sakinah.tasbih.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Emerald = Color(0xFF185348)
val DeepEmerald = Color(0xFF153F36)
val Mint = Color(0xFFE3EBDD)
val Gold = Color(0xFF996244)
val Sand = Color(0xFFF7F4E9)
val Ink = Color(0xFF17231F)
val SoftInk = Color(0xFF56645F)
val Night = Color(0xFF081713)
val NightSurface = Color(0xFF10251F)
val NightMint = Color(0xFF8ED8C0)

/**
 * Brand-only colors stay stable when the user enables Android dynamic color.
 * Functional controls still use Material color roles; these values are reserved
 * for the approved Anaa lettering, alif/noon icon, and muted copper accents.
 */
@Immutable
data class SakinahBrandColors(
    val antiqueGold: Color,
    val onAntiqueGold: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
    val backdropTop: Color,
    val backdropBottom: Color,
    val ornament: Color,
    val readingPaper: Color,
)

internal val LightSakinahBrandColors = SakinahBrandColors(
    antiqueGold = Gold,
    onAntiqueGold = Color(0xFFFFF8F1),
    heroStart = Emerald,
    heroEnd = Emerald,
    onHero = Color(0xFFFFFBF3),
    backdropTop = Color(0xFFFAF8F2),
    backdropBottom = Color(0xFFFAF8F2),
    ornament = Color(0xFFA66E4E),
    readingPaper = Color(0xFFFFFDF9),
)

internal val DarkSakinahBrandColors = SakinahBrandColors(
    antiqueGold = Color(0xFFDCAA87),
    onAntiqueGold = Color(0xFF271A08),
    heroStart = Color(0xFF174D42),
    heroEnd = Color(0xFF174D42),
    onHero = Color(0xFFE4F0EB),
    backdropTop = Night,
    backdropBottom = Night,
    ornament = Color(0xFFDCAA87),
    readingPaper = Color(0xFF142A23),
)

val LocalSakinahBrandColors = staticCompositionLocalOf { LightSakinahBrandColors }
