package com.quanlychitieuapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reusable empty state illustration — Canvas geometric shapes + Icon overlay.
 * Uses MaterialTheme.colorScheme.primary with low alpha for consistent theming.
 */
@Composable
fun EmptyStateIllustration(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = primary.copy(alpha = 0.08f),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.05f),
                radius = w * 0.3f,
                center = Offset(w * 0.7f, h * 0.3f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.06f),
                radius = w * 0.2f,
                center = Offset(w * 0.25f, h * 0.7f)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = primary.copy(alpha = 0.4f)
        )
    }
}
