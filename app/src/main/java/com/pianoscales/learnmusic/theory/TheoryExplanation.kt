package com.pianoscales.learnmusic.theory

import com.pianoscales.learnmusic.theory.fingering.FingeringGuide

data class TheoryExplanation(
    val title: String,
    val formula: String,
    val formulaMeaning: Map<String, String>,
    val constructionSteps: List<ConstructionStep>,
    val scaleDegrees: List<ScaleDegreeInfo>,
    val generalExplanation: String,
    val fingeringGuides: List<FingeringGuide> = emptyList(),
    val fingeringExplanation: String = ""
)

data class ConstructionStep(
    val interval: String,
    val resultNote: Note
)

data class ScaleDegreeInfo(
    val note: Note,
    val degree: String,
    val name: String
)
