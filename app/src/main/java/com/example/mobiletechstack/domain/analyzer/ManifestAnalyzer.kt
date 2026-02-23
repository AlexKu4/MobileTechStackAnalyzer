package com.example.mobiletechstack.domain.analyzer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.example.mobiletechstack.domain.model.AppVersionInfo
import com.example.mobiletechstack.domain.model.SecurityFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ManifestAnalyzer(private val context: Context) {

    private val packageManager = context.packageManager

    @SuppressLint("ObsoleteSdkInt")
    suspend fun extractVersionInfo(packageName: String): AppVersionInfo? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)

            AppVersionInfo(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    applicationInfo.minSdkVersion
                } else {
                    0
                },
                targetSdkVersion = applicationInfo.targetSdkVersion,
                compileSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applicationInfo.compileSdkVersion
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    suspend fun extractSecurityFlags(packageName: String): SecurityFlags? = withContext(Dispatchers.IO) {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)

            SecurityFlags(
                isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                allowBackup = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0,
                usesCleartextTraffic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
                } else {
                    true
                },
                hasCode = (applicationInfo.flags and ApplicationInfo.FLAG_HAS_CODE) != 0
            )
        } catch (e: Exception) {
            null
        }
    }
}