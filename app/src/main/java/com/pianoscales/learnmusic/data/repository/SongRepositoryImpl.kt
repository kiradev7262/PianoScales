package com.pianoscales.learnmusic.data.repository

import android.content.Context
import com.pianoscales.learnmusic.domain.songs.SongRepository
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.songs.NoteWithOctave
import com.pianoscales.learnmusic.ui.songs.Song
import com.pianoscales.learnmusic.ui.songs.SongLine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SongRepository {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())

    companion object {
        private const val REMOTE_JSON_URL = "https://kiradev7262.github.io/PianoScales/songs.content.json"
        private const val OFFLINE_ASSET_PATH = "songsContent.json"
    }

    init {
        loadOfflineData()
    }

    private fun loadOfflineData() {
        try {
            val jsonString = context.assets.open(OFFLINE_ASSET_PATH).bufferedReader().use { it.readText() }
            val songsList = parseSongs(jsonString)
            _songs.value = songsList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getSongs(): Flow<List<Song>> = _songs.asStateFlow()

    override suspend fun refreshSongs() {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(REMOTE_JSON_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                        val remoteSongs = parseSongs(jsonString)
                        if (remoteSongs.isNotEmpty()) {
                            mergeSongs(remoteSongs)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun mergeSongs(remoteSongs: List<Song>) {
        val currentSongs = _songs.value.toMutableList()
        remoteSongs.forEach { remoteSong ->
            val index = currentSongs.indexOfFirst { it.songId == remoteSong.songId }
            if (index != -1) {
                if (remoteSong.version >= currentSongs[index].version) {
                    currentSongs[index] = remoteSong
                }
            } else {
                currentSongs.add(remoteSong)
            }
        }
        _songs.value = currentSongs
    }

    private fun parseSongs(jsonString: String): List<Song> {
        val songsList = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val linesArray = obj.getJSONArray("lines")
                val songLines = mutableListOf<SongLine>()
                
                for (j in 0 until linesArray.length()) {
                    val notesArray = linesArray.getJSONArray(j)
                    val notes = mutableListOf<NoteWithOctave>()
                    for (k in 0 until notesArray.length()) {
                        val noteStr = notesArray.getString(k)
                        parseNote(noteStr)?.let { notes.add(it) }
                    }
                    songLines.add(SongLine(notes))
                }

                songsList.add(
                    Song(
                        songId = obj.getString("songId"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        difficulty = obj.getString("difficulty"),
                        version = obj.getInt("version"),
                        lines = songLines
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songsList
    }

    private fun parseNote(noteStr: String): NoteWithOctave? {
        if (noteStr.length < 2) return null
        val octave = noteStr.last().digitToIntOrNull() ?: return null
        val noteName = noteStr.dropLast(1)
        val note = Note.fromString(noteName) ?: return null
        return NoteWithOctave(note, octave)
    }
}
