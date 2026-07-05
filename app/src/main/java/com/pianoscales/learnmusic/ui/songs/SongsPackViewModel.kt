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
    val customSongs: List<Song> = emptyList(),
    val pianoMode: PianoMode = PianoMode.VIRTUAL,
    val showOnboarding: Boolean = false,
    val isExporting: Boolean = false,
    val selectedExportSongIds: Set<String> = emptySet()
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

        songRepository.getCustomSongs().onEach { customSongs ->
            _uiState.update { it.copy(customSongs = customSongs) }
        }.launchIn(viewModelScope)

        settingsRepository.getPianoMode().onEach { mode ->
            _uiState.update { it.copy(pianoMode = mode) }
        }.launchIn(viewModelScope)
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            songRepository.deleteSong(songId)
        }
    }

    fun startExport() {
        _uiState.update { it.copy(isExporting = true, selectedExportSongIds = emptySet()) }
    }

    fun dismissExport() {
        _uiState.update { it.copy(isExporting = false, selectedExportSongIds = emptySet()) }
    }

    fun toggleSongSelection(songId: String) {
        _uiState.update { state ->
            val current = state.selectedExportSongIds
            val new = if (current.contains(songId)) {
                current - songId
            } else {
                current + songId
            }
            state.copy(selectedExportSongIds = new)
        }
    }

    fun selectAllSongs() {
        _uiState.update { state ->
            state.copy(selectedExportSongIds = state.customSongs.map { it.songId }.toSet())
        }
    }

    fun deselectAllSongs() {
        _uiState.update { it.copy(selectedExportSongIds = emptySet()) }
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
