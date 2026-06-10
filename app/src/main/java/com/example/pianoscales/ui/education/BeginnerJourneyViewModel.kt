package com.example.pianoscales.ui.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.audio.playback.NotePlayer
import com.example.pianoscales.domain.progress.BeginnerProgressRepository
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeginnerJourneyUiState(
    val completedLessons: Set<Int> = emptySet(),
    val progressPercentage: Float = 0f
)

@HiltViewModel
class BeginnerJourneyViewModel @Inject constructor(
    private val repository: BeginnerProgressRepository,
    private val notePlayer: NotePlayer
) : ViewModel() {

    val uiState: StateFlow<BeginnerJourneyUiState> = repository.getCompletedLessons()
        .map { completed ->
            BeginnerJourneyUiState(
                completedLessons = completed,
                progressPercentage = completed.size.toFloat() / 5f
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BeginnerJourneyUiState()
        )

    fun completeLesson(lessonId: Int) {
        viewModelScope.launch {
            repository.completeLesson(lessonId)
        }
    }

    fun playNote(note: Note, octave: Int = 4) {
        notePlayer.playNote(note, octave)
    }

    fun playScaleDemo(notes: List<Note>) {
        viewModelScope.launch {
            notePlayer.playSequence(notes)
        }
    }
}
