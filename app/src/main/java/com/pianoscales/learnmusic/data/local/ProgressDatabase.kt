package com.pianoscales.learnmusic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LessonProgressEntity::class, UserProfileEntity::class], version = 3, exportSchema = false)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun userProfileDao(): UserProfileDao
}
