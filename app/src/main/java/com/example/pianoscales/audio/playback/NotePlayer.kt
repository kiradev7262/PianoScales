package com.example.pianoscales.audio.playback

import com.example.pianoscales.theory.Note
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotePlayer @Inject constructor(
    private val soundPoolManager: SoundPoolManager
) {
    val isLoaded = soundPoolManager.isLoaded

    suspend fun playSequence(
        notes: List<Note>,
        delayMs: Long = 500L,
        onNoteStarted: (Note) -> Unit = {}
    ) {
        var currentOctave = 4
        var lastNoteOrdinal = -1

        notes.forEachIndexed { index, note ->
            if (index > 0 && note.ordinal <= lastNoteOrdinal) {
                currentOctave++
            }
            lastNoteOrdinal = note.ordinal

            // [PLAYBACK DEBUG]
            val noteFilePart = note.getFilePart()
            android.util.Log.d("PLAYBACK DEBUG", """
                [PLAYBACK DEBUG]
                Note: ${note.displayName}
                Octave: $currentOctave
                File: $noteFilePart$currentOctave.ogg
                Index: ${index + 1}
            """.trimIndent())

            onNoteStarted(note)
            playNote(note, currentOctave)
            delay(delayMs)
        }
    }

    fun playNote(note: Note, octave: Int = 4) {
        soundPoolManager.playNote(note, octave)
    }
}
