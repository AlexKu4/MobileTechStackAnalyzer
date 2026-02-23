package com.example.mobiletechstack.domain.model

data class AppVersionInfo(
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val compileSdkVersion: Int?
)

data class SecurityFlags(
    val isDebuggable: Boolean,
    val allowBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val hasCode: Boolean
)