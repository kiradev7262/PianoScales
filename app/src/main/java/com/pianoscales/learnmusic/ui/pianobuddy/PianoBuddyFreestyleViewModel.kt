package com.pianoscales.learnmusic.ui.pianobuddy

import androidx.lifecycle.ViewModel
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

    fun setInputMode(mode: FreestyleInputMode) {
        _inputMode.value = mode
    }

    fun onNotePlayed(note: Note, octave: Int, frequency: Float = 0f) {
        if (inputMode.value == FreestyleInputMode.VIRTUAL_PIANO) {
            // For virtual piano, we want to allow repeated taps of the same note.
            // Reset the last sent MIDI state before sending the new note.
            noteEventDispatcher.dispatchSilence()
        }
        noteEventDispatcher.dispatchNote(note, octave, frequency)
    }

    fun onSilence() {
        noteEventDispatcher.dispatchSilence()
    }
}
