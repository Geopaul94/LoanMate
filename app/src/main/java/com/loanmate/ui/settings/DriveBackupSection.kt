package com.loanmate.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.viewmodel.DriveBackupViewModel

@Composable
fun DriveBackupSection(
    viewModel: DriveBackupViewModel = hiltViewModel(),
    onShowSnackbar: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingRestoreFileId by remember { mutableStateOf<String?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.handleSignInResult(result.data) }

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let { onShowSnackbar(it) }
    }

    if (!uiState.isConfigured) {
        UnconfiguredCard()
        return
    }

    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (uiState.accountEmail != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Google Drive sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium)
            }

            if (uiState.accountEmail == null) {
                Text(
                    "Sign in with Google to back up your data to your Drive's hidden app folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { signInLauncher.launch(viewModel.signInIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(uiState.accountEmail!!,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::signOut) {
                        Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sign out")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.backupNow(context) },
                        enabled = !uiState.isBusy,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (uiState.isBusy) "Working..." else "Backup now") }
                    OutlinedButton(
                        onClick = viewModel::refreshBackups,
                        enabled = !uiState.isBusy
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }

                if (uiState.backups.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Recent backups",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    uiState.backups.take(5).forEach { backup ->
                        BackupRow(
                            timeLabel = viewModel.formatBackupTime(backup.modifiedTimeMs),
                            sizeLabel = "${(backup.sizeBytes / 1024).coerceAtLeast(1)} KB",
                            onRestore = { pendingRestoreFileId = backup.id }
                        )
                    }
                }
            }
        }
    }

    pendingRestoreFileId?.let { fileId ->
        AlertDialog(
            onDismissRequest = { pendingRestoreFileId = null },
            title = { Text("Restore this backup?") },
            text = { Text("Your current loans, payments, and achievements will be replaced.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreFileId = null
                    viewModel.restoreFromDrive(fileId)
                }) { Text("Restore", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreFileId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BackupRow(timeLabel: String, sizeLabel: String, onRestore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(timeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(sizeLabel, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onRestore) {
            Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Restore")
        }
    }
}

@Composable
private fun UnconfiguredCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Google Drive sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium)
            Text(
                text = "Drive sync needs a one-time Google Cloud setup. " +
                       "See docs/SETUP_DRIVE.md in the project, add your OAuth web client ID " +
                       "to local.properties (GOOGLE_OAUTH_WEB_CLIENT_ID=...), and rebuild.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
