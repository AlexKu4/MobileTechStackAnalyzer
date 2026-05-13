package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.PatternRepository
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class APKAnalyzer(private val context: Context) {

    private val dexClassExtractor = DexClassExtractor(context)
    private val permissionAnalyzer = PermissionAnalyzer(context)
    private val manifestAnalyzer = ManifestAnalyzer(context)
    private val dexAnalyzer = DexAnalyzer(
        context,
        PatternRepository(context, AppDatabase.getInstance(context).libraryPatternDao())
    )
    private val languageDetector = LanguageDetector(dexClassExtractor)
    private val frameworkDetector = FrameworkDetector(dexClassExtractor)

    suspend fun analyzeApp(packageName: String): AnalysisResult = coroutineScope{
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val apkPath = appInfo.sourceDir

            val nativeLibsDeferred = async { NativeLibraryAnalyzer.extractNativeLibraries(apkPath) }
            val dexClassesDeferred = async { dexClassExtractor.extractAllClassNames(apkPath) }
            val versionInfoDeferred = async { manifestAnalyzer.extractVersionInfo(packageName) }
            val securityFlagsDeferred = async { manifestAnalyzer.extractSecurityFlags(packageName) }
            val permissionsDeferred = async { permissionAnalyzer.extractPermissions(packageName) }

            val nativeLibs = nativeLibsDeferred.await()
            val dexClasses = dexClassesDeferred.await()

            val obfuscationDeferred = async { ObfuscationDetector().hasObfuscation(apkPath, dexClasses) }
            val frameworkInfoDeferred = async { frameworkDetector.detectFrameworkDetailed(apkPath, nativeLibs, dexClasses) }
            val languageInfoDeferred = async { languageDetector.detectLanguagesDetailed(apkPath, nativeLibs, dexClasses) }
            val detectionResultDeferred = async { dexAnalyzer.detectLibrariesAndUnknown(apkPath, dexClasses) }

            val frameworkInfo = frameworkInfoDeferred.await()
            val framework = frameworkInfo.type.displayName

            val languageInfo = languageInfoDeferred.await()
            val language = when {
                languageInfo.languages.size > 1 -> {
                    val others = languageInfo.languages
                        .filter { it != languageInfo.primary }
                        .joinToString(", ") { it.displayName }
                    "${languageInfo.primary.displayName} & $others"
                }
                languageInfo.languages.size == 1 -> languageInfo.languages.first().displayName
                else -> "Unknown"
            }

            val detectionResult = detectionResultDeferred.await()
            val detectedLibraries = detectionResult.detectedLibraries
            val unknownPackages = detectionResult.unknownPackages
            val versionInfo = versionInfoDeferred.await()
            val securityFlags = securityFlagsDeferred.await()
            val permissions = permissionsDeferred.await()

            val primaryAbi = NativeLibraryAnalyzer.getPrimaryAbi(nativeLibs)
            val is64Bit = primaryAbi.contains("64")
            val supportedAbis = NativeLibraryAnalyzer.getAbis(nativeLibs)
            val appName = appInfo.loadLabel(packageManager).toString()
            val apkSize = File(apkPath).length()
            val hasObfuscation = obfuscationDeferred.await()

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
                unknownPackages,
                frameworkInfo,
                languageInfo,
                hasObfuscation
            )
        }
    }

    suspend fun analyzeExternalApk(filePath: String, displayName: String): AnalysisResult = coroutineScope {
        withContext(Dispatchers.IO) {
            // packageName тянем напрямую из манифеста, обходя PackageManager
            val packageName = ManifestAnalyzer.extractPackageName(filePath)
                ?: "external.${displayName.lowercase().replace(Regex("[^a-z0-9]"), "_")}"

            val nativeLibsDeferred = async { NativeLibraryAnalyzer.extractNativeLibraries(filePath) }
            val dexClassesDeferred = async { dexClassExtractor.extractAllClassNames(filePath) }
            val versionInfoDeferred = async { manifestAnalyzer.extractVersionInfoFromFile(filePath) }
            val securityFlagsDeferred = async { manifestAnalyzer.extractSecurityFlagsFromFile(filePath) }
            val permissionsDeferred = async { permissionAnalyzer.extractPermissionsFromFile(filePath) }

            val nativeLibs = nativeLibsDeferred.await()
            val dexClasses = dexClassesDeferred.await()

            val obfuscationDeferred = async { ObfuscationDetector().hasObfuscation(filePath, dexClasses) }
            val frameworkInfoDeferred = async { frameworkDetector.detectFrameworkDetailed(filePath, nativeLibs, dexClasses) }
            val languageInfoDeferred = async { languageDetector.detectLanguagesDetailed(filePath, nativeLibs, dexClasses) }
            val detectionResultDeferred = async { dexAnalyzer.detectLibrariesAndUnknown(filePath, dexClasses) }

            val frameworkInfo = frameworkInfoDeferred.await()
            val framework = frameworkInfo.type.displayName

            val languageInfo = languageInfoDeferred.await()
            val language = when {
                languageInfo.languages.size > 1 -> {
                    val others = languageInfo.languages
                        .filter { it != languageInfo.primary }
                        .joinToString(", ") { it.displayName }
                    "${languageInfo.primary.displayName} & $others"
                }
                languageInfo.languages.size == 1 -> languageInfo.languages.first().displayName
                else -> "Unknown"
            }

            val detectionResult = detectionResultDeferred.await()
            val detectedLibraries = detectionResult.detectedLibraries
            val unknownPackages = detectionResult.unknownPackages
            val versionInfo = versionInfoDeferred.await()
            val securityFlags = securityFlagsDeferred.await()
            val permissions = permissionsDeferred.await()

            val primaryAbi = NativeLibraryAnalyzer.getPrimaryAbi(nativeLibs)
            val is64Bit = primaryAbi.contains("64")
            val supportedAbis = NativeLibraryAnalyzer.getAbis(nativeLibs)
            val apkSize = File(filePath).length()
            val hasObfuscation = obfuscationDeferred.await()

            AnalysisResult(
                packageName = packageName,
                appName = displayName,
                nativeLibraries = nativeLibs,
                apkPath = filePath,
                apkSize = apkSize,
                framework = framework,
                language = language,
                primaryAbi = primaryAbi,
                is64Bit = is64Bit,
                supportedAbis = supportedAbis,
                permissions = permissions,
                versionInfo = versionInfo,
                securityFlags = securityFlags,
                detectedLibraries = detectedLibraries,
                unknownPackages = unknownPackages,
                frameworkInfo = frameworkInfo,
                languageInfo = languageInfo,
                hasObfuscation = hasObfuscation,
                isExternal = true
            )
        }
    }
}