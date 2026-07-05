package com.pianoscales.learnmusic.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pianoscales.learnmusic.ui.songs.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object SongExportManager {

    fun exportSongs(context: Context, songs: List<Song>): Intent? {
        if (songs.isEmpty()) return null

        try {
            val exportObject = JSONObject().apply {
                put("version", 1)
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date()))
                
                val songsArray = JSONArray()
                songs.forEach { song ->
                    songsArray.put(JSONObject().apply {
                        put("songId", song.songId)
                        put("title", song.title)
                        put("description", song.description)
                        put("difficulty", song.difficulty)
                        put("available", true)
                        put("builtIn", false)
                        put("version", song.version)
                        
                        val linesArray = JSONArray()
                        song.lines.forEach { line ->
                            val notesArray = JSONArray()
                            line.notes.forEach { noteWithOctave ->
                                notesArray.put("${noteWithOctave.note.displayName}${noteWithOctave.octave}")
                            }
                            linesArray.put(notesArray)
                        }
                        put("lines", linesArray)
                    })
                }
                put("songs", songsArray)
            }

            val jsonString = exportObject.toString(4)
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "piano_scales_my_songs_$dateStr.json"
            
            val tempFile = File(context.cacheDir, fileName)
            tempFile.writeText(jsonString)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            return Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "My Piano Scales Songs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
