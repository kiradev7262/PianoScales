package com.pianoscales.learnmusic.audio.pitch

import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.songs.NoteWithOctave
import kotlin.math.log2
import kotlin.math.roundToInt

object PitchToNoteMapper {
    /**
     * Standard frequency for A4 is 440Hz.
     * The formula to find the number of semitones from A4 is:
     * n = 12 * log2(fn / 440)
     */
    fun mapFrequencyToNote(frequency: Float): Note? {
        val midiNote = frequencyToMidi(frequency) ?: return null
        val noteIndex = midiNote % 12
        return Note.entries.getOrNull(noteIndex)
    }

    fun mapFrequencyToNoteWithOctave(frequency: Float): NoteWithOctave? {
        val midiNote = frequencyToMidi(frequency) ?: return null
        val noteIndex = midiNote % 12
        val octave = (midiNote / 12) - 1
        val note = Note.entries.getOrNull(noteIndex) ?: return null
        return NoteWithOctave(note, octave)
    }

    private fun frequencyToMidi(frequency: Float): Int? {
        if (frequency <= 0) return null
        val semitonesFromA4 = 12 * log2(frequency / 440.0)
        return (semitonesFromA4 + 69).roundToInt()
    }
}
