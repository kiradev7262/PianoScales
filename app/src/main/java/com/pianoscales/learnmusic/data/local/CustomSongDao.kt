package com.pianoscales.learnmusic.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSongDao {
    @Query("SELECT * FROM custom_songs ORDER BY modifiedAt DESC")
    fun getAllCustomSongs(): Flow<List<CustomSongEntity>>

    @Query("SELECT * FROM custom_songs WHERE songId = :songId")
    suspend fun getSongById(songId: String): CustomSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: CustomSongEntity)

    @Delete
    suspend fun deleteSong(song: CustomSongEntity)
    
    @Query("DELETE FROM custom_songs WHERE songId = :songId")
    suspend fun deleteSongById(songId: String)
}
