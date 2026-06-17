package com.pianoscales.learnmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val imagePath: String? = null,
    val displayName: String? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastPracticeDate: Long = 0
)
