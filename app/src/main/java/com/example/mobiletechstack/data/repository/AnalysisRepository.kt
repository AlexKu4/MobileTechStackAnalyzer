package com.example.mobiletechstack.data.repository

import com.example.mobiletechstack.data.db.AnalysisResultDao
import com.example.mobiletechstack.data.db.AnalysisResultEntity
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


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
            val result = gson.fromJson(entity.resultJson, AnalysisResult::class.java)
            if (result?.packageName == null) {
                dao.deleteByPackageName(packageName)
                null
            } else {
                result
            }
        } catch (e: Exception) {
            // Удаляет битый JSON сразу
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

    suspend fun getLastAnalyzedAt(packageName: String): Long? =
        dao.getByPackageName(packageName)?.analyzedAt

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

    // автоматически обновляется при любом изменении таблицы
    fun observeHistory(): Flow<List<HistoryEntry>> =
        dao.observeAllSortedByDate().map { entities ->
            entities.mapNotNull { entity ->
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
