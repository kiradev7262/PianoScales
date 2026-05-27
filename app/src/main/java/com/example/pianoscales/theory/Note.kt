package com.example.pianoscales.theory

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

    companion object {
        fun fromString(name: String): Note? = entries.find { it.name == name || it.displayName == name }
    }
}
