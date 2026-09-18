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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingLoanId = mutableStateOf<Long?>(null)

    // Hold the branded splash until the onboarding flag has loaded from disk,
    // so the user never sees a blank/loading frame between splash and content.
    @Volatile private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        enableEdgeToEdge()
        pendingLoanId.value = extractLoanId(intent)
        setContent {
            LoanMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
                        true -> AppShell(pendingLoanId = pendingLoanId.value) {
                            pendingLoanId.value = null
                        }
                    }
                }
            }
        }
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

    companion object {
        const val EXTRA_LOAN_ID = "loanId"
    }
}

@androidx.compose.runtime.Composable
private fun AppShell(pendingLoanId: Long?, onDeepLinkConsumed: () -> Unit) {
    val navController = rememberNavController()
    val showBottomBar = shouldShowBottomBar(navController)

    Scaffold(
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
        Box(modifier = Modifier.padding(innerPadding)) {
            LoanMateNavHost(
                navController = navController,
                deepLinkLoanId = pendingLoanId,
                onDeepLinkConsumed = onDeepLinkConsumed
            )
        }
    }
}
