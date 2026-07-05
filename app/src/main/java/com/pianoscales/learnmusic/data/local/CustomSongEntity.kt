package com.pianoscales.learnmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_songs")
data class CustomSongEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val version: Int,
    val linesJson: String,
    val createdAt: Long,
    val modifiedAt: Long
)
