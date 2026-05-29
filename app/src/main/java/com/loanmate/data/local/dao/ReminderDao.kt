package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE loanId = :loanId")
    fun getRemindersByLoanId(loanId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE loanId = :loanId")
    suspend fun deleteRemindersByLoanId(loanId: Long)
}
