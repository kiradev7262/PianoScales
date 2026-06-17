package com.pianoscales.learnmusic.domain.video

import com.pianoscales.learnmusic.data.models.VideoMetadata
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getVideoMetadata(conceptId: String): Flow<VideoMetadata?>
    suspend fun refreshMetadata()
}
