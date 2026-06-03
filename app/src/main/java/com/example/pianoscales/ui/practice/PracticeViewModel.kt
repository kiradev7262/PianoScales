package com.example.pianoscales.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.audio.pitch.PitchDetector
import com.example.pianoscales.audio.playback.NotePlayer
import com.example.pianoscales.domain.progress.ProgressRepository
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import com.example.pianoscales.theory.TheoryExplanation
import com.example.pianoscales.theory.fingering.FingeringGuide
import com.example.pianoscales.theory.fingering.Hand
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
    val includeOctave: Boolean = false,
    val selectedHand: Hand = Hand.RIGHT,
    val theoryExplanation: TheoryExplanation? = null,
    val isTheoryExpanded: Boolean = false,
    val guidedPractice: GuidedPracticeState = GuidedPracticeState(),
    val isLessonAlreadyCompleted: Boolean = false,
    val showFirstTimeCompletion: Boolean = false
) {
    fun getCurrentFingeringGuide(): FingeringGuide? {
        return theoryExplanation?.fingeringGuides?.find { it.hand == selectedHand }
    }
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val notePlayer: NotePlayer,
    private val pitchDetector: PitchDetector,
    private val progressRepository: ProgressRepository
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
        
        // Load completion status
        viewModelScope.launch {
            progressRepository.getAllProgress().collect { allProgress ->
                val completed = allProgress.any { it.rootNote == rootNote && it.conceptType == conceptType && it.completed }
                _uiState.update { it.copy(isLessonAlreadyCompleted = completed) }
            }
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

    fun toggleHand(hand: Hand) {
        _uiState.update { 
            it.copy(
                selectedHand = hand,
                guidedPractice = it.guidedPractice.copy(
                    targetFinger = it.theoryExplanation?.fingeringGuides
                        ?.find { guide -> guide.hand == hand }
                        ?.steps?.getOrNull(it.guidedPractice.currentIndex)?.finger
                )
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
                _uiState.update { it.copy(detectedFrequency = frequency, inputVolume = volume) }
                if (isStable && note != null) {
                    evaluateNote(note, isStable)
                } else if (!isStable) {
                    // Reset evaluation when pitch becomes unstable
                    _uiState.update { currentState ->
                        currentState.copy(
                            detectedNote = null,
                            isStablePitch = false,
                            guidedPractice = if (currentState.guidedPractice.lastEvaluatedNote != null) {
                                currentState.guidedPractice.copy(lastEvaluatedNote = null)
                            } else currentState.guidedPractice
                        )
                    }
                }
            }
        }
    }

    fun onKeyClick(note: Note) {
        // 0. Reset pitch detector filters to avoid old note hold interference
        pitchDetector.resetFilters()

        if (_uiState.value.isListening) {
            stopListening()
        }
        // 1. Play audio
        notePlayer.playNote(note)
        
        // 2. Evaluate note for lesson progression (simulates a stable pitch detection)
        evaluateNote(note, isStable = true)
        
        // 3. Briefly show as detected note for visual feedback
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _uiState.update { 
                if (it.detectedNote == note) it.copy(detectedNote = null, isStablePitch = false)
                else it
            }
        }
    }

    private fun evaluateNote(note: Note, isStable: Boolean) {
        _uiState.update { currentState ->
            var newCompletedNotes = currentState.completedNotes
            var newGuidedPractice = currentState.guidedPractice
            val target = currentState.guidedPractice.targetNote

            // Guided practice logic
            if (currentState.guidedPractice.isRunning && !currentState.guidedPractice.lessonCompleted) {
                // Only evaluate if this is a new stable note detection
                if (note != currentState.guidedPractice.lastEvaluatedNote) {
                    if (target != null) {
                        if (note == target) {
                            // Correct note
                            val nextIndex = currentState.guidedPractice.currentIndex + 1
                            val isCompleted = nextIndex >= currentState.generatedNotes.size
                            val fingeringGuide = currentState.getCurrentFingeringGuide()

                            newGuidedPractice = currentState.guidedPractice.copy(
                                currentIndex = nextIndex,
                                targetNote = if (isCompleted) null else currentState.generatedNotes.getOrNull(nextIndex),
                                targetFinger = if (isCompleted) null else fingeringGuide?.steps?.getOrNull(nextIndex)?.finger,
                                completedNotes = currentState.guidedPractice.completedNotes + currentState.guidedPractice.currentIndex,
                                lessonCompleted = isCompleted,
                                lastResult = PracticeResult.Correct(fingeringGuide?.steps?.getOrNull(currentState.guidedPractice.currentIndex)?.finger),
                                lastEvaluatedNote = note
                            )

                            // Also mark as completed in regular practice set for consistent visual progress
                            if (currentState.generatedNotes.contains(note)) {
                                newCompletedNotes = currentState.completedNotes + note
                            }

                            if (isCompleted) {
                                onLessonCompleted(currentState.rootNote, currentState.conceptType)
                            }
                        } else {
                            // Incorrect note - do NOT advance, do NOT add to completed notes
                            newGuidedPractice = currentState.guidedPractice.copy(
                                lastResult = PracticeResult.Incorrect(target, note),
                                lastEvaluatedNote = note
                            )
                        }
                    }
                }
            } else if (!currentState.guidedPractice.isRunning) {
                // Regular practice highlighting (only when guided practice is not active)
                if (currentState.generatedNotes.contains(note)) {
                    newCompletedNotes = currentState.completedNotes + note
                }
            }

            currentState.copy(
                detectedNote = note,
                isStablePitch = isStable,
                completedNotes = newCompletedNotes,
                guidedPractice = newGuidedPractice
            )
        }
    }

    fun startGuidedPractice() {
        _uiState.update { 
            if (it.generatedNotes.isEmpty()) return@update it
            val fingeringGuide = it.getCurrentFingeringGuide()
            it.copy(
                guidedPractice = GuidedPracticeState(
                    isRunning = true,
                    currentIndex = 0,
                    targetNote = it.generatedNotes[0],
                    targetFinger = fingeringGuide?.steps?.getOrNull(0)?.finger,
                    completedNotes = emptySet(),
                    lessonCompleted = false,
                    lastResult = null,
                    lastEvaluatedNote = null
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
            it.copy(guidedPractice = GuidedPracticeState(isRunning = false))
        }
    }

    fun resetGuidedPractice() {
        stopGuidedPractice()
    }

    fun playTargetNote() {
        val target = _uiState.value.guidedPractice.targetNote
        if (target != null) {
            notePlayer.playNote(target)
        }
    }

    private fun onLessonCompleted(rootNote: Note, conceptType: ConceptType) {
        val wasAlreadyCompleted = _uiState.value.isLessonAlreadyCompleted
        viewModelScope.launch {
            progressRepository.saveProgress(rootNote, conceptType, true)
            if (!wasAlreadyCompleted) {
                _uiState.update { it.copy(showFirstTimeCompletion = true) }
            }
        }
        android.util.Log.d("GuidedPractice", "Lesson Completed: $rootNote $conceptType")
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showFirstTimeCompletion = false) }
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
