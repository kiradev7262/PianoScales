package com.pianoscales.learnmusic.data.repository

import android.content.Context
import com.pianoscales.learnmusic.data.models.VideoMetadata
import com.pianoscales.learnmusic.domain.video.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoRepository {

    private val _videoMetadata = MutableStateFlow<Map<String, VideoMetadata>>(emptyMap())

    companion object {
        private const val REMOTE_JSON_URL = "https://kiradev7262.github.io/PianoScales/video_content.json"
        private const val OFFLINE_ASSET_PATH = "video_content.json"
    }

    init {
        loadOfflineData()
    }

    private fun loadOfflineData() {
        try {
            val jsonString = context.assets.open(OFFLINE_ASSET_PATH).bufferedReader().use { it.readText() }
            val metadataMap = parseJson(jsonString)
            _videoMetadata.value = metadataMap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getVideoMetadata(conceptId: String): Flow<VideoMetadata?> {
        return _videoMetadata.map { it[conceptId] }
    }

    override suspend fun refreshMetadata() {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(REMOTE_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                        val metadataMap = parseJson(jsonString)
                        if (metadataMap.isNotEmpty()) {
                            _videoMetadata.value = metadataMap
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                // Silently fail and keep offline data as per Step 5
                e.printStackTrace()
            }
        }
    }

    private fun parseJson(jsonString: String): Map<String, VideoMetadata> {
        val map = mutableMapOf<String, VideoMetadata>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val conceptId = obj.getString("conceptId")
                val metadata = VideoMetadata(
                    conceptId = conceptId,
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    youtubeUrl = if (obj.isNull("youtubeUrl")) null else obj.getString("youtubeUrl"),
                    available = obj.getBoolean("available")
                )
                map[conceptId] = metadata
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}
