package com.example.mobiletechstack.domain.model

data class AnalysisResult(
    val packageName: String,
    val appName: String,
    val nativeLibraries: List<LibraryInfo>,
    val apkPath: String,
    val apkSize: Long,
    val framework: String,
    val language: String
)