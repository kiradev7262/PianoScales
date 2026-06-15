package com.example.pianoscales.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.pianoscales.domain.progress.BeginnerLessonProgress
import com.example.pianoscales.domain.progress.BeginnerProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeginnerProgressRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BeginnerProgressRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("beginner_journey_prefs", Context.MODE_PRIVATE)
    private val KEY_COMPLETED_LESSONS = "completed_lessons"

    override fun getCompletedLessons(): Flow<Set<Int>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == KEY_COMPLETED_LESSONS) {
                val completed = sharedPreferences.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
                trySend(completed.map { it.toInt() }.toSet())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        val initial = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        trySend(initial.map { it.toInt() }.toSet())

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        val initial = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        emit(initial.map { it.toInt() }.toSet())
    }

    override fun getCompletedLessonsWithTimestamps(): Flow<List<BeginnerLessonProgress>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
            val completedIds = sharedPreferences.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
            val list = completedIds.map { idStr ->
                val id = idStr.toInt()
                val timestamp = sharedPreferences.getLong("completed_${id}_at", 0L)
                BeginnerLessonProgress(id, true, if (timestamp == 0L) null else timestamp)
            }
            trySend(list)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        val initialIds = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        val initialList = initialIds.map { idStr ->
            val id = idStr.toInt()
            val timestamp = prefs.getLong("completed_${id}_at", 0L)
            BeginnerLessonProgress(id, true, if (timestamp == 0L) null else timestamp)
        }
        trySend(initialList)

        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.onStart {
        val initialIds = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        val initialList = initialIds.map { idStr ->
            val id = idStr.toInt()
            val timestamp = prefs.getLong("completed_${id}_at", 0L)
            BeginnerLessonProgress(id, true, if (timestamp == 0L) null else timestamp)
        }
        emit(initialList)
    }

    override suspend fun completeLesson(lessonId: Int) {
        val completed = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        completed.add(lessonId.toString())
        prefs.edit()
            .putStringSet(KEY_COMPLETED_LESSONS, completed)
            .putLong("completed_${lessonId}_at", System.currentTimeMillis())
            .apply()
    }

    override suspend fun clearProgress() {
        val completedIds = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        val editor = prefs.edit()
        editor.remove(KEY_COMPLETED_LESSONS)
        completedIds.forEach { id ->
            editor.remove("completed_${id}_at")
        }
        editor.apply()
    }
}
