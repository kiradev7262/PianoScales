package com.pianoscales.learnmusic.audio.pitch

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import be.tarsos.dsp.pitch.Yin
import com.pianoscales.learnmusic.theory.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class PitchDetector @Inject constructor() {

    companion object {
        private const val TAG = "PitchDetector"
        private const val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 1024
        
        // UX Tuning Thresholds
        private const val MIN_AMPLITUDE_THRESHOLD = 0.005f // Filter out background noise
        private const val MIN_PROBABILITY_THRESHOLD = 0.90f // Only confident pitches
        private const val MIN_STABLE_FRAMES = 4 // Must be consistent for ~180ms
        private const val NOTE_HOLD_MS = 800L // Keep note on screen after sound stops
    }

    private val mutex = Mutex()
    private var audioRecord: AudioRecord? = null
    
    @Volatile
    private var isRunning = false

    // Stability filtering state
    private var consecutiveNote: Note? = null
    private var consecutiveCount = 0
    private var lastStableNote: Note? = null
    private var lastStableTime = 0L

    suspend fun startListening(
        onResult: (Note?, Float, Float, Boolean) -> Unit
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
                        val filteredNote = processPitch(frequency, probability, rms)
                        
                        // 4. Determine Stability for UI
                        val isStable = filteredNote != null && 
                                      filteredNote == lastStableNote && 
                                      consecutiveCount >= MIN_STABLE_FRAMES
                        
                        onResult(filteredNote, frequency, rms, isStable)
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

    private fun processPitch(frequency: Float, probability: Float, amplitude: Float): Note? {
        val currentTime = System.currentTimeMillis()
        
        // A. Amplitude Threshold - Ignore silent/low noise frames
        if (amplitude < MIN_AMPLITUDE_THRESHOLD) {
            return checkHold(currentTime)
        }

        // B. Confidence Filter - Ignore unpitched noise or low confidence detections
        if (probability < MIN_PROBABILITY_THRESHOLD || frequency <= 0) {
            return checkHold(currentTime)
        }

        val currentNote = PitchToNoteMapper.mapFrequencyToNote(frequency)
        
        // C. Stability Filter - Require N consecutive frames of same note
        if (currentNote != null && currentNote == consecutiveNote) {
            consecutiveCount++
        } else {
            consecutiveNote = currentNote
            consecutiveCount = 1
        }

        if (consecutiveCount >= MIN_STABLE_FRAMES) {
            lastStableNote = currentNote
            lastStableTime = currentTime
            return currentNote
        }

        // D. Note Hold - Keep showing the last stable note briefly
        return checkHold(currentTime)
    }

    private fun checkHold(currentTime: Long): Note? {
        if (lastStableNote != null && (currentTime - lastStableTime) < NOTE_HOLD_MS) {
            return lastStableNote
        }
        
        // If hold expired, clear everything
        if (lastStableNote != null) {
            resetFilters()
        }
        return null
    }

    fun resetFilters() {
        consecutiveCount = 0
        consecutiveNote = null
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
