package com.pianoscales.learnmusic.audio.pitch

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.pianoscales.learnmusic.BuildConfig
import be.tarsos.dsp.pitch.Yin
import com.pianoscales.learnmusic.theory.Note
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
        private const val MIN_PROBABILITY_THRESHOLD = 0.88f // Increased from 0.85 for better stability
        private const val MIN_STABLE_FRAMES = 2 // Consecutive frames required after median filtering
        private const val NOTE_HOLD_MS = 400L // Keep note on screen after sound stops
        private const val OCTAVE_STABILITY_MS = 100L // Validation window for octave jumps
        private const val MEDIAN_WINDOW_SIZE = 5 // Window for frequency median filtering
        
        // Harmonic Stabilization
        private const val HARMONIC_STABILITY_MS = 250L // Persistence required for octave jumps
        private const val HARMONIC_TOLERANCE = 0.05f // ±5% acceptance window for harmonics
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

    // Octave stabilization state
    private var candidateMidi: Int? = null
    private var candidateStartTime = 0L

    // Harmonic stabilization state
    private var lastStableFrequency: Float? = null
    private var frequencyCandidate: Float? = null
    private var frequencyCandidateStartTime = 0L

    // Median filter state
    private val frequencyWindow = mutableListOf<Float>()

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
        
        // Phase 1: Comprehensive Debug Logging (Raw Input)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            val rawMidi = PitchToNoteMapper.frequencyToMidi(frequency)
            val rawNote = rawMidi?.let { Note.entries[(it % 12 + 12) % 12] }
            val rawOctave = rawMidi?.let { (it / 12) - 1 }
            Log.d(TAG, """
                Pitch Debug (Raw)
                Frequency : ${String.format(Locale.US, "%.2f", frequency)} Hz
                Confidence : ${String.format(Locale.US, "%.2f", probability)}
                Amplitude : ${String.format(Locale.US, "%.4f", amplitude)}
                Midi : ${rawMidi ?: "--"}
                Note : ${rawNote?.displayName ?: "--"}${rawOctave ?: ""}
                Time : $currentTime
            """.trimIndent())
        }

        // A. Amplitude Gate
        if (amplitude < MIN_AMPLITUDE_THRESHOLD) {
            logRejection("Low amplitude", frequency, probability, amplitude)
            cancelCandidate("Silence detected")
            return checkHoldMidi(currentTime)
        }

        // B. Confidence Filter
        if (probability < MIN_PROBABILITY_THRESHOLD || frequency <= 0) {
            logRejection(if (frequency <= 0) "Invalid frequency" else "Low confidence", frequency, probability, amplitude)
            cancelCandidate("Unstable pitch detection")
            return checkHoldMidi(currentTime)
        }

        // Phase 3.1: Median Filter on Frequency
        frequencyWindow.add(frequency)
        if (frequencyWindow.size > MEDIAN_WINDOW_SIZE) {
            frequencyWindow.removeAt(0)
        }
        
        val sortedFrequencies = frequencyWindow.sorted()
        val medianFrequency = if (sortedFrequencies.isNotEmpty()) {
            sortedFrequencies[sortedFrequencies.size / 2]
        } else frequency

        // Phase 3.2: Harmonic Stabilizer (Frequency-level stabilization before MIDI conversion)
        val stabilizedFrequency = stabilizeHarmonics(medianFrequency, currentTime)

        val currentMidi = PitchToNoteMapper.frequencyToMidi(stabilizedFrequency) ?: run {
            logRejection("MIDI conversion failed", stabilizedFrequency, probability, amplitude)
            cancelCandidate("Invalid MIDI")
            return checkHoldMidi(currentTime)
        }
        
        // C. Stability Filter - Require N consecutive frames of same MIDI note (includes octave)
        if (currentMidi == consecutiveMidi) {
            consecutiveCount++
        } else {
            consecutiveMidi = currentMidi
            consecutiveCount = 1
        }

        if (consecutiveCount >= MIN_STABLE_FRAMES) {
            // Logic for Octave Stabilization
            
            // 1. Initial detection or returning from silence
            if (lastStableMidi == null) {
                updateAcceptedNote(currentMidi, stabilizedFrequency, currentTime)
                return currentMidi
            }

            // 2. Different note class (e.g. C4 -> D4) - Accept immediately
            if (currentMidi % 12 != lastStableMidi!! % 12) {
                cancelCandidate("Different note class detected")
                updateAcceptedNote(currentMidi, stabilizedFrequency, currentTime)
                return currentMidi
            }

            // 3. Same note class (e.g. C4 -> C3) - Start or continue octave validation
            if (currentMidi != lastStableMidi) {
                if (candidateMidi == currentMidi) {
                    // Candidate persists, check if enough time has passed
                    if (currentTime - candidateStartTime >= OCTAVE_STABILITY_MS) {
                        Log.d(TAG, "Candidate accepted after ${currentTime - candidateStartTime} ms")
                        updateAcceptedNote(currentMidi, stabilizedFrequency, currentTime)
                        candidateMidi = null
                        return currentMidi
                    }
                } else {
                    // New octave candidate
                    candidateMidi = currentMidi
                    candidateStartTime = currentTime
                    Log.d(TAG, "Accepted Note: ${lastStableNote?.displayName}${(lastStableMidi!! / 12) - 1}")
                    Log.d(TAG, "Candidate Octave: ${Note.entries[(currentMidi % 12 + 12) % 12].displayName}${(currentMidi / 12) - 1}")
                }
                // Phase 3.4: Preserve Last Stable Note while candidate is being validated
                return lastStableMidi
            } else {
                // It matches the last stable MIDI exactly
                if (candidateMidi != null) {
                    cancelCandidate("Returned to original octave")
                }
                lastStableTime = currentTime
                lastStableFrequency = stabilizedFrequency
                return lastStableMidi
            }
        }

        // D. Note Hold - Keep showing the last stable note briefly
        return checkHoldMidi(currentTime)
    }

    private fun logRejection(reason: String, frequency: Float, confidence: Float, amplitude: Float) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, """
                Rejected sample
                Reason: $reason
                Frequency: ${String.format(Locale.US, "%.2f", frequency)} Hz
                Confidence: ${String.format(Locale.US, "%.2f", confidence)}
                Amplitude: ${String.format(Locale.US, "%.4f", amplitude)}
            """.trimIndent())
        }
    }

    private fun updateAcceptedNote(midi: Int, frequency: Float, currentTime: Long) {
        if (midi != lastStableMidi) {
            val currentNote = Note.entries[(midi % 12 + 12) % 12]
            val octave = (midi / 12) - 1
            Log.d(TAG, "Frequency=${String.format(Locale.US, "%.2f", frequency)}Hz Note=${currentNote.displayName} Octave=$octave Midi=$midi Timestamp=$currentTime")
        }
        lastStableMidi = midi
        lastStableNote = Note.entries[(midi % 12 + 12) % 12]
        lastStableTime = currentTime
        lastStableFrequency = frequency
    }

    private fun cancelCandidate(reason: String) {
        if (candidateMidi != null) {
            Log.d(TAG, "Candidate rejected ($reason)")
            candidateMidi = null
            candidateStartTime = 0L
        }
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
        lastStableFrequency = null
        candidateMidi = null
        candidateStartTime = 0L
        frequencyCandidate = null
        frequencyCandidateStartTime = 0L
        frequencyWindow.clear()
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

    private fun stabilizeHarmonics(frequency: Float, currentTime: Long): Float {
        val stable = lastStableFrequency ?: return frequency

        // 1. Generate Candidates (Octave harmonics/subharmonics)
        val candidateMap = mapOf(
            "× 1" to frequency,
            "× 2" to frequency * 2.0f,
            "÷ 2" to frequency / 2.0f,
            "× 4" to frequency * 4.0f,
            "÷ 4" to frequency / 4.0f
        )

        var bestMultiplier: String? = null
        var matchedFrequency = 0f

        for ((multiplier, candidate) in candidateMap) {
            val ratio = candidate / stable
            if (ratio > 1.0f - HARMONIC_TOLERANCE && ratio < 1.0f + HARMONIC_TOLERANCE) {
                bestMultiplier = multiplier
                matchedFrequency = candidate
                break
            }
        }

        if (bestMultiplier != null) {
            // Case 2: Potential harmonic flicker. Check for persistence.
            if (frequencyCandidate == null || !isHarmonicOf(frequency, frequencyCandidate!!)) {
                // New harmonic candidate detected
                frequencyCandidate = frequency
                frequencyCandidateStartTime = currentTime
                
                logHarmonicMatch(frequency, stable, matchedFrequency, bestMultiplier, true, stable)
                return stable
            } else {
                // Existing harmonic candidate persists
                if (currentTime - frequencyCandidateStartTime >= HARMONIC_STABILITY_MS) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Persistent harmonic change accepted: $frequency Hz")
                    }
                    return frequency
                } else {
                    return stable
                }
            }
        } else {
            // Case 1: Not a harmonic relationship. Accept new frequency immediately.
            if (frequencyCandidate != null || (BuildConfig.DEBUG && Math.abs(frequency - stable) > 5.0f)) {
                logHarmonicMatch(frequency, stable, frequency, "N/A", false, frequency)
            }
            frequencyCandidate = null
            frequencyCandidateStartTime = 0L
            return frequency
        }
    }

    private fun isHarmonicOf(f1: Float, f2: Float): Boolean {
        if (f1 <= 0 || f2 <= 0) return false
        val candidates = listOf(f1, f1 * 2f, f1 / 2f, f1 * 4f, f1 / 4f)
        for (c in candidates) {
            val ratio = c / f2
            if (ratio > 1.0f - HARMONIC_TOLERANCE && ratio < 1.0f + HARMONIC_TOLERANCE) return true
        }
        return false
    }

    private fun logHarmonicMatch(raw: Float, stable: Float, candidate: Float, mult: String, matched: Boolean, result: Float) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, """
            ========== Harmonic Stabilizer ==========
            Raw Frequency      : ${String.format(Locale.US, "%.1f", raw)} Hz
            Stable Frequency   : ${String.format(Locale.US, "%.1f", stable)} Hz
            
            Candidate:
            ${if (mult == "N/A") "No harmonic relationship found." else "${String.format(Locale.US, "%.1f", raw)} $mult = ${String.format(Locale.US, "%.1f", candidate)} Hz"}
            
            Matched Stable:
            ${if (matched) "YES" else "NO"}
            
            Using:
            ${String.format(Locale.US, "%.1f", result)} Hz
            =========================================
        """.trimIndent())
    }
}
