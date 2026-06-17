package com.pianoscales.learnmusic.ui.education

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.playback.NotePlayer
import com.pianoscales.learnmusic.domain.progress.BeginnerProgressRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeginnerJourneyUiState(
    val completedLessons: Set<Int> = emptySet(),
    val progressPercentage: Float = 0f,
    val isDemoPlaying: Boolean = false,
    val playingNotes: List<Note>? = null
)

@HiltViewModel
class BeginnerJourneyViewModel @Inject constructor(
    private val repository: BeginnerProgressRepository,
    private val notePlayer: NotePlayer,
    private val profileRepository: com.pianoscales.learnmusic.domain.profile.ProfileRepository
) : ViewModel() {

    private val _isDemoPlaying = MutableStateFlow(false)
    private val _playingNotes = MutableStateFlow<List<Note>?>(null)

    val uiState: StateFlow<BeginnerJourneyUiState> = combine(
        repository.getCompletedLessons(),
        _isDemoPlaying,
        _playingNotes
    ) { completed, isPlaying, notes ->
        BeginnerJourneyUiState(
            completedLessons = completed,
            progressPercentage = completed.size.toFloat() / 5f,
            isDemoPlaying = isPlaying,
            playingNotes = notes
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BeginnerJourneyUiState()
    )

    private var playbackJob: Job? = null

    fun completeLesson(lessonId: Int) {
        viewModelScope.launch {
            repository.completeLesson(lessonId)
            profileRepository.updateStreak()
        }
    }

    fun playNote(note: Note, octave: Int = 4) {
        notePlayer.playNote(note, octave)
    }

    fun playScaleDemo(notes: List<Note>) {
        if (_isDemoPlaying.value) return

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _isDemoPlaying.value = true
            _playingNotes.value = notes
            try {
                notePlayer.playSequence(notes)
            } finally {
                _isDemoPlaying.value = false
                _playingNotes.value = null
            }
        }
    }

    fun stopDemo() {
        playbackJob?.cancel()
        _isDemoPlaying.value = false
        _playingNotes.value = null
    }
}
