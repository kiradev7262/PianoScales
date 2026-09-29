package com.pianoscales.learnmusic.ui.songs

import com.pianoscales.learnmusic.theory.Note
import org.junit.Assert.assertEquals
import org.junit.Test

class SongDataTest {

    @Test
    fun testNestedSongLinesFlattenToSingleOrderedSequence() {
        val song = Song(
            songId = "test_song",
            title = "Test Song",
            description = "Description",
            difficulty = "Beginner",
            version = 1,
            lines = listOf(
                SongLine(listOf(
                    NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 4), NoteWithOctave(Note.A, 4)
                )),
                SongLine(listOf(
                    NoteWithOctave(Note.G, 4), NoteWithOctave(Note.C, 5), NoteWithOctave(Note.B, 4)
                ))
            )
        )

        val flattened = song.notes

        assertEquals(6, flattened.size)
        assertEquals(NoteWithOctave(Note.G, 4), flattened[0])
        assertEquals(NoteWithOctave(Note.G, 4), flattened[1])
        assertEquals(NoteWithOctave(Note.A, 4), flattened[2])
        assertEquals(NoteWithOctave(Note.G, 4), flattened[3])
        assertEquals(NoteWithOctave(Note.C, 5), flattened[4])
        assertEquals(NoteWithOctave(Note.B, 4), flattened[5])
    }

    @Test
    fun testFlatSongFormatProducesSameSequence() {
        val song = Song(
            songId = "test_song_flat",
            title = "Test Song Flat",
            description = "Description",
            difficulty = "Beginner",
            version = 1,
            lines = listOf(
                SongLine(listOf(
                    NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 4), NoteWithOctave(Note.A, 4),
                    NoteWithOctave(Note.G, 4), NoteWithOctave(Note.C, 5), NoteWithOctave(Note.B, 4)
                ))
            )
        )

        val flattened = song.notes

        assertEquals(6, flattened.size)
        assertEquals(NoteWithOctave(Note.G, 4), flattened[0])
        assertEquals(NoteWithOctave(Note.C, 5), flattened[4])
    }
}
