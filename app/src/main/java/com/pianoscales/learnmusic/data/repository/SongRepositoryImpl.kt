package com.pianoscales.learnmusic.data.repository

import android.content.Context
import com.pianoscales.learnmusic.data.local.CustomSongDao
import com.pianoscales.learnmusic.data.local.CustomSongEntity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val customSongDao: CustomSongDao
) : SongRepository {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())

    companion object {
        private const val REMOTE_JSON_URL = "https://pianoscales.app/json/songs_content.json"
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

    override fun getCustomSongs(): Flow<List<Song>> {
        return customSongDao.getAllCustomSongs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveSong(song: Song) {
        withContext(Dispatchers.IO) {
            customSongDao.insertSong(song.toEntity())
        }
    }

    override suspend fun deleteSong(songId: String) {
        withContext(Dispatchers.IO) {
            customSongDao.deleteSongById(songId)
        }
    }

    override suspend fun getSongById(songId: String): Song? {
        return withContext(Dispatchers.IO) {
            customSongDao.getSongById(songId)?.toDomain()
        }
    }

    private fun Song.toEntity(): CustomSongEntity {
        val hasTimestamps = lines.any { line -> line.notes.any { it.timestamp != null } }
        
        val linesJson = JSONObject().apply {
            if (!startingNote.isNullOrBlank()) {
                put("startingNote", startingNote)
            }
            put("lines", JSONArray().apply {
                lines.forEach { line ->
                    put(JSONArray().apply {
                        line.notes.forEach { noteWithOctave ->
                            put("${noteWithOctave.note.name}${noteWithOctave.octave}")
                        }
                    })
                }
            })
            
            if (hasTimestamps) {
                put("timestamps", JSONArray().apply {
                    lines.forEach { line ->
                        put(JSONArray().apply {
                            line.notes.forEach { noteWithOctave ->
                                put(noteWithOctave.timestamp ?: JSONObject.NULL)
                            }
                        })
                    }
                })
            }
        }.toString()

        return CustomSongEntity(
            songId = songId,
            title = title,
            description = description,
            difficulty = difficulty,
            version = if (hasTimestamps) 2 else version,
            linesJson = linesJson,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

    private fun CustomSongEntity.toDomain(): Song {
        var songLines = emptyList<SongLine>()
        var startingNoteStr: String? = null
        try {
            val trimmed = linesJson.trim()
            if (trimmed.startsWith("{")) {
                val root = JSONObject(linesJson)
                startingNoteStr = root.optString("startingNote").takeIf { !it.isNullOrBlank() }
                songLines = parseSongLinesFromObject(root)
            } else {
                val linesArray = JSONArray(linesJson)
                val tempObj = JSONObject().apply {
                    put("lines", linesArray)
                }
                songLines = parseSongLinesFromObject(tempObj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Song(
            songId = songId,
            title = title,
            description = description,
            difficulty = difficulty,
            version = version,
            lines = songLines,
            startingNote = startingNoteStr,
            builtIn = false,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }

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

    override fun parseSongsFromJson(jsonString: String): List<Song> {
        return try {
            val jsonObject = org.json.JSONObject(jsonString)
            if (jsonObject.has("songs")) {
                parseSongs(jsonObject.getJSONArray("songs").toString())
            } else {
                parseSongs(jsonString)
            }
        } catch (e: Exception) {
            parseSongs(jsonString)
        }
    }

    private fun parseSongs(jsonString: String): List<Song> {
        val songsList = mutableListOf<Song>()
        try {
            val trimmed = jsonString.trim()
            val jsonArray = if (trimmed.startsWith("{")) {
                val obj = JSONObject(jsonString)
                if (obj.has("songs")) {
                    obj.getJSONArray("songs")
                } else {
                    JSONArray().put(obj)
                }
            } else {
                JSONArray(jsonString)
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val songLines = parseSongLinesFromObject(obj)

                songsList.add(
                    Song(
                        songId = obj.getString("songId"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        difficulty = obj.getString("difficulty"),
                        version = obj.optInt("version", 1),
                        lines = songLines,
                        startingNote = obj.optString("startingNote").takeIf { !it.isNullOrBlank() },
                        builtIn = obj.optBoolean("builtIn", false),
                        createdAt = obj.optLong("createdAt", 0L),
                        modifiedAt = obj.optLong("modifiedAt", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return songsList
    }

    private fun parseSongLinesFromObject(obj: JSONObject): List<SongLine> {
        val songLines = mutableListOf<SongLine>()
        val timestampsArray = obj.optJSONArray("timestamps")

        if (obj.has("notes")) {
            val notesArray = obj.getJSONArray("notes")
            val notes = parseNotesArray(notesArray, timestampsArray?.optJSONArray(0))
            if (notes.isNotEmpty()) {
                songLines.add(SongLine(notes))
            }
        } else if (obj.has("lines")) {
            val linesArray = obj.getJSONArray("lines")
            if (linesArray.length() > 0) {
                val firstItem = linesArray.get(0)
                if (firstItem is JSONArray) {
                    for (j in 0 until linesArray.length()) {
                        val notesArray = linesArray.getJSONArray(j)
                        val tsArray = timestampsArray?.optJSONArray(j)
                        val notes = parseNotesArray(notesArray, tsArray)
                        songLines.add(SongLine(notes))
                    }
                } else {
                    // Flat array under "lines"
                    val notes = parseNotesArray(linesArray, timestampsArray?.optJSONArray(0))
                    if (notes.isNotEmpty()) {
                        songLines.add(SongLine(notes))
                    }
                }
            }
        }
        return songLines
    }

    private fun parseNotesArray(notesArray: JSONArray, tsArray: JSONArray?): List<NoteWithOctave> {
        val notes = mutableListOf<NoteWithOctave>()
        for (k in 0 until notesArray.length()) {
            val noteEntry = notesArray.get(k)
            val timestamp = if (tsArray != null && !tsArray.isNull(k)) tsArray.getLong(k) else null
            if (noteEntry is JSONObject) {
                val noteStr = noteEntry.getString("n")
                val t = if (noteEntry.has("t")) noteEntry.getLong("t") else timestamp
                parseNote(noteStr)?.let { notes.add(it.copy(timestamp = t)) }
            } else if (noteEntry is String) {
                parseNote(noteEntry)?.let { notes.add(it.copy(timestamp = timestamp)) }
            }
        }
        return notes
    }

    private fun parseNote(noteStr: String): NoteWithOctave? {
        if (noteStr.length < 2) return null
        val octave = noteStr.last().digitToIntOrNull() ?: return null
        val noteName = noteStr.dropLast(1)
        val note = Note.fromString(noteName) ?: return null
        return NoteWithOctave(note, octave)
    }
}
