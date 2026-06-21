package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.domain.settings.SettingsRepository
import com.pianoscales.learnmusic.domain.songs.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongsPackUiState(
    val songs: List<Song> = emptyList(),
    val pianoMode: PianoMode = PianoMode.VIRTUAL,
    val showOnboarding: Boolean = false
)

@HiltViewModel
class SongsPackViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SongsPackUiState())
    val uiState: StateFlow<SongsPackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            songRepository.refreshSongs()
        }

        songRepository.getSongs().onEach { songs ->
            _uiState.update { it.copy(songs = songs) }
        }.launchIn(viewModelScope)

        settingsRepository.getPianoMode().onEach { mode ->
            _uiState.update { it.copy(pianoMode = mode) }
        }.launchIn(viewModelScope)
    }

    fun toggleExternalPianoMode(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                val shouldShowOnboarding = settingsRepository.shouldShowExternalPianoOnboarding().first()
                if (shouldShowOnboarding) {
                    _uiState.update { it.copy(showOnboarding = true) }
                } else {
                    // This will be called if onboarding was already accepted but mode was OFF
                    // Note: Permission check happens in UI
                    _uiState.update { it.copy(pianoMode = PianoMode.EXTERNAL) }
                }
            }
        } else {
            viewModelScope.launch {
                settingsRepository.setPianoMode(PianoMode.VIRTUAL)
            }
        }
    }

    fun onOnboardingAccepted(dontShowAgain: Boolean) {
        _uiState.update { it.copy(showOnboarding = false) }
        viewModelScope.launch {
            if (dontShowAgain) {
                settingsRepository.setExternalPianoOnboardingShown(true)
            }
            // Preference update happens AFTER permission is granted in UI
            // but for now we update UI state to signal intent
            _uiState.update { it.copy(pianoMode = PianoMode.EXTERNAL) }
        }
    }

    fun onOnboardingCancelled() {
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun confirmExternalMode() {
        viewModelScope.launch {
            settingsRepository.setPianoMode(PianoMode.EXTERNAL)
        }
    }

    fun revertToVirtualMode() {
        viewModelScope.launch {
            settingsRepository.setPianoMode(PianoMode.VIRTUAL)
        }
    }
}
