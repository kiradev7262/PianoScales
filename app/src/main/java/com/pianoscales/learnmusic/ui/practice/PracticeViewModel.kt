package com.pianoscales.learnmusic.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.pitch.PitchDetector
import com.pianoscales.learnmusic.audio.playback.NotePlayer
import com.pianoscales.learnmusic.data.models.VideoMetadata
import com.pianoscales.learnmusic.domain.progress.ProgressRepository
import com.pianoscales.learnmusic.domain.video.VideoRepository
import com.pianoscales.learnmusic.theory.ConceptType
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.TheoryExplanation
import com.pianoscales.learnmusic.theory.fingering.FingerInfo
import com.pianoscales.learnmusic.theory.fingering.FingeringGuide
import com.pianoscales.learnmusic.theory.fingering.Hand
import com.pianoscales.learnmusic.theory.generators.TheoryEngine
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ble.PianoBuddyBleManager
import com.pianoscales.learnmusic.domain.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PracticeUiState(
    val rootNote: Note = Note.C,
    val conceptType: ConceptType = ConceptType.MAJOR_SCALE,
    val generatedNotes: List<Note> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPlayingNote: Note? = null,
    val currentPlayingIndex: Int = -1,
    val currentPlayingOctave: Int = 4,
    val isListening: Boolean = false,
    val detectedNote: Note? = null,
    val detectedFrequency: Float = 0f,
    val detectedMidi: Int? = null,
    val detectedOctave: Int? = null,
    val detectionConfidence: Float = 0f,
    val detectionTimestamp: Long = 0L,
    val isStablePitch: Boolean = false,
    val inputVolume: Float = 0f,
    val isAudioLoaded: Boolean = false,
    val completedNotes: Set<Note> = emptySet(),
    val includeOctave: Boolean = true,
    val selectedHand: Hand = Hand.RIGHT,
    val theoryExplanation: TheoryExplanation? = null,
    val isTheoryExpanded: Boolean = false,
    val guidedPractice: GuidedPracticeState = GuidedPracticeState(),
    val videoMetadata: VideoMetadata? = null,
    val isLessonAlreadyCompleted: Boolean = false,
    val showFirstTimeCompletion: Boolean = false,
    val isAscendingDescendingMode: Boolean = false,
    val pendingActionAfterPermission: (() -> Unit)? = null,
    val pianoBuddyConnectionState: BleConnectionState = BleConnectionState.IDLE
) {
    fun getCurrentFingeringGuide(): FingeringGuide? {
        return theoryExplanation?.fingeringGuides?.find { it.hand == selectedHand }
    }

    fun getGuidedPracticeNotes(): List<Note> {
        if (generatedNotes.isEmpty()) return emptyList()
        if (!isAscendingDescendingMode) return generatedNotes

        val descending = generatedNotes.reversed().drop(1)
        return generatedNotes + descending
    }

    fun getGuidedPracticeFingering(): List<FingerInfo?> {
        val baseFingering: List<FingerInfo?> = getCurrentFingeringGuide()?.steps?.map { it.finger }
            ?: List(generatedNotes.size) { null }

        if (!isAscendingDescendingMode) return baseFingering

        val descending = baseFingering.reversed().drop(1)
        return baseFingering + descending
    }
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val notePlayer: NotePlayer,
    private val pitchDetector: PitchDetector,
    private val progressRepository: ProgressRepository,
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
    private val pianoBuddyManager: PianoBuddyBleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var listeningJob: kotlinx.coroutines.Job? = null
    private var lastVirtualKeyPressTime: Long = 0L
    private val SUPPRESSION_WINDOW_MS = 500L

    init {
        viewModelScope.launch {
            notePlayer.isLoaded.collect { loaded ->
                _uiState.update { it.copy(isAudioLoaded = loaded) }
            }
        }
        viewModelScope.launch {
            pianoBuddyManager.connectionState.collectLatest { state ->
                _uiState.update { it.copy(pianoBuddyConnectionState = state) }
            }
        }
    }

    private fun getMidiNoteForIndex(index: Int): Int {
        val notes = _uiState.value.getGuidedPracticeNotes()
        if (index < 0 || index >= notes.size) return -1

        var currentOctave = 4
        var lastNoteOrdinal = -1
        val ascendingSize = _uiState.value.generatedNotes.size

        for (i in 0..index) {
            val note = notes[i]
            if (i > 0) {
                if (i < ascendingSize) {
                    // Ascending portion
                    if (note.ordinal <= lastNoteOrdinal) {
                        currentOctave++
                    }
                } else {
                    // Descending portion
                    if (note.ordinal >= lastNoteOrdinal) {
                        currentOctave--
                    }
                }
            }
            lastNoteOrdinal = note.ordinal
            if (i == index) {
                return (currentOctave + 1) * 12 + note.ordinal
            }
        }
        return -1
    }

    private fun sendTargetNoteToPianoBuddy(midiNote: Int, frequency: Float = 0f) {
        if (_uiState.value.pianoBuddyConnectionState == BleConnectionState.CONNECTED) {
            pianoBuddyManager.sendTargetNote(midiNote, frequency)
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
        
        // Load video metadata
        val conceptId = "${rootNote.name.lowercase()}_${conceptType.name.lowercase()}"
        viewModelScope.launch {
            videoRepository.getVideoMetadata(conceptId).collect { metadata ->
                _uiState.update { it.copy(videoMetadata = metadata) }
            }
        }

        viewModelScope.launch {
            videoRepository.refreshMetadata()
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

    fun toggleAscendingDescendingMode() {
        _uiState.update { it.copy(isAscendingDescendingMode = !it.isAscendingDescendingMode) }
    }

    fun playSequence() {
        if (_uiState.value.isPlaying || _uiState.value.isListening || !_uiState.value.isAudioLoaded) return

        val guidedNotes = _uiState.value.getGuidedPracticeNotes()
        if (guidedNotes.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            profileRepository.updateStreak()

            // We play the notes one by one to ensure correct octave handling for descending mode
            guidedNotes.forEachIndexed { index, note ->
                if (!_uiState.value.isPlaying) return@forEachIndexed

                val midiNote = getMidiNoteForIndex(index)
                val octave = (midiNote / 12) - 1

                lastVirtualKeyPressTime = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        currentPlayingNote = note,
                        currentPlayingIndex = index,
                        currentPlayingOctave = octave
                    )
                }

                sendTargetNoteToPianoBuddy(midiNote, 0f)
                notePlayer.playNote(note, octave)

                kotlinx.coroutines.delay(500)
            }

            _uiState.update { it.copy(isPlaying = false, currentPlayingNote = null, currentPlayingIndex = -1) }
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListeningWithPermission() {
        _uiState.update { it.copy(pendingActionAfterPermission = null) }
        startListening()
    }

    private fun startListening() {
        if (_uiState.value.isPlaying) return
        
        // Ensure previous job is cancelled before starting a new one
        listeningJob?.cancel()
        
        _uiState.update { 
            it.copy(
                isListening = true, 
                detectedNote = null, 
                detectedFrequency = 0f,
                isStablePitch = false,
                inputVolume = 0f
            ) 
        }
        listeningJob = viewModelScope.launch {
            pitchDetector.startListening { result ->
                _uiState.update {
                    it.copy(
                        detectedFrequency = result.frequency,
                        inputVolume = result.amplitude,
                        detectedMidi = result.midi,
                        detectedOctave = result.octave,
                        detectionConfidence = result.confidence,
                        detectionTimestamp = result.timestamp
                    )
                }
                if (result.isStable && result.note != null) {
                    evaluateNote(result.note, result.isStable)
                } else if (!result.isStable) {
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

        // Track key press time for microphone suppression (to ignore app audio)
        lastVirtualKeyPressTime = System.currentTimeMillis()

        // 1. Play audio
        notePlayer.playNote(note)
        
        // 2. Briefly show as detected note for visual feedback
        // Note: We do NOT call evaluateNote here to ensure Reference Keyboard
        // interactions do not advance lessons or mark progress.
        _uiState.update { it.copy(detectedNote = note, isStablePitch = true) }
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _uiState.update { 
                if (it.detectedNote == note) it.copy(detectedNote = null, isStablePitch = false)
                else it
            }
        }
    }

    private fun evaluateNote(note: Note, isStable: Boolean) {
        // Suppress microphone input if it's too close to a Reference Keyboard press or app audio
        if (System.currentTimeMillis() - lastVirtualKeyPressTime < SUPPRESSION_WINDOW_MS) {
            return
        }

        var guidedNote: Int? = null

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
                            val guidedNotes = currentState.getGuidedPracticeNotes()
                            val guidedFingering = currentState.getGuidedPracticeFingering()

                            val nextIndex = currentState.guidedPractice.currentIndex + 1
                            val isCompleted = nextIndex >= guidedNotes.size

                            newGuidedPractice = currentState.guidedPractice.copy(
                                currentIndex = nextIndex,
                                targetNote = if (isCompleted) null else guidedNotes.getOrNull(nextIndex),
                                targetFinger = if (isCompleted) null else guidedFingering.getOrNull(nextIndex),
                                completedNotes = currentState.guidedPractice.completedNotes + currentState.guidedPractice.currentIndex,
                                lessonCompleted = isCompleted,
                                lastResult = PracticeResult.Correct(guidedFingering.getOrNull(currentState.guidedPractice.currentIndex)),
                                lastEvaluatedNote = note
                            )

                            // Also mark as completed in regular practice set for consistent visual progress
                            if (currentState.generatedNotes.contains(note)) {
                                newCompletedNotes = currentState.completedNotes + note
                            }

                            if (isCompleted) {
                                onLessonCompleted(currentState.rootNote, currentState.conceptType)
                            } else {
                                val current = getMidiNoteForIndex(nextIndex)
                                guidedNote = current
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

        guidedNote?.let { n -> sendTargetNoteToPianoBuddy(n) }
    }

    fun startGuidedPractice() {
        _uiState.update { 
            val guidedNotes = it.getGuidedPracticeNotes()
            if (guidedNotes.isEmpty()) return@update it
            val guidedFingering = it.getGuidedPracticeFingering()
            it.copy(
                guidedPractice = GuidedPracticeState(
                    isRunning = true,
                    currentIndex = 0,
                    targetNote = guidedNotes[0],
                    targetFinger = guidedFingering.getOrNull(0),
                    completedNotes = emptySet(),
                    lessonCompleted = false,
                    lastResult = null,
                    lastEvaluatedNote = null
                )
            )
        }
        val currentMidi = getMidiNoteForIndex(0)
        sendTargetNoteToPianoBuddy(currentMidi)
    }

    fun startGuidedPracticeWithPermission() {
        _uiState.update { it.copy(pendingActionAfterPermission = null) }
        startGuidedPractice()
        startListening()
    }

    fun setPendingAction(action: () -> Unit) {
        _uiState.update { it.copy(pendingActionAfterPermission = action) }
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
        val currentIndex = _uiState.value.guidedPractice.currentIndex
        val midiNote = getMidiNoteForIndex(currentIndex)

        if (midiNote != -1) {
            val target = _uiState.value.guidedPractice.targetNote ?: return
            val octave = (midiNote / 12) - 1

            lastVirtualKeyPressTime = System.currentTimeMillis()
            notePlayer.playNote(target, octave)
            sendTargetNoteToPianoBuddy(midiNote)
        }
    }

    private fun onLessonCompleted(rootNote: Note, conceptType: ConceptType) {
        stopListening() // Stop microphone when lesson finishes to ensure clean state for retry
        val wasAlreadyCompleted = _uiState.value.isLessonAlreadyCompleted
        viewModelScope.launch {
            progressRepository.saveProgress(rootNote, conceptType, true)
            profileRepository.updateStreak()
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

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        pitchDetector.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    override fun onCleared() {
        super.onCleared()
        pitchDetector.stopListening()
    }
}
