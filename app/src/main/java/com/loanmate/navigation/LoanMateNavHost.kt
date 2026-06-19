package com.loanmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.loanmate.ui.dashboard.DashboardScreen
import com.loanmate.ui.loan.add.AddLoanScreen
import com.loanmate.ui.loan.details.LoanDetailsScreen
import com.loanmate.ui.analytics.AnalyticsScreen
import com.loanmate.ui.settings.SettingsScreen
import com.loanmate.ui.achievements.AchievementsScreen

const val SAVED_STATE_DELETED_LOAN_ID = "deletedLoanId"

@Composable
fun LoanMateNavHost(
    navController: NavHostController,
    deepLinkLoanId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    LaunchedEffect(deepLinkLoanId) {
        if (deepLinkLoanId != null) {
            navController.navigate(Screen.LoanDetails.createRoute(deepLinkLoanId))
            onDeepLinkConsumed()
        }
    }

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {

        composable(Screen.Dashboard.route) { backStackEntry ->
            DashboardScreen(
                onAddLoan = { navController.navigate(Screen.AddLoan.createRoute()) },
                onLoanClick = { loanId -> navController.navigate(Screen.LoanDetails.createRoute(loanId)) },
                onAnalytics = { navController.navigate(Screen.Analytics.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onAchievements = { navController.navigate(Screen.Achievements.route) },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        composable(
            route = Screen.AddLoan.route,
            arguments = listOf(navArgument("loanId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments?.getLong("loanId") ?: -1L
            AddLoanScreen(
                loanId = if (loanId == -1L) null else loanId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LoanDetails.route,
            arguments = listOf(navArgument("loanId") { type = NavType.LongType })
        ) { backStackEntry ->
            val loanId = backStackEntry.arguments!!.getLong("loanId")
            LoanDetailsScreen(
                loanId = loanId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddLoan.createRoute(loanId)) },
                onDeleted = { id ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(SAVED_STATE_DELETED_LOAN_ID, id)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(onBack = { navController.popBackStack() })
        }
    }
}
