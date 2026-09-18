package com.loanmate.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loanmate.ui.components.IconChip
import com.loanmate.viewmodel.BackupEvent
import com.loanmate.viewmodel.BackupViewModel
import com.loanmate.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingRestoreConfirm by remember { mutableStateOf<Uri?>(null) }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestoreConfirm = uri }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            when (event) {
                is BackupEvent.SharePdf -> shareFile(context, event.authority, event.file, "application/pdf")
                is BackupEvent.ShareBackup -> shareFile(context, event.authority, event.file, "application/json")
                is BackupEvent.Toast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsGroup("PREFERENCES") {
                    ToggleRow(Icons.Default.DarkMode, "Dark Mode",
                        "Follow system or force dark", uiState.isDarkMode, viewModel::setDarkMode)
                    ToggleRow(Icons.Default.Notifications, "EMI Reminders",
                        "Get notified before EMI due dates",
                        uiState.notificationsEnabled, viewModel::setNotifications)
                }
            }
            
            item {
                SettingsGroup("SECURITY") {
                    ToggleRow(Icons.Default.Fingerprint, "Biometric Lock",
                        "Use fingerprint or face to open app",
                        uiState.biometricEnabled, viewModel::setBiometric)
                    ToggleRow(Icons.Default.VisibilityOff, "Privacy Mode",
                        "Mask loan amounts on dashboard",
                        uiState.hideValues, viewModel::setHideValues)
                }
            }
            
            item {
                SettingsGroup("DATA MANAGEMENT") {
                    ActionRow(Icons.Default.PictureAsPdf, "Export to PDF",
                        "Generate printable loan statement") {
                        backupViewModel.exportPdf(context)
                    }
                    ActionRow(Icons.Default.CloudUpload, "Cloud Backup",
                        "Save loans + payments to JSON") {
                        backupViewModel.exportBackup(context)
                    }
                    ActionRow(Icons.Default.CloudDownload, "Restore Backup",
                        "Restore from a JSON file") {
                        restorePicker.launch(arrayOf("application/json", "*/*"))
                    }
                }
            }

            item {
                SettingsGroup("CLOUD SYNC") {
                    DriveBackupSection(
                        onShowSnackbar = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "LoanMate v1.0.3",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    TextButton(onClick = { /* Privacy Policy */ }) {
                        Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    pendingRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreConfirm = null },
            title = { Text("Restore from backup?") },
            text = { Text("This will replace all current loans, payments, and achievements. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreConfirm = null
                    scope.launch {
                        val text = readUriAsText(context, uri)
                        if (text != null) backupViewModel.restoreBackup(text)
                        else snackbarHostState.showSnackbar("Could not read backup file")
                    }
                }) { Text("Restore", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

private fun shareFile(context: android.content.Context, authority: String, file: java.io.File, mime: String) {
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${file.name}").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun readUriAsText(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) { null }
}

