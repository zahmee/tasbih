package com.sakinah.tasbih.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.ArabicFontStyle

val SakinahUiFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.Bold),
)

val DhikrFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

val IslamicDisplayFontFamily = FontFamily(
    Font(R.font.aref_ruqaa_regular, FontWeight.Normal),
    Font(R.font.aref_ruqaa_bold, FontWeight.Bold),
)

/** The clean Kufi UI face visually associated with the Baqiyat reference app. */
val BaqiyatFontFamily = FontFamily(
    Font(R.font.droid_arabic_kufi_regular, FontWeight.Normal),
    Font(R.font.droid_arabic_kufi_bold, FontWeight.Bold),
)

val LocalDhikrFontFamily = staticCompositionLocalOf { DhikrFontFamily }

/**
 * A single selected face drives both the UI and the reading surfaces. Keeping one source
 * prevents headings or dhikr text from silently ignoring the user's font preference.
 */
fun fontFor(style: ArabicFontStyle): FontFamily = when (style) {
    ArabicFontStyle.Sakinah -> SakinahUiFontFamily
    ArabicFontStyle.Amiri -> DhikrFontFamily
    ArabicFontStyle.Ruqaa -> IslamicDisplayFontFamily
    ArabicFontStyle.Baqiyat -> BaqiyatFontFamily
}

fun dhikrFontFor(style: ArabicFontStyle): FontFamily = fontFor(style)

fun sakinahTypography(style: ArabicFontStyle): Typography {
    val uiFont = fontFor(style)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = 54.sp,
            lineHeight = 66.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 56.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 48.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 42.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 38.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 34.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 30.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 26.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 29.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 23.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 19.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = uiFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        ),
    )
}
