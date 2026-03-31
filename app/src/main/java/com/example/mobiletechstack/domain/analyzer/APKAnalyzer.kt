package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class APKAnalyzer(private val context: Context) {

    private val permissionAnalyzer = PermissionAnalyzer(context)
    private val manifestAnalyzer = ManifestAnalyzer(context)
    private val dexAnalyzer = DexAnalyzer(context)
    private val languageDetector = LanguageDetector(context)

    suspend fun analyzeApp(packageName: String): AnalysisResult {
        return withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val apkPath = appInfo.sourceDir

            val nativeLibs = NativeLibraryAnalyzer.extractNativeLibraries(apkPath)

            val frameworkInfo = FrameworkDetector.detectFrameworkDetailed(apkPath, nativeLibs)
            val framework = frameworkInfo.type.displayName

            val languageInfo = languageDetector.detectLanguagesDetailed(apkPath, nativeLibs)
            val language = when {
                languageInfo.languages.size > 1 -> {
                    val others = languageInfo.languages
                        .filter { it != languageInfo.primary }
                        .joinToString(", ") { it.displayName }
                    "${languageInfo.primary.displayName} & $others"
                }
                languageInfo.languages.size == 1 -> {
                    languageInfo.languages.first().displayName
                }
                else -> "Unknown"
            }

            val primaryAbi = NativeLibraryAnalyzer.getPrimaryAbi(nativeLibs)

            val is64Bit = primaryAbi.contains("64")

            val supportedAbis = NativeLibraryAnalyzer.getAbis(nativeLibs)

            val appName = appInfo.loadLabel(packageManager).toString()

            val apkSize = File(apkPath).length()

            val permissions = permissionAnalyzer.extractPermissions(packageName)

            val versionInfo = manifestAnalyzer.extractVersionInfo(packageName)
            val securityFlags = manifestAnalyzer.extractSecurityFlags(packageName)

            val detectedLibraries = dexAnalyzer.detectLibraries(apkPath)

            AnalysisResult(
                packageName,
                appName,
                nativeLibs,
                apkPath,
                apkSize,
                framework,
                language,
                primaryAbi,
                is64Bit,
                supportedAbis,
                permissions,
                versionInfo,
                securityFlags,
                detectedLibraries,
                frameworkInfo,
                languageInfo
            )
        }
    }
}