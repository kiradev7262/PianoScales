package com.pianoscales.learnmusic.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.domain.progress.BeginnerProgressRepository
import com.pianoscales.learnmusic.domain.progress.GetNoteProgressUseCase
import com.pianoscales.learnmusic.domain.progress.GetOverallProgressUseCase
import com.pianoscales.learnmusic.domain.progress.LessonProgress
import com.pianoscales.learnmusic.domain.progress.NoteProgress
import com.pianoscales.learnmusic.domain.progress.OverallProgress
import com.pianoscales.learnmusic.domain.progress.ProgressRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteSelectorUiState(
    val noteProgress: Map<Note, NoteProgress> = emptyMap(),
    val overallProgress: OverallProgress = OverallProgress(0, 0, 0f),
    val latestProgress: LessonProgress? = null,
    val profileImagePath: String? = null,
    val displayName: String = "Learner",
    val isBeginnerJourneyComplete: Boolean = false
)

@HiltViewModel
class NoteSelectorViewModel @Inject constructor(
    private val getNoteProgressUseCase: GetNoteProgressUseCase,
    private val getOverallProgressUseCase: GetOverallProgressUseCase,
    private val progressRepository: ProgressRepository,
    private val beginnerRepository: BeginnerProgressRepository,
    private val profileRepository: com.pianoscales.learnmusic.domain.profile.ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteSelectorUiState())
    val uiState: StateFlow<NoteSelectorUiState> = _uiState.asStateFlow()

    init {
        profileRepository.getProfileImage().onEach { path ->
            _uiState.update { it.copy(profileImagePath = path) }
        }.launchIn(viewModelScope)

        profileRepository.getDisplayName().onEach { name ->
            _uiState.update { it.copy(displayName = name) }
        }.launchIn(viewModelScope)

        getOverallProgressUseCase().onEach { overall ->
            _uiState.update { it.copy(overallProgress = overall) }
        }.launchIn(viewModelScope)

        progressRepository.getAllProgress().onEach { allProgress ->
            val latest = allProgress.filter { !it.completed }.maxByOrNull { it.completedAt }
                ?: allProgress.maxByOrNull { it.completedAt }
            _uiState.update { it.copy(latestProgress = latest) }
        }.launchIn(viewModelScope)

        Note.entries.forEach { note ->
            getNoteProgressUseCase(note).onEach { progress ->
                _uiState.update { 
                    val newMap = it.noteProgress.toMutableMap()
                    newMap[note] = progress
                    it.copy(noteProgress = newMap)
                }
            }.launchIn(viewModelScope)
        }

        beginnerRepository.getCompletedLessons().onEach { completed ->
            _uiState.update { it.copy(isBeginnerJourneyComplete = completed.size >= 5) }
        }.launchIn(viewModelScope)
    }

    fun updateProfileImage(path: String) {
        viewModelScope.launch {
            profileRepository.updateProfileImage(path)
        }
    }
}
