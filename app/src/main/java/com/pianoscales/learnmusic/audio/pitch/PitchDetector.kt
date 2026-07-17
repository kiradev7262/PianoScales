package com.pianoscales.learnmusic.audio.pitch

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import be.tarsos.dsp.pitch.Yin
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.songs.NoteWithOctave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class PitchDetector @Inject constructor() {

    data class DetectionResult(
        val note: Note?,
        val midi: Int?,
        val octave: Int?,
        val frequency: Float,
        val amplitude: Float,
        val isStable: Boolean,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "PitchDetector"
        private const val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 1024
        
        // UX Tuning Thresholds
        private const val MIN_AMPLITUDE_THRESHOLD = 0.005f // Filter out background noise
        private const val MIN_PROBABILITY_THRESHOLD = 0.85f // Slightly lowered from 0.90 for better responsiveness
        private const val MIN_STABLE_FRAMES = 2 // Reduced from 4 for much faster response (~92ms)
        private const val NOTE_HOLD_MS = 400L // Reduced from 800ms for more responsive UI
    }

    private val mutex = Mutex()
    private var audioRecord: AudioRecord? = null
    
    @Volatile
    private var isRunning = false

    // Stability filtering state
    private var consecutiveMidi: Int? = null
    private var consecutiveCount = 0
    private var lastStableNote: Note? = null
    private var lastStableMidi: Int? = null
    private var lastStableTime = 0L

    suspend fun startListening(
        onResult: (DetectionResult) -> Unit
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                // 1. Ensure any previous state is cleaned up
                isRunning = false
                cleanupResources()

                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize.coerceAtLeast(BUFFER_SIZE * 2)
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize")
                    return@withContext
                }

                audioRecord?.startRecording()
                isRunning = true

                val buffer = ShortArray(BUFFER_SIZE)
                val floatBuffer = FloatArray(BUFFER_SIZE)
                val yin = Yin(SAMPLE_RATE.toFloat(), BUFFER_SIZE)

                // Reset state
                resetFilters()

                Log.d(TAG, "Pitch detection loop started")

                while (isRunning && isActive) {
                    // Use a local reference to avoid race conditions if audioRecord is nulled elsewhere
                    val currentRecord = audioRecord 
                    val read = currentRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    
                    if (read > 0 && isRunning && isActive) {
                        // 1. Calculate Amplitude (RMS)
                        var sum = 0.0
                        for (i in 0 until read) {
                            floatBuffer[i] = buffer[i].toFloat() / Short.MAX_VALUE
                            sum += floatBuffer[i] * floatBuffer[i]
                        }
                        val rms = sqrt(sum / read).toFloat()
                        
                        // 2. Detect Pitch using TarsosDSP YIN
                        val result = yin.getPitch(floatBuffer)
                        val frequency = result.pitch
                        val probability = result.probability
                        
                        // 3. Apply Filtering Logic
                        val filteredMidi = processPitch(frequency, probability, rms)
                        
                        // 4. Determine Stability for UI
                        val isStable = filteredMidi != null && 
                                      filteredMidi == lastStableMidi && 
                                      consecutiveCount >= MIN_STABLE_FRAMES

                        val midi = PitchToNoteMapper.frequencyToMidi(frequency)
                        val octave = midi?.let { (it / 12) - 1 }
                        val note = midi?.let { Note.entries.getOrNull(it % 12) }
                        
                        onResult(DetectionResult(
                            note = note, // Report note even if not stable for real-time UI feedback
                            midi = midi,
                            octave = octave,
                            frequency = frequency,
                            amplitude = rms,
                            isStable = isStable,
                            confidence = probability
                        ))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in listening loop: ${e.message}")
            } finally {
                Log.d(TAG, "Pitch detection loop exiting, cleaning up...")
                isRunning = false
                cleanupResources()
            }
        }
    }

    private fun cleanupResources() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null
    }

    private fun processPitch(frequency: Float, probability: Float, amplitude: Float): Int? {
        val currentTime = System.currentTimeMillis()
        
        // A. Amplitude Threshold - Ignore silent/low noise frames
        if (amplitude < MIN_AMPLITUDE_THRESHOLD) {
            return checkHoldMidi(currentTime)
        }

        // B. Confidence Filter - Ignore unpitched noise or low confidence detections
        if (probability < MIN_PROBABILITY_THRESHOLD || frequency <= 0) {
            return checkHoldMidi(currentTime)
        }

        val currentMidi = PitchToNoteMapper.frequencyToMidi(frequency)
        
        // C. Stability Filter - Require N consecutive frames of same MIDI note (includes octave)
        if (currentMidi != null && currentMidi == consecutiveMidi) {
            consecutiveCount++
        } else {
            consecutiveMidi = currentMidi
            consecutiveCount = 1
        }

        if (consecutiveCount >= MIN_STABLE_FRAMES && currentMidi != null) {
            if (currentMidi != lastStableMidi) {
                val currentNote = Note.entries.getOrNull(currentMidi % 12)
                val octave = (currentMidi / 12) - 1
                Log.d(TAG, "Frequency=${String.format(Locale.US, "%.2f", frequency)}Hz Note=${currentNote?.displayName} Octave=$octave Midi=$currentMidi Timestamp=$currentTime")
            }
            lastStableMidi = currentMidi
            lastStableNote = Note.entries.getOrNull(currentMidi % 12)
            lastStableTime = currentTime
            return currentMidi
        }

        // D. Note Hold - Keep showing the last stable note briefly
        return checkHoldMidi(currentTime)
    }

    private fun checkHoldMidi(currentTime: Long): Int? {
        if (lastStableMidi != null && (currentTime - lastStableTime) < NOTE_HOLD_MS) {
            return lastStableMidi
        }
        
        // If hold expired, clear everything
        if (lastStableMidi != null) {
            resetFilters()
        }
        return null
    }

    fun resetFilters() {
        consecutiveCount = 0
        consecutiveMidi = null
        lastStableMidi = null
        lastStableNote = null
        lastStableTime = 0L
    }

    fun stopListening() {
        isRunning = false
        // Request stop immediately to unblock any pending read()
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error calling stop on AudioRecord: ${e.message}")
        }
    }
}
