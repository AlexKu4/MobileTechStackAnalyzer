package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.data.repository.PatternRepository
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryCategory
import timber.log.Timber


class DexAnalyzer(private val context: Context, private val patternRepository: PatternRepository) {

    private var cachedPatterns: Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>>? = null

    private suspend fun getOrLoadPatterns(): Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>> =
        cachedPatterns ?: patternRepository.getPatterns().also { cachedPatterns = it }

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
}