package com.pianoscales.learnmusic.audio

import com.pianoscales.learnmusic.theory.Note
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NoteEvent(
    val midiNote: Int,
    val frequency: Float = 0f
)

@Singleton
class NoteEventDispatcher @Inject constructor() {
    private val _noteEvents = MutableSharedFlow<NoteEvent>(extraBufferCapacity = 10)
    val noteEvents = _noteEvents.asSharedFlow()

    fun dispatchNote(note: Note, octave: Int, frequency: Float = 0f) {
        val midiNote = (octave + 1) * 12 + note.ordinal
        _noteEvents.tryEmit(NoteEvent(midiNote, frequency))
    }

    fun dispatchMidiNote(midiNote: Int, frequency: Float = 0f) {
        _noteEvents.tryEmit(NoteEvent(midiNote, frequency))
    }

    fun dispatchSilence() {
        _noteEvents.tryEmit(NoteEvent(-1, 0f))
    }
}
