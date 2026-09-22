package com.loanmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.loanmate.navigation.LoanMateNavHost
import com.loanmate.ui.onboarding.OnboardingScreen
import com.loanmate.ui.shell.LoanMateBottomBar
import com.loanmate.ui.shell.shouldShowBottomBar
import com.loanmate.ui.theme.LoanMateTheme
import com.loanmate.viewmodel.OnboardingViewModel
import com.loanmate.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loanmate.utils.AppUpdateHelper
import com.loanmate.worker.DriveSyncWorker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appUpdateHelper: AppUpdateHelper

    private val pendingLoanId = mutableStateOf<Long?>(null)
    private var isAuthenticated = mutableStateOf(false)

    // Hold the branded splash until the onboarding flag has loaded from disk,
    // so the user never sees a blank/loading frame between splash and content.
    @Volatile private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        enableEdgeToEdge()
        pendingLoanId.value = extractLoanId(intent)

        appUpdateHelper.checkForUpdates()
        scheduleDriveSync()

        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

            // If biometric is NOT enabled, we consider the user authenticated.
            // If it IS enabled, we wait for the prompt to succeed.
            val effectiveAuth = !settingsState.biometricEnabled || isAuthenticated.value

            LaunchedEffect(settingsState.biometricEnabled) {
                if (settingsState.biometricEnabled && !isAuthenticated.value) {
                    showBiometricPrompt()
                }
            }

            LoanMateTheme(darkTheme = settingsState.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!effectiveAuth) {
                        // Show a locked state or just stay blank (Splash is still there via condition maybe?)
                        // Actually keepSplashOnScreen = false will be set once onboarding resolves.
                        // We should probably show a "Locked" UI if biometric is enabled but not auth'd.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { showBiometricPrompt() }) {
                                Text("Tap to Unlock", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        val onboardingVm: OnboardingViewModel = hiltViewModel()
                        val hasSeen by onboardingVm.hasSeenOnboarding.collectAsStateWithLifecycle()

                        // Release the splash the moment the flag resolves.
                        if (hasSeen != null) {
                            SideEffect { keepSplashOnScreen = false }
                        }

                        when (hasSeen) {
                            null -> Unit // splash still covering the screen
                            false -> OnboardingScreen(
                                viewModel = onboardingVm,
                                onFinished = { /* state flow will recompose into Main */ }
                            )
                            true -> AppShell(
                                pendingLoanId = pendingLoanId.value,
                                appUpdateHelper = appUpdateHelper,
                                activity = this
                            ) {
                                pendingLoanId.value = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated.value = true
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("LoanMate Locked")
            .setSubtitle("Use biometric to unlock")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateHelper.unregisterListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractLoanId(intent)?.let { pendingLoanId.value = it }
    }

    private fun extractLoanId(intent: Intent?): Long? {
        val id = intent?.getLongExtra(EXTRA_LOAN_ID, -1L) ?: -1L
        return if (id > 0) id else null
    }

    private fun scheduleDriveSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "drive_sync_on_launch",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val EXTRA_LOAN_ID = "loanId"
    }
}

@Composable
private fun AppShell(
    pendingLoanId: Long?,
    appUpdateHelper: AppUpdateHelper,
    activity: ComponentActivity,
    onDeepLinkConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val showBottomBar = shouldShowBottomBar(navController)
    val snackbarHostState = remember { SnackbarHostState() }
    val updateStatus by appUpdateHelper.updateStatus.collectAsStateWithLifecycle()

    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is AppUpdateHelper.UpdateStatus.Available -> {
                val result = snackbarHostState.showSnackbar(
                    message = "New version available",
                    actionLabel = "Update",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateHelper.startFlexibleUpdate(activity)
                }
            }
            is AppUpdateHelper.UpdateStatus.Downloaded -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Update downloaded",
                    actionLabel = "Restart",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateHelper.completeUpdate()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                LoanMateBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LoanMateNavHost(
                navController = navController,
                deepLinkLoanId = pendingLoanId,
                onDeepLinkConsumed = onDeepLinkConsumed
            )
        }
    }
}
