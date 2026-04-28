package com.example.mobiletechstack.data.repository

import com.example.mobiletechstack.data.db.AnalysisResultDao
import com.example.mobiletechstack.data.db.AnalysisResultEntity
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

private class FakeAnalysisResultDao : AnalysisResultDao {
    val storage = mutableMapOf<String, AnalysisResultEntity>()

    override suspend fun getByPackageName(packageName: String) = storage[packageName]
    override suspend fun upsert(entity: AnalysisResultEntity) { storage[entity.packageName] = entity }
    override suspend fun getAllSortedByDate() = storage.values.sortedByDescending { it.analyzedAt }
    override suspend fun deleteByPackageName(packageName: String) { storage.remove(packageName) }
    override suspend fun trimToLimit(limit: Int) {
        val toKeep = storage.values.sortedByDescending { it.analyzedAt }.take(limit).map { it.packageName }.toSet()
        storage.keys.retainAll(toKeep)
    }
}

class AnalysisRepositoryTest {

    private val dao = FakeAnalysisResultDao()
    private val repository = AnalysisRepository(dao)

    private fun makeResult(packageName: String = "com.example.app") = AnalysisResult(
        packageName = packageName,
        appName = "Test App",
        nativeLibraries = emptyList(),
        apkPath = "/data/app/test.apk",
        apkSize = 1024L,
        framework = "Native Android",
        language = "Kotlin",
        primaryAbi = "arm64-v8a",
        is64Bit = true,
        supportedAbis = listOf("arm64-v8a"),
        hasObfuscation = false
    )

    @Test
    fun getCached_returnsNullIfNotSaved() = runBlocking {
        assertNull(repository.getCached("com.example.app"))
    }

    @Test
    fun saveAndGetCached_roundTrip() = runBlocking {
        val result = makeResult()
        repository.save(result)
        assertEquals(result, repository.getCached(result.packageName))
    }

    @Test
    fun save_overwritesExistingEntry() = runBlocking {
        repository.save(makeResult())
        repository.save(makeResult())
        assertEquals(1, dao.storage.size)
    }

    @Test
    fun getHistory_returnsAllEntries() = runBlocking {
        repository.save(makeResult("com.app.a"))
        repository.save(makeResult("com.app.b"))
        assertEquals(2, repository.getHistory().size)
    }

    @Test
    fun getHistory_entriesHavePositiveTimestamp() = runBlocking {
        repository.save(makeResult())
        assertTrue(repository.getHistory().first().analyzedAt > 0)
    }

    @Test
    fun getCached_returnsNullAndDeletesCorruptJson() = runBlocking {
        dao.upsert(AnalysisResultEntity("com.example.app", 1000L, "not-valid-json{{{"))
        assertNull(repository.getCached("com.example.app"))
        assertNull(dao.storage["com.example.app"])
    }

    @Test
    fun save_trimsToLimitAfterExceeding() = runBlocking {
        // Сохраняем CACHE_LIMIT + 5 записей и проверяем что хранится не больше лимита
        repeat(AnalysisRepository.CACHE_LIMIT + 5) { i ->
            repository.save(makeResult("com.app.$i"))
        }
        assertTrue(dao.storage.size <= AnalysisRepository.CACHE_LIMIT)
    }
}
