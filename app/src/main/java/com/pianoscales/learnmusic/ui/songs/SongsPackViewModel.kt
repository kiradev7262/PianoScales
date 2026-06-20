package com.pianoscales.learnmusic.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoscales.learnmusic.domain.songs.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SongsPackViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {
    val songs: StateFlow<List<Song>> = songRepository.getSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            songRepository.refreshSongs()
        }
    }
}
