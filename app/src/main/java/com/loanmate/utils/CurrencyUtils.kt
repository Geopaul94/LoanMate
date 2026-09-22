package com.loanmate.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {

    private val indiaFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun format(amount: Double, mask: Boolean = false): String = 
        if (mask) "••••" else indiaFormat.format(amount)

    fun formatShort(amount: Double, mask: Boolean = false): String {
        if (mask) return "••••"
        return when {
            amount >= 10_00_000 -> "₹${String.format("%.1f", amount / 10_00_000)}L"
            amount >= 1_000 -> "₹${String.format("%.1f", amount / 1_000)}K"
            else -> format(amount)
        }
    }
}
