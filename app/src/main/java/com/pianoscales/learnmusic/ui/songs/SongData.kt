package com.pianoscales.learnmusic.ui.songs

import com.pianoscales.learnmusic.theory.Note

data class Song(
    val songId: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val version: Int,
    val lines: List<SongLine>
)

data class SongLine(
    val notes: List<NoteWithOctave>
)

data class NoteWithOctave(
    val note: Note,
    val octave: Int
)

val HappyBirthday = Song(
    songId = "happy_birthday",
    title = "Happy Birthday",
    description = "Learn Happy Birthday one note at a time.",
    difficulty = "Beginner",
    version = 1,
    lines = listOf(
        SongLine(listOf(
            NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 4), NoteWithOctave(Note.A, 4), 
            NoteWithOctave(Note.G, 4), NoteWithOctave(Note.C, 5), NoteWithOctave(Note.B, 4)
        )),
        SongLine(listOf(
            NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 4), NoteWithOctave(Note.A, 4), 
            NoteWithOctave(Note.G, 4), NoteWithOctave(Note.D, 5), NoteWithOctave(Note.C, 5)
        )),
        SongLine(listOf(
            NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 4), NoteWithOctave(Note.G, 5), 
            NoteWithOctave(Note.E, 5), NoteWithOctave(Note.C, 5), NoteWithOctave(Note.B, 4), NoteWithOctave(Note.A, 4)
        )),
        SongLine(listOf(
            NoteWithOctave(Note.F, 5), NoteWithOctave(Note.F, 5), NoteWithOctave(Note.E, 5), 
            NoteWithOctave(Note.C, 5), NoteWithOctave(Note.D, 5), NoteWithOctave(Note.C, 5)
        ))
    )
)
