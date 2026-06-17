package com.example.pianoscales.ui.practice

import com.example.pianoscales.theory.Note
import com.example.pianoscales.theory.fingering.FingerInfo

data class GuidedPracticeState(
    val isRunning: Boolean = false,
    val currentIndex: Int = 0,
    val targetNote: Note? = null,
    val targetFinger: FingerInfo? = null,
    val completedNotes: Set<Int> = emptySet(), // Store indices of completed notes
    val lessonCompleted: Boolean = false,
    val lastResult: PracticeResult? = null,
    val lastEvaluatedNote: Note? = null // Prevents duplicate evaluation of the same note detection
)

sealed class PracticeResult {
    data class Correct(val expectedFinger: FingerInfo?) : PracticeResult()
    data class Incorrect(
        val expected: Note,
        val detected: Note
    ) : PracticeResult()
}
