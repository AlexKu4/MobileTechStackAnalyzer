package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipFile


class DexAnalyzer(private val context: Context) {

    private val analyticsPatterns = mapOf(
        // Firebase
        "com.google.firebase.analytics" to "Firebase Analytics",
        "com.google.android.gms.measurement" to "Firebase Analytics (GMS)",

        // Google Analytics
        "com.google.android.gms.analytics" to "Google Analytics",

        // Facebook
        "com.facebook.appevents" to "Facebook Analytics",
        "com.facebook.FacebookSdk" to "Facebook SDK",

        // AppsFlyer
        "com.appsflyer" to "AppsFlyer",

        // Amplitude
        "com.amplitude" to "Amplitude",

        // Mixpanel
        "com.mixpanel.android" to "Mixpanel",

        // Branch.io
        "io.branch.referral" to "Branch.io",

        // Adjust
        "com.adjust.sdk" to "Adjust",

        // Crashlytics / Firebase Crashlytics
        "com.crashlytics" to "Crashlytics",
        "com.google.firebase.crashlytics" to "Firebase Crashlytics",

        // Flurry
        "com.flurry.android" to "Flurry Analytics",

        // Localytics
        "com.localytics.android" to "Localytics",

        // Segment
        "com.segment.analytics" to "Segment"
    )

    suspend fun detectAnalyticsLibraries(apkPath: String): List<DetectedLibrary> = withContext(Dispatchers.IO) {
        try {
            val detectedLibraries = mutableSetOf<DetectedLibrary>()

            val dexFiles = extractDexFiles(apkPath)

            dexFiles.forEach { dexFile ->
                val libraries = analyzeDexFile(dexFile)
                detectedLibraries.addAll(libraries)
            }

            dexFiles.forEach { it.delete() }

            detectedLibraries.toList().sortedBy { it.name }

        } catch (e: Exception) {
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

            dex.classes.forEach { classDef ->
                val className = classDef.type
                    .removePrefix("L")
                    .removeSuffix(";")
                    .replace('/', '.')

                analyticsPatterns.forEach { (pattern, libraryName) ->
                    if (className.startsWith(pattern)) {
                        detectedLibraries.add(
                            DetectedLibrary(
                                name = libraryName,
                                packageName = pattern,
                                category = LibraryCategory.ANALYTICS
                            )
                        )
                    }
                }
            }

        } catch (e: Exception) {
            // ignore exception
        }

        return detectedLibraries.toList()
    }
}