package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.LibraryInfo
import java.util.zip.ZipFile

object NativeLibraryAnalyzer {

    fun extractNativeLibraries(apkPath: String): List<LibraryInfo> {
        val libraries = mutableListOf<LibraryInfo>()

        try {
            ZipFile(apkPath).use { zipFile ->
                zipFile.entries().asSequence()
                    .filter { entry ->
                        entry.name.startsWith("lib/") &&
                                entry.name.endsWith(".so") &&
                                !entry.isDirectory
                    }
                    .forEach { entry ->
                        val parts = entry.name.split("/")
                        if (parts.size == 3) {
                            val abi = parts[1]
                            val libName = parts[2]

                            libraries.add(
                                LibraryInfo(
                                    name = libName,
                                    abi = abi,
                                    size = entry.size
                                )
                            )
                        }
                    }
            }
        } catch (e: Exception) {
            println("Error extracting native libraries: ${e.message}")
        }

        return libraries
    }

    fun getAbis(libraries: List<LibraryInfo>): List<String> {
        return libraries
            .map { it.abi }
            .distinct()
            .sortedByDescending { abi ->
                when {
                    abi.contains("64") -> 2
                    else -> 1
                }
            }
    }

    fun getPrimaryAbi(libraries: List<LibraryInfo>): String {
        val abis = getAbis(libraries)

        return when {
            abis.contains("arm64-v8a") -> "arm64-v8a"
            abis.contains("x86_64") -> "x86_64"
            abis.contains("armeabi-v7a") -> "armeabi-v7a"
            abis.contains("x86") -> "x86"
            abis.contains("armeabi") -> "armeabi"
            abis.isNotEmpty() -> abis.first()
            else -> "Unknown"
        }
    }
}