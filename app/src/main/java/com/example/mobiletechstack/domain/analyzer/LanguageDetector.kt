package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LanguageInfo
import com.example.mobiletechstack.domain.model.ProgrammingLanguage
import com.example.mobiletechstack.domain.model.LibraryInfo
import java.util.zip.ZipFile


object LanguageDetector {

    fun detectLanguagesDetailed(apkPath: String, nativeLibs: List<LibraryInfo>): LanguageInfo {
        val detectedLanguages = mutableSetOf<ProgrammingLanguage>()

        if (hasKotlin(apkPath)) {
            detectedLanguages.add(ProgrammingLanguage.KOTLIN)
        }

        if (hasJava(apkPath)) {
            detectedLanguages.add(ProgrammingLanguage.JAVA)
        }

        if (hasCpp(nativeLibs)) {
            detectedLanguages.add(ProgrammingLanguage.CPP)
        }

        if (hasC(nativeLibs)) {
            detectedLanguages.add(ProgrammingLanguage.C)
        }

        if (hasJavaScript(apkPath)) {
            detectedLanguages.add(ProgrammingLanguage.JAVASCRIPT)
        }

        if (hasDart(apkPath)) {
            detectedLanguages.add(ProgrammingLanguage.DART)
        }

        if (hasCSharp(apkPath, nativeLibs)) {
            detectedLanguages.add(ProgrammingLanguage.CSHARP)
        }

        if (hasPython(apkPath)) {
            detectedLanguages.add(ProgrammingLanguage.PYTHON)
        }

        val primary = determinePrimaryLanguage(detectedLanguages.toList(), apkPath)

        return LanguageInfo(
            languages = detectedLanguages.toList().sortedBy { it.displayName },
            primary = primary
        )
    }

    private fun hasKotlin(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasKotlinMetadata = entries.any {
                it.name.contains("META-INF/kotlin") ||
                        it.name.contains(".kotlin_module")
            }

            val hasKotlinStdlib = entries.any {
                it.name.contains("kotlin-stdlib") ||
                        it.name.contains("kotlin/")
            }

            val hasKotlinx = entries.any {
                it.name.contains("kotlinx/")
            }

            return hasKotlinMetadata || hasKotlinStdlib || hasKotlinx
        }
    }

    private fun hasJava(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()
            val hasDex = entries.any { it.name.matches(Regex("classes\\d*\\.dex")) }
            val hasJavaPackages = entries.any {
                it.name.startsWith("java/") ||
                        it.name.startsWith("javax/")
            }

            return hasDex || hasJavaPackages
        }
    }

    private fun hasCpp(nativeLibs: List<LibraryInfo>): Boolean {
        val hasCppStdlib = nativeLibs.any {
            it.name.contains("libc++_shared.so") ||
                    it.name.contains("libstdc++.so")
        }

        val hasGnuStl = nativeLibs.any {
            it.name.contains("libgnustl")
        }

        return hasCppStdlib || hasGnuStl
    }

    private fun hasC(nativeLibs: List<LibraryInfo>): Boolean {

        return nativeLibs.any {
            it.name.endsWith(".so") &&
                    !it.name.contains("c++") &&
                    !it.name.contains("stdc++")
        }
    }

    private fun hasJavaScript(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasJsBundle = entries.any {
                it.name.contains(".bundle") ||
                        it.name.contains(".js") ||
                        it.name.contains("assets/index.android.bundle")
            }
            val hasCordovaJs = entries.any {
                it.name.contains("cordova.js")
            }

            return hasJsBundle || hasCordovaJs
        }
    }

    private fun hasDart(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasFlutterAssets = entries.any {
                it.name.startsWith("flutter_assets/")
            }

            val hasDartKernel = entries.any {
                it.name.contains("kernel_blob.bin")
            }

            val hasDartSnapshot = entries.any {
                it.name.contains("isolate_snapshot") ||
                        it.name.contains("vm_snapshot")
            }

            return hasFlutterAssets || hasDartKernel || hasDartSnapshot
        }
    }

    private fun hasCSharp(apkPath: String, nativeLibs: List<LibraryInfo>): Boolean {
        val hasMonoRuntime = nativeLibs.any {
            it.name.contains("libmonodroid.so") ||
                    it.name.contains("libmonosgen")
        }

        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasAssemblies = entries.any {
                it.name.endsWith(".dll") ||
                        it.name.startsWith("assemblies/")
            }

            return hasMonoRuntime || hasAssemblies
        }
    }

    private fun hasPython(apkPath: String): Boolean {
        ZipFile(apkPath).use { zip ->
            val entries = zip.entries().toList()

            val hasPythonStdlib = entries.any {
                it.name.contains("python") ||
                        it.name.contains(".py") ||
                        it.name.contains(".pyc")
            }

            val hasKivy = entries.any {
                it.name.contains("kivy")
            }

            val hasChaquopy = entries.any {
                it.name.contains("chaquopy")
            }

            return hasPythonStdlib || hasKivy || hasChaquopy
        }
    }

    private fun determinePrimaryLanguage(
        languages: List<ProgrammingLanguage>,
        apkPath: String
    ): ProgrammingLanguage {
        if (languages.isEmpty()) {
            return ProgrammingLanguage.UNKNOWN
        }

        if (languages.contains(ProgrammingLanguage.DART)) {
            return ProgrammingLanguage.DART
        }

        if (languages.contains(ProgrammingLanguage.JAVASCRIPT)) {
            return ProgrammingLanguage.JAVASCRIPT
        }

        if (languages.contains(ProgrammingLanguage.CSHARP)) {
            return ProgrammingLanguage.CSHARP
        }

        if (languages.contains(ProgrammingLanguage.PYTHON)) {
            return ProgrammingLanguage.PYTHON
        }
        val hasKotlin = languages.contains(ProgrammingLanguage.KOTLIN)
        val hasJava = languages.contains(ProgrammingLanguage.JAVA)

        if (hasKotlin && !hasJava) {
            return ProgrammingLanguage.KOTLIN
        }

        if (hasKotlin && hasJava) {
            ZipFile(apkPath).use { zip ->
                val hasKotlinMetadata = zip.entries().toList().any {
                    it.name.contains("META-INF/kotlin")
                }
                return if (hasKotlinMetadata) {
                    ProgrammingLanguage.KOTLIN
                } else {
                    ProgrammingLanguage.JAVA
                }
            }
        }

        if (hasJava) {
            return ProgrammingLanguage.JAVA  // Pure Java
        }

        if (languages.contains(ProgrammingLanguage.CPP)) {
            return ProgrammingLanguage.CPP
        }

        if (languages.contains(ProgrammingLanguage.C)) {
            return ProgrammingLanguage.C
        }

        return languages.first()
    }

    fun detectLanguage(apkPath: String): String {
        val languageInfo = detectLanguagesDetailed(apkPath, emptyList())

        return when {
            languageInfo.languages.size > 1 -> {
                val others = languageInfo.languages
                    .filter { it != languageInfo.primary }
                    .joinToString(", ") { it.displayName }

                if (others.isNotEmpty()) {
                    "${languageInfo.primary.displayName} & $others"
                } else {
                    languageInfo.primary.displayName
                }
            }
            languageInfo.languages.size == 1 -> {
                languageInfo.languages.first().displayName
            }
            else -> "Unknown"
        }
    }
}