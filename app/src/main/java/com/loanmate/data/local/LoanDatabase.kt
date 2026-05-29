package com.loanmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loanmate.data.local.dao.*

@Database(
    entities = [
        LoanEntity::class,
        PaymentHistoryEntity::class,
        DocumentEntity::class,
        AchievementEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao
    abstract fun documentDao(): DocumentDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao
}
