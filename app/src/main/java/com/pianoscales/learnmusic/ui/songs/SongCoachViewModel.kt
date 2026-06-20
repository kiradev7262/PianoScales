package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.domain.songs.SongRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongCoachUiState(
    val song: Song? = null,
    val currentLineIndex: Int = 0,
    val currentNoteIndex: Int = 0,
    val isCompleted: Boolean = false
) {
    val currentLine: SongLine? get() = song?.lines?.getOrNull(currentLineIndex)
    val currentNote: NoteWithOctave? get() = currentLine?.notes?.getOrNull(currentNoteIndex)
}

@HiltViewModel
class SongCoachViewModel @Inject constructor(
    private val soundPoolManager: SoundPoolManager,
    private val songRepository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCoachUiState())
    val uiState: StateFlow<SongCoachUiState> = _uiState.asStateFlow()

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
    }

    fun onNotePlayed(note: Note, octave: Int) {
        soundPoolManager.playNote(note, octave)
        
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
}
