package com.example.pianoscales.theory

data class TheoryExplanation(
    val title: String,
    val formula: String,
    val formulaMeaning: Map<String, String>,
    val constructionSteps: List<ConstructionStep>,
    val scaleDegrees: List<ScaleDegreeInfo>,
    val generalExplanation: String
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
