package com.example.pianoscales.ui.practice

import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note

object ConceptVideoProvider {
    // Mappings for YouTube Short IDs and Descriptions
    private val mappings = mapOf(
        Note.C to mapOf(
            ConceptType.MAJOR_SCALE to ConceptWatchContent(
                videoUrl = "AZxvfLIxF2o",
                description = """
                    The C Major scale is the most fundamental scale in Western music. 
                    It contains no sharps or flats, making it unique and easy to learn.
                    
                    Notes:
                    C → D → E → F → G → A → B → C
                    
                    Key Characteristics:
                    • All white keys
                    • Clear, bright sound
                    • Foundation for music theory
                """.trimIndent()
            ),
            ConceptType.NATURAL_MINOR_SCALE to ConceptWatchContent(
                videoUrl = "PLACEHOLDER_1",
                description = "Learn the soulful C Natural Minor scale."
            ),
            ConceptType.MAJOR_CHORD to ConceptWatchContent(
                videoUrl = "UEAarNTWWHI",
                description = "\uD83C\uDFB9 Learn the C Major Chord in just 10 seconds!\n" +
                        "\n" +
                        "Notes:\n" +
                        "C • E • G\n" +
                        "\n" +
                        "Simply play these three notes together to create one of the most important chords in piano music.\n" +
                        "\n" +
                        "Perfect for beginners starting their piano journey!\n" +
                        "\n" +
                        "Follow for daily piano lessons, chords, and scales.\n" +
                        "\n" +
                        "#CMajorChord #LearnPiano #PianoChords #PianoLessons #PianoForBeginners #MusicTheory #Shorts\n"
            )


        ),
        Note.G to mapOf(
            ConceptType.MAJOR_SCALE to ConceptWatchContent(
                videoUrl = "z3WzYUH7Wg0",
                description = """
                    The G Major scale introduces your first sharp! 
                    It is widely used in folk and pop music.
                    
                    Notes:
                    G → A → B → C → D → E → F♯ → G
                    
                    Key Characteristics:
                    • Contains one sharp: F♯
                    • Warm and resonant quality
                """.trimIndent()
            )
        ),
        Note.D to mapOf(
            ConceptType.MAJOR_SCALE to ConceptWatchContent(
                videoUrl = "7YmK3l6N9gE",
                description = """
                    The D Major scale is a favorite for string players and pianist alike.
                    It has a triumphant and majestic feel.
                    
                    Notes:
                    D → E → F♯ → G → A → B → C♯ → D
                    
                    Key Characteristics:
                    • Contains two sharps: F♯ and C♯
                    • Brilliant and energetic tone
                """.trimIndent()
            )
        )
    )

    fun getContent(note: Note, conceptType: ConceptType): ConceptWatchContent? {
        return mappings[note]?.get(conceptType)
    }

    @Deprecated("Use getContent instead", ReplaceWith("getContent(note, conceptType)?.videoUrl"))
    fun getVideoId(note: Note, conceptType: ConceptType): String? {
        return getContent(note, conceptType)?.videoUrl
    }
}
