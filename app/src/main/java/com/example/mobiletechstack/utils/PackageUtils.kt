package com.example.mobiletechstack.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.mobiletechstack.domain.model.AppInfo
import java.io.File

fun PackageManager.getInstalledApps(): List<AppInfo> {
    val installedApps = getInstalledApplications(PackageManager.GET_META_DATA)

    return installedApps
        .filter { app ->
            val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !isSystemApp || isUpdatedSystemApp
        }
        .mapNotNull { app ->
            try {
                val packageInfo = getPackageInfo(app.packageName, 0)
                val apkFile = File(app.sourceDir)

                AppInfo(
                    packageName = app.packageName,
                    appName = app.loadLabel(this).toString(),
                    icon = app.loadIcon(this),
                    versionName = packageInfo.versionName ?: "Unknown",
                    apkPath = app.sourceDir,
                    apkSize = if (apkFile.exists()) apkFile.length() else 0L
                )
            } catch (e: Exception) {
                null
            }
        }
        .sortedBy { it.appName.lowercase() }
}

fun Long.formatSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$this B"
    }
}