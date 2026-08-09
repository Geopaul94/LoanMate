package com.loanmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A friendly, on-brand empty state: a layered tinted halo behind a big emoji,
 * a punchy title, a warm one-liner, and an optional call-to-action.
 * Used everywhere the app has nothing to show yet.
 */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    compact: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 24.dp else 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
    ) {
        // Layered halo behind the emoji
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(if (compact) 96.dp else 120.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(if (compact) 68.dp else 84.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = if (compact) 34.sp else 42.sp)
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onCta,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ctaLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
