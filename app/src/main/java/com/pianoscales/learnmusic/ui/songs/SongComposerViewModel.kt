package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.pitch.PitchDetector
import com.pianoscales.learnmusic.audio.pitch.PitchToNoteMapper
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.domain.songs.SongRepository
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class SongComposerUiState(
    val songId: String = UUID.randomUUID().toString(),
    val title: String = "",
    val pianoMode: PianoMode = PianoMode.VIRTUAL,
    val lines: List<List<NoteWithOctave>> = listOf(emptyList()),
    val currentLineIndex: Int = 0,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val showTitleInput: Boolean = true,
    val isListening: Boolean = false,
    val detectedNote: Note? = null,
    val detectedOctave: Int? = null,
    val detectedMidi: Int? = null,
    val detectedFrequency: Float = 0f,
    val confidence: Float = 0f,
    val timestamp: Long = 0L
) {
    val currentLine: List<NoteWithOctave> get() = lines.getOrElse(currentLineIndex) { emptyList() }
}

@HiltViewModel
class SongComposerViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val soundPoolManager: SoundPoolManager,
    private val pitchDetector: PitchDetector,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongComposerUiState())
    val uiState: StateFlow<SongComposerUiState> = _uiState.asStateFlow()

    private var pitchDetectionJob: kotlinx.coroutines.Job? = null
    private var recordingStartTime: Long = 0L

    init {
        val songId: String? = savedStateHandle["songId"]
        if (songId != null) {
            viewModelScope.launch {
                val song = songRepository.getSongById(songId)
                if (song != null) {
                    _uiState.update { 
                        it.copy(
                            songId = song.songId,
                            title = song.title,
                            lines = song.lines.map { line -> line.notes },
                            currentLineIndex = song.lines.size - 1,
                            isEditMode = true,
                            showTitleInput = false
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setPianoMode(mode: PianoMode) {
        _uiState.update { it.copy(pianoMode = mode) }
    }

    fun startComposer() {
        if (_uiState.value.title.isBlank()) return
        _uiState.update { it.copy(showTitleInput = false) }
        recordingStartTime = System.currentTimeMillis()
        if (_uiState.value.pianoMode == PianoMode.EXTERNAL) {
            startListening()
        }
    }

    fun onNotePlayed(note: Note, octave: Int) {
        if (_uiState.value.pianoMode == PianoMode.EXTERNAL) return
        recordNote(note, octave)
        soundPoolManager.playNote(note, octave)
    }

    private fun recordNote(note: Note, octave: Int) {
        val timestamp = System.currentTimeMillis() - recordingStartTime
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            val currentLine = newLines[state.currentLineIndex].toMutableList()
            currentLine.add(NoteWithOctave(note, octave, timestamp))
            newLines[state.currentLineIndex] = currentLine
            state.copy(lines = newLines)
        }
    }

    fun deleteLastNote() {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            val currentLine = newLines[state.currentLineIndex].toMutableList()
            if (currentLine.isNotEmpty()) {
                currentLine.removeAt(currentLine.size - 1)
                newLines[state.currentLineIndex] = currentLine
                state.copy(lines = newLines)
            } else {
                state
            }
        }
    }

    fun clearLine() {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            newLines[state.currentLineIndex] = emptyList()
            state.copy(lines = newLines)
        }
    }

    fun deleteLine() {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            if (newLines.size > 1) {
                newLines.removeAt(state.currentLineIndex)
                val newIndex = if (state.currentLineIndex >= newLines.size) {
                    newLines.size - 1
                } else {
                    state.currentLineIndex
                }
                state.copy(lines = newLines, currentLineIndex = newIndex)
            } else {
                // If it's the only line, just clear it
                newLines[0] = emptyList()
                state.copy(lines = newLines, currentLineIndex = 0)
            }
        }
    }

    fun setCurrentLineIndex(index: Int) {
        if (index in _uiState.value.lines.indices) {
            _uiState.update { it.copy(currentLineIndex = index) }
        }
    }

    fun undo() {
        deleteLastNote()
    }

    fun addLine() {
        _uiState.update { state ->
            val newLines = state.lines.toMutableList()
            newLines.add(emptyList())
            state.copy(lines = newLines, currentLineIndex = newLines.size - 1)
        }
    }

    fun finish(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank()) return
        
        // Filter out empty lines
        val finalLines = state.lines.filter { it.isNotEmpty() }.map { SongLine(it) }
        if (finalLines.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val song = Song(
                songId = state.songId,
                title = state.title,
                description = "Custom song created on ${java.text.DateFormat.getDateInstance().format(now)}",
                difficulty = "Custom",
                version = 1,
                lines = finalLines,
                builtIn = false,
                createdAt = if (state.isEditMode) 0L else now,
                modifiedAt = now
            )
            songRepository.saveSong(song)
            onSuccess()
        }
    }

    private fun startListening() {
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
                if (result.isStable && result.note != null) {
                    val detectedNoteWithOctave = PitchToNoteMapper.mapFrequencyToNoteWithOctave(result.frequency)
                    if (detectedNoteWithOctave != null) {
                        recordNote(detectedNoteWithOctave.note, detectedNoteWithOctave.octave)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pitchDetectionJob?.cancel()
        pitchDetector.stopListening()
    }
}
