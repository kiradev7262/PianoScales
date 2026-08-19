package com.pianoscales.learnmusic.theory.generators

import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.playground.*
import kotlin.random.Random

object PlaygroundEngine {

    fun getSisterChords(root: Note, isMajor: Boolean): List<PlaygroundChord> {
        return if (isMajor) {
            getMajorDiatonicChords(root)
        } else {
            getMinorDiatonicChords(root)
        }
    }

    fun getCousinChords(root: Note, isMajor: Boolean): List<PlaygroundChord> {
        val cousins = mutableListOf<PlaygroundChord>()
        
        // Parallel Major/Minor
        if (isMajor) {
            cousins.addAll(getMinorDiatonicChords(root).take(3)) // Parallel minor's i, iv, v or similar
        } else {
            cousins.addAll(getMajorDiatonicChords(root).take(3))
        }

        // Neighbors on Circle of Fifths
        val currentIndex = CircleOfFifthsEngine.fullCircle.indexOfFirst { it.note == root }
        if (currentIndex != -1) {
            val clockwise = CircleOfFifthsEngine.fullCircle[(currentIndex + 1) % 12]
            val counterClockwise = CircleOfFifthsEngine.fullCircle[(currentIndex + 11) % 12]
            
            cousins.add(createChord(clockwise.note, if (isMajor) ChordQuality.MAJOR else ChordQuality.MINOR, "V of V" ))
            cousins.add(createChord(counterClockwise.note, if (isMajor) ChordQuality.MAJOR else ChordQuality.MINOR, "IV of I"))
        }

        return cousins.distinctBy { "${it.root}${it.quality}" }
    }

    private fun getMajorDiatonicChords(root: Note): List<PlaygroundChord> {
        val scaleNotes = TheoryEngine.generateNotes(root, com.pianoscales.learnmusic.theory.ConceptType.MAJOR_SCALE)
        return listOf(
            createChord(scaleNotes[0], ChordQuality.MAJOR, "I"),
            createChord(scaleNotes[1], ChordQuality.MINOR, "ii"),
            createChord(scaleNotes[2], ChordQuality.MINOR, "iii"),
            createChord(scaleNotes[3], ChordQuality.MAJOR, "IV"),
            createChord(scaleNotes[4], ChordQuality.MAJOR, "V"),
            createChord(scaleNotes[5], ChordQuality.MINOR, "vi"),
            createChord(scaleNotes[6], ChordQuality.DIMINISHED, "vii°")
        )
    }

    private fun getMinorDiatonicChords(root: Note): List<PlaygroundChord> {
        val scaleNotes = TheoryEngine.generateNotes(root, com.pianoscales.learnmusic.theory.ConceptType.NATURAL_MINOR_SCALE)
        return listOf(
            createChord(scaleNotes[0], ChordQuality.MINOR, "i"),
            createChord(scaleNotes[1], ChordQuality.DIMINISHED, "ii°"),
            createChord(scaleNotes[2], ChordQuality.MAJOR, "III"),
            createChord(scaleNotes[3], ChordQuality.MINOR, "iv"),
            createChord(scaleNotes[4], ChordQuality.MINOR, "v"),
            createChord(scaleNotes[5], ChordQuality.MAJOR, "VI"),
            createChord(scaleNotes[6], ChordQuality.MAJOR, "VII")
        )
    }

    private fun createChord(root: Note, quality: ChordQuality, romanNumeral: String): PlaygroundChord {
        val displayName = when (quality) {
            ChordQuality.MAJOR -> root.displayName
            ChordQuality.MINOR -> "${root.displayName}m"
            ChordQuality.DIMINISHED -> "${root.displayName}dim"
            else -> root.displayName
        }
        
        val notes = when (quality) {
            ChordQuality.MAJOR -> TheoryEngine.generateNotes(root, com.pianoscales.learnmusic.theory.ConceptType.MAJOR_CHORD)
            ChordQuality.MINOR -> TheoryEngine.generateNotes(root, com.pianoscales.learnmusic.theory.ConceptType.MINOR_CHORD)
            ChordQuality.DIMINISHED -> listOf(root, Note.entries[(root.ordinal + 3) % 12], Note.entries[(root.ordinal + 6) % 12])
            else -> emptyList()
        }

        return PlaygroundChord(root, quality, displayName, romanNumeral, notes)
    }

    fun generateProgression(
        root: Note,
        isMajor: Boolean,
        style: ProgressionStyle,
        length: Int = 4
    ): PlaygroundProgression {
        val sisters = getSisterChords(root, isMajor)
        val cousins = getCousinChords(root, isMajor)
        
        val progressionChords = mutableListOf<PlaygroundChord>()
        
        when (style) {
            ProgressionStyle.POP -> {
                // Classic I-V-vi-IV or similar
                val patterns = if (isMajor) {
                    listOf(listOf(0, 4, 5, 3), listOf(0, 5, 3, 4), listOf(5, 3, 0, 4))
                } else {
                    listOf(listOf(0, 5, 2, 6), listOf(0, 6, 5, 4))
                }
                val pattern = patterns.random()
                pattern.forEach { progressionChords.add(sisters[it]) }
            }
            ProgressionStyle.CHILL -> {
                // I-vi-ii-V or I-IV-I-IV
                val patterns = if (isMajor) {
                    listOf(listOf(0, 5, 1, 4), listOf(0, 3, 0, 3), listOf(3, 4, 0, 5))
                } else {
                    listOf(listOf(0, 3, 0, 3), listOf(0, 2, 5, 6))
                }
                val pattern = patterns.random()
                pattern.forEach { progressionChords.add(sisters[it]) }
            }
            ProgressionStyle.EMOTIONAL -> {
                // vi-IV-I-V or i-VI-III-VII
                val patterns = if (isMajor) {
                    listOf(listOf(5, 3, 0, 4), listOf(5, 1, 3, 0))
                } else {
                    listOf(listOf(0, 5, 2, 6), listOf(5, 6, 0, 4))
                }
                val pattern = patterns.random()
                pattern.forEach { progressionChords.add(sisters[it]) }
            }
            ProgressionStyle.UNEXPECTED -> {
                // Mix in some cousins
                progressionChords.add(sisters[0]) // Start with Tonic
                for (i in 1 until length - 1) {
                    if (Random.nextBoolean()) {
                        progressionChords.add(sisters.random())
                    } else {
                        progressionChords.add(cousins.random())
                    }
                }
                progressionChords.add(sisters[4]) // End with Dominant or something stable
            }
            else -> {
                // Default random from sisters
                for (i in 0 until length) {
                    progressionChords.add(sisters.random())
                }
            }
        }

        return PlaygroundProgression(progressionChords, root, isMajor, style)
    }
}
