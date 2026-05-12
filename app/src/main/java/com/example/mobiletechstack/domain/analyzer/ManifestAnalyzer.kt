package com.example.mobiletechstack.domain.analyzer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.example.mobiletechstack.domain.model.AppVersionInfo
import com.example.mobiletechstack.domain.model.SecurityFlags
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

class ManifestAnalyzer(private val context: Context) {

    private val packageManager = context.packageManager

    @SuppressLint("ObsoleteSdkInt")
    suspend fun extractVersionInfo(packageName: String): AppVersionInfo? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            AppVersionInfo(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    applicationInfo.minSdkVersion
                } else {
                    0
                },
                targetSdkVersion = applicationInfo.targetSdkVersion,
                compileSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applicationInfo.compileSdkVersion
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract version info for $packageName")
            null
        }
    }

    suspend fun extractSecurityFlags(packageName: String): SecurityFlags? {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            SecurityFlags(
                isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                allowBackup = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0,
                usesCleartextTraffic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
                } else {
                    true
                },
                hasCode = (applicationInfo.flags and ApplicationInfo.FLAG_HAS_CODE) != 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract security flags for $packageName")
            null
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    suspend fun extractVersionInfoFromFile(apkPath: String): AppVersionInfo? {
        return try {
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0) ?: return null
            val applicationInfo = packageInfo.applicationInfo ?: return null
            applicationInfo.sourceDir = apkPath
            applicationInfo.publicSourceDir = apkPath
            AppVersionInfo(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    applicationInfo.minSdkVersion
                } else {
                    0
                },
                targetSdkVersion = applicationInfo.targetSdkVersion,
                compileSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    applicationInfo.compileSdkVersion
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract version info from $apkPath")
            null
        }
    }

    // Версия для внешнего APK, флаги тянем из манифеста файла
    @SuppressLint("ObsoleteSdkInt")
    suspend fun extractSecurityFlagsFromFile(apkPath: String): SecurityFlags? {
        return try {
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0) ?: return null
            val applicationInfo = packageInfo.applicationInfo ?: return null
            applicationInfo.sourceDir = apkPath
            applicationInfo.publicSourceDir = apkPath
            SecurityFlags(
                isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                allowBackup = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0,
                usesCleartextTraffic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    (applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
                } else {
                    true
                },
                hasCode = (applicationInfo.flags and ApplicationInfo.FLAG_HAS_CODE) != 0
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract security flags from $apkPath")
            null
        }
    }

    companion object {

        fun extractPackageName(apkPath: String): String? {
            return try {
                ZipFile(apkPath).use { zip ->
                    val entry = zip.getEntry("AndroidManifest.xml") ?: return@use null
                    val data = zip.getInputStream(entry).use { it.readBytes() }
                    parsePackageFromAxml(data)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract package name from $apkPath")
                null
            }
        }

        private fun parsePackageFromAxml(data: ByteArray): String? {
            val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 8) return null

            // Заголовок XML chunk
            val xmlType = buf.short.toInt() and 0xFFFF
            if (xmlType != 0x0003) return null
            buf.short
            buf.int

            // Строковый пул
            val poolStart = buf.position()
            val poolType = buf.short.toInt() and 0xFFFF
            if (poolType != 0x0001) return null
            buf.short
            val poolChunkSize = buf.int
            val stringCount = buf.int
            buf.int
            val flags = buf.int
            val stringsStart = buf.int
            buf.int
            val isUtf8 = (flags and 0x100) != 0

            val offsets = IntArray(stringCount) { buf.int }
            val stringsBase = poolStart + stringsStart

            fun readString(idx: Int): String {
                if (idx < 0 || idx >= stringCount) return ""
                buf.position(stringsBase + offsets[idx])
                return if (isUtf8) {
                    readUtf8Length(buf)
                    val byteLen = readUtf8Length(buf)
                    val bytes = ByteArray(byteLen)
                    buf.get(bytes)
                    String(bytes, Charsets.UTF_8)
                } else {
                    val charLen = readUtf16Length(buf)
                    val bytes = ByteArray(charLen * 2)
                    buf.get(bytes)
                    String(bytes, Charsets.UTF_16LE)
                }
            }

            // Перепрыгиваем строковый пул и идём по chunks до первого XML_START_ELEMENT
            buf.position(poolStart + poolChunkSize)

            while (buf.remaining() >= 8) {
                val chunkStart = buf.position()
                val type = buf.short.toInt() and 0xFFFF
                buf.short
                val size = buf.int
                if (size <= 0) return null

                if (type == 0x0102) {
                    buf.int  // line
                    buf.int  // comment
                    buf.int  // ns
                    val nameIdx = buf.int
                    buf.short  // attributeStart
                    buf.short  // attributeSize
                    val attrCount = buf.short.toInt() and 0xFFFF
                    buf.short  // idIndex
                    buf.short  // classIndex
                    buf.short  // styleIndex

                    if (readString(nameIdx) == "manifest") {
                        buf.position(chunkStart + 0x24)
                        repeat(attrCount) {
                            buf.int  // ns
                            val attrNameIdx = buf.int
                            val rawValueIdx = buf.int
                            buf.short  // size
                            buf.get()  // reserved
                            buf.get()  // type
                            val data2 = buf.int

                            if (readString(attrNameIdx) == "package") {
                                return if (rawValueIdx in 0 until stringCount) readString(rawValueIdx)
                                else readString(data2)
                            }
                        }
                        return null
                    }
                }
                buf.position(chunkStart + size)
            }
            return null
        }

        private fun readUtf8Length(buf: ByteBuffer): Int {
            var len = buf.get().toInt() and 0xFF
            if (len and 0x80 != 0) {
                len = ((len and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
            }
            return len
        }

        private fun readUtf16Length(buf: ByteBuffer): Int {
            var len = buf.short.toInt() and 0xFFFF
            if (len and 0x8000 != 0) {
                len = ((len and 0x7FFF) shl 16) or (buf.short.toInt() and 0xFFFF)
            }
            return len
        }
    }
}