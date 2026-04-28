package com.example.mobiletechstack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_results")
data class AnalysisResultEntity(
    @PrimaryKey val packageName: String,
    val analyzedAt: Long,
    val resultJson: String
)
