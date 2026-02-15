package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class APKAnalyzer(private val context: Context) {

    private val permissionAnalyzer = PermissionAnalyzer(context)

    suspend fun analyzeApp(packageName: String): AnalysisResult {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val apkPath = appInfo.sourceDir

            val nativeLibs = NativeLibraryAnalyzer.extractNativeLibraries(apkPath)

            val framework = FrameworkDetector.detectFramework(apkPath, nativeLibs)

            val language = FrameworkDetector.detectLanguage(apkPath)

            val primaryAbi = NativeLibraryAnalyzer.getPrimaryAbi(nativeLibs)

            val is64Bit = primaryAbi.contains("64")

            val supportedAbis = NativeLibraryAnalyzer.getAbis(nativeLibs)

            val appName = appInfo.loadLabel(packageManager).toString()

            val apkSize = File(apkPath).length()

            val permissions = permissionAnalyzer.extractPermissions(packageName)


            AnalysisResult(
                packageName,
                appName,
                nativeLibs,
                apkPath, apkSize, framework, language, primaryAbi, is64Bit, supportedAbis, permissions
            )
        }
    }
}