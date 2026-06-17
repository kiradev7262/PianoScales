package com.pianoscales.learnmusic.data.repository

import com.pianoscales.learnmusic.data.local.LessonProgressEntity
import com.pianoscales.learnmusic.data.local.ProgressDao
import com.pianoscales.learnmusic.domain.progress.LessonProgress
import com.pianoscales.learnmusic.domain.progress.ProgressRepository
import com.pianoscales.learnmusic.theory.ConceptType
import com.pianoscales.learnmusic.theory.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao
) : ProgressRepository {

    override fun getAllProgress(): Flow<List<LessonProgress>> {
        return progressDao.getAllProgress().map { entities ->
            entities.map { entity ->
                LessonProgress(
                    rootNote = Note.valueOf(entity.rootNote),
                    conceptType = ConceptType.valueOf(entity.conceptType),
                    completed = entity.completed,
                    completedAt = entity.completedAt
                )
            }
        }
    }

    override suspend fun saveProgress(rootNote: Note, conceptType: ConceptType, completed: Boolean) {
        val lessonId = "${rootNote.name}_${conceptType.name}"
        val entity = LessonProgressEntity(
            lessonId = lessonId,
            rootNote = rootNote.name,
            conceptType = conceptType.name,
            completed = completed,
            completedAt = System.currentTimeMillis()
        )
        progressDao.saveProgress(entity)
    }

    override suspend fun clearProgress() {
        progressDao.clearAllProgress()
    }
}
