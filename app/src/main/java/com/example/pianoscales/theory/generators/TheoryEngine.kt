package com.example.pianoscales.theory.generators

import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note

object TheoryEngine {

    fun generateNotes(root: Note, type: ConceptType): List<Note> {
        return when (type) {
            ConceptType.MAJOR_SCALE -> generateScale(root, listOf(2, 2, 1, 2, 2, 2, 1))
            ConceptType.NATURAL_MINOR_SCALE -> generateScale(root, listOf(2, 1, 2, 2, 1, 2, 2))
            ConceptType.MAJOR_CHORD -> generateChord(root, listOf(0, 4, 7))
            ConceptType.MINOR_CHORD -> generateChord(root, listOf(0, 3, 7))
            ConceptType.MAJOR_ARPEGGIO -> generateChord(root, listOf(0, 4, 7, 12))
            ConceptType.MINOR_ARPEGGIO -> generateChord(root, listOf(0, 3, 7, 12))
        }
    }

    private fun generateScale(root: Note, intervals: List<Int>): List<Note> {
        val notes = mutableListOf<Note>()
        var currentIndex = root.ordinal
        notes.add(Note.entries[currentIndex])
        
        // We take the first 7 notes for a standard scale (intervals.size is usually 7)
        for (interval in intervals.dropLast(1)) {
            currentIndex = (currentIndex + interval) % Note.entries.size
            notes.add(Note.entries[currentIndex])
        }
        return notes
    }

    private fun generateChord(root: Note, semitones: List<Int>): List<Note> {
        return semitones.map { semitone ->
            val index = (root.ordinal + semitone) % Note.entries.size
            Note.entries[index]
        }
    }
}
