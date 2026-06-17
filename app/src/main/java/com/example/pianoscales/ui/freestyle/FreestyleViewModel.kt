package com.example.pianoscales.ui.freestyle

import androidx.lifecycle.ViewModel
import com.example.pianoscales.audio.playback.NotePlayer
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FreestyleViewModel @Inject constructor(
    private val notePlayer: NotePlayer
) : ViewModel() {

    fun playNote(note: Note, octave: Int) {
        notePlayer.playNote(note, octave)
    }
}
