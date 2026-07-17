package com.pianoscales.learnmusic.audio_intelligence.voice_training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.audio.pitch.PitchDetector
import com.pianoscales.learnmusic.audio.playback.SoundPoolManager
import com.pianoscales.learnmusic.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceTrainingViewModel @Inject constructor(
    private val pitchDetector: PitchDetector,
    private val soundPoolManager: SoundPoolManager,
    private val profileRepository: com.pianoscales.learnmusic.domain.profile.ProfileRepository
) : ViewModel() {

    private val _detectedNote = MutableStateFlow<Note?>(null)
    val detectedNote = _detectedNote.asStateFlow()

    private val _frequency = MutableStateFlow(0f)
    val frequency = _frequency.asStateFlow()

    private val _isStable = MutableStateFlow(false)
    val isStable = _isStable.asStateFlow()

    private val _midi = MutableStateFlow<Int?>(null)
    val midi = _midi.asStateFlow()

    private val _octave = MutableStateFlow<Int?>(null)
    val octave = _octave.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence = _confidence.asStateFlow()

    private val _timestamp = MutableStateFlow(0L)
    val timestamp = _timestamp.asStateFlow()

    private val _targetNote = MutableStateFlow<Note?>(null)
    val targetNote = _targetNote.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        viewModelScope.launch {
            pitchDetector.startListening { result ->
                _detectedNote.value = result.note
                _frequency.value = result.frequency
                _isStable.value = result.isStable
                _midi.value = result.midi
                _octave.value = result.octave
                _confidence.value = result.confidence
                _timestamp.value = result.timestamp

                if (result.isStable && result.note != null && result.note == _targetNote.value) {
                    viewModelScope.launch { profileRepository.updateStreak() }
                }
            }
        }
    }

    fun stopListening() {
        _isListening.value = false
        pitchDetector.stopListening()
    }

    fun playReferenceNote() {
        _targetNote.value?.let {
            soundPoolManager.playNote(it, 4)
        }
    }

    fun setTargetNote(note: Note) {
        _targetNote.value = note
        soundPoolManager.playNote(note, 4)
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
