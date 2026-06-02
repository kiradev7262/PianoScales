package com.example.pianoscales.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfileImage(): Flow<String?>
    suspend fun updateProfileImage(path: String)
}
