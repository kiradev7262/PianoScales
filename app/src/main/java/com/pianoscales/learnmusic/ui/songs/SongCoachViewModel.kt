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
    val isListening: Boolean = false,
    val isDemoPlaying: Boolean = false
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
    private var demoJob: Job? = null

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
        if (_uiState.value.isListening || _uiState.value.isDemoPlaying) return
        
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
        if (_uiState.value.pianoMode == PianoMode.EXTERNAL || _uiState.value.isDemoPlaying) return

        soundPoolManager.playNote(note, octave)
        evaluateNote(note, octave)
    }

    private fun evaluateNote(note: Note, octave: Int) {
        val state = _uiState.value
        if (state.isCompleted || state.song == null || state.isDemoPlaying) return

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

    fun toggleDemo() {
        if (_uiState.value.isDemoPlaying) {
            stopDemo()
        } else {
            startDemo()
        }
    }

    private fun startDemo() {
        val song = _uiState.value.song ?: return
        
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            val wasListening = _uiState.value.isListening
            val previousLineIndex = _uiState.value.currentLineIndex
            val previousNoteIndex = _uiState.value.currentNoteIndex
            val previousCompleted = _uiState.value.isCompleted
            
            stopListening()
            
            _uiState.update { it.copy(isDemoPlaying = true, currentLineIndex = 0, currentNoteIndex = 0, isCompleted = false) }
            
            try {
                song.lines.forEachIndexed { lineIndex, line ->
                    _uiState.update { it.copy(currentLineIndex = lineIndex, currentNoteIndex = 0) }
                    line.notes.forEachIndexed { noteIndex, noteWithOctave ->
                        _uiState.update { it.copy(currentNoteIndex = noteIndex) }
                        soundPoolManager.playNote(noteWithOctave.note, noteWithOctave.octave)
                        kotlinx.coroutines.delay(450)
                    }
                    kotlinx.coroutines.delay(200) // Small gap between lines
                }
            } finally {
                _uiState.update { 
                    it.copy(
                        isDemoPlaying = false, 
                        currentLineIndex = previousLineIndex, 
                        currentNoteIndex = previousNoteIndex,
                        isCompleted = previousCompleted
                    ) 
                }
                if (wasListening || _uiState.value.pianoMode == PianoMode.EXTERNAL) {
                    startListening()
                }
            }
        }
    }

    private fun stopDemo() {
        demoJob?.cancel()
    }

    fun reset() {
        if (_uiState.value.isDemoPlaying) stopDemo()
        _uiState.update { it.copy(currentLineIndex = 0, currentNoteIndex = 0, isCompleted = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
