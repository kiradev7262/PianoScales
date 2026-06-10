package com.example.pianoscales.data.repository

import android.content.Context
import android.content.SharedPreferences
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

    override suspend fun completeLesson(lessonId: Int) {
        val completed = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        completed.add(lessonId.toString())
        prefs.edit().putStringSet(KEY_COMPLETED_LESSONS, completed).apply()
    }

    override suspend fun clearProgress() {
        prefs.edit().remove(KEY_COMPLETED_LESSONS).apply()
    }
}
