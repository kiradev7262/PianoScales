package com.pianoscales.learnmusic.ui.songs

import com.pianoscales.learnmusic.theory.Note

data class BlackKeyInfo(
    val noteWithOctave: NoteWithOctave,
    val boundaryIndex: Int,
)

class SongKeyboardLayout private constructor(
    val startNoteWithOctave: NoteWithOctave,
    val whiteKeys: List<NoteWithOctave>,
    val blackKeys: List<BlackKeyInfo>,
) {
    val totalWhiteKeys: Int = whiteKeys.size

    fun getNoteRect(note: Note, octave: Int, totalWidthPx: Float): Pair<Float, Float>? {
        if (totalWidthPx <= 0f) return null
        val whiteKeyWidthPx = totalWidthPx / totalWhiteKeys.toFloat()
        val blackKeyWidthPx = whiteKeyWidthPx * 0.62f

        // Check if white key
        val whiteIndex = whiteKeys.indexOfFirst { (it.note == note) && (it.octave == octave) }
        if (whiteIndex != -1) {
            val xOffsetPx = whiteIndex * whiteKeyWidthPx
            return Pair(xOffsetPx, whiteKeyWidthPx)
        }

        // Check if black key
        val blackInfo = blackKeys.find { (it.noteWithOctave.note == note) && (it.noteWithOctave.octave == octave) }
        if (blackInfo != null) {
            val xOffsetPx = blackInfo.boundaryIndex * whiteKeyWidthPx - (blackKeyWidthPx / 2f)
            return Pair(xOffsetPx, blackKeyWidthPx)
        }

        return null
    }

    companion object {
        private val whiteNoteCycle = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)

        fun create(startNoteWithOctave: NoteWithOctave, numWhiteKeys: Int = 15): SongKeyboardLayout {
            // Ensure starting note is a white key
            val baseWhite = if (isWhiteKeyNote(startNoteWithOctave.note)) {
                startNoteWithOctave.note
            } else {
                when (startNoteWithOctave.note) {
                    Note.C_SHARP -> Note.C
                    Note.D_SHARP -> Note.D
                    Note.F_SHARP -> Note.F
                    Note.G_SHARP -> Note.G
                    Note.A_SHARP -> Note.A
                    else -> Note.C
                }
            }

            val startIdx = whiteNoteCycle.indexOf(baseWhite).coerceAtLeast(0)
            val whiteKeysList = mutableListOf<NoteWithOctave>()
            val blackKeysList = mutableListOf<BlackKeyInfo>()

            for (k in 0 until numWhiteKeys) {
                val cycleIdx = (startIdx + k) % 7
                val octaveOffset = (startIdx + k) / 7
                val currentOctave = startNoteWithOctave.octave + octaveOffset
                val currentNote = whiteNoteCycle[cycleIdx]

                val currentWhite = NoteWithOctave(currentNote, currentOctave)
                whiteKeysList.add(currentWhite)

                // Check for black key following this white key
                val sharpNote = when (currentNote) {
                    Note.C -> Note.C_SHARP
                    Note.D -> Note.D_SHARP
                    Note.F -> Note.F_SHARP
                    Note.G -> Note.G_SHARP
                    Note.A -> Note.A_SHARP
                    else -> null
                }

                if (sharpNote != null && (k + 1 < numWhiteKeys)) {
                    blackKeysList.add(
                        BlackKeyInfo(
                            noteWithOctave = NoteWithOctave(sharpNote, currentOctave),
                            boundaryIndex = k + 1,
                        )
                    )
                }
            }

            return SongKeyboardLayout(
                startNoteWithOctave = NoteWithOctave(baseWhite, startNoteWithOctave.octave),
                whiteKeys = whiteKeysList,
                blackKeys = blackKeysList,
            )
        }
    }
}

fun isWhiteKeyNote(note: Note): Boolean {
    return note == Note.C || note == Note.D || note == Note.E || note == Note.F ||
           note == Note.G || note == Note.A || note == Note.B
}

fun parseNoteWithOctaveStr(noteStr: String?): NoteWithOctave? {
    if (noteStr.isNullOrBlank() || noteStr.length < 2) return null
    val octave = noteStr.last().digitToIntOrNull() ?: return null
    val noteName = noteStr.dropLast(1)
    val note = Note.fromString(noteName) ?: return null
    return NoteWithOctave(note, octave)
}

/**
 * Resolves the starting white key for a song's 2-octave keyboard viewport.
 */
fun resolveStartingNote(song: Song): NoteWithOctave {
    val configured = parseNoteWithOctaveStr(song.startingNote)
    if (configured != null) {
        return if (isWhiteKeyNote(configured.note)) {
            configured
        } else {
            val baseWhite = when (configured.note) {
                Note.C_SHARP -> Note.C
                Note.D_SHARP -> Note.D
                Note.F_SHARP -> Note.F
                Note.G_SHARP -> Note.G
                Note.A_SHARP -> Note.A
                else -> configured.note
            }
            NoteWithOctave(baseWhite, configured.octave)
        }
    }

    // Fallback: Calculate from lowest note in song
    val allNotes = song.lines.flatMap { it.notes }
    if (allNotes.isEmpty()) return NoteWithOctave(Note.C, 4)

    val lowestNote = allNotes.minByOrNull { (it.octave + 1) * 12 + it.note.ordinal }
        ?: NoteWithOctave(Note.C, 4)

    val baseWhite = when (lowestNote.note) {
        Note.C_SHARP -> Note.C
        Note.D_SHARP -> Note.D
        Note.F_SHARP -> Note.F
        Note.G_SHARP -> Note.G
        Note.A_SHARP -> Note.A
        else -> lowestNote.note
    }

    // Shift down 1 white key for visual padding
    val whiteCycle = listOf(Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B)
    val idx = whiteCycle.indexOf(baseWhite)
    val shiftedIdx = if (idx > 0) idx - 1 else 6
    val shiftedOctave = if (idx > 0) lowestNote.octave else lowestNote.octave - 1

    return NoteWithOctave(whiteCycle[shiftedIdx], shiftedOctave.coerceAtLeast(1))
}
