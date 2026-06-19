package com.loanmate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loanmate.data.local.dao.*

@Database(
    entities = [
        LoanEntity::class,
        PaymentHistoryEntity::class,
        DocumentEntity::class,
        AchievementEntity::class,
        ReminderEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun paymentHistoryDao(): PaymentHistoryDao
    abstract fun documentDao(): DocumentDao
    abstract fun achievementDao(): AchievementDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        // v1 → v2: add soft-delete columns to loans
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE loans ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loans ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }
    }
}
