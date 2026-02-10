package com.example.myapplication.domain.model

data class AnalysisResult(
    val packageName: String,
    val appName: String,
    val framework: String,
    val language: String,
    val primaryAbi: String,
    val is64Bit: Boolean,
    val nativeLibraries: List<LibraryInfo>,
    val activitiesCount: Int,
    val servicesCount: Int,
    val receiversCount: Int,
    val providersCount: Int,
    val permissionsCount: Int,
    val apkSize: Long,
    val analyzedAt: Long = System.currentTimeMillis()
)