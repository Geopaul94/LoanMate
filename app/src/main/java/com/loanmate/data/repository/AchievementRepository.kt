package com.loanmate.data.repository

import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.local.AchievementType
import com.loanmate.data.local.dao.AchievementDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(private val dao: AchievementDao) {

    fun getAllAchievements(): Flow<List<AchievementEntity>> = dao.getAllAchievements()

    fun getEarnedAchievements(): Flow<List<AchievementEntity>> = dao.getEarnedAchievements()

    suspend fun unlockAchievement(type: AchievementType) {
        val existing = dao.getAchievementByType(type)
        if (existing != null && !existing.isEarned) {
            dao.updateAchievement(existing.copy(isEarned = true, earnedAt = System.currentTimeMillis()))
        }
    }

    suspend fun seedAchievements() {
        val defaults = listOf(
            AchievementEntity(AchievementType.FIRST_EMI_PAID, "First Step", "Paid your first EMI!", "🎯"),
            AchievementEntity(AchievementType.FIRST_LOAN_COMPLETED, "Debt Slayer", "Completed your first loan!", "⚔️"),
            AchievementEntity(AchievementType.THREE_LOANS_COMPLETED, "Triple Win", "Completed 3 loans!", "🏆"),
            AchievementEntity(AchievementType.EMI_STREAK_5, "On a Roll", "5 EMIs paid on time!", "🔥"),
            AchievementEntity(AchievementType.EMI_STREAK_10, "Consistency King", "10 EMIs paid on time!", "👑"),
            AchievementEntity(AchievementType.ON_TIME_PAYER, "Punctual Payer", "Never missed a due date!", "⏰"),
            AchievementEntity(AchievementType.DEBT_DESTROYER, "Debt Destroyer", "Crushed all your loans!", "💪")
        )
        defaults.forEach { achievement ->
            if (dao.getAchievementByType(achievement.type) == null) {
                dao.insertAchievement(achievement)
            }
        }
    }
}
