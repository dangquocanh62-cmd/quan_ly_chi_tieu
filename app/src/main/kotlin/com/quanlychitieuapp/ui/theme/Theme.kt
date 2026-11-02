package com.quanlychitieuapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Color palette
val PrimaryBlue = Color(0xFF1B3A5C)
val PrimaryBlueDark = Color(0xFF12283F)
val SecondaryGreen = Color(0xFF0F6E56)
val ErrorRed = Color(0xFFD85A30)
val WarningOrange = Color(0xFFBA7517)
val SurfaceLight = Color(0xFFF8F9FA)
val SurfaceDark = Color(0xFF1E1E2E)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF2A2A3E)
val BackgroundDark = Color(0xFF12121A)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F6),
    onPrimaryContainer = Color(0xFF071422),
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCECE2),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFDE4D9),
    background = SurfaceLight,
    onBackground = Color(0xFF1A1A2E),
    surface = CardLight,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8EAED),
    outline = Color(0xFFBDC1C6),
    outlineVariant = Color(0xFFE0E3E8)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90C2F7),
    onPrimary = Color(0xFF071422),
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFD3E4F6),
    secondary = Color(0xFF8ED2C1),
    onSecondary = Color(0xFF042018),
    secondaryContainer = Color(0xFF063327),
    error = Color(0xFFFFB49A),
    onError = Color(0xFF561A05),
    background = BackgroundDark,
    onBackground = Color(0xFFE3E2E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = CardDark,
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun QuanLyChiTieuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
