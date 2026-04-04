package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import com.example.mobiletechstack.domain.model.DetectedLibrary
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipFile


class DexAnalyzer(private val context: Context) {

    suspend fun detectLibraries(apkPath: String): List<DetectedLibrary> {
        return try {
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

            val allPatterns = LibraryPatterns.getAllPatterns()

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
            // ignore
        }

        return detectedLibraries.toList()
    }

    suspend fun detectAnalyticsLibraries(apkPath: String): List<DetectedLibrary> {
        return detectLibraries(apkPath)
    }
}