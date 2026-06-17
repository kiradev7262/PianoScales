package com.pianoscales.learnmusic.audio.pitch

import com.pianoscales.learnmusic.theory.Note
import kotlin.math.log2
import kotlin.math.roundToInt

object PitchToNoteMapper {
    /**
     * Standard frequency for A4 is 440Hz.
     * The formula to find the number of semitones from A4 is:
     * n = 12 * log2(fn / 440)
     */
    fun mapFrequencyToNote(frequency: Float): Note? {
        if (frequency <= 0) return null

        val semitonesFromA4 = 12 * log2(frequency / 440.0)
        val roundedSemitones = semitonesFromA4.roundToInt()
        
        // Note.A is ordinal 9 (C=0, C#=1, D=2, D#=3, E=4, F=5, F#=6, G=7, G#=8, A=9, A#=10, B=11)
        // We add 9 to the rounded semitones to get the absolute index relative to C
        var noteIndex = (roundedSemitones + 9) % 12
        if (noteIndex < 0) noteIndex += 12

        return Note.entries.getOrNull(noteIndex)
    }
}
