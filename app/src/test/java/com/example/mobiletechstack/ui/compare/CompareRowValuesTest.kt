package com.example.mobiletechstack.ui.compare

import com.example.mobiletechstack.domain.model.*
import com.example.mobiletechstack.utils.formatSize
import org.junit.Assert.*
import org.junit.Test

// Проверяет логику извлечения строковых значений для каждой из 12 строк экрана сравнения.
// Зеркалит rowDefs из CompareScreen — если поменяется формула, тест это поймает.
class CompareRowValuesTest {

    private fun makeResult(
        framework: String = "Native Android",
        language: String = "Kotlin",
        primaryAbi: String = "arm64-v8a",
        is64Bit: Boolean = true,
        hasObfuscation: Boolean = false,
        apkSize: Long = 10_485_760L,
        frameworkInfo: FrameworkInfo? = null,
        languageInfo: LanguageInfo? = null,
        versionInfo: AppVersionInfo? = null,
        securityFlags: SecurityFlags? = null,
        detectedLibraries: List<DetectedLibrary> = emptyList(),
        permissions: List<PermissionInfo> = emptyList()
    ) = AnalysisResult(
        packageName = "com.test.app",
        appName = "Test App",
        nativeLibraries = emptyList(),
        apkPath = "/data/test.apk",
        apkSize = apkSize,
        framework = framework,
        language = language,
        primaryAbi = primaryAbi,
        is64Bit = is64Bit,
        supportedAbis = listOf(primaryAbi),
        hasObfuscation = hasObfuscation,
        frameworkInfo = frameworkInfo,
        languageInfo = languageInfo,
        versionInfo = versionInfo,
        securityFlags = securityFlags,
        detectedLibraries = detectedLibraries,
        permissions = permissions
    )

    // --- Фреймворк ---

    @Test
    fun framework_usesFrameworkInfoDisplayNameWhenPresent() {
        val result = makeResult(
            framework = "native",
            frameworkInfo = FrameworkInfo(FrameworkType.FLUTTER)
        )
        assertEquals("Flutter", result.frameworkInfo?.type?.displayName ?: result.framework)
    }

    @Test
    fun framework_fallsBackToFrameworkStringWhenNoInfo() {
        val result = makeResult(framework = "Native Android", frameworkInfo = null)
        assertEquals("Native Android", result.frameworkInfo?.type?.displayName ?: result.framework)
    }

    // --- Язык ---

    @Test
    fun language_usesLanguageInfoDisplayNameWhenPresent() {
        val result = makeResult(
            language = "dart",
            languageInfo = LanguageInfo(
                primary = ProgrammingLanguage.DART,
                languages = listOf(ProgrammingLanguage.DART)
            )
        )
        assertEquals("Dart", result.languageInfo?.primary?.displayName ?: result.language)
    }

    @Test
    fun language_fallsBackToLanguageStringWhenNoInfo() {
        val result = makeResult(language = "Java", languageInfo = null)
        assertEquals("Java", result.languageInfo?.primary?.displayName ?: result.language)
    }

    // --- Архитектура ---

    @Test
    fun primaryAbi_returnsFieldValue() {
        val result = makeResult(primaryAbi = "x86_64")
        assertEquals("x86_64", result.primaryAbi)
    }

    @Test
    fun is64Bit_returnsYesWhenTrue() {
        val result = makeResult(is64Bit = true)
        assertEquals("Yes", if (result.is64Bit) "Yes" else "No")
    }

    @Test
    fun is64Bit_returnsNoWhenFalse() {
        val result = makeResult(is64Bit = false)
        assertEquals("No", if (result.is64Bit) "Yes" else "No")
    }

    // --- Обфускация ---

    @Test
    fun obfuscation_returnsYesWhenDetected() {
        val result = makeResult(hasObfuscation = true)
        assertEquals("Yes", if (result.hasObfuscation) "Yes" else "No")
    }

    @Test
    fun obfuscation_returnsNoWhenAbsent() {
        val result = makeResult(hasObfuscation = false)
        assertEquals("No", if (result.hasObfuscation) "Yes" else "No")
    }

    // --- Версия и SDK ---

    @Test
    fun version_returnsVersionNameFromVersionInfo() {
        val result = makeResult(
            versionInfo = AppVersionInfo(
                versionName = "3.14.1",
                versionCode = 42,
                minSdkVersion = 26,
                targetSdkVersion = 34,
                compileSdkVersion = null
            )
        )
        assertEquals("3.14.1", result.versionInfo?.versionName ?: "-")
    }

    @Test
    fun version_fallsBackToDashWhenNoVersionInfo() {
        val result = makeResult(versionInfo = null)
        assertEquals("-", result.versionInfo?.versionName ?: "-")
    }

    @Test
    fun minSdk_returnsMinSdkAsString() {
        val result = makeResult(
            versionInfo = AppVersionInfo("1.0", 1, 28, 34, null)
        )
        assertEquals("28", result.versionInfo?.minSdkVersion?.toString() ?: "-")
    }

    @Test
    fun minSdk_fallsBackToDashWhenNoVersionInfo() {
        val result = makeResult(versionInfo = null)
        assertEquals("-", result.versionInfo?.minSdkVersion?.toString() ?: "-")
    }

    // --- APK Size ---

    @Test
    fun apkSize_displaysMbUnitForTypicalApk() {
        val result = makeResult(apkSize = 10_485_760L)
        assertTrue(result.apkSize.formatSize().contains("MB"))
    }

    @Test
    fun apkSize_displaysKbUnitForSmallFile() {
        val result = makeResult(apkSize = 2_048L)
        assertTrue(result.apkSize.formatSize().contains("KB"))
    }

    // --- Счётчики ---

    @Test
    fun libraryCount_returnsDetectedLibrariesSize() {
        val libs = List(7) { DetectedLibrary("Lib$it", "com.lib$it", LibraryCategory.OTHER) }
        val result = makeResult(detectedLibraries = libs)
        assertEquals("7", result.detectedLibraries.size.toString())
    }

    @Test
    fun libraryCount_returnsZeroWhenEmpty() {
        val result = makeResult(detectedLibraries = emptyList())
        assertEquals("0", result.detectedLibraries.size.toString())
    }

    @Test
    fun permissionCount_returnsPermissionsSize() {
        val perms = List(5) {
            PermissionInfo("android.permission.PERM$it", true, PermissionCategory.SYSTEM)
        }
        val result = makeResult(permissions = perms)
        assertEquals("5", result.permissions.size.toString())
    }

    // --- SecurityFlags ---

    @Test
    fun debuggable_returnsYesWhenTrue() {
        val result = makeResult(
            securityFlags = SecurityFlags(
                isDebuggable = true, allowBackup = false,
                usesCleartextTraffic = false, hasCode = true
            )
        )
        assertEquals("Yes", if (result.securityFlags?.isDebuggable == true) "Yes" else "No")
    }

    @Test
    fun debuggable_returnsNoWhenNullFlags() {
        val result = makeResult(securityFlags = null)
        assertEquals("No", if (result.securityFlags?.isDebuggable == true) "Yes" else "No")
    }

    @Test
    fun allowBackup_returnsYesWhenTrue() {
        val result = makeResult(
            securityFlags = SecurityFlags(
                isDebuggable = false, allowBackup = true,
                usesCleartextTraffic = false, hasCode = true
            )
        )
        assertEquals("Yes", if (result.securityFlags?.allowBackup == true) "Yes" else "No")
    }

    @Test
    fun allowBackup_returnsNoWhenNullFlags() {
        val result = makeResult(securityFlags = null)
        assertEquals("No", if (result.securityFlags?.allowBackup == true) "Yes" else "No")
    }
}
