package com.example.pianoscales.theory.fingering

import com.example.pianoscales.theory.ConceptCategory
import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note

object FingeringEngine {

    fun generateFingering(
        notes: List<Note>,
        root: Note,
        type: ConceptType,
        hand: Hand
    ): FingeringGuide {
        val fingerNumbers = when (type.category) {
            ConceptCategory.SCALE -> getScaleFingering(root, type, hand, notes.size)
            ConceptCategory.CHORD -> getChordFingering(root, type, hand, notes.size)
            ConceptCategory.ARPEGGIO -> getArpeggioFingering(root, type, hand, notes.size)
        }

        val steps = notes.zip(fingerNumbers).map { (note, fingerNum) ->
            FingeringStep(note, FingerInfo.fromNumber(fingerNum))
        }

        return FingeringGuide(hand, steps)
    }

    private fun getScaleFingering(root: Note, type: ConceptType, hand: Hand, size: Int): List<Int> {
        // Standard beginner fingerings for Major and Natural Minor scales
        // Most white key scales follow these patterns
        return if (hand == Hand.RIGHT) {
            when (root) {
                Note.F -> listOf(1, 2, 3, 4, 1, 2, 3, 4).take(size)
                else -> listOf(1, 2, 3, 1, 2, 3, 4, 5).take(size)
            }
        } else {
            when (root) {
                Note.B -> listOf(4, 3, 2, 1, 4, 3, 2, 1).take(size)
                else -> listOf(5, 4, 3, 2, 1, 3, 2, 1).take(size)
            }
        }
    }

    private fun getChordFingering(root: Note, type: ConceptType, hand: Hand, size: Int): List<Int> {
        // Standard triad fingering: 1-3-5 for RH, 5-3-1 for LH
        return if (hand == Hand.RIGHT) {
            listOf(1, 3, 5).take(size)
        } else {
            listOf(5, 3, 1).take(size)
        }
    }

    private fun getArpeggioFingering(root: Note, type: ConceptType, hand: Hand, size: Int): List<Int> {
        // Standard arpeggio fingering: 1-2-3-5 for RH, 5-3-2-1 for LH
        return if (hand == Hand.RIGHT) {
            listOf(1, 2, 3, 5).take(size)
        } else {
            listOf(5, 3, 2, 1).take(size)
        }
    }
}
