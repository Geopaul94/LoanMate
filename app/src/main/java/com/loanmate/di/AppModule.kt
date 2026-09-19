package com.loanmate.di

import android.content.Context
import com.loanmate.utils.AppUpdateHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppUpdateHelper(@ApplicationContext context: Context): AppUpdateHelper {
        return AppUpdateHelper(context)
    }
}
