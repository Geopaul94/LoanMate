package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.local.AchievementType
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY earnedAt DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()

    @Query("SELECT * FROM achievements WHERE isEarned = 1")
    fun getEarnedAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE type = :type")
    suspend fun getAchievementByType(type: AchievementType): AchievementEntity?
}
