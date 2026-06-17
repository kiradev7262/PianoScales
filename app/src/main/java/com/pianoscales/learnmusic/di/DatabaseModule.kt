package com.pianoscales.learnmusic.di

import android.content.Context
import androidx.room.Room
import com.pianoscales.learnmusic.data.local.ProgressDao
import com.pianoscales.learnmusic.data.local.ProgressDatabase
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
    fun provideProgressDatabase(@ApplicationContext context: Context): ProgressDatabase {
        return Room.databaseBuilder(
            context,
            ProgressDatabase::class.java,
            "piano_scales_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProgressDao(database: ProgressDatabase): ProgressDao {
        return database.progressDao()
    }

    @Provides
    fun provideUserProfileDao(database: ProgressDatabase): com.pianoscales.learnmusic.data.local.UserProfileDao {
        return database.userProfileDao()
    }
}
