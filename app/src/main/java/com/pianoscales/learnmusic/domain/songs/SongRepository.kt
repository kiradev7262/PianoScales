package com.pianoscales.learnmusic.domain.songs

import com.pianoscales.learnmusic.ui.songs.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    suspend fun refreshSongs()
    
    fun getCustomSongs(): Flow<List<Song>>
    suspend fun saveSong(song: Song)
    suspend fun deleteSong(songId: String)
    suspend fun getSongById(songId: String): Song?
}
