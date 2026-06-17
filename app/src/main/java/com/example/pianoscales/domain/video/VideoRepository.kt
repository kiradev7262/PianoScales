package com.example.pianoscales.domain.video

import com.example.pianoscales.data.models.VideoMetadata
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getVideoMetadata(conceptId: String): Flow<VideoMetadata?>
    suspend fun refreshMetadata()
}
