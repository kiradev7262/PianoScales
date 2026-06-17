package com.example.pianoscales.theory.fingering

import com.example.pianoscales.theory.Note

data class FingerInfo(
    val number: Int,
    val name: String
) {
    companion object {
        val THUMB = FingerInfo(1, "Thumb")
        val INDEX = FingerInfo(2, "Index")
        val MIDDLE = FingerInfo(3, "Middle")
        val RING = FingerInfo(4, "Ring")
        val PINKY = FingerInfo(5, "Pinky")

        fun fromNumber(number: Int): FingerInfo = when (number) {
            1 -> THUMB
            2 -> INDEX
            3 -> MIDDLE
            4 -> RING
            5 -> PINKY
            else -> THUMB
        }
        
        val ALL_FINGERS = listOf(THUMB, INDEX, MIDDLE, RING, PINKY)
    }
}

data class FingeringStep(
    val note: Note,
    val finger: FingerInfo
)

data class FingeringGuide(
    val hand: Hand,
    val steps: List<FingeringStep>
)
