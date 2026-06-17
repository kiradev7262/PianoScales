package com.pianoscales.learnmusic.di

import com.pianoscales.learnmusic.data.repository.ProgressRepositoryImpl
import com.pianoscales.learnmusic.data.repository.VideoRepositoryImpl
import com.pianoscales.learnmusic.domain.progress.ProgressRepository
import com.pianoscales.learnmusic.domain.video.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        progressRepositoryImpl: ProgressRepositoryImpl
    ): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: com.pianoscales.learnmusic.data.profile.ProfileRepositoryImpl
    ): com.pianoscales.learnmusic.domain.profile.ProfileRepository

    @Binds
    @Singleton
    abstract fun bindBeginnerProgressRepository(
        beginnerProgressRepositoryImpl: com.pianoscales.learnmusic.data.repository.BeginnerProgressRepositoryImpl
    ): com.pianoscales.learnmusic.domain.progress.BeginnerProgressRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        videoRepositoryImpl: VideoRepositoryImpl
    ): VideoRepository
}
