package com.loanmate.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.loanmate.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showTrashBinDialog by remember { mutableStateOf(false) }
    val trashLoans by viewModel.trashLoans.collectAsStateWithLifecycle()

    if (showTrashBinDialog) {
        TrashBinDialog(
            trashLoans = trashLoans,
            onDismiss = { showTrashBinDialog = false },
            onRestoreItem = { viewModel.restoreFromTrash(it) },
            onPermanentDeleteItem = { viewModel.permanentlyDeleteFromTrash(it) },
            onEmptyTrash = { viewModel.emptyTrash() }
        )
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(it) } }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.setGoogleAccount(context, account)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Sign-in failed: ${e.message}") }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SectionLabel("Appearance")
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Follow system or force dark theme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = viewModel::setDarkMode,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Preferences & Security")
            SettingsCard {
                Column {
                    ToggleRow(
                        icon = Icons.Outlined.Notifications,
                        title = "EMI Reminders",
                        subtitle = "Get notified before EMI due dates",
                        checked = uiState.notificationsEnabled,
                        onToggle = viewModel::setNotifications
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ToggleRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Lock",
                        subtitle = "Use fingerprint or face to open app",
                        checked = uiState.biometricEnabled,
                        onToggle = viewModel::setBiometric
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ToggleRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Privacy Mode",
                        subtitle = "Mask loan amounts on dashboard",
                        checked = uiState.hideValues,
                        onToggle = viewModel::setHideValues
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Google Drive Cloud Backup")
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                uiState.googleAccount?.email ?: "Not connected",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (uiState.googleAccount == null) {
                                Text(
                                    "Sign in to enable automatic cloud backups",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (uiState.googleAccount == null) {
                                    googleLauncher.launch(viewModel.authManager.getSignInIntent())
                                } else {
                                    viewModel.authManager.signOut { viewModel.setGoogleAccount(context, null) }
                                }
                            },
                            colors = if (uiState.googleAccount != null)
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            else ButtonDefaults.buttonColors()
                        ) {
                            Text(if (uiState.googleAccount == null) "Connect" else "Disconnect")
                        }
                    }

                    if (uiState.googleAccount != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-Backup", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Sync data daily to Google Drive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isDriveEnabled,
                                onCheckedChange = { viewModel.toggleDriveBackup(context, it) }
                            )
                        }

                        if (uiState.lastDriveSync > 0) {
                            Text(
                                "Last synced: ${
                                    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(
                                        Date(uiState.lastDriveSync)
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.triggerDriveBackup(context) },
                                enabled = !uiState.isProcessing,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.CloudUpload,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Backup Now")
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.triggerDriveRestore(context) },
                                enabled = !uiState.isProcessing,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.Restore,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Data Management")
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "Trash Bin",
                    subtitle = if (trashLoans.isEmpty()) "Empty" else "${trashLoans.size} ${if (trashLoans.size == 1) "loan" else "loans"} • Auto-deletes after 30 days",
                    onClick = { showTrashBinDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Local Backup")
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.CloudUpload,
                    title = "Export Data",
                    subtitle = "Save all loans, payments & documents as ZIP backup",
                    onClick = { exportLauncher.launch("loanmate_backup.zip") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.CloudDownload,
                    title = "Import Backup",
                    subtitle = "Restore loans, payments & documents from a backup file",
                    onClick = { importLauncher.launch(arrayOf("application/zip")) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("Support & Feedback")
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy & data safety",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://geopaul94.github.io/LoanMate/privacy.html")
                        )
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Share,
                    title = "Share LoanMate",
                    subtitle = "Invite friends to use the app",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out LoanMate, the best way to track your loans and EMIs! Download here: https://play.google.com/store/apps/details?id=com.geo.loanmate"
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share LoanMate via"
                            )
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Feedback,
                    title = "Feedback & Suggestions",
                    subtitle = "Suggest a feature or report an issue",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:geopaul94@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "LoanMate Feedback")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            scope.launch { snackbarHostState.showSnackbar("No email app found") }
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.StarRate,
                    title = "Rate on Play Store",
                    subtitle = "Support us with a 5-star rating",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.geo.loanmate")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.geo.loanmate")
                            )
                            context.startActivity(webIntent)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("About")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "LoanMate",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        val versionName = remember {
                            try {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                            } catch (e: Exception) {
                                "1.0.5"
                            }
                        }
                        Text(
                            text = "Version $versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
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
                Icon(
                    icon,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
