package com.pianoscales.learnmusic.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfileImage(): Flow<String?>
    suspend fun updateProfileImage(path: String)
    fun getDisplayName(): Flow<String>
    suspend fun updateDisplayName(name: String)
    fun getStreakInfo(): Flow<StreakInfo>
    suspend fun updateStreak()
}

data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastPracticeDate: Long
)
