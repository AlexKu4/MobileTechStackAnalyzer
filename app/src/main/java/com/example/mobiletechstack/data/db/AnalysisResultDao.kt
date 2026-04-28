package com.example.mobiletechstack.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AnalysisResultDao {

    @Query("SELECT * FROM analysis_results WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AnalysisResultEntity?

    @Upsert
    suspend fun upsert(entity: AnalysisResultEntity)

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC")
    suspend fun getAllSortedByDate(): List<AnalysisResultEntity>

    @Query("DELETE FROM analysis_results WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
