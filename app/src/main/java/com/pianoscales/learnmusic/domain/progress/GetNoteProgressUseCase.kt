package com.pianoscales.learnmusic.domain.progress

import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.theory.ConceptType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNoteProgressUseCase @Inject constructor(
    private val repository: ProgressRepository
) {
    operator fun invoke(note: Note): Flow<NoteProgress> {
        return repository.getAllProgress().map { allProgress ->
            val noteLessons = allProgress.filter { it.rootNote == note }
            val completedCount = noteLessons.count { it.completed }
            val totalConcepts = ConceptType.entries.size
            
            NoteProgress(
                completedLessons = completedCount,
                totalLessons = totalConcepts,
                percentage = if (totalConcepts > 0) completedCount.toFloat() / totalConcepts else 0f
            )
        }
    }
}

data class NoteProgress(
    val completedLessons: Int,
    val totalLessons: Int,
    val percentage: Float
)
