package com.pianoscales.learnmusic.theory

enum class ConceptType(
    val displayName: String,
    val category: ConceptCategory
) {
    MAJOR_SCALE("Major Scale", ConceptCategory.SCALE),
    NATURAL_MINOR_SCALE("Natural Minor Scale", ConceptCategory.SCALE),
    MAJOR_CHORD("Major Chord", ConceptCategory.CHORD),
    MINOR_CHORD("Minor Chord", ConceptCategory.CHORD),
    MAJOR_ARPEGGIO("Major Arpeggio", ConceptCategory.ARPEGGIO),
    MINOR_ARPEGGIO("Minor Arpeggio", ConceptCategory.ARPEGGIO)
}

enum class ConceptCategory {
    SCALE, CHORD, ARPEGGIO
}
