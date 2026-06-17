package com.pianoscales.learnmusic.data.profile

import com.pianoscales.learnmusic.data.local.UserProfileDao
import com.pianoscales.learnmusic.data.local.UserProfileEntity
import com.pianoscales.learnmusic.domain.profile.ProfileRepository
import com.pianoscales.learnmusic.domain.profile.StreakInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ProfileRepository {
    override fun getProfileImage(): Flow<String?> {
        return userProfileDao.getUserProfile().map { it?.imagePath }
    }

    override suspend fun updateProfileImage(path: String) {
        val current = userProfileDao.getUserProfile().first() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(imagePath = path))
    }

    override fun getDisplayName(): Flow<String> {
        return userProfileDao.getUserProfile().map { it?.displayName ?: "Learner" }
    }

    override suspend fun updateDisplayName(name: String) {
        val current = userProfileDao.getUserProfile().first() ?: UserProfileEntity()
        userProfileDao.upsertProfile(current.copy(displayName = name))
    }

    override fun getStreakInfo(): Flow<StreakInfo> {
        return userProfileDao.getUserProfile().map { 
            StreakInfo(
                currentStreak = it?.currentStreak ?: 0,
                bestStreak = it?.bestStreak ?: 0,
                lastPracticeDate = it?.lastPracticeDate ?: 0
            )
        }
    }

    override suspend fun updateStreak() {
        val currentProfile = userProfileDao.getUserProfile().first() ?: UserProfileEntity()
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val lastPractice = currentProfile.lastPracticeDate
        
        if (today == lastPractice) return 
        
        val newStreak = if (today == lastPractice + 1) {
            currentProfile.currentStreak + 1
        } else {
            1
        }
        
        val newBest = if (newStreak > currentProfile.bestStreak) newStreak else currentProfile.bestStreak
        
        userProfileDao.upsertProfile(
            currentProfile.copy(
                currentStreak = newStreak,
                bestStreak = newBest,
                lastPracticeDate = today
            )
        )
    }
}
