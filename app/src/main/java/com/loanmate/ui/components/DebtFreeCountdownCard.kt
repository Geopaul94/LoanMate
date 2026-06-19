package com.loanmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanmate.utils.DateUtils
import com.loanmate.utils.EmiCalculator

@Composable
fun DebtFreeCountdownCard(debtFreeDateMs: Long, modifier: Modifier = Modifier) {
    val remaining = EmiCalculator.timeUntil(debtFreeDateMs)

    val gradient = when {
        remaining.totalDays < 365 -> listOf(Color(0xFF43A047), Color(0xFF66BB6A))      // green
        remaining.totalDays < 1095 -> listOf(Color(0xFFFB8C00), Color(0xFFFFA726))     // orange (1-3y)
        else -> listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))                            // blue (3y+)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradient))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Debt-free in",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    if (remaining.years > 0) {
                        TimeChunk(value = remaining.years, label = if (remaining.years == 1) "year" else "years")
                        Spacer(Modifier.width(12.dp))
                    }
                    if (remaining.months > 0 || remaining.years > 0) {
                        TimeChunk(value = remaining.months, label = if (remaining.months == 1) "month" else "months")
                        Spacer(Modifier.width(12.dp))
                    }
                    TimeChunk(value = remaining.days, label = if (remaining.days == 1) "day" else "days")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Estimated: ${DateUtils.formatDate(debtFreeDateMs)}",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimeChunk(value: Int, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value.toString(),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}
