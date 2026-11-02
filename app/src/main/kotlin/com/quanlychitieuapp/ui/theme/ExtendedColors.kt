package com.quanlychitieuapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val lending: Color,
    val borrowing: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        lending = Color.Unspecified,
        borrowing = Color.Unspecified
    )
}

internal val LightExtendedColors = ExtendedColors(
    lending = Color(0xFF4CAF50),
    borrowing = Color(0xFFF44336)
)

internal val DarkExtendedColors = ExtendedColors(
    lending = Color(0xFF81C995),
    borrowing = Color(0xFFFFB4AB)
)
