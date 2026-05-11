package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Качает APK по http(s)-ссылке в кэш приложения и проверяет, что это действительно ZIP/APK.
class ApkDownloader(private val context: Context) {

    suspend fun download(url: String, onProgress: (Int) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("URL must start with http:// or https://"))
        }

        // Кладём загрузки в отдельную папку внутри cacheDir, чтобы не смешивать с другими временными файлами
        val downloadsDir = File(context.cacheDir, "apk_downloads").apply { if (!exists()) mkdirs() }

        var outFile: File? = null
        var connection: HttpURLConnection? = null
        var success = false

        try {
            // Сами обходим редиректы — HttpURLConnection не следует через смену схемы (http↔https)
            val resolved = openConnectionFollowingRedirects(url)
            connection = resolved.connection

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(RuntimeException("HTTP $responseCode"))
            }

            // Имя берём из финального URL после редиректов — оригинал мог быть коротким редирект-ссылкой
            val fileName = deriveFileName(resolved.finalUrl)
            outFile = File(downloadsDir, fileName)

            // Если сервер прислал Content-Length и файл слишком большой — выходим до начала записи
            val totalSize = connection.contentLengthLong
            if (totalSize > MAX_SIZE_BYTES) {
                return@withContext Result.failure(RuntimeException("File is too large (max 300 MB)"))
            }

            connection.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var downloaded = 0L
                    var lastProgress = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read

                        // Защита от "ленивого" Content-Length — проверяем на лету
                        if (downloaded > MAX_SIZE_BYTES) {
                            return@withContext Result.failure(RuntimeException("File is too large (max 300 MB)"))
                        }

                        if (totalSize > 0) {
                            val progress = (downloaded * 100 / totalSize).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            // Проверяем ZIP-сигнатуру PK\x03\x04 — отсекаем HTML-заглушки и редиректы на не-APK
            if (!hasZipSignature(outFile)) {
                return@withContext Result.failure(RuntimeException("URL does not point to a valid APK file"))
            }

            success = true
            Result.success(outFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to download APK from $url")
            Result.failure(e)
        } finally {
            connection?.disconnect()
            // Чистим частично скачанный файл при любой неудаче
            if (!success) outFile?.delete()
        }
    }

    // Ходит по 3xx-редиректам вручную, чтобы переживать смену схемы http↔https. Лимит хопов от циклов.
    private fun openConnectionFollowingRedirects(startUrl: String): ResolvedConnection {
        var currentUrl = startUrl
        repeat(MAX_REDIRECTS) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 60_000
                requestMethod = "GET"
                instanceFollowRedirects = false
            }
            conn.connect()
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw RuntimeException("HTTP $code without Location header")
                }
                // Поддерживаем относительные Location: разрешаем через предыдущий URL
                val nextUrl = URL(URL(currentUrl), location).toString()
                if (!nextUrl.startsWith("http://") && !nextUrl.startsWith("https://")) {
                    throw RuntimeException("Redirect target must be http(s): $nextUrl")
                }
                currentUrl = nextUrl
                return@repeat
            }
            return ResolvedConnection(conn, currentUrl)
        }
        throw RuntimeException("Too many redirects (>$MAX_REDIRECTS)")
    }

    private fun deriveFileName(url: String): String {
        val candidate = url.substringAfterLast('/', "")
            .substringBefore('?')
            .substringBefore('#')
        return if (candidate.isNotBlank() && candidate.endsWith(".apk", ignoreCase = true)) {
            candidate
        } else {
            "downloaded_apk.apk"
        }
    }

    private fun hasZipSignature(file: File): Boolean {
        if (file.length() < 4) return false
        return file.inputStream().use { input ->
            val header = ByteArray(4)
            val read = input.read(header)
            read == 4 &&
                header[0] == 0x50.toByte() &&
                header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() &&
                header[3] == 0x04.toByte()
        }
    }

    private data class ResolvedConnection(val connection: HttpURLConnection, val finalUrl: String)

    companion object {
        private const val MAX_SIZE_BYTES = 300L * 1024 * 1024
        private const val MAX_REDIRECTS = 5
    }
}
