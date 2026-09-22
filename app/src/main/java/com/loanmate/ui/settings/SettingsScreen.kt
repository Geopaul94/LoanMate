package com.loanmate.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
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
import java.io.File

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
    var pendingRestoreConfirm by remember { mutableStateOf<File?>(null) }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val tempFile = File(context.cacheDir, "temp_restore.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { input.copyTo(it) }
                }
                pendingRestoreConfirm = tempFile
            }
        }
    }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            when (event) {
                is BackupEvent.SharePdf -> shareFile(context, event.authority, event.file, "application/pdf")
                is BackupEvent.ShareBackup -> shareFile(context, event.authority, event.file, "application/zip")
                is BackupEvent.Toast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
                        "Save loans, payments & documents to ZIP") {
                        backupViewModel.exportBackup(context)
                    }
                    ActionRow(Icons.Default.CloudDownload, "Restore Backup",
                        "Restore from a ZIP file") {
                        restorePicker.launch(arrayOf("application/zip", "*/*"))
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
                SettingsGroup("ABOUT & SUPPORT") {
                    ActionRow(Icons.Default.Policy, "Privacy Policy", "Read our terms and data safety") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://geopaul94.github.io/LoanMate/privacy.html"))
                        context.startActivity(intent)
                    }
                    ActionRow(Icons.Default.Share, "Share LoanMate", "Spread the word to friends and family") {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out LoanMate, the best way to track your loans and EMIs! Download here: https://play.google.com/store/apps/details?id=com.geo.loanmate")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share LoanMate via"))
                    }
                    ActionRow(Icons.Default.Feedback, "Feedback & Bug Report", "Suggest a feature or report an issue") {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:geopaul94@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "LoanMate Feedback (v1.0.4)")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            scope.launch { snackbarHostState.showSnackbar("No email app found") }
                        }
                    }
                    ActionRow(Icons.Default.Star, "Rate on Play Store", "Support us with a 5-star rating") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.geo.loanmate")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.geo.loanmate"))
                            context.startActivity(webIntent)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "LoanMate v1.0.4",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Made with ❤️ for financial freedom",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    pendingRestoreConfirm?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestoreConfirm = null },
            title = { Text("Restore from backup?") },
            text = { Text("This will replace all current loans, payments, and documents. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreConfirm = null
                    backupViewModel.restoreBackup(file)
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

private fun shareFile(context: android.content.Context, authority: String, file: File, mime: String) {
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


