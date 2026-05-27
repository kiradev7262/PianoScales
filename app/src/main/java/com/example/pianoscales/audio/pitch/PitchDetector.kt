package com.example.pianoscales.audio.pitch

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PitchDetector @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val BUFFER_SIZE = 1024
    }

    private var audioRecord: AudioRecord? = null
    private var isRunning = false

    suspend fun startListening(onPitchDetected: (Float) -> Unit) = withContext(Dispatchers.IO) {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                isRunning = false
                return@withContext
            }

            audioRecord?.startRecording()
            isRunning = true

            val buffer = ShortArray(BUFFER_SIZE)
            while (isRunning) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (read > 0) {
                    val pitch = detectPitch(buffer)
                    if (pitch > 0) {
                        onPitchDetected(pitch)
                    }
                }
            }
        } catch (_: SecurityException) {
            isRunning = false
        } finally {
            stopListening()
        }
    }

    private fun detectPitch(buffer: ShortArray): Float {
        // Simple Zero Crossing implementation as fallback if TarsosDSP is unavailable
        var crossings = 0
        for (i in 1 until buffer.size) {
            if ((buffer[i-1] >= 0 && buffer[i] < 0) || (buffer[i-1] < 0 && buffer[i] >= 0)) {
                crossings++
            }
        }
        val frequency = (crossings.toFloat() * SAMPLE_RATE) / (2 * buffer.size)
        return if (frequency in 50f..2000f) frequency else -1f
    }

    fun stopListening() {
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
