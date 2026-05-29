package com.loanmate.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatMonthYear(timestamp: Long): String = monthYearFormat.format(Date(timestamp))

    fun getDaysUntil(timestamp: Long): Int {
        val diff = timestamp - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    fun nextEmiDate(firstEmiDateMs: Long, completedEmis: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = firstEmiDateMs
        cal.add(Calendar.MONTH, completedEmis)
        return cal.timeInMillis
    }

    fun getDueDateStatus(daysUntil: Int): DueDateStatus = when {
        daysUntil < 0 -> DueDateStatus.OVERDUE
        daysUntil <= 3 -> DueDateStatus.URGENT
        daysUntil <= 7 -> DueDateStatus.UPCOMING
        else -> DueDateStatus.SAFE
    }

    enum class DueDateStatus { OVERDUE, URGENT, UPCOMING, SAFE }
}
