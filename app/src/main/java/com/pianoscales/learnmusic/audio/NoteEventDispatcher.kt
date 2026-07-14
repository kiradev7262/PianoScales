package com.pianoscales.learnmusic.audio

import com.pianoscales.learnmusic.theory.Note
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteEventDispatcher @Inject constructor() {
    private val _noteEvents = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val noteEvents = _noteEvents.asSharedFlow()

    fun dispatchNote(note: Note, octave: Int) {
        val midiNote = (octave + 1) * 12 + note.ordinal
        _noteEvents.tryEmit(midiNote)
    }

    fun dispatchMidiNote(midiNote: Int) {
        _noteEvents.tryEmit(midiNote)
    }
}
