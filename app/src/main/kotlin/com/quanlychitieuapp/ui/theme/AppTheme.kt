package com.quanlychitieuapp.ui.theme

import androidx.compose.runtime.Composable

object AppTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
