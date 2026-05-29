package com.loanmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanmate.data.model.LoanType

@Composable
fun LoanTypeIcon(
    loanType: LoanType,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = loanTypeColor(loanType)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = loanType.emoji,
            fontSize = (size.value * 0.5f).sp,
            textAlign = TextAlign.Center
        )
    }
}

fun loanTypeColor(loanType: LoanType): Color = when (loanType) {
    LoanType.PERSONAL -> Color(0xFF1565C0)
    LoanType.CAR -> Color(0xFF2E7D32)
    LoanType.BIKE -> Color(0xFF6A1B9A)
    LoanType.HOME -> Color(0xFFE65100)
    LoanType.GOLD -> Color(0xFFFFB300)
    LoanType.EDUCATION -> Color(0xFF00838F)
    LoanType.CREDIT_CARD -> Color(0xFFC62828)
    LoanType.MOBILE -> Color(0xFF283593)
    LoanType.KSFE -> Color(0xFF00695C)
    LoanType.LIC -> Color(0xFF4527A0)
    LoanType.BUSINESS -> Color(0xFF558B2F)
    LoanType.CONSUMER -> Color(0xFF0277BD)
    LoanType.CUSTOM -> Color(0xFF546E7A)
}
