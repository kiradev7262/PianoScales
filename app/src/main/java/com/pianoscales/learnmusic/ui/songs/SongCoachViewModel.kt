package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.audio.pitch.PitchDetector
import com.pianoscales.learnmusic.audio.pitch.PitchToNoteMapper
import com.pianoscales.learnmusic.domain.settings.SettingsRepository
import com.pianoscales.learnmusic.domain.songs.SongRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongCoachUiState(
    val song: Song? = null,
    val currentLineIndex: Int = 0,
    val currentNoteIndex: Int = 0,
    val isCompleted: Boolean = false,
    val pianoMode: PianoMode = PianoMode.VIRTUAL,
    val isListening: Boolean = false
) {
    val currentLine: SongLine? get() = song?.lines?.getOrNull(currentLineIndex)
    val currentNote: NoteWithOctave? get() = currentLine?.notes?.getOrNull(currentNoteIndex)
}

@HiltViewModel
class SongCoachViewModel @Inject constructor(
    private val soundPoolManager: SoundPoolManager,
    private val songRepository: SongRepository,
    private val pitchDetector: PitchDetector,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCoachUiState())
    val uiState: StateFlow<SongCoachUiState> = _uiState.asStateFlow()

    private var pitchDetectionJob: Job? = null

    init {
        val songId: String? = savedStateHandle["songId"]
        if (songId != null) {
            viewModelScope.launch {
                songRepository.getSongs().collect { songs ->
                    val song = songs.find { it.songId == songId }
                    if (song != null) {
                        _uiState.update { it.copy(song = song) }
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.getPianoMode().collect { mode ->
                _uiState.update { it.copy(pianoMode = mode) }
                if (mode == PianoMode.EXTERNAL) {
                    startListening()
                } else {
                    stopListening()
                }
            }
        }
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        
        _uiState.update { it.copy(isListening = true) }
        pitchDetectionJob?.cancel()
        pitchDetectionJob = viewModelScope.launch {
            pitchDetector.startListening { _, frequency, _, isStable ->
                if (isStable) {
                    val detectedNoteWithOctave = PitchToNoteMapper.mapFrequencyToNoteWithOctave(frequency)
                    if (detectedNoteWithOctave != null) {
                        evaluateNote(detectedNoteWithOctave.note, detectedNoteWithOctave.octave)
                    }
                }
            }
        }
    }

    fun stopListening() {
        pitchDetectionJob?.cancel()
        pitchDetectionJob = null
        pitchDetector.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    fun onNotePlayed(note: Note, octave: Int) {
        if (_uiState.value.pianoMode == PianoMode.EXTERNAL) return

        soundPoolManager.playNote(note, octave)
        evaluateNote(note, octave)
    }

    private fun evaluateNote(note: Note, octave: Int) {
        val state = _uiState.value
        if (state.isCompleted || state.song == null) return

        val expected = state.currentNote ?: return
        if (note == expected.note && octave == expected.octave) {
            advance()
        }
    }

    private fun advance() {
        _uiState.update { state ->
            val song = state.song ?: return@update state
            val currentLine = state.currentLine ?: return@update state
            
            val nextNoteIndex = state.currentNoteIndex + 1
            if (nextNoteIndex < currentLine.notes.size) {
                state.copy(currentNoteIndex = nextNoteIndex)
            } else {
                val nextLineIndex = state.currentLineIndex + 1
                if (nextLineIndex < song.lines.size) {
                    state.copy(currentLineIndex = nextLineIndex, currentNoteIndex = 0)
                } else {
                    state.copy(isCompleted = true)
                }
            }
        }
    }

    fun reset() {
        _uiState.update { it.copy(currentLineIndex = 0, currentNoteIndex = 0, isCompleted = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
