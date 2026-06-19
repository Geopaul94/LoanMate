package com.loanmate.di

import android.content.Context
import androidx.room.Room
import com.loanmate.data.local.LoanDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoanDatabase =
        Room.databaseBuilder(context, LoanDatabase::class.java, "loanmate.db")
            .addMigrations(LoanDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideLoanDao(db: LoanDatabase) = db.loanDao()
    @Provides fun providePaymentDao(db: LoanDatabase) = db.paymentHistoryDao()
    @Provides fun provideDocumentDao(db: LoanDatabase) = db.documentDao()
    @Provides fun provideAchievementDao(db: LoanDatabase) = db.achievementDao()
    @Provides fun provideReminderDao(db: LoanDatabase) = db.reminderDao()
}
