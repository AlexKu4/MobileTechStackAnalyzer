package com.example.mobiletechstack.domain.analyzer

import java.util.zip.ZipFile
import com.example.mobiletechstack.domain.model.LibraryInfo
import com.example.mobiletechstack.domain.model.FrameworkInfo
import com.example.mobiletechstack.domain.model.FrameworkType


object FrameworkDetector {

    fun detectFrameworkDetailed(apkPath: String, nativeLibs: List<LibraryInfo>): FrameworkInfo {

        if (isFlutter(apkPath)) {
            return FrameworkInfo(type = FrameworkType.FLUTTER)
        }

        if (isUnity(apkPath, nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.UNITY)
        }

        if (isReactNative(apkPath)) {
            return FrameworkInfo(type = FrameworkType.REACT_NATIVE)
        }

        if (isXamarin(apkPath, nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.XAMARIN)
        }

        if (isKotlinMultiplatform(nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.KOTLIN_MULTIPLATFORM)
        }

        if (isCordova(apkPath)) {
            val type = if (isIonic(apkPath)) {
                FrameworkType.IONIC
            } else {
                FrameworkType.CORDOVA
            }
            return FrameworkInfo(type = type)
        }

        if (isNativeScript(apkPath)) {
            return FrameworkInfo(type = FrameworkType.NATIVE_SCRIPT)
        }

        return FrameworkInfo(type = FrameworkType.NATIVE_ANDROID)
    }

    private fun isFlutter(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasFlutterAssets = entries.any { it.name.startsWith("flutter_assets/") }
            val hasLibFlutter = entries.any { it.name.contains("libflutter.so") }

            val hasKernelBlob = entries.any { it.name.contains("kernel_blob.bin") }
            val hasIsolateSnapshot = entries.any {
                it.name.contains("isolate_snapshot_data") ||
                        it.name.contains("isolate_snapshot_instr")
            }
            val hasVmSnapshot = entries.any {
                it.name.contains("vm_snapshot_data") ||
                        it.name.contains("vm_snapshot_instr")
            }
            val hasFlutterIcudtl = entries.any { it.name.contains("icudtl.dat") }
            val hasFlutterEmbedding = entries.any { it.name.contains("io/flutter/") }

            return (hasFlutterAssets && hasLibFlutter) ||
                    hasKernelBlob ||
                    (hasIsolateSnapshot && hasVmSnapshot) ||
                    (hasFlutterAssets && hasFlutterIcudtl && hasFlutterEmbedding)
        }
    }

    private fun isReactNative(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasJsBundle = entries.any {
                it.name.contains("index.android.bundle") ||
                        it.name.contains("index.bundle") ||
                        it.name.endsWith(".bundle")
            }
            val hasReactNativeJni = entries.any { it.name.contains("libreactnativejni.so") }

            val hasHermes = entries.any { it.name.contains("libhermes.so") }
            val hasJsc = entries.any {
                it.name.contains("libjsc.so") ||
                        it.name.contains("libjscexecutor.so")
            }

            val hasReactNativeBlob = entries.any { it.name.contains("libreact") }
            val hasFbjni = entries.any { it.name.contains("libfbjni.so") }
            val hasYoga = entries.any { it.name.contains("libyoga.so") }
            val hasTurboModules = entries.any { it.name.contains("turbomodulejsijni") }

            return (hasJsBundle && (hasReactNativeJni || hasHermes || hasJsc)) ||
                    (hasReactNativeBlob && hasFbjni && hasYoga) ||
                    (hasHermes && hasFbjni)
        }
    }

    private fun isUnity(apkPath: String, nativeLibs: List<LibraryInfo>): Boolean {
        val hasLibUnity = nativeLibs.any { it.name.contains("libunity.so") }
        val hasLibMain = nativeLibs.any { it.name == "libmain.so" }
        val hasLibIl2cpp = nativeLibs.any { it.name.contains("libil2cpp.so") }
        val hasLibMono = nativeLibs.any {
            it.name.contains("libmonobdwgc") ||
                    it.name.contains("libmono-native")
        }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasUnityDefaultResources = entries.any {
                it.name.contains("unity default resources") ||
                        it.name.contains("unity_default_resources")
            }
            val hasUnityBuiltinExtra = entries.any { it.name.contains("unity_builtin_extra") }
            val hasGlobalMetadata = entries.any { it.name.contains("global-metadata.dat") }

            val hasUnityAssets = entries.any { it.name.startsWith("assets/bin/Data/") }
            val hasDataFolder = entries.any { it.name.contains("assets/bin/Data/") }
            val hasLevelFiles = entries.any {
                it.name.contains("level") && it.name.contains("assets/bin/Data/")
            }

            return hasLibUnity ||
                    (hasUnityDefaultResources && hasUnityBuiltinExtra) ||
                    (hasLibIl2cpp && hasGlobalMetadata) ||
                    (hasLibMain && hasUnityAssets && hasDataFolder) ||
                    (hasLibMono && hasUnityAssets)
        }
    }

    private fun isXamarin(apkPath: String, nativeLibs: List<LibraryInfo>): Boolean {
        val hasMonodroid = nativeLibs.any { it.name.contains("libmonodroid.so") }
        val hasMonoSgen = nativeLibs.any { it.name.contains("libmonosgen") }
        val hasMonoPosixHelper = nativeLibs.any { it.name.contains("libMonoPosixHelper.so") }
        val hasXamarinApp = nativeLibs.any { it.name.contains("libxamarin-app.so") }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasAssembliesFolder = entries.any { it.name.startsWith("assemblies/") }
            val hasXamarinAndroid = entries.any {
                it.name.contains("Xamarin.Android") ||
                        it.name.contains("Mono.Android")
            }
            val hasDllFiles = entries.any { it.name.endsWith(".dll") }

            val hasAssembliesBlob = entries.any { it.name.contains("assemblies.blob") }
            val hasTypemapIndex = entries.any { it.name.contains("typemap.index") }

            return hasMonodroid ||
                    (hasMonoSgen && hasAssembliesFolder) ||
                    (hasMonoSgen && hasXamarinAndroid) ||
                    (hasXamarinApp && hasDllFiles) ||
                    (hasMonoPosixHelper && hasAssembliesBlob && hasTypemapIndex)
        }
    }

    private fun isCordova(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasCordovaJs = entries.any {
                it.name.contains("cordova.js") ||
                        it.name.contains("cordova-js-src")
            }
            val hasCordovaPlugins = entries.any {
                it.name.contains("cordova_plugins.js") ||
                        it.name.contains("cordova-plugin")
            }

            val hasWwwFolder = entries.any { it.name.startsWith("assets/www/") }
            val hasIndexHtml = entries.any {
                it.name == "assets/www/index.html" ||
                        it.name.contains("www/index.html")
            }

            val hasConfigXml = entries.any { it.name.contains("config.xml") }
            val hasCordovaConfig = entries.any { it.name.contains("res/xml/config.xml") }

            return hasCordovaJs ||
                    (hasWwwFolder && hasCordovaPlugins) ||
                    (hasIndexHtml && hasCordovaConfig) ||
                    (hasWwwFolder && hasConfigXml)
        }
    }

    private fun isIonic(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasIonicJs = entries.any { it.name.contains("ionic.js") }
            val hasIonicBundle = entries.any {
                it.name.contains("ionic.bundle") ||
                        it.name.contains("ionic-angular")
            }
            val hasIonicPackage = entries.any { it.name.contains("@ionic/") }
            val hasCapacitor = entries.any {
                it.name.contains("capacitor") ||
                        it.name.contains("@capacitor/")
            }

            return hasIonicJs || hasIonicBundle || hasIonicPackage || hasCapacitor
        }
    }

    private fun isKotlinMultiplatform(nativeLibs: List<LibraryInfo>): Boolean {
        val hasSkiko = nativeLibs.any { it.name.contains("libskiko") }

        val hasKotlinNative = nativeLibs.any {
            it.name.contains("libknbinary.so") ||
                    it.name.contains("libknbindings.so")
        }

        return hasSkiko || hasKotlinNative
    }

    private fun isNativeScript(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasTnsJavaClasses = entries.any { it.name.contains("tns-java-classes.jar") }
            val hasTnsRuntime = entries.any { it.name.contains("tns_modules") }

            val hasAppFolder = entries.any { it.name.startsWith("assets/app/") }
            val hasPackageJson = entries.any { it.name.contains("assets/app/package.json") }
            val hasNativeScriptConfig = entries.any { it.name.contains("nsconfig.json") }

            return hasTnsJavaClasses ||
                    hasTnsRuntime ||
                    (hasAppFolder && hasPackageJson) ||
                    hasNativeScriptConfig
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