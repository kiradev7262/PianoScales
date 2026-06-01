package com.example.pianoscales.theory.generators

import com.example.pianoscales.theory.*

object TheoryEngine {

    fun generateNotes(root: Note, type: ConceptType, includeOctave: Boolean = false): List<Note> {
        return when (type) {
            ConceptType.MAJOR_SCALE -> {
                val intervals = listOf(2, 2, 1, 2, 2, 2, 1)
                generateScale(root, intervals, includeOctave)
            }
            ConceptType.NATURAL_MINOR_SCALE -> {
                val intervals = listOf(2, 1, 2, 2, 1, 2, 2)
                generateScale(root, intervals, includeOctave)
            }
            ConceptType.MAJOR_CHORD -> generateChord(root, listOf(0, 4, 7))
            ConceptType.MINOR_CHORD -> generateChord(root, listOf(0, 3, 7))
            ConceptType.MAJOR_ARPEGGIO -> generateChord(root, listOf(0, 4, 7, 12))
            ConceptType.MINOR_ARPEGGIO -> generateChord(root, listOf(0, 3, 7, 12))
        }
    }

    private fun generateScale(root: Note, intervals: List<Int>, includeOctave: Boolean): List<Note> {
        val notes = mutableListOf<Note>()
        var currentIndex = root.ordinal
        notes.add(Note.entries[currentIndex])

        val intervalsToUse = if (includeOctave) intervals else intervals.dropLast(1)
        
        for (interval in intervalsToUse) {
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

    fun generateTheory(root: Note, type: ConceptType, includeOctave: Boolean): TheoryExplanation {
        val notes = generateNotes(root, type, includeOctave)
        
        return when (type.category) {
            ConceptCategory.SCALE -> generateScaleTheory(root, type, notes, includeOctave)
            ConceptCategory.CHORD -> generateChordTheory(root, type, notes)
            ConceptCategory.ARPEGGIO -> generateArpeggioTheory(root, type, notes)
        }
    }

    private fun generateScaleTheory(root: Note, type: ConceptType, notes: List<Note>, includeOctave: Boolean): TheoryExplanation {
        val intervals = when (type) {
            ConceptType.MAJOR_SCALE -> listOf(2, 2, 1, 2, 2, 2, 1)
            ConceptType.NATURAL_MINOR_SCALE -> listOf(2, 1, 2, 2, 1, 2, 2)
            else -> emptyList()
        }

        val formula = intervals.joinToString(" ") { if (it == 2) "W" else "H" }
        val formulaMeaning = mapOf("W" to "Whole Step (2 Semitones)", "H" to "Half Step (1 Semitone)")
        
        val constructionSteps = mutableListOf<ConstructionStep>()
        var currentNote = root
        val intervalsToUse = if (includeOctave) intervals else intervals.dropLast(1)
        
        intervalsToUse.forEach { interval ->
            val nextNoteIndex = (currentNote.ordinal + interval) % Note.entries.size
            val nextNote = Note.entries[nextNoteIndex]
            constructionSteps.add(ConstructionStep(if (interval == 2) "W" else "H", nextNote))
            currentNote = nextNote
        }

        val degreeNames = listOf("Tonic", "Supertonic", "Mediant", "Subdominant", "Dominant", "Submediant", "Leading Tone", "Octave")
        val scaleDegrees = notes.mapIndexed { index, note ->
            ScaleDegreeInfo(note, (index + 1).toString(), degreeNames.getOrElse(index) { "" })
        }

        return TheoryExplanation(
            title = "${root.displayName} ${type.displayName}",
            formula = formula,
            formulaMeaning = formulaMeaning,
            constructionSteps = constructionSteps,
            scaleDegrees = scaleDegrees,
            generalExplanation = if (type == ConceptType.MAJOR_SCALE) {
                "The Major Scale is the foundation of Western music, known for its bright and happy sound."
            } else {
                "The Natural Minor Scale has a darker, more somber character compared to the Major Scale."
            }
        )
    }

    private fun generateChordTheory(root: Note, type: ConceptType, notes: List<Note>): TheoryExplanation {
        val isMajor = type == ConceptType.MAJOR_CHORD
        val formula = if (isMajor) "1 - 3 - 5" else "1 - ♭3 - 5"
        val formulaMeaning = mapOf(
            "1" to "Root",
            if (isMajor) "3" to "Major Third" else "♭3" to "Minor Third",
            "5" to "Perfect Fifth"
        )

        val constructionSteps = listOf(
            ConstructionStep("Root", notes[0]),
            ConstructionStep(if (isMajor) "Major Third" else "Minor Third", notes[1]),
            ConstructionStep("Perfect Fifth", notes[2])
        )

        val degreeNames = listOf("Root", if (isMajor) "Major Third" else "Minor Third", "Perfect Fifth")
        val scaleDegrees = notes.mapIndexed { index, note ->
            ScaleDegreeInfo(note, if (index == 0) "1" else if (index == 1) (if (isMajor) "3" else "♭3") else "5", degreeNames[index])
        }

        return TheoryExplanation(
            title = "${root.displayName} ${type.displayName}",
            formula = formula,
            formulaMeaning = formulaMeaning,
            constructionSteps = constructionSteps,
            scaleDegrees = scaleDegrees,
            generalExplanation = if (isMajor) {
                "Major chords contain a major third interval, creating a bright and stable sound."
            } else {
                "Minor chords contain a minor third interval, creating a darker, more emotional sound."
            }
        )
    }

    private fun generateArpeggioTheory(root: Note, type: ConceptType, notes: List<Note>): TheoryExplanation {
        val isMajor = type == ConceptType.MAJOR_ARPEGGIO
        val formula = if (isMajor) "1 - 3 - 5 - 8" else "1 - ♭3 - 5 - 8"
        val formulaMeaning = mapOf(
            "1" to "Root",
            if (isMajor) "3" to "Major Third" else "♭3" to "Minor Third",
            "5" to "Perfect Fifth",
            "8" to "Octave"
        )

        val constructionSteps = listOf(
            ConstructionStep("Root", notes[0]),
            ConstructionStep(if (isMajor) "Major Third" else "Minor Third", notes[1]),
            ConstructionStep("Perfect Fifth", notes[2]),
            ConstructionStep("Octave", notes[3])
        )

        val degreeNames = listOf("Root", if (isMajor) "Major Third" else "Minor Third", "Perfect Fifth", "Octave")
        val scaleDegrees = notes.mapIndexed { index, note ->
            val degree = when (index) {
                0 -> "1"
                1 -> if (isMajor) "3" else "♭3"
                2 -> "5"
                else -> "8"
            }
            ScaleDegreeInfo(note, degree, degreeNames[index])
        }

        return TheoryExplanation(
            title = "${root.displayName} ${type.displayName}",
            formula = formula,
            formulaMeaning = formulaMeaning,
            constructionSteps = constructionSteps,
            scaleDegrees = scaleDegrees,
            generalExplanation = "An arpeggio is a chord played one note at a time rather than simultaneously."
        )
    }
}
