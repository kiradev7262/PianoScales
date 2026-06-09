package com.example.pianoscales.audio_intelligence.ear_training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.audio.playback.SoundPoolManager
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EarTrainingViewModel @Inject constructor(
    private val soundPoolManager: SoundPoolManager
) : ViewModel() {

    private val _targetNote = MutableStateFlow<Note?>(null)
    val targetNote = _targetNote.asStateFlow()

    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote = _selectedNote.asStateFlow()

    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect = _isCorrect.asStateFlow()

    fun playNewNote() {
        _selectedNote.value = null
        _isCorrect.value = null
        val randomNote = Note.entries.random()
        _targetNote.value = randomNote
        soundPoolManager.playNote(randomNote, 4)
    }

    fun repeatNote() {
        _targetNote.value?.let {
            soundPoolManager.playNote(it, 4)
        }
    }

    fun onNoteSelected(note: Note) {
        if (_selectedNote.value != null) return // Already answered
        
        _selectedNote.value = note
        _isCorrect.value = (note == _targetNote.value)
        
        if (note != _targetNote.value) {
            // Optionally play the correct note after a short delay if user was wrong?
            // For now just show feedback
        }
    }
    
    // Interval Training logic can also go here or in a separate ViewModel
    private val _intervalTarget = MutableStateFlow<Pair<Note, Note>?>(null)
    val intervalTarget = _intervalTarget.asStateFlow()
    
    fun playNewInterval() {
        _selectedNote.value = null
        _isCorrect.value = null
        
        val rootNote = Note.entries.random()
        val intervals = listOf(1, 2, 3, 4, 7) // m2, M2, m3, M3, P5
        val interval = intervals.random()
        val targetIndex = (Note.entries.indexOf(rootNote) + interval) % 12
        val targetNote = Note.entries[targetIndex]
        
        _intervalTarget.value = rootNote to targetNote
        
        viewModelScope.launch {
            soundPoolManager.playNote(rootNote, 4)
            delay(800)
            soundPoolManager.playNote(targetNote, 4)
        }
    }
    
    fun checkInterval(intervalName: String) {
        val target = _intervalTarget.value ?: return
        val rootIndex = Note.entries.indexOf(target.first)
        val targetIndex = Note.entries.indexOf(target.second)
        val diff = (targetIndex - rootIndex + 12) % 12
        
        val correctInterval = when(diff) {
            1 -> "Minor 2nd"
            2 -> "Major 2nd"
            3 -> "Minor 3rd"
            4 -> "Major 3rd"
            7 -> "Perfect 5th"
            else -> "Other"
        }
        
        _isCorrect.value = (intervalName == correctInterval)
    }
}
