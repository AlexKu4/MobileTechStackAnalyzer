package com.example.mobiletechstack.data.repository

import android.content.Context
import com.example.mobiletechstack.data.db.LibraryPatternDao
import com.example.mobiletechstack.data.db.LibraryPatternEntity
import com.example.mobiletechstack.domain.analyzer.LibraryPatterns
import com.example.mobiletechstack.domain.model.LibraryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_URL =
    "https://raw.githubusercontent.com/AlexKu4/MobileTechStackAnalyzer/master/patterns.json"
private const val ASSET_FILE = "patterns.json"

// Управляет источниками паттернов: Room, assets, Kotlin-fallback
class PatternRepository(
    private val context: Context,
    private val dao: LibraryPatternDao
) {

    // Если Room пуст — читает assets, при успехе сохраняет в Room; иначе возвращает Kotlin-fallback
    suspend fun getPatterns(): Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>> =
        withContext(Dispatchers.IO) {
            if (dao.count() > 0) {
                return@withContext dao.getAll().toPatternMap()
            }
            try {
                val json = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
                parseAndSave(json) ?: LibraryPatterns.getAllPatterns()
            } catch (e: Exception) {
                Timber.e(e, "Не удалось прочитать $ASSET_FILE из assets")
                LibraryPatterns.getAllPatterns()
            }
        }

    // Скачивает JSON с GitHub, парсит и сохраняет в Room; false при любой ошибке
    suspend fun updateFromGitHub(): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = downloadJson(GITHUB_URL)
            parseAndSave(json) != null
        } catch (e: Exception) {
            Timber.e(e, "Не удалось обновить паттерны с GitHub")
            false
        }
    }

    private fun downloadJson(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 10_000
            requestMethod = "GET"
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    // Парсит JSON паттернов, заменяет содержимое Room, возвращает null при ошибке парсинга
    private suspend fun parseAndSave(
        json: String
    ): Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>>? {
        return try {
            val patternsObj = JSONObject(json).getJSONObject("patterns")
            val entities = mutableListOf<LibraryPatternEntity>()
            val result = mutableMapOf<LibraryCategory, List<LibraryPatterns.LibraryPattern>>()

            for (categoryName in patternsObj.keys()) {
                val category = LibraryCategory.values().find { it.name == categoryName } ?: continue
                val array = patternsObj.getJSONArray(categoryName)
                val patterns = mutableListOf<LibraryPatterns.LibraryPattern>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkg = obj.getString("package")
                    val name = obj.getString("name")
                    patterns.add(LibraryPatterns.LibraryPattern(pkg, name))
                    entities.add(LibraryPatternEntity(pkg, name, categoryName))
                }
                result[category] = patterns
            }

            dao.deleteAll()
            dao.insertAll(entities)
            result
        } catch (e: Exception) {
            Timber.e(e, "Ошибка парсинга JSON паттернов")
            null
        }
    }

    private fun List<LibraryPatternEntity>.toPatternMap(): Map<LibraryCategory, List<LibraryPatterns.LibraryPattern>> =
        groupBy { it.category }
            .mapKeys { (key, _) ->
                LibraryCategory.values().find { it.name == key } ?: LibraryCategory.OTHER
            }
            .mapValues { (_, list) ->
                list.map { LibraryPatterns.LibraryPattern(it.packagePattern, it.libraryName) }
            }
}
