package com.example.mobiletechstack.data.repository

import com.example.mobiletechstack.data.db.AnalysisResultDao
import com.example.mobiletechstack.data.db.AnalysisResultEntity
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.google.gson.Gson

// Запись из истории — результат анализа вместе с датой сохранения
data class HistoryEntry(
    val result: AnalysisResult,
    val analyzedAt: Long
)

// Репозиторий сериализует AnalysisResult в JSON через Gson
class AnalysisRepository(private val dao: AnalysisResultDao) {

    private val gson = Gson()

    // Возвращает закэшированный результат или null, если записи нет или JSON повреждён
    suspend fun getCached(packageName: String): AnalysisResult? {
        val entity = dao.getByPackageName(packageName) ?: return null
        return try {
            gson.fromJson(entity.resultJson, AnalysisResult::class.java)
        } catch (e: Exception) {
            // Удаляет битый JSON сразу, чтобы не показывать мусор при следующем открытии
            dao.deleteByPackageName(packageName)
            null
        }
    }

    suspend fun save(result: AnalysisResult) {
        val entity = AnalysisResultEntity(
            packageName = result.packageName,
            analyzedAt = System.currentTimeMillis(),
            resultJson = gson.toJson(result)
        )
        dao.upsert(entity)
        dao.trimToLimit(CACHE_LIMIT)
    }

    // Возвращает все сохранённые результаты, отсортированные от новых к старым
    suspend fun getHistory(): List<HistoryEntry> {
        return dao.getAllSortedByDate().mapNotNull { entity ->
            try {
                HistoryEntry(
                    result = gson.fromJson(entity.resultJson, AnalysisResult::class.java),
                    analyzedAt = entity.analyzedAt
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        const val CACHE_LIMIT = 50
    }
}
