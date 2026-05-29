package com.loanmate.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddLoan : Screen("add_loan?loanId={loanId}") {
        fun createRoute(loanId: Long = -1L) = "add_loan?loanId=$loanId"
    }
    object LoanDetails : Screen("loan_details/{loanId}") {
        fun createRoute(loanId: Long) = "loan_details/$loanId"
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object Achievements : Screen("achievements")
}
