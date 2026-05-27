package com.example.pianoscales.audio.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.pianoscales.R
import com.example.pianoscales.theory.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPoolManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val noteToSoundId = mutableMapOf<Note, Int>()

    init {
        loadSounds()
    }

    private fun loadSounds() {
        Note.entries.forEach { note ->
            val resId = getResIdForNote(note)
            if (resId != 0) {
                val soundId = soundPool.load(context, resId, 1)
                noteToSoundId[note] = soundId
            }
        }
    }

    private fun getResIdForNote(note: Note): Int {
        val name = when (note) {
            Note.C -> "c4"
            Note.C_SHARP -> "cs4"
            Note.D -> "d4"
            Note.D_SHARP -> "ds4"
            Note.E -> "e4"
            Note.F -> "f4"
            Note.F_SHARP -> "fs4"
            Note.G -> "g4"
            Note.G_SHARP -> "gs4"
            Note.A -> "a4"
            Note.A_SHARP -> "as4"
            Note.B -> "b4"
        }
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    fun playNote(note: Note) {
        val soundId = noteToSoundId[note]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
