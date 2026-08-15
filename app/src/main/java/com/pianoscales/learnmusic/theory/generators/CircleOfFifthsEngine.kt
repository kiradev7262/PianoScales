package com.pianoscales.learnmusic.theory.generators

import com.pianoscales.learnmusic.theory.Note

data class CircleKey(
    val note: Note,
    val displayName: String,
    val accidentalCount: Int,
    val isSharp: Boolean,
    val relativeMinor: Note,
    val relativeMinorDisplayName: String
)

enum class RelationshipType {
    NONE, SISTER, COUSIN, POWER
}

object CircleOfFifthsEngine {

    val fullCircle = listOf(
        CircleKey(Note.C, "C", 0, true, Note.A, "A"),
        CircleKey(Note.G, "G", 1, true, Note.E, "E"),
        CircleKey(Note.D, "D", 2, true, Note.B, "B"),
        CircleKey(Note.A, "A", 3, true, Note.F_SHARP, "F#"),
        CircleKey(Note.E, "E", 4, true, Note.C_SHARP, "C#"),
        CircleKey(Note.B, "B", 5, true, Note.G_SHARP, "G#"),
        CircleKey(Note.F_SHARP, "F#", 6, true, Note.D_SHARP, "D#"),
        CircleKey(Note.C_SHARP, "Db", 5, false, Note.A_SHARP, "Bb"),
        CircleKey(Note.G_SHARP, "Ab", 4, false, Note.F, "F"),
        CircleKey(Note.D_SHARP, "Eb", 3, false, Note.C, "C"),
        CircleKey(Note.A_SHARP, "Bb", 2, false, Note.G, "G"),
        CircleKey(Note.F, "F", 1, false, Note.D, "D")
    )

    fun getSharpsCount(note: Note): Int {
        return fullCircle.find { it.note == note && it.isSharp }?.accidentalCount ?: 0
    }

    fun getFlatsCount(note: Note): Int {
        return fullCircle.find { it.note == note && !it.isSharp }?.accidentalCount ?: 0
    }
    
    fun getNoteName(note: Note, context: CircleKey): String {
        if (context.isSharp) return note.displayName
        
        return when (note) {
            Note.C_SHARP -> "Db"
            Note.D_SHARP -> "Eb"
            Note.F_SHARP -> "Gb"
            Note.G_SHARP -> "Ab"
            Note.A_SHARP -> "Bb"
            else -> note.displayName
        }
    }

    fun getChordFamilyRelationship(selectedKey: CircleKey, targetKey: CircleKey): RelationshipType {
        val selectedIndex = fullCircle.indexOf(selectedKey)
        val targetIndex = fullCircle.indexOf(targetKey)
        if (selectedIndex == -1 || targetIndex == -1) return RelationshipType.NONE
        
        val clockwiseDistance = (targetIndex - selectedIndex + 12) % 12
        
        return when (clockwiseDistance) {
            0, 1, 11 -> RelationshipType.SISTER
            2, 3, 4 -> RelationshipType.COUSIN
            else -> RelationshipType.NONE
        }
    }

    fun isPowerNote(selectedKey: CircleKey, targetKey: CircleKey): Boolean {
        val selectedIndex = fullCircle.indexOf(selectedKey)
        val targetIndex = fullCircle.indexOf(targetKey)
        if (selectedIndex == -1 || targetIndex == -1) return false
        
        val clockwiseDistance = (targetIndex - selectedIndex + 12) % 12
        
        return clockwiseDistance in 0..4
    }
}
