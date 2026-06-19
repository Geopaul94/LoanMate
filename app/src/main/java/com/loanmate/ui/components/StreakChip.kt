package com.loanmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StreakChip(current: Int, longest: Int, modifier: Modifier = Modifier) {
    if (current == 0 && longest == 0) return
    val tier = when {
        current >= 30 -> Color(0xFFD84315)  // deep orange
        current >= 10 -> Color(0xFFEF6C00)
        current >= 3 -> Color(0xFFFB8C00)
        else -> Color(0xFFFFA726)
    }
    Row(
        modifier = modifier
            .background(tier.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$current",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = tier
        )
        if (longest > current) {
            Text(
                text = " / $longest best",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
