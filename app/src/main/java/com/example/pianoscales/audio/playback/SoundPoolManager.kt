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
    private var totalSounds = 0
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10) // Increased for multi-octave support
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val keyToSoundId = mutableMapOf<Pair<Note, Int>, Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedCount++
                if (loadedCount >= totalSounds) {
                    scope.launch {
                        delay(200)
                        _isLoaded.value = true
                        Log.d("SoundPool", "All $totalSounds sounds loaded successfully")
                    }
                }
            }
        }
        loadSounds()
    }

    private fun loadSounds() {
        val notes = Note.entries
        val octaves = listOf(3, 4, 5, 6, 7) // Full range C3 - C7
        totalSounds = notes.size * octaves.size
        
        octaves.forEach { octave ->
            notes.forEach { note ->
                val resId = getResIdForNote(note, octave)
                if (resId != 0) {
                    val soundId = soundPool.load(context, resId, 1)
                    keyToSoundId[note to octave] = soundId
                } else {
                    Log.w("SoundPool", "Resource for $note octave $octave not found")
                    totalSounds--
                }
            }
        }
    }

    private fun getResIdForNote(note: Note, octave: Int): Int {
        val notePart = when (note) {
            Note.C -> "c"
            Note.C_SHARP -> "cs"
            Note.D -> "d"
            Note.D_SHARP -> "ds"
            Note.E -> "e"
            Note.F -> "f"
            Note.F_SHARP -> "fs"
            Note.G -> "g"
            Note.G_SHARP -> "gs"
            Note.A -> "a"
            Note.A_SHARP -> "as"
            Note.B -> "b"
        }
        val name = "$notePart$octave"
        return context.resources.getIdentifier(name, "raw", context.packageName)
    }

    fun playNote(note: Note, octave: Int = 4) {
        if (!_isLoaded.value) {
            Log.w("SoundPool", "Playback attempted before sounds loaded: $note $octave")
            return
        }

        val soundId = keyToSoundId[note to octave]
        
        // [PIANO DEBUG - MULTI OCTAVE]
        val resId = getResIdForNote(note, octave)
        val resName = if (resId != 0) context.resources.getResourceEntryName(resId) else "unknown"
        
        Log.d("PIANO DEBUG", """
            [PIANO DEBUG - MULTI OCTAVE]
            Key: ${note.displayName}$octave
            Note: ${note.displayName}
            Octave: $octave
            Mapped File: $resName.ogg
            Resource ID: R.raw.$resName
        """.trimIndent())

        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            Log.e("SoundPool", "Invalid soundId for note: $note octave: $octave")
        }
    }

    fun release() {
        soundPool.release()
    }
}
