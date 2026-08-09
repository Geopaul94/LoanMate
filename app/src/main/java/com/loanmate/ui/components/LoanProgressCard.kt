package com.loanmate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loanmate.data.local.LoanEntity
import com.loanmate.ui.theme.DangerRed
import com.loanmate.ui.theme.SuccessGreen
import com.loanmate.ui.theme.WarningAmber
import com.loanmate.utils.CurrencyUtils
import com.loanmate.utils.DateUtils
import com.loanmate.utils.EmiCalculator
import com.loanmate.utils.MissedPaymentDetector

@Composable
fun LoanProgressCard(
    loan: LoanEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = EmiCalculator.getProgressPercent(loan.completedEmis, loan.totalEmis) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "progress"
    )

    val nextDueDate = DateUtils.nextEmiDate(loan.firstEmiDate, loan.completedEmis)
    val daysUntil = DateUtils.getDaysUntil(nextDueDate)
    val dueDateStatus = DateUtils.getDueDateStatus(daysUntil)
    val missed = remember(loan) { MissedPaymentDetector.detect(loan) }

    val statusColor = when (dueDateStatus) {
        DateUtils.DueDateStatus.OVERDUE -> DangerRed
        DateUtils.DueDateStatus.URGENT -> WarningAmber
        DateUtils.DueDateStatus.UPCOMING -> WarningAmber
        DateUtils.DueDateStatus.SAFE -> SuccessGreen
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (missed.missedCount > 0) {
                MissedBanner(missed)
                Spacer(Modifier.height(12.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LoanTypeIcon(loanType = loan.loanType)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loan.loanName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = loan.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatShort(loan.outstandingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = loanTypeColor(loan.loanType),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = statusColor
                    )
                    Text(
                        text = when {
                            daysUntil < 0 -> "Overdue by ${-daysUntil} days"
                            daysUntil == 0 -> "Due today"
                            else -> "Due in $daysUntil days"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}% done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MissedBanner(info: MissedPaymentDetector.MissedInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(Dimens.ChipRadius)
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${info.missedCount} missed EMI${if (info.missedCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            val tail = if (info.showCibilWarning) " · may impact CIBIL" else ""
            Text(
                text = "Est. penalty: ${CurrencyUtils.format(info.estimatedPenalty)}$tail",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )
        }
    }
}
