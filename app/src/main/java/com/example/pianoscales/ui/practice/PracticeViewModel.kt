package com.example.pianoscales.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.audio.pitch.PitchDetector
import com.example.pianoscales.audio.playback.NotePlayer
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.theory.TheoryExplanation
import com.example.pianoscales.theory.generators.TheoryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    val rootNote: Note = Note.C,
    val conceptType: ConceptType = ConceptType.MAJOR_SCALE,
    val generatedNotes: List<Note> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPlayingNote: Note? = null,
    val isListening: Boolean = false,
    val detectedNote: Note? = null,
    val detectedFrequency: Float = 0f,
    val isStablePitch: Boolean = false,
    val inputVolume: Float = 0f,
    val isAudioLoaded: Boolean = false,
    val completedNotes: Set<Note> = emptySet(),
    val includeOctave: Boolean = true,
    val theoryExplanation: TheoryExplanation? = null,
    val isTheoryExpanded: Boolean = false,
    val guidedPractice: GuidedPracticeState = GuidedPracticeState()
)

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val notePlayer: NotePlayer,
    private val pitchDetector: PitchDetector
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notePlayer.isLoaded.collect { loaded ->
                _uiState.update { it.copy(isAudioLoaded = loaded) }
            }
        }
    }

    fun init(rootNote: Note, conceptType: ConceptType) {
        _uiState.update { 
            val theory = TheoryEngine.generateTheory(rootNote, conceptType, it.includeOctave)
            it.copy(
                rootNote = rootNote,
                conceptType = conceptType,
                generatedNotes = TheoryEngine.generateNotes(rootNote, conceptType, it.includeOctave),
                completedNotes = emptySet(),
                theoryExplanation = theory,
                guidedPractice = GuidedPracticeState() // Reset guided practice on init
            )
        }
    }

    fun toggleOctave() {
        _uiState.update { 
            val newIncludeOctave = !it.includeOctave
            val theory = TheoryEngine.generateTheory(it.rootNote, it.conceptType, newIncludeOctave)
            it.copy(
                includeOctave = newIncludeOctave,
                generatedNotes = TheoryEngine.generateNotes(it.rootNote, it.conceptType, newIncludeOctave),
                completedNotes = emptySet(),
                theoryExplanation = theory,
                guidedPractice = GuidedPracticeState() // Reset guided practice
            )
        }
    }

    fun toggleTheoryExpansion() {
        _uiState.update { it.copy(isTheoryExpanded = !it.isTheoryExpanded) }
    }

    fun playSequence() {
        if (_uiState.value.isPlaying || _uiState.value.isListening || !_uiState.value.isAudioLoaded) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            notePlayer.playSequence(
                notes = _uiState.value.generatedNotes,
                onNoteStarted = { note ->
                    _uiState.update { it.copy(currentPlayingNote = note) }
                }
            )
            _uiState.update { it.copy(isPlaying = false, currentPlayingNote = null) }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (_uiState.value.isPlaying) return
        
        _uiState.update { 
            it.copy(
                isListening = true, 
                detectedNote = null, 
                detectedFrequency = 0f,
                isStablePitch = false,
                inputVolume = 0f
            ) 
        }
        viewModelScope.launch {
            pitchDetector.startListening { note, frequency, volume, isStable ->
                _uiState.update { currentState ->
                    var newCompletedNotes = currentState.completedNotes
                    var newGuidedPractice = currentState.guidedPractice

                    if (isStable && note != null) {
                        // Regular practice highlighting
                        if (currentState.generatedNotes.contains(note)) {
                            newCompletedNotes = currentState.completedNotes + note
                        }

                        // Guided practice logic
                        if (currentState.guidedPractice.isRunning && !currentState.guidedPractice.lessonCompleted) {
                            val target = currentState.guidedPractice.targetNote
                            if (target != null) {
                                if (note == target) {
                                    // Correct note
                                    val nextIndex = currentState.guidedPractice.currentIndex + 1
                                    val isCompleted = nextIndex >= currentState.generatedNotes.size
                                    newGuidedPractice = currentState.guidedPractice.copy(
                                        currentIndex = nextIndex,
                                        targetNote = if (isCompleted) null else currentState.generatedNotes.getOrNull(nextIndex),
                                        completedNotes = currentState.guidedPractice.completedNotes + currentState.guidedPractice.currentIndex,
                                        lessonCompleted = isCompleted,
                                        lastResult = PracticeResult.Correct
                                    )
                                    
                                    if (isCompleted) {
                                        onLessonCompleted(currentState.rootNote, currentState.conceptType)
                                    }
                                } else {
                                    // Incorrect note - only update if it's a different note than target
                                    newGuidedPractice = currentState.guidedPractice.copy(
                                        lastResult = PracticeResult.Incorrect(target, note)
                                    )
                                }
                            }
                        }
                    }

                    currentState.copy(
                        detectedNote = note,
                        detectedFrequency = if (note != null) frequency else 0f,
                        inputVolume = volume,
                        isStablePitch = isStable,
                        completedNotes = newCompletedNotes,
                        guidedPractice = newGuidedPractice
                    )
                }
            }
        }
    }

    fun startGuidedPractice() {
        _uiState.update { 
            if (it.generatedNotes.isEmpty()) return@update it
            it.copy(
                guidedPractice = GuidedPracticeState(
                    isRunning = true,
                    currentIndex = 0,
                    targetNote = it.generatedNotes[0],
                    completedNotes = emptySet(),
                    lessonCompleted = false,
                    lastResult = null
                )
            )
        }
        // Auto-start listening if not already
        if (!_uiState.value.isListening) {
            startListening()
        }
    }

    fun stopGuidedPractice() {
        _uiState.update { 
            it.copy(guidedPractice = it.guidedPractice.copy(isRunning = false))
        }
    }

    fun resetGuidedPractice() {
        startGuidedPractice()
    }

    fun playTargetNote() {
        val target = _uiState.value.guidedPractice.targetNote
        if (target != null) {
            notePlayer.playNote(target)
        }
    }

    private fun onLessonCompleted(rootNote: Note, conceptType: ConceptType) {
        // Hook for future persistence
        android.util.Log.d("GuidedPractice", "Lesson Completed: $rootNote $conceptType")
    }

    fun resetProgress() {
        _uiState.update { it.copy(completedNotes = emptySet()) }
    }

    private fun stopListening() {
        pitchDetector.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        super.onCleared()
        pitchDetector.stopListening()
    }
}
