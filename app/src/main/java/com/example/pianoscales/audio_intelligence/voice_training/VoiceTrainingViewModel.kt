package com.example.pianoscales.audio_intelligence.voice_training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pianoscales.audio.pitch.PitchDetector
import com.example.pianoscales.audio.playback.SoundPoolManager
import com.example.pianoscales.theory.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceTrainingViewModel @Inject constructor(
    private val pitchDetector: PitchDetector,
    private val soundPoolManager: SoundPoolManager,
    private val profileRepository: com.example.pianoscales.domain.profile.ProfileRepository
) : ViewModel() {

    private val _detectedNote = MutableStateFlow<Note?>(null)
    val detectedNote = _detectedNote.asStateFlow()

    private val _frequency = MutableStateFlow(0f)
    val frequency = _frequency.asStateFlow()

    private val _isStable = MutableStateFlow(false)
    val isStable = _isStable.asStateFlow()

    private val _targetNote = MutableStateFlow<Note?>(null)
    val targetNote = _targetNote.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        viewModelScope.launch {
            pitchDetector.startListening { note, freq, _, stable ->
                _detectedNote.value = note
                _frequency.value = freq
                _isStable.value = stable
                if (stable && note != null && note == _targetNote.value) {
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
