package com.pianoscales.learnmusic.ui.freestyle

import androidx.lifecycle.ViewModel
import com.pianoscales.learnmusic.audio.playback.NotePlayer
import com.pianoscales.learnmusic.theory.Note
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
