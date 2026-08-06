package com.pianoscales.learnmusic.ui.songs

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.audio.pitch.PitchDetector
import com.pianoscales.learnmusic.audio.pitch.PitchToNoteMapper
import com.pianoscales.learnmusic.ble.BleConnectionState
import com.pianoscales.learnmusic.ble.PianoBuddyBleManager
import com.pianoscales.learnmusic.domain.settings.SettingsRepository
import com.pianoscales.learnmusic.domain.songs.SongRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongCoachUiState(
    val song: Song? = null,
    val currentLineIndex: Int = 0,
    val currentNoteIndex: Int = 0,
    val isCompleted: Boolean = false,
    val pianoMode: PianoMode = PianoMode.VIRTUAL,
    val isListening: Boolean = false,
    val isDemoPlaying: Boolean = false,
    val detectedNote: Note? = null,
    val detectedOctave: Int? = null,
    val detectedMidi: Int? = null,
    val detectedFrequency: Float = 0f,
    val confidence: Float = 0f,
    val timestamp: Long = 0L,
    val pianoBuddyConnectionState: BleConnectionState = BleConnectionState.IDLE
) {
    val currentLine: SongLine? get() = song?.lines?.getOrNull(currentLineIndex)
    val currentNote: NoteWithOctave? get() = currentLine?.notes?.getOrNull(currentNoteIndex)
}

@HiltViewModel
class SongCoachViewModel @Inject constructor(
    private val soundPoolManager: SoundPoolManager,
    private val songRepository: SongRepository,
    private val pitchDetector: PitchDetector,
    private val settingsRepository: SettingsRepository,
    private val pianoBuddyManager: PianoBuddyBleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongCoachUiState())
    val uiState: StateFlow<SongCoachUiState> = _uiState.asStateFlow()

    private var pitchDetectionJob: Job? = null
    private var demoJob: Job? = null

    private enum class InputState {
        WAITING_FOR_PRESS,
        WAITING_FOR_RELEASE
    }

    private var inputState = InputState.WAITING_FOR_PRESS
    private var lastMatchedMidi: Int? = null
    private var silenceCounter = 0
    private val REQUIRED_SILENCE_FRAMES = 3 // ~60-100ms at typical detection rates

    init {
        val songId: String? = savedStateHandle["songId"]
        if (songId != null) {
            viewModelScope.launch {
                combine(
                    songRepository.getSongs(),
                    songRepository.getCustomSongs()
                ) { builtIn, custom ->
                    builtIn + custom
                }.collect { allSongs ->
                    val song = allSongs.find { it.songId == songId }
                    if (song != null) {
                        _uiState.update { it.copy(song = song) }
                        sendTargetNoteToPianoBuddy()
                    }
                }
            }
        }

        viewModelScope.launch {
            pianoBuddyManager.connectionState.collect { connectionState ->
                _uiState.update { it.copy(pianoBuddyConnectionState = connectionState) }
                if (connectionState == BleConnectionState.CONNECTED) {
                    sendTargetNoteToPianoBuddy()
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.getPianoMode().collect { mode ->
                _uiState.update { it.copy(pianoMode = mode) }
                if (mode == PianoMode.EXTERNAL) {
                    startListening()
                } else {
                    stopListening()
                }
            }
        }
    }

    fun startListening() {
        if (_uiState.value.isListening || _uiState.value.isDemoPlaying) return
        
        _uiState.update { it.copy(isListening = true) }
        pitchDetectionJob?.cancel()
        pitchDetectionJob = viewModelScope.launch {
            pitchDetector.startListening { result ->
                _uiState.update { 
                    it.copy(
                        detectedNote = result.note,
                        detectedOctave = result.octave,
                        detectedMidi = result.midi,
                        detectedFrequency = result.frequency,
                        confidence = result.confidence,
                        timestamp = result.timestamp
                    )
                }

                // Songs Press-Release State Machine (External Piano Only)
                val isActuallyPlaying = result.isStable && result.note != null && result.amplitude > 0.005f

                if (isActuallyPlaying) {
                    silenceCounter = 0
                    val detectedNoteWithOctave = PitchToNoteMapper.mapFrequencyToNoteWithOctave(result.frequency)
                    if (detectedNoteWithOctave != null) {
                        evaluateNote(detectedNoteWithOctave.note, detectedNoteWithOctave.octave)
                    }
                } else {
                    if (_uiState.value.pianoMode == PianoMode.EXTERNAL) {
                        silenceCounter++
                        if (silenceCounter >= REQUIRED_SILENCE_FRAMES) {
                            if (inputState == InputState.WAITING_FOR_RELEASE) {
                                Log.d("SongCoach", "Release confirmed after $silenceCounter frames of silence. Transitioning to WAITING_FOR_PRESS")
                            }
                            inputState = InputState.WAITING_FOR_PRESS
                            lastMatchedMidi = null
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        pitchDetectionJob?.cancel()
        pitchDetectionJob = null
        pitchDetector.stopListening()
        inputState = InputState.WAITING_FOR_PRESS
        lastMatchedMidi = null
        silenceCounter = 0
        _uiState.update { it.copy(isListening = false) }
    }

    fun onNotePlayed(note: Note, octave: Int) {
        if (_uiState.value.pianoMode == PianoMode.EXTERNAL || _uiState.value.isDemoPlaying) return

        soundPoolManager.playNote(note, octave)
        evaluateNote(note, octave)
    }

    private fun evaluateNote(note: Note, octave: Int) {
        val state = _uiState.value
        if (state.isCompleted || state.song == null || state.isDemoPlaying) return

        if (state.pianoMode == PianoMode.EXTERNAL) {
            val expected = state.currentNote ?: return
            val detectedMidi = (octave + 1) * 12 + note.ordinal
            
            Log.d("SongCoach", "Evaluate: Index ${state.currentNoteIndex}, Expected: ${expected.note}${expected.octave}, Detected: $note$octave, State: $inputState, LastMatched: $lastMatchedMidi")

            // State machine check
            if (inputState == InputState.WAITING_FOR_RELEASE) {
                if (detectedMidi == lastMatchedMidi) {
                    // Still holding the previous note, ignore
                    return
                } else {
                    // Different note detected - allow "legato" transition for different notes
                    Log.d("SongCoach", "Different note detected ($note$octave). Assuming release of $lastMatchedMidi. Transitioning to WAITING_FOR_PRESS")
                    inputState = InputState.WAITING_FOR_PRESS
                    // Continue to check if this new note matches the current expected note
                }
            }

            if (note == expected.note && octave == expected.octave) {
                Log.d("SongCoach_BLE", "Match Detected: $note$octave. Current Index: ${state.currentNoteIndex}")
                advance()
                inputState = InputState.WAITING_FOR_RELEASE
                lastMatchedMidi = detectedMidi
            }
        } else {
            // Virtual piano - legacy behavior (no release required)
            val expected = state.currentNote ?: return
            if (note == expected.note && octave == expected.octave) {
                advance()
            }
        }
    }

    private fun sendTargetNoteToPianoBuddy(forceBlink: Boolean = false) {
        val state = _uiState.value
        if (state.pianoBuddyConnectionState != BleConnectionState.CONNECTED) return

        val current = state.currentNote ?: return
        val currentMidi = (current.octave + 1) * 12 + current.note.ordinal
        
        if (forceBlink) {
            pianoBuddyManager.sendTargetNoteWithBlink(currentMidi, state.detectedFrequency)
        } else {
            pianoBuddyManager.sendTargetNote(currentMidi, state.detectedFrequency)
        }
    }

    private fun advance() {
        val previousNote = _uiState.value.currentNote
        val previousMidi = previousNote?.let { (it.octave + 1) * 12 + it.note.ordinal }

        _uiState.update { state ->
            val song = state.song ?: return@update state
            val currentLine = state.currentLine ?: return@update state
            
            val nextNoteIndex = state.currentNoteIndex + 1
            if (nextNoteIndex < currentLine.notes.size) {
                state.copy(currentNoteIndex = nextNoteIndex)
            } else {
                val nextLineIndex = state.currentLineIndex + 1
                if (nextLineIndex < song.lines.size) {
                    state.copy(currentLineIndex = nextLineIndex, currentNoteIndex = 0)
                } else {
                    state.copy(isCompleted = true)
                }
            }
        }

        val state = _uiState.value
        val currentNote = state.currentNote
        val currentMidi = currentNote?.let { (it.octave + 1) * 12 + it.note.ordinal }
        
        Log.d("SongCoach_BLE", "Song Index After Advance: ${state.currentNoteIndex}")
        Log.d("SongCoach_BLE", "Previous Expected Note: ${previousNote?.note}${previousNote?.octave} (MIDI: $previousMidi)")
        Log.d("SongCoach_BLE", "Current Expected Note After Advance: ${currentNote?.note}${currentNote?.octave} (MIDI: $currentMidi)")
        
        if (currentNote != null) {
            val isRepeated = previousMidi == currentMidi
            Log.d("SongCoach_BLE", "Repeated Note Detected: $isRepeated")
            
            val shouldBlink = state.pianoMode == PianoMode.EXTERNAL && 
                             state.pianoBuddyConnectionState == BleConnectionState.CONNECTED && 
                             isRepeated
                             
            sendTargetNoteToPianoBuddy(forceBlink = shouldBlink)
        } else {
            sendTargetNoteToPianoBuddy()
        }
    }

    fun toggleDemo() {
        if (_uiState.value.isDemoPlaying) {
            stopDemo()
        } else {
            startDemo()
        }
    }

    private fun startDemo() {
        val song = _uiState.value.song ?: return
        
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            val wasListening = _uiState.value.isListening
            val previousLineIndex = _uiState.value.currentLineIndex
            val previousNoteIndex = _uiState.value.currentNoteIndex
            val previousCompleted = _uiState.value.isCompleted
            
            stopListening()
            
            _uiState.update { it.copy(isDemoPlaying = true, currentLineIndex = 0, currentNoteIndex = 0, isCompleted = false) }
            
            try {
                var lastTimestamp: Long? = null
                
                song.lines.forEachIndexed { lineIndex, line ->
                    line.notes.forEachIndexed { noteIndex, noteWithOctave ->
                        val currentTimestamp = noteWithOctave.timestamp
                        val lastTs = lastTimestamp
                        if (lastTs != null && currentTimestamp != null) {
                            val delayVal = (currentTimestamp - lastTs).coerceAtLeast(0)
                            delay(delayVal)
                        } else if (lineIndex > 0 || noteIndex > 0) {
                            // Legacy fixed interval
                            val delayVal = if (noteIndex == 0) 650L else 450L
                            delay(delayVal)
                        }

                        _uiState.update { it.copy(currentLineIndex = lineIndex, currentNoteIndex = noteIndex) }
                        soundPoolManager.playNote(noteWithOctave.note, noteWithOctave.octave)
                        lastTimestamp = currentTimestamp
                    }
                }
            } finally {
                _uiState.update { 
                    it.copy(
                        isDemoPlaying = false, 
                        currentLineIndex = previousLineIndex, 
                        currentNoteIndex = previousNoteIndex,
                        isCompleted = previousCompleted
                    ) 
                }
                sendTargetNoteToPianoBuddy()
                if (wasListening || _uiState.value.pianoMode == PianoMode.EXTERNAL) {
                    startListening()
                }
            }
        }
    }

    private fun stopDemo() {
        demoJob?.cancel()
    }

    fun reset() {
        if (_uiState.value.isDemoPlaying) stopDemo()
        _uiState.update { it.copy(currentLineIndex = 0, currentNoteIndex = 0, isCompleted = false) }
        inputState = InputState.WAITING_FOR_PRESS
        lastMatchedMidi = null
        silenceCounter = 0
        sendTargetNoteToPianoBuddy()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
