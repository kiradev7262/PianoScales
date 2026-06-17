package com.pianoscales.learnmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey
    val lessonId: String,
    val rootNote: String,
    val conceptType: String,
    val completed: Boolean,
    val completedAt: Long
)
