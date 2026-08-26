package com.sakinah.tasbih.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.ThemeMode

private val ColorSand = Color(0xFFF1E2C6)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFFFFFBF3),
    primaryContainer = Mint,
    onPrimaryContainer = DeepEmerald,
    secondary = Gold,
    onSecondary = Color(0xFFFFFBF3),
    secondaryContainer = ColorSand,
    onSecondaryContainer = Color(0xFF3E2D12),
    tertiary = Color(0xFF496B63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E8E1),
    onTertiaryContainer = Color(0xFF213E38),
    background = Sand,
    onBackground = Ink,
    surface = Color(0xFFFFFCF7),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E7DE),
    onSurfaceVariant = SoftInk,
    outline = Color(0xFF737C77),
    outlineVariant = Color(0xFFC5CCC7),
    surfaceDim = Color(0xFFDDD9CF),
    surfaceBright = Color(0xFFFFFCF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF7F0),
    surfaceContainer = Color(0xFFF4F1E9),
    surfaceContainerHigh = Color(0xFFEEECE4),
    surfaceContainerHighest = Color(0xFFE8E6DE),
)

private val DarkColors = darkColorScheme(
    primary = NightMint,
    onPrimary = DeepEmerald,
    primaryContainer = Color(0xFF174D42),
    onPrimaryContainer = Mint,
    secondary = Color(0xFFE0B46D),
    onSecondary = Night,
    secondaryContainer = Color(0xFF493719),
    onSecondaryContainer = Color(0xFFFFDEAA),
    tertiary = Color(0xFFB9CCC4),
    onTertiary = Night,
    tertiaryContainer = Color(0xFF294840),
    onTertiaryContainer = Color(0xFFD4E9E1),
    background = Night,
    onBackground = Color(0xFFE0E8E2),
    surface = NightSurface,
    onSurface = Color(0xFFE0E8E2),
    surfaceVariant = Color(0xFF2D423C),
    onSurfaceVariant = Color(0xFFB9CCC4),
    outline = Color(0xFF91A49E),
    outlineVariant = Color(0xFF40564F),
    surfaceDim = Color(0xFF081713),
    surfaceBright = Color(0xFF2B3E38),
    surfaceContainerLowest = Color(0xFF06110E),
    surfaceContainerLow = Color(0xFF0C1E19),
    surfaceContainer = Color(0xFF10251F),
    surfaceContainerHigh = Color(0xFF192F28),
    surfaceContainerHighest = Color(0xFF243A33),
)

private val SakinahShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun SakinahTheme(
    dynamicColor: Boolean,
    themeMode: ThemeMode,
    arabicFontStyle: ArabicFontStyle,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val brandColors = if (darkTheme) DarkSakinahBrandColors else LightSakinahBrandColors

    val typography = sakinahTypography(arabicFontStyle)
    CompositionLocalProvider(
        LocalDhikrFontFamily provides dhikrFontFor(arabicFontStyle),
        LocalSakinahBrandColors provides brandColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = SakinahShapes,
        ) {
            // Covers unstyled Text calls too, so the selection truly applies everywhere.
            CompositionLocalProvider(LocalTextStyle provides typography.bodyLarge) {
                content()
            }
        }
    }
}
