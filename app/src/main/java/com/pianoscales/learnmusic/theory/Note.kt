package com.pianoscales.learnmusic.theory

enum class Note(val displayName: String) {
    C("C"),
    C_SHARP("C#"),
    D("D"),
    D_SHARP("D#"),
    E("E"),
    F("F"),
    F_SHARP("F#"),
    G("G"),
    G_SHARP("G#"),
    A("A"),
    A_SHARP("A#"),
    B("B");

    fun getFilePart(): String {
        return when (this) {
            C -> "c"
            C_SHARP -> "cs"
            D -> "d"
            D_SHARP -> "ds"
            E -> "e"
            F -> "f"
            F_SHARP -> "fs"
            G -> "g"
            G_SHARP -> "gs"
            A -> "a"
            A_SHARP -> "as"
            B -> "b"
        }
    }

    companion object {
        fun fromString(name: String): Note? = entries.find { it.name == name || it.displayName == name }
    }
}
