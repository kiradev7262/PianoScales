package com.pianoscales.learnmusic.ui.education

import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.theory.ConceptType
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.generators.CircleKey
import com.pianoscales.learnmusic.theory.generators.CircleOfFifthsEngine
import com.pianoscales.learnmusic.theory.generators.TheoryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class CircleInteractiveMode {
    NONE, CHORD_FAMILIES, POWER_NOTES
}

data class CircleOfFifthsUiState(
    val selectedKey: CircleKey = CircleOfFifthsEngine.fullCircle[0],
    val isMajor: Boolean = true,
    val interactiveMode: CircleInteractiveMode = CircleInteractiveMode.NONE,
    val highlightedNotes: List<Note> = emptyList(),
    val currentLessonStep: Int = 0,
    val visibleKeyCount: Int = 12,
    val isGuidedMode: Boolean = false,
    val lessonText: String = ""
)

@HiltViewModel
class CircleOfFifthsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CircleOfFifthsUiState())
    val uiState: StateFlow<CircleOfFifthsUiState> = _uiState.asStateFlow()

    init {
        selectKey(CircleOfFifthsEngine.fullCircle[0])
    }

    fun selectKey(key: CircleKey) {
        updateHighlightedNotes(key, _uiState.value.isMajor)
        _uiState.update { it.copy(selectedKey = key) }

        if (_uiState.value.isGuidedMode) {
            checkLessonProgress(key)
        }
    }

    fun setInteractiveMode(mode: CircleInteractiveMode) {
        _uiState.update { it.copy(interactiveMode = mode) }
    }

    fun toggleMajorMinor() {
        val newIsMajor = !_uiState.value.isMajor
        updateHighlightedNotes(_uiState.value.selectedKey, newIsMajor)
        _uiState.update { it.copy(isMajor = newIsMajor) }
    }

    private fun updateHighlightedNotes(key: CircleKey, isMajor: Boolean) {
        val root = if (isMajor) key.note else key.relativeMinor
        val type = if (isMajor) ConceptType.MAJOR_SCALE else ConceptType.NATURAL_MINOR_SCALE
        val notes = TheoryEngine.generateNotes(root, type)
        _uiState.update { it.copy(highlightedNotes = notes) }
    }

    private fun checkLessonProgress(key: CircleKey) {
        val step = _uiState.value.currentLessonStep
        if (step == 1 && key.note == Note.C) {
            nextLessonStep()
        } else if (step == 2 && key.note == Note.G) {
            nextLessonStep()
        } else if (step == 5 && key.note == Note.F) {
            nextLessonStep()
        }
    }

    fun startGuidedLesson() {
        _uiState.update { it.copy(
            isGuidedMode = true,
            currentLessonStep = 1,
            visibleKeyCount = 1,
            lessonText = "Welcome to the Circle of Fifths! Let's start with C Major. Tap the 'C' at the top."
        ) }
    }
    
    fun nextLessonStep() {
        val nextStep = _uiState.value.currentLessonStep + 1
        val newVisibleCount = when (nextStep) {
            1 -> 1
            2 -> 2
            3 -> 2
            4 -> 8 // Show all sharp side
            5 -> 9 // Show F
            6 -> 12 // Show all
            else -> 12
        }
        
        val text = when (nextStep) {
            2 -> "G is the 5th note in the C Major scale (C-D-E-F-G). Tap G to move clockwise!"
            3 -> "Moving clockwise means moving by a 'Perfect Fifth'. This relationship builds the circle."
            4 -> "Each step clockwise adds one sharp (#). See how the keys build up from 0 to 7 sharps."
            5 -> "Moving counter-clockwise from C takes us to F. F is a fifth BELOW C. Tap F!"
            6 -> "Each step counter-clockwise adds one flat (b). Now the Circle of Fifths is complete!"
            else -> "Great job! You now understand the foundation of key signatures."
        }
        
        _uiState.update { it.copy(
            currentLessonStep = nextStep,
            visibleKeyCount = newVisibleCount,
            lessonText = text,
            isGuidedMode = nextStep <= 6
        ) }
    }
}
