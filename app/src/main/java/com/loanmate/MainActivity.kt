package com.loanmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.loanmate.navigation.LoanMateNavHost
import com.loanmate.ui.onboarding.OnboardingScreen
import com.loanmate.ui.theme.LoanMateTheme
import com.loanmate.viewmodel.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingLoanId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingLoanId.value = extractLoanId(intent)
        setContent {
            LoanMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingVm: OnboardingViewModel = hiltViewModel()
                    val hasSeen by onboardingVm.hasSeenOnboarding.collectAsStateWithLifecycle()
                    when (hasSeen) {
                        null -> SplashLoader()
                        false -> OnboardingScreen(
                            viewModel = onboardingVm,
                            onFinished = { /* state flow will recompose into Main */ }
                        )
                        true -> {
                            val navController = rememberNavController()
                            LoanMateNavHost(
                                navController = navController,
                                deepLinkLoanId = pendingLoanId.value,
                                onDeepLinkConsumed = { pendingLoanId.value = null }
                            )
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
private fun SplashLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
