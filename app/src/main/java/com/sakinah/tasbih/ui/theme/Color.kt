package com.sakinah.tasbih.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Emerald = Color(0xFF0B6253)
val DeepEmerald = Color(0xFF063D34)
val Mint = Color(0xFFDCEFE8)
val Gold = Color(0xFFB88642)
val Sand = Color(0xFFF8F4E9)
val Ink = Color(0xFF17231F)
val SoftInk = Color(0xFF56645F)
val Night = Color(0xFF081713)
val NightSurface = Color(0xFF10251F)
val NightMint = Color(0xFF8ED8C0)

/**
 * Brand-only colors stay stable when the user enables Android dynamic color.
 * Functional controls still use Material color roles; these values are reserved
 * for the quiet Islamic ornament, parchment glow, and antique-gold accents.
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
    onAntiqueGold = Color(0xFF2C210F),
    heroStart = Emerald,
    heroEnd = Color(0xFF084B40),
    onHero = Color(0xFFFFFBF3),
    backdropTop = Color(0xFFFFFCF6),
    backdropBottom = Sand,
    ornament = Color(0xFF9B713A),
    readingPaper = Color(0xFFFFFDF9),
)

internal val DarkSakinahBrandColors = SakinahBrandColors(
    antiqueGold = Color(0xFFE0B46D),
    onAntiqueGold = Color(0xFF271A08),
    heroStart = Color(0xFF174D42),
    heroEnd = Color(0xFF0D342C),
    onHero = Color(0xFFE4F0EB),
    backdropTop = Color(0xFF10231D),
    backdropBottom = Night,
    ornament = Color(0xFFD5A65F),
    readingPaper = Color(0xFF142A23),
)

val LocalSakinahBrandColors = staticCompositionLocalOf { LightSakinahBrandColors }
