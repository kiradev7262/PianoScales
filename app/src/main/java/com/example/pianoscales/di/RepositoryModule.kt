package com.example.pianoscales.di

import com.example.pianoscales.data.repository.ProgressRepositoryImpl
import com.example.pianoscales.domain.progress.ProgressRepository
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
        profileRepositoryImpl: com.example.pianoscales.data.profile.ProfileRepositoryImpl
    ): com.example.pianoscales.domain.profile.ProfileRepository
}
