package com.example.mobiletechstack.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalysisResultEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisResultDao(): AnalysisResultDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "techstack.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
