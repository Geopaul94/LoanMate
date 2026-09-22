package com.loanmate.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.loanmate.data.local.LoanEntity

@Composable
fun TrashBinDialog(
    trashLoans: List<LoanEntity>,
    onDismiss: () -> Unit,
    onRestoreItem: (Long) -> Unit,
    onPermanentDeleteItem: (Long) -> Unit,
    onEmptyTrash: () -> Unit
) {
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    if (showEmptyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = { Text("Empty Trash Bin?") },
            text = { Text("All trashed loans will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyConfirmDialog = false
                        onEmptyTrash()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trash Bin (${trashLoans.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Items auto-delete after 30 days",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (trashLoans.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirmDialog = true }) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Empty Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (trashLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trash Bin is empty",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(trashLoans, key = { it.id }) { loan ->
                            TrashItemRow(
                                loan = loan,
                                onRestore = { onRestoreItem(loan.id) },
                                onDelete = { onPermanentDeleteItem(loan.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashItemRow(
    loan: LoanEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val deletedAt = loan.deletedAt ?: System.currentTimeMillis()
    val now = System.currentTimeMillis()
    val daysInTrash = ((now - deletedAt) / (1000 * 60 * 60 * 24)).toInt()
    val daysLeft = (30 - daysInTrash).coerceAtLeast(0)
    val daysText = if (daysLeft <= 0) "Deletes today" else "Deletes in $daysLeft ${if (daysLeft == 1) "day" else "days"}"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.loanName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (loan.bankName.isNotBlank()) {
                    Text(
                        text = loan.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Outlined.Restore,
                    contentDescription = "Restore Item",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = "Delete Permanently",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
