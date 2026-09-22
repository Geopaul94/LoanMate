package com.loanmate.di

import com.loanmate.utils.BackupManager
import com.loanmate.utils.BackupService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupService(impl: BackupManager): BackupService
}
