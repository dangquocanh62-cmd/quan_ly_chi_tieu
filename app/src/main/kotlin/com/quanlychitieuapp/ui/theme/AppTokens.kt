package com.quanlychitieuapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object AppShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(24.dp)
}

object AppShadow {
    val elevation = 2.dp
    val color = PrimaryBlue.copy(alpha = 0.08f)
}

fun Modifier.cardShadow(shape: Shape = AppShapes.medium): Modifier = this.shadow(
    elevation = AppShadow.elevation,
    shape = shape,
    ambientColor = AppShadow.color,
    spotColor = AppShadow.color
)
