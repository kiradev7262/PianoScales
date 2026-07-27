package com.pianoscales.learnmusic.ui.pianobuddy

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.BuildConfig
import com.pianoscales.learnmusic.audio.NoteEventDispatcher
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class FreestyleInputMode {
    VIRTUAL_PIANO,
    EXTERNAL_PIANO
}

@HiltViewModel
class PianoBuddyFreestyleViewModel @Inject constructor(
    private val noteEventDispatcher: NoteEventDispatcher
) : ViewModel() {
    private val _inputMode = MutableStateFlow(FreestyleInputMode.VIRTUAL_PIANO)
    val inputMode = _inputMode.asStateFlow()

    private val _activePointers = mutableMapOf<Long, Int>() // pointerId -> midiNote

    fun setInputMode(mode: FreestyleInputMode) {
        _inputMode.value = mode
        _activePointers.clear()
        noteEventDispatcher.dispatchActiveNotes(emptySet())
    }

    fun onNotePlayed(note: Note, octave: Int, frequency: Float = 0f) {
        if (inputMode.value == FreestyleInputMode.VIRTUAL_PIANO) {
            // For virtual piano, we want to allow repeated taps of the same note.
            // Reset the last sent MIDI state before sending the new note.
            noteEventDispatcher.dispatchSilence()
        }
        noteEventDispatcher.dispatchNote(note, octave, frequency)
    }

    fun onNoteDown(note: Note, octave: Int, pointerId: Long) {
        if (inputMode.value != FreestyleInputMode.VIRTUAL_PIANO) return
        
        val midiNote = (octave + 1) * 12 + note.ordinal
        _activePointers[pointerId] = midiNote
        
        if (BuildConfig.DEBUG) {
            Log.d("PianoBuddy", "Finger Down | Added: ${note.displayName}$octave | Pointer: $pointerId | All: ${getPressedNoteNames()}")
        }
        dispatchActiveNotes()
    }

    fun onNoteUp(pointerId: Long) {
        if (inputMode.value != FreestyleInputMode.VIRTUAL_PIANO) return

        val removedMidi = _activePointers.remove(pointerId)
        
        if (removedMidi != null && BuildConfig.DEBUG) {
            val note = Note.entries[(removedMidi % 12 + 12) % 12]
            val octave = (removedMidi / 12) - 1
            Log.d("PianoBuddy", "Finger Up | Removed: ${note.displayName}$octave | Pointer: $pointerId | All: ${getPressedNoteNames()}")
        }
        dispatchActiveNotes()
    }

    private fun getPressedNoteNames(): List<String> {
        return _activePointers.values.distinct().map { midi ->
            val note = Note.entries[(midi % 12 + 12) % 12]
            val octave = (midi / 12) - 1
            "${note.displayName}$octave"
        }
    }

    private fun dispatchActiveNotes() {
        noteEventDispatcher.dispatchActiveNotes(_activePointers.values.toSet())
    }

    fun onSilence() {
        noteEventDispatcher.dispatchSilence()
    }
}
