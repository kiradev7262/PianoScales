package com.pianoscales.learnmusic.theory

import com.pianoscales.learnmusic.theory.generators.PlaygroundEngine
import com.pianoscales.learnmusic.theory.playground.ChordQuality
import com.pianoscales.learnmusic.theory.playground.ProgressionStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaygroundEngineTest {

    @Test
    fun `test sister chords for C Major`() {
        val sisters = PlaygroundEngine.getSisterChords(Note.C, true)
        assertEquals(7, sisters.size)
        assertEquals(Note.C, sisters[0].root)
        assertEquals(ChordQuality.MAJOR, sisters[0].quality) // I
        assertEquals(Note.D, sisters[1].root)
        assertEquals(ChordQuality.MINOR, sisters[1].quality) // ii
        assertEquals(Note.G, sisters[4].root)
        assertEquals(ChordQuality.MAJOR, sisters[4].quality) // V
    }

    @Test
    fun `test sister chords for A Minor`() {
        val sisters = PlaygroundEngine.getSisterChords(Note.A, false)
        assertEquals(7, sisters.size)
        assertEquals(Note.A, sisters[0].root)
        assertEquals(ChordQuality.MINOR, sisters[0].quality) // i
        assertEquals(Note.C, sisters[2].root)
        assertEquals(ChordQuality.MAJOR, sisters[2].quality) // III
        assertEquals(Note.E, sisters[4].root)
        assertEquals(ChordQuality.MINOR, sisters[4].quality) // v
    }

    @Test
    fun `test cousin chords exist`() {
        val cousins = PlaygroundEngine.getCousinChords(Note.C, true)
        assertTrue(cousins.isNotEmpty())
    }

    @Test
    fun `test progression generation for C Major Pop style`() {
        val progression = PlaygroundEngine.generateProgression(Note.C, true, ProgressionStyle.POP)
        assertEquals(4, progression.chords.size)
        assertTrue(progression.chords.all { chord -> 
            val isSister = PlaygroundEngine.getSisterChords(Note.C, true).any { it.root == chord.root && it.quality == chord.quality }
            val isCousin = PlaygroundEngine.getCousinChords(Note.C, true).any { it.root == chord.root && it.quality == chord.quality }
            isSister || isCousin
        })
    }
}
