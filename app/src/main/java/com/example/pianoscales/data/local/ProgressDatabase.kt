package com.example.pianoscales.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LessonProgressEntity::class], version = 1, exportSchema = false)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
