package com.example.pianoscales.data.profile

import com.example.pianoscales.data.local.UserProfileDao
import com.example.pianoscales.data.local.UserProfileEntity
import com.example.pianoscales.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ProfileRepository {
    override fun getProfileImage(): Flow<String?> {
        return userProfileDao.getUserProfile().map { it?.imagePath }
    }

    override suspend fun updateProfileImage(path: String) {
        userProfileDao.upsertProfile(UserProfileEntity(imagePath = path))
    }
}
