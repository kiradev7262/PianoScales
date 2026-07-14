package com.pianoscales.learnmusic.di

import com.pianoscales.learnmusic.ble.PianoBuddyBleManager
import com.pianoscales.learnmusic.ble.PianoBuddyBleManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BleModule {

    @Binds
    @Singleton
    abstract fun bindBleManager(impl: PianoBuddyBleManagerImpl): PianoBuddyBleManager
}
