package com.example.pianoscales.audio.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.pianoscales.R
import com.example.pianoscales.theory.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPoolManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var loadedCount = 0
    private var totalSounds = 12
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val noteToSoundId = mutableMapOf<Note, Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedCount++
                Log.d("SoundPool", "Loaded sound $sampleId: $loadedCount/$totalSounds")
                if (loadedCount >= totalSounds) {
                    scope.launch {
                        delay(200) // Small delay to ensure SoundPool is ready
                        _isLoaded.value = true
                        Log.d("SoundPool", "All sounds loaded successfully")
                    }
                }
            } else {
                Log.e("SoundPool", "Error loading sound $sampleId: status $status")
            }
        }
        loadSounds()
    }

    private fun loadSounds() {
        val notes = Note.entries
        totalSounds = notes.size
        notes.forEach { note ->
            val resId = getResIdForNote(note)
            if (resId != 0) {
                val soundId = soundPool.load(context, resId, 1)
                noteToSoundId[note] = soundId
            } else {
                Log.w("SoundPool", "Resource for $note not found")
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
        if (!_isLoaded.value) {
            Log.w("SoundPool", "Playback attempted before sounds loaded for note: $note")
            return
        }

        val soundId = noteToSoundId[note]
        if (soundId != null && soundId != 0) {
            Log.d("SoundPool", "Playing note: $note (soundId: $soundId)")
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            Log.e("SoundPool", "Invalid soundId for note: $note")
        }
    }

    fun release() {
        soundPool.release()
    }
}
