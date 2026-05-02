package com.example.mobiletechstack.domain.analyzer

import java.util.zip.ZipFile

class ObfuscationDetector {

    fun hasObfuscation(apkPath: String, dexClasses: Set<String>): Boolean {
        if (dexClasses.isNotEmpty()) {
            val shortCount = dexClasses.count { className ->
                val simpleName = className.substringAfterLast('.')
                simpleName.length < 3 && simpleName.all { it.isLetter() }
            }
            if (shortCount.toDouble() / dexClasses.size > 0.2) return true
        }

        val hasSingleLetterPackages = dexClasses.any { className ->
            className.split('.').any { it.length == 1 && it[0].isLetter() }
        }
        if (hasSingleLetterPackages) return true

        try {
            ZipFile(apkPath).use { zip ->
                val entries = zip.entries().toList()
                if (entries.any { entry ->
                        entry.name.contains("META-INF/proguard/") ||
                                entry.name.contains("proguard-project.txt") ||
                                entry.name.contains("proguard-android.txt") ||
                                entry.name.contains("dexguard")
                    }) {
                    return true
                }
            }
        } catch (e: Exception) {

        }

        val hasDexGuardClasses = dexClasses.any { className ->
            className.startsWith("com.saikoa.dexguard.") ||
                    className.startsWith("com.guardsquare.dexguard.")
        }
        return hasDexGuardClasses
    }

}