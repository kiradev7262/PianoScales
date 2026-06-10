package com.example.pianoscales.domain.progress

import kotlinx.coroutines.flow.Flow

interface BeginnerProgressRepository {
    fun getCompletedLessons(): Flow<Set<Int>>
    suspend fun completeLesson(lessonId: Int)
    suspend fun clearProgress()
}

data class BeginnerLessonProgress(
    val lessonId: Int,
    val unlocked: Boolean,
    val completed: Boolean,
    val completedTimestamp: Long? = null
)
