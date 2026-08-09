package com.loanmate.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.loanmate.navigation.Screen

data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomTabs = listOf(
    BottomTab(Screen.Dashboard.route, "Home",
        Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(Screen.Calendar.route, "Calendar",
        Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomTab(Screen.Analytics.route, "Analytics",
        Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomTab(Screen.Achievements.route, "Rewards",
        Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    BottomTab(Screen.Settings.route, "Settings",
        Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * Routes that should HIDE the bottom nav (full-height detail screens).
 * Anything else with a top-level route in [bottomTabs] shows the bar.
 */
private val routesWithoutBottomBar = setOf(
    Screen.AddLoan.route,
    Screen.LoanDetails.route,
    Screen.Calculator.route,
    Screen.PayoffStrategy.route
)

@Composable
fun shouldShowBottomBar(navController: NavHostController): Boolean {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: return false
    return currentRoute !in routesWithoutBottomBar
}

@Composable
fun LoanMateBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        bottomTabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            // Pop back to start so tapping Home always returns to Dashboard root
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true
            )
        }
    }
}
