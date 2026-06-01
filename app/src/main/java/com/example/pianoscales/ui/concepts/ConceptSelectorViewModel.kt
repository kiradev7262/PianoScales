package com.example.pianoscales.ui.concepts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.domain.progress.ProgressRepository
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ConceptSelectorUiState(
    val completedConcepts: Set<ConceptType> = emptySet()
)

@HiltViewModel
class ConceptSelectorViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConceptSelectorUiState())
    val uiState: StateFlow<ConceptSelectorUiState> = _uiState.asStateFlow()

    fun loadProgress(note: Note) {
        progressRepository.getAllProgress()
            .map { allProgress ->
                allProgress
                    .filter { it.rootNote == note && it.completed }
                    .map { it.conceptType }
                    .toSet()
            }
            .onEach { completed ->
                _uiState.update { it.copy(completedConcepts = completed) }
            }
            .launchIn(viewModelScope)
    }
}
