package com.example.pianoscales.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.domain.progress.GetNoteProgressUseCase
import com.example.pianoscales.domain.progress.GetOverallProgressUseCase
import com.example.pianoscales.domain.progress.NoteProgress
import com.example.pianoscales.domain.progress.OverallProgress
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class NoteSelectorUiState(
    val noteProgress: Map<Note, NoteProgress> = emptyMap(),
    val overallProgress: OverallProgress = OverallProgress(0, 0, 0f)
)

@HiltViewModel
class NoteSelectorViewModel @Inject constructor(
    private val getNoteProgressUseCase: GetNoteProgressUseCase,
    private val getOverallProgressUseCase: GetOverallProgressUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteSelectorUiState())
    val uiState: StateFlow<NoteSelectorUiState> = _uiState.asStateFlow()

    init {
        getOverallProgressUseCase().onEach { overall ->
            _uiState.update { it.copy(overallProgress = overall) }
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
    }
}
