package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LibraryInfo
import java.util.zip.ZipFile
import com.example.mobiletechstack.domain.model.FrameworkInfo
import com.example.mobiletechstack.domain.model.FrameworkType


object FrameworkDetector {

    fun detectFrameworkDetailed(apkPath: String, nativeLibs: List<LibraryInfo>): FrameworkInfo {

        if (isFlutter(apkPath)) {
            return FrameworkInfo(type = FrameworkType.FLUTTER)
        }

        if (isReactNative(apkPath)) {
            return FrameworkInfo(type = FrameworkType.REACT_NATIVE)
        }

        if (isXamarin(apkPath, nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.XAMARIN)
        }

        if (isUnity(apkPath, nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.UNITY)
        }

        if (isCordova(apkPath)) {
            val type = if (isIonic(apkPath)) {
                FrameworkType.IONIC
            } else {
                FrameworkType.CORDOVA
            }
            return FrameworkInfo(type = type)
        }

         if (isKotlinMultiplatform(nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.KOTLIN_MULTIPLATFORM)
        }

        if (isNativeScript(apkPath)) {
            return FrameworkInfo(type = FrameworkType.NATIVE_SCRIPT)
        }

        return FrameworkInfo(type = FrameworkType.NATIVE_ANDROID)
    }

    private fun isFlutter(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            return entries.any { it.name.startsWith("flutter_assets/") } ||
                    entries.any { it.name.contains("libflutter.so") }
        }
    }

    private fun isReactNative(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            return entries.any {
                it.name.contains("index.android.bundle") ||
                        it.name.contains("index.bundle")
            } || entries.any { it.name.contains("libreactnativejni.so") }
        }
    }

    private fun isXamarin(apkPath: String, nativeLibs: List<LibraryInfo>): Boolean {
        if (nativeLibs.any { it.name.contains("libmonodroid.so") }) {
            return true
        }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()
            return entries.any { it.name.startsWith("assemblies/") }
        }
    }

    private fun isUnity(apkPath: String, nativeLibs: List<LibraryInfo>): Boolean {
        if (nativeLibs.any { it.name.contains("libunity.so") }) {
            return true
        }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()
            return entries.any {
                it.name.contains("unity default resources") ||
                        it.name.contains("unity_builtin_extra")
            }
        }
    }

    private fun isCordova(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            return entries.any { it.name.contains("cordova.js") } ||
                    entries.any { it.name.startsWith("assets/www/") }
        }
    }

    private fun isIonic(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            return zip.entries().toList().any {
                it.name.contains("ionic.js") || it.name.contains("@ionic")
            }
        }
    }

    private fun isKotlinMultiplatform(nativeLibs: List<LibraryInfo>): Boolean {
        return nativeLibs.any { it.name.contains("libskiko") }
    }

    private fun isNativeScript(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()
            return entries.any { it.name.contains("tns-java-classes.jar") }
        }
    }

    fun detectFramework(apkPath: String, nativeLibs: List<LibraryInfo>): String {
        val frameworkInfo = detectFrameworkDetailed(apkPath, nativeLibs)
        return frameworkInfo.type.displayName
    }

    fun detectLanguage(apkPath: String): String {
        val hasKotlin = hasKotlinClasses(apkPath)
        val hasJava = hasJavaClasses(apkPath)

        return when {
            hasKotlin && hasJava -> "Kotlin & Java"
            hasKotlin -> "Kotlin"
            hasJava -> "Java"
            else -> "Unknown"
        }
    }

    private fun hasKotlinClasses(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            return zip.entries().toList().any {
                it.name.contains("kotlin") || it.name.contains("META-INF/kotlin")
            }
        }
    }

    private fun hasJavaClasses(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            return zip.entries().toList().any {
                it.name.matches(Regex("classes\\d*\\.dex"))
            }
        }
    }
}