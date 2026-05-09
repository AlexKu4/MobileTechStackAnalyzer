package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.data.repository.PatternRepository
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryCategory
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import timber.log.Timber
import java.io.File
import java.util.zip.ZipFile


class DexAnalyzer(private val context: Context, private val patternRepository: PatternRepository) {

    private var cachedPatterns: Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>>? = null

    private suspend fun getOrLoadPatterns(): Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>> =
        cachedPatterns ?: patternRepository.getPatterns().also { cachedPatterns = it }

    suspend fun detectLibraries(apkPath: String, dexClasses: Set<String>): List<DetectedLibrary> {
        return try {
            val detectedLibraries = mutableSetOf<DetectedLibrary>()
            val allPatterns = getOrLoadPatterns()
            dexClasses.forEach { className ->
                allPatterns.forEach { (category, patterns) ->
                    patterns.forEach { pattern ->
                        if (className.startsWith(pattern.packagePattern)) {
                            detectedLibraries.add(
                                DetectedLibrary(
                                    name = pattern.libraryName,
                                    packageName = pattern.packagePattern,
                                    category = category
                                )
                            )
                        }
                    }
                }
            }
            detectedLibraries.toList().sortedBy { it.name }
        } catch (e: Exception) {
            Timber.e(e, "Failed to detect libraries from APK: $apkPath")
            emptyList()
        }
    }

    private fun extractDexFiles(apkPath: String): List<File> {
        val dexFiles = mutableListOf<File>()
        val tempDir = context.cacheDir

        ZipFile(apkPath).use { zipFile ->
            zipFile.entries().asSequence()
                .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                .forEach { entry ->
                    val tempFile = File(tempDir, entry.name)

                    zipFile.getInputStream(entry).use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    dexFiles.add(tempFile)
                }
        }

        return dexFiles
    }

    private fun analyzeDexFile(dexFile: File): List<DetectedLibrary> {
        val detectedLibraries = mutableSetOf<DetectedLibrary>()

        try {
            val dex = DexFileFactory.loadDexFile(dexFile, Opcodes.getDefault())

            val allPatterns = cachedPatterns ?: LibraryPatterns.getAllPatterns()

            dex.classes.forEach { classDef ->
                val className = classDef.type
                    .removePrefix("L")
                    .removeSuffix(";")
                    .replace('/', '.')

                allPatterns.forEach { (category, patterns) ->
                    patterns.forEach { pattern ->
                        if (className.startsWith(pattern.packagePattern)) {
                            detectedLibraries.add(
                                DetectedLibrary(
                                    name = pattern.libraryName,
                                    packageName = pattern.packagePattern,
                                    category = category
                                )
                            )
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Timber.d(e, "Error analyzing DEX file ${dexFile.name} (non-critical)")
        }

        return detectedLibraries.toList()
    }

    data class LibraryDetectionResult(
        val detectedLibraries: List<DetectedLibrary>,
        val unknownPackages: List<String>
    )

    suspend fun detectLibrariesAndUnknown(apkPath: String, dexClasses: Set<String>): LibraryDetectionResult {
        val detectedLibraries = mutableSetOf<DetectedLibrary>()
        val allPatterns = getOrLoadPatterns()
        val knownPrefixes = mutableSetOf<String>()
        allPatterns.values.flatten().forEach { pattern ->
            knownPrefixes.add(pattern.packagePattern)
        }

        val ignoredPrefixes = setOf(
            "android.", "androidx.", "java.", "javax.", "kotlin.", "kotlinx.", "com.android.",
            "com.google.android", "com.google.common", "org.jetbrains", "org.json", "org.xml",
            "org.w3c", "org.apache.http", "org.apache.commons", "org.junit", "org.mockito",
            "sun.", "dalvik.", "libcore."
        )

        val unknownPackages = mutableSetOf<String>()

        for (className in dexClasses) {
            var matched = false
            for ((category, patterns) in allPatterns) {
                for (pattern in patterns) {
                    if (className.startsWith(pattern.packagePattern)) {
                        detectedLibraries.add(
                            DetectedLibrary(
                                name = pattern.libraryName,
                                packageName = pattern.packagePattern,
                                category = category
                            )
                        )
                        matched = true
                        break
                    }
                }
                if (matched) break
            }
            if (!matched) {
                val segments = className.split('.')
                val twoSegmentPrefix = if (segments.size >= 2) "${segments[0]}.${segments[1]}" else segments[0]
                if (ignoredPrefixes.none { twoSegmentPrefix.startsWith(it) }) {
                    if (knownPrefixes.none { twoSegmentPrefix.startsWith(it) }) {
                        unknownPackages.add(twoSegmentPrefix)
                    }
                }
            }
        }

        return LibraryDetectionResult(
            detectedLibraries = detectedLibraries.toList().sortedBy { it.name },
            unknownPackages = unknownPackages.toList().sorted()
        )
    }

    suspend fun detectAnalyticsLibraries(apkPath: String, dexClasses: Set<String>): List<DetectedLibrary> {
        return detectLibraries(apkPath, dexClasses)
    }
}