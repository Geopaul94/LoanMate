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
import com.loanmate.viewmodel.BackupEvent
import com.loanmate.viewmodel.BackupViewModel
import com.loanmate.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SectionHeader("Appearance")
                ToggleRow(Icons.Default.DarkMode, "Dark Mode",
                    "Follow system or force dark", uiState.isDarkMode, viewModel::setDarkMode)
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Notifications")
                ToggleRow(Icons.Default.Notifications, "EMI Reminders",
                    "Get notified before EMI due dates",
                    uiState.notificationsEnabled, viewModel::setNotifications)
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Security")
                ToggleRow(Icons.Default.Fingerprint, "Biometric Lock",
                    "Use fingerprint or face to open app",
                    uiState.biometricEnabled, viewModel::setBiometric)
                ToggleRow(Icons.Default.VisibilityOff, "Hide Sensitive Values",
                    "Mask loan amounts on dashboard",
                    uiState.hideValues, viewModel::setHideValues)
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("Data")
                ActionRow(Icons.Default.PictureAsPdf, "Export to PDF",
                    "Generate a printable loan statement") {
                    backupViewModel.exportPdf(context)
                }
                ActionRow(Icons.Default.CloudUpload, "Backup data",
                    "Save all loans + payments to a JSON file") {
                    backupViewModel.exportBackup(context)
                }
                ActionRow(Icons.Default.CloudDownload, "Restore from backup",
                    "Pick a previously saved JSON file") {
                    restorePicker.launch(arrayOf("application/json", "*/*"))
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader("About")
                ActionRow(Icons.Default.Info, "About LoanMate", "Version 1.0") {}
                ActionRow(Icons.Default.Policy, "Privacy Policy", "Read our privacy policy") {}
            }
        }
    }

    pendingRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreConfirm = null },
            title = { Text("Restore from backup?") },
            text = { Text("This will replace all current loans, payments, and achievements with the contents of the backup file. This cannot be undone.") },
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
