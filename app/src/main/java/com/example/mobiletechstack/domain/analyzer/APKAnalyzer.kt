package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class APKAnalyzer(private val context: Context) {

    suspend fun analyzeApp(packageName: String): AnalysisResult = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        val apkPath = appInfo.sourceDir

        val nativeLibs = NativeLibraryAnalyzer.extractNativeLibraries(apkPath)

        val framework = FrameworkDetector.detectFramework(apkPath, nativeLibs)

        val language = FrameworkDetector.detectLanguage(apkPath)

        val appName = appInfo.loadLabel(packageManager).toString()

        val apkSize = File(apkPath).length()

        AnalysisResult(
            packageName, appName, nativeLibs, apkPath, apkSize, framework, language
        )
    }
}