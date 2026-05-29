package com.loanmate.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {

    private val indiaFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun format(amount: Double): String = indiaFormat.format(amount)

    fun formatShort(amount: Double): String = when {
        amount >= 10_00_000 -> "₹${String.format("%.1f", amount / 10_00_000)}L"
        amount >= 1_000 -> "₹${String.format("%.1f", amount / 1_000)}K"
        else -> format(amount)
    }
}
