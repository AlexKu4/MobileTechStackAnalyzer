package com.example.mobiletechstack.domain.analyzer

import java.util.zip.ZipFile
import com.example.mobiletechstack.domain.model.LibraryInfo
import com.example.mobiletechstack.domain.model.FrameworkInfo
import com.example.mobiletechstack.domain.model.FrameworkType
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File


object FrameworkDetector {

    fun detectFrameworkDetailed(apkPath: String, nativeLibs: List<LibraryInfo>): FrameworkInfo {
        val dexClasses = extractDexClasses(apkPath)

        if (isFlutter(apkPath, dexClasses)) {
            return FrameworkInfo(type = FrameworkType.FLUTTER)
        }

        if (isUnity(apkPath, nativeLibs, dexClasses)) {
            return FrameworkInfo(type = FrameworkType.UNITY)
        }

        if (isReactNative(apkPath, dexClasses)) {
            return FrameworkInfo(type = FrameworkType.REACT_NATIVE)
        }

        if (isXamarin(apkPath, nativeLibs, dexClasses)) {
            return FrameworkInfo(type = FrameworkType.XAMARIN)
        }

        if (isKotlinMultiplatform(nativeLibs)) {
            return FrameworkInfo(type = FrameworkType.KOTLIN_MULTIPLATFORM)
        }

        if (isCordova(apkPath, dexClasses)) {
            val type = if (isIonic(apkPath)) {
                FrameworkType.IONIC
            } else {
                FrameworkType.CORDOVA
            }
            return FrameworkInfo(type = type)
        }

        if (!isNativeScript(apkPath, dexClasses)) {
            return FrameworkInfo(type = FrameworkType.NATIVE_ANDROID)
        }
        return FrameworkInfo(type = FrameworkType.NATIVE_SCRIPT)
    }

    private fun extractDexClasses(apkPath: String): Set<String> {
        val classes = mutableSetOf<String>()
        val tempDir = File(apkPath).parentFile ?: return emptySet()

        try {
            ZipFile(apkPath).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                    .take(3)
                    .forEach { entry ->
                        val tempFile = File(tempDir, "temp_${entry.name}")

                        try {
                            zip.getInputStream(entry).use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            val dex = DexFileFactory.loadDexFile(tempFile, Opcodes.getDefault())

                            dex.classes.forEach { classDef ->
                                val className = classDef.type
                                    .removePrefix("L")
                                    .removeSuffix(";")
                                    .replace('/', '.')

                                classes.add(className)
                            }

                        } finally {
                            tempFile.delete()
                        }
                    }
            }
        } catch (e: Exception) {
            return emptySet()
        }

        return classes
    }

    private fun isFlutter(apkPath: String, dexClasses: Set<String>): Boolean {
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

            val hasFlutterClasses = dexClasses.any { className ->
                className.startsWith("io.flutter.") ||
                        className.startsWith("io.flutter.embedding.") ||
                        className.startsWith("io.flutter.plugin.") ||
                        className.startsWith("io.flutter.view.") ||
                        className == "io.flutter.FlutterInjector"
            }

            return hasFlutterClasses ||
                    (hasFlutterAssets && hasLibFlutter) ||
                    hasKernelBlob ||
                    (hasIsolateSnapshot && hasVmSnapshot)
        }
    }

    private fun isReactNative(apkPath: String, dexClasses: Set<String>): Boolean {
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

            val hasFbjni = entries.any { it.name.contains("libfbjni.so") }
            val hasYoga = entries.any { it.name.contains("libyoga.so") }
            val hasReactNativeBlob = entries.any { it.name.contains("libreact") }
            val hasTurboModules = entries.any { it.name.contains("turbomodulejsijni") }

            val hasReactNativeClasses = dexClasses.any { className ->
                className.startsWith("com.facebook.react.") ||
                        className.startsWith("com.facebook.hermes.") ||
                        className == "com.facebook.react.ReactApplication" ||
                        className == "com.facebook.react.ReactPackage" ||
                        className == "com.facebook.react.bridge.ReactContext"
            }

            val hasStrongRnSignature = (hasFbjni && hasYoga) ||
                    (hasHermes && hasFbjni) ||
                    hasReactNativeBlob ||
                    hasTurboModules

            return hasReactNativeClasses ||
                    (hasJsBundle && (hasHermes || hasJsc)) ||
                    hasStrongRnSignature
        }
    }

    private fun isUnity(apkPath: String, nativeLibs: List<LibraryInfo>, dexClasses: Set<String>): Boolean {
        val hasLibUnity = nativeLibs.any { it.name.contains("libunity.so") }
        val hasLibMain = nativeLibs.any { it.name == "libmain.so" }
        val hasLibIl2cpp = nativeLibs.any { it.name.contains("libil2cpp.so") }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasUnityDefaultResources = entries.any {
                it.name.contains("unity default resources") ||
                        it.name.contains("unity_default_resources")
            }
            val hasUnityBuiltinExtra = entries.any { it.name.contains("unity_builtin_extra") }
            val hasGlobalMetadata = entries.any { it.name.contains("global-metadata.dat") }
            val hasUnityAssets = entries.any { it.name.startsWith("assets/bin/Data/") }

            // DEX классы Unity
            val hasUnityClasses = dexClasses.any { className ->
                className.startsWith("com.unity3d.") ||
                        className == "com.unity3d.player.UnityPlayer" ||
                        className == "com.unity3d.player.UnityPlayerActivity"
            }

            return hasUnityClasses ||
                    hasLibUnity ||
                    (hasUnityDefaultResources && hasUnityBuiltinExtra) ||
                    (hasLibIl2cpp && hasGlobalMetadata) ||
                    (hasLibMain && hasUnityAssets)
        }
    }

    private fun isXamarin(apkPath: String, nativeLibs: List<LibraryInfo>, dexClasses: Set<String>): Boolean {
        val hasMonodroid = nativeLibs.any { it.name.contains("libmonodroid.so") }
        val hasMonoSgen = nativeLibs.any { it.name.contains("libmonosgen") }
        val hasXamarinApp = nativeLibs.any { it.name.contains("libxamarin-app.so") }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasAssembliesFolder = entries.any { it.name.startsWith("assemblies/") }
            val hasXamarinAndroid = entries.any {
                it.name.contains("Xamarin.Android") ||
                        it.name.contains("Mono.Android")
            }

            val hasXamarinClasses = dexClasses.any { className ->
                className.startsWith("mono.") ||
                        className.startsWith("mono.android.") ||
                        className == "mono.MonoRuntimeProvider" ||
                        className == "mono.MonoPackageManager"
            }

            return hasXamarinClasses ||
                    hasMonodroid ||
                    (hasMonoSgen && hasAssembliesFolder) ||
                    (hasXamarinApp && hasXamarinAndroid)
        }
    }

    private fun isCordova(apkPath: String, dexClasses: Set<String>): Boolean {
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

            val hasCordovaClasses = dexClasses.any { className ->
                className.startsWith("org.apache.cordova.") ||
                        className == "org.apache.cordova.CordovaActivity" ||
                        className == "org.apache.cordova.CordovaPlugin" ||
                        className == "org.apache.cordova.CordovaWebView"
            }

            return hasCordovaClasses ||
                    hasCordovaJs ||
                    (hasWwwFolder && hasCordovaPlugins)
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

    private fun isNativeScript(apkPath: String, dexClasses: Set<String>): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasTnsJavaClasses = entries.any { it.name.contains("tns-java-classes.jar") }
            val hasTnsRuntime = entries.any { it.name.contains("tns_modules") }

            val hasNativeScriptClasses = dexClasses.any { className ->
                className.startsWith("com.tns.") ||
                        className == "com.tns.Runtime" ||
                        className == "com.tns.NativeScriptApplication"
            }

            return hasNativeScriptClasses ||
                    hasTnsJavaClasses ||
                    hasTnsRuntime
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