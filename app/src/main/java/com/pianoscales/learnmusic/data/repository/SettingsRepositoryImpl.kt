package com.pianoscales.learnmusic.data.repository

import android.content.Context
import com.pianoscales.learnmusic.domain.settings.SettingsRepository
import com.pianoscales.learnmusic.ui.songs.PianoMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val sharedPreferences = context.getSharedPreferences("piano_scales_settings", Context.MODE_PRIVATE)
    
    private val _shouldShowOnboarding = MutableStateFlow(
        sharedPreferences.getBoolean(KEY_EXTERNAL_PIANO_ONBOARDING, true)
    )

    private val _pianoMode = MutableStateFlow(
        PianoMode.valueOf(sharedPreferences.getString(KEY_PIANO_MODE, PianoMode.VIRTUAL.name) ?: PianoMode.VIRTUAL.name)
    )

    override fun shouldShowExternalPianoOnboarding(): Flow<Boolean> = _shouldShowOnboarding.asStateFlow()

    override suspend fun setExternalPianoOnboardingShown(shown: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_EXTERNAL_PIANO_ONBOARDING, !shown).apply()
        _shouldShowOnboarding.value = !shown
    }

    override fun getPianoMode(): Flow<PianoMode> = _pianoMode.asStateFlow()

    override suspend fun setPianoMode(mode: PianoMode) {
        sharedPreferences.edit().putString(KEY_PIANO_MODE, mode.name).apply()
        _pianoMode.value = mode
    }

    companion object {
        private const val KEY_EXTERNAL_PIANO_ONBOARDING = "external_piano_onboarding_show"
        private const val KEY_PIANO_MODE = "piano_mode"
    }
}
