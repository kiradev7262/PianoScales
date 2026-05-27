package com.example.pianoscales.audio.playback

import com.example.pianoscales.theory.Note
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotePlayer @Inject constructor(
    private val soundPoolManager: SoundPoolManager
) {
    suspend fun playSequence(
        notes: List<Note>,
        delayMs: Long = 500L,
        onNoteStarted: (Note) -> Unit = {}
    ) {
        notes.forEach { note ->
            onNoteStarted(note)
            soundPoolManager.playNote(note)
            delay(delayMs)
        }
    }
}
