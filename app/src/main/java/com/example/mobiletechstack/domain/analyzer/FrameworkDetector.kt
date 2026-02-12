package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LibraryInfo
import java.util.zip.ZipFile

object FrameworkDetector {

    fun detectFramework(apkPath: String, libraries: List<LibraryInfo>): String {
        val libNames = libraries.map { it.name.lowercase() }

        // Flutter
        if (libNames.any { it.contains("libflutter.so") }) {
            return "Flutter"
        }

        // React Native
        if (libNames.any { it.contains("libreactnative") || it.contains("libhermes") }) {
            return "React Native"
        }

        // Unity
        if (libNames.any { it.contains("libunity.so") } ||
            libNames.any { it.contains("libil2cpp.so") }) {
            return "Unity"
        }

        // Xamarin
        if (libNames.any { it.contains("libmonodroid") || it.contains("libmonosgen") }) {
            return "Xamarin"
        }

        // Cocos2d
        if (libNames.any { it.contains("libcocos2d") }) {
            return "Cocos2d"
        }

        // Godot
        if (libNames.any { it.contains("libgodot") }) {
            return "Godot"
        }

        // Unreal Engine
        if (libNames.any { it.contains("libue4") || it.contains("libunreal") }) {
            return "Unreal Engine"
        }

        val assetsInfo = analyzeApkStructure(apkPath)

        // Flutter
        if (assetsInfo.hasFlutterAssets) {
            return "Flutter"
        }

        // React Native
        if (assetsInfo.hasReactNativeBundle) {
            return "React Native"
        }

        // Cordova/Ionic
        if (assetsInfo.hasCordova) {
            return "Cordova/Ionic"
        }

        // Unity
        if (assetsInfo.hasUnityAssets) {
            return "Unity"
        }

        // Xamarin
        if (assetsInfo.hasXamarinAssemblies) {
            return "Xamarin"
        }

        // Определяем Native Android (Kotlin vs Java)
        val usesKotlin = detectKotlin(apkPath)

        return if (usesKotlin) {
            "Native Android (Kotlin)"
        } else {
            "Native Android (Java)"
        }
    }

     private fun analyzeApkStructure(apkPath: String): ApkStructureInfo {
        try {
            ZipFile(apkPath).use { zipFile ->
                val entries = zipFile.entries().asSequence()
                    .map { it.name }
                    .toList()

                return ApkStructureInfo(
                    // Flutter
                    hasFlutterAssets = entries.any {
                        it.startsWith("assets/flutter_assets/") ||
                                it.contains("isolate_snapshot_data") ||
                                it.contains("vm_snapshot_data")
                    },

                    // React Native
                    hasReactNativeBundle = entries.any {
                        it.contains("index.android.bundle") ||
                                it.contains("assets/index.android.js")
                    },

                    // Cordova/Ionic
                    hasCordova = entries.any {
                        it.startsWith("assets/www/cordova") ||
                                it.contains("cordova.js")
                    },

                    // Unity
                    hasUnityAssets = entries.any {
                        it.startsWith("assets/bin/Data/") ||
                                it.contains("resources.assets")
                    },

                    // Xamarin
                    hasXamarinAssemblies = entries.any {
                        it.startsWith("assets/assemblies/") ||
                                it.contains("Mono.Android.dll")
                    }
                )
            }
        } catch (e: Exception) {
            return ApkStructureInfo()
        }
    }

    private fun detectKotlin(apkPath: String): Boolean {
        try {
            ZipFile(apkPath).use { zipFile ->
                val entries = zipFile.entries().asSequence()
                    .map { it.name }
                    .toList()

                if (entries.any { it.startsWith("kotlin/") }) {
                    return true
                }

                if (entries.any {
                        it.contains("kotlin", ignoreCase = true) &&
                                it.endsWith(".kotlin_module")
                    }) {
                    return true
                }

                return false
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun detectLanguage(apkPath: String): String {
        return if (detectKotlin(apkPath)) "Kotlin" else "Java"
    }
}

private data class ApkStructureInfo(
    val hasFlutterAssets: Boolean = false,
    val hasReactNativeBundle: Boolean = false,
    val hasCordova: Boolean = false,
    val hasUnityAssets: Boolean = false,
    val hasXamarinAssemblies: Boolean = false
)