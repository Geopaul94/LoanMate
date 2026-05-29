package com.loanmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AchievementType {
    FIRST_EMI_PAID,
    FIRST_LOAN_COMPLETED,
    THREE_LOANS_COMPLETED,
    EMI_STREAK_5,
    EMI_STREAK_10,
    ON_TIME_PAYER,
    DEBT_DESTROYER
}

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val type: AchievementType,
    val title: String,
    val description: String,
    val emoji: String,
    val earnedAt: Long? = null,
    val isEarned: Boolean = false
)
