package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SongCoachUiState(
    val song: Song = HappyBirthday,
    val currentLineIndex: Int = 0,
    val currentNoteIndex: Int = 0,
    val isCompleted: Boolean = false
) {
    val currentLine: SongLine get() = song.lines[currentLineIndex]
    val currentNote: NoteWithOctave get() = currentLine.notes[currentNoteIndex]
}

@HiltViewModel
class SongCoachViewModel @Inject constructor(
    private val soundPoolManager: SoundPoolManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCoachUiState())
    val uiState: StateFlow<SongCoachUiState> = _uiState.asStateFlow()

    fun onNotePlayed(note: Note, octave: Int) {
        soundPoolManager.playNote(note, octave)
        
        val state = _uiState.value
        if (state.isCompleted) return

        val expected = state.currentNote
        if (note == expected.note && octave == expected.octave) {
            advance()
        }
    }

    private fun advance() {
        _uiState.update { state ->
            val nextNoteIndex = state.currentNoteIndex + 1
            if (nextNoteIndex < state.currentLine.notes.size) {
                state.copy(currentNoteIndex = nextNoteIndex)
            } else {
                val nextLineIndex = state.currentLineIndex + 1
                if (nextLineIndex < state.song.lines.size) {
                    state.copy(currentLineIndex = nextLineIndex, currentNoteIndex = 0)
                } else {
                    state.copy(isCompleted = true)
                }
            }
        }
    }

    fun reset() {
        _uiState.update { SongCoachUiState() }
    }
}
