package com.example.pianoscales.domain.progress

import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getAllProgress(): Flow<List<LessonProgress>>
    suspend fun saveProgress(rootNote: Note, conceptType: ConceptType, completed: Boolean)
    suspend fun clearProgress()
}

data class LessonProgress(
    val rootNote: Note,
    val conceptType: ConceptType,
    val completed: Boolean,
    val completedAt: Long
)
