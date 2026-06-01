package com.example.pianoscales.ui.practice

import com.example.pianoscales.theory.Note

data class GuidedPracticeState(
    val isRunning: Boolean = false,
    val currentIndex: Int = 0,
    val targetNote: Note? = null,
    val completedNotes: Set<Int> = emptySet(), // Store indices of completed notes
    val lessonCompleted: Boolean = false,
    val lastResult: PracticeResult? = null,
    val lastEvaluatedNote: Note? = null // Prevents duplicate evaluation of the same note detection
)

sealed class PracticeResult {
    data object Correct : PracticeResult()
    data class Incorrect(
        val expected: Note,
        val detected: Note
    ) : PracticeResult()
}
