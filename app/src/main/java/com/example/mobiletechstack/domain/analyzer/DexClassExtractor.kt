package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import timber.log.Timber
import java.io.File
import java.util.zip.ZipFile

class DexClassExtractor(private val context: Context) {

    private var cachedClasses: Set<String>? = null
    private var cachedApkPath: String? = null

    suspend fun extractAllClassNames(apkPath: String): Set<String> {
        if (cachedApkPath == apkPath && cachedClasses != null) {
            return cachedClasses!!
        }
        val allClasses = mutableSetOf<String>()
        val tempDir = context.cacheDir

        try {
            ZipFile(apkPath).use { zip ->
                val dexEntries = zip.entries().asSequence()
                    .filter { it.name.matches(Regex("classes\\d*\\.dex")) }
                    .toList()

                if (dexEntries.isEmpty()) {
                    cachedClasses = emptySet()
                    return emptySet()
                }

                dexEntries.forEach { entry ->
                    val tempFile =
                        File(tempDir, "temp_dex_${System.currentTimeMillis()}_${entry.name}")
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
                            allClasses.add(className)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to process dex entry: ${entry.name}")
                    } finally {
                        tempFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract class names from APK: $apkPath")
            cachedClasses = emptySet()
            return emptySet()
        }

        cachedApkPath = apkPath
        cachedClasses = allClasses
        return allClasses
    }

}