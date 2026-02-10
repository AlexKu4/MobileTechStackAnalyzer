package com.example.mobiletechstack.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val versionName: String,
    val apkPath: String,
    val apkSize: Long
)