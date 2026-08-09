package com.loanmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared design tokens + primitives so every screen reads as one system.
 * Card radius, chip radius, spacing rhythm all live here.
 */
object Dimens {
    val CardRadius = 20.dp
    val SmallCardRadius = 16.dp
    val ChipRadius = 12.dp
    val ScreenPadding = 16.dp
    val SectionGap = 20.dp
    val ItemGap = 12.dp
    val CardElevation = 1.dp
    val HeroElevation = 6.dp
}

/**
 * An icon inside a soft, tinted rounded chip. The signature treatment used
 * across summary cards, list rows, and section headers.
 */
@Composable
fun IconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    alpha: Float = 0.14f
) {
    Box(
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = alpha), RoundedCornerShape(Dimens.ChipRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** A consistent section header used above grouped content. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.padding(vertical = 4.dp)
    )
}
