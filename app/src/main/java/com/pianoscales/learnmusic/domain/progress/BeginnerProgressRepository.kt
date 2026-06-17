package com.pianoscales.learnmusic.domain.progress

import kotlinx.coroutines.flow.Flow

interface BeginnerProgressRepository {
    fun getCompletedLessons(): Flow<Set<Int>>
    fun getCompletedLessonsWithTimestamps(): Flow<List<BeginnerLessonProgress>>
    suspend fun completeLesson(lessonId: Int)
    suspend fun clearProgress()
}

data class BeginnerLessonProgress(
    val lessonId: Int,
    val completed: Boolean,
    val completedTimestamp: Long? = null
)
