package com.pianoscales.learnmusic.domain.settings

import com.pianoscales.learnmusic.ui.songs.PianoMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun shouldShowExternalPianoOnboarding(): Flow<Boolean>
    suspend fun setExternalPianoOnboardingShown(shown: Boolean)
    fun getPianoMode(): Flow<PianoMode>
    suspend fun setPianoMode(mode: PianoMode)
}
