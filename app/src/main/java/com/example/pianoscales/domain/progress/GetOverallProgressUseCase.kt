package com.example.pianoscales.domain.progress

import com.example.pianoscales.theory.ConceptType
import com.example.pianoscales.theory.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetOverallProgressUseCase @Inject constructor(
    private val repository: ProgressRepository
) {
    operator fun invoke(): Flow<OverallProgress> {
        return repository.getAllProgress().map { completedLessons ->
            val totalNotes = Note.entries.size
            val totalConcepts = ConceptType.entries.size
            val totalLessons = totalNotes * totalConcepts
            val completedCount = completedLessons.count { it.completed }
            
            OverallProgress(
                completedLessons = completedCount,
                totalLessons = totalLessons,
                percentage = if (totalLessons > 0) completedCount.toFloat() / totalLessons else 0f
            )
        }
    }
}

data class OverallProgress(
    val completedLessons: Int,
    val totalLessons: Int,
    val percentage: Float
)
