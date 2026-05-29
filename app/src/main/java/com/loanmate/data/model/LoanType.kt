package com.loanmate.data.model

enum class LoanType(val displayName: String, val emoji: String) {
    PERSONAL("Personal Loan", "💼"),
    CAR("Car Loan", "🚗"),
    BIKE("Bike Loan", "🏍️"),
    HOME("Home Loan", "🏠"),
    GOLD("Gold Loan", "🥇"),
    EDUCATION("Education Loan", "🎓"),
    CREDIT_CARD("Credit Card EMI", "💳"),
    MOBILE("Mobile EMI", "📱"),
    KSFE("KSFE Chitty", "🏦"),
    LIC("LIC Premium", "🛡️"),
    BUSINESS("Business Loan", "🏢"),
    CONSUMER("Consumer Loan", "🛒"),
    CUSTOM("Custom", "📋")
}

enum class InterestType(val displayName: String) {
    FIXED("Fixed"),
    FLOATING("Floating"),
    REDUCING_BALANCE("Reducing Balance")
}

enum class TenureUnit(val displayName: String) {
    MONTHS("Months"),
    YEARS("Years")
}

enum class LoanStatus {
    ACTIVE, COMPLETED, PAUSED
}
