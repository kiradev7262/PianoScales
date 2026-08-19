package com.pianoscales.learnmusic.theory.playground

import com.pianoscales.learnmusic.theory.Note

enum class ChordQuality {
    MAJOR, MINOR, DIMINISHED, MAJOR7, MINOR7, DOMINANT7
}

data class PlaygroundChord(
    val root: Note,
    val quality: ChordQuality,
    val displayName: String,
    val romanNumeral: String,
    val notes: List<Note> = emptyList()
)

enum class ProgressionStyle {
    CHILL, POP, EMOTIONAL, UNEXPECTED, TENSION, CLASSIC
}

data class PlaygroundProgression(
    val chords: List<PlaygroundChord>,
    val rootNote: Note,
    val isMajor: Boolean,
    val style: ProgressionStyle
)

enum class RelationshipStrength {
    VERY_CLOSE, CLOSE, RELATED, DISTANT
}

data class ChordRelationship(
    val chord: PlaygroundChord,
    val strength: RelationshipStrength,
    val description: String
)
