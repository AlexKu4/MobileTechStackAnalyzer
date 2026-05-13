package com.example.mobiletechstack.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisResultDao {

    @Query("SELECT * FROM analysis_results WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AnalysisResultEntity?

    @Upsert
    suspend fun upsert(entity: AnalysisResultEntity)

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC")
    suspend fun getAllSortedByDate(): List<AnalysisResultEntity>

    @Query("SELECT * FROM analysis_results ORDER BY analyzedAt DESC")
    fun observeAllSortedByDate(): Flow<List<AnalysisResultEntity>>

    @Query("DELETE FROM analysis_results WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM analysis_results WHERE packageName NOT IN (SELECT packageName FROM analysis_results ORDER BY analyzedAt DESC LIMIT :limit)")
    suspend fun trimToLimit(limit: Int)
}
