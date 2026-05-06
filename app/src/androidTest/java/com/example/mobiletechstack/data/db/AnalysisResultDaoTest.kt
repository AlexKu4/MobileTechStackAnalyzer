package com.example.mobiletechstack.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnalysisResultDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AnalysisResultDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.analysisResultDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveAndGetByPackageName() = runBlocking {
        val entity = AnalysisResultEntity(
            packageName = "com.example.app",
            analyzedAt = 1000L,
            resultJson = "{}"
        )
        dao.upsert(entity)

        val result = dao.getByPackageName("com.example.app")
        assertEquals(entity, result)
    }

    @Test
    fun getByPackageName_returnsNullIfNotExists() = runBlocking {
        val result = dao.getByPackageName("com.nonexistent.app")
        assertNull(result)
    }

    @Test
    fun upsert_updatesExistingRecord() = runBlocking {
        val original = AnalysisResultEntity("com.example.app", 1000L, "{\"v\":1}")
        dao.upsert(original)

        val updated = original.copy(resultJson = "{\"v\":2}")
        dao.upsert(updated)

        val result = dao.getByPackageName("com.example.app")
        assertEquals("{\"v\":2}", result?.resultJson)
        assertEquals(1, dao.getAllSortedByDate().size)
    }

    @Test
    fun getAllSortedByDate_returnsNewestFirst() = runBlocking {
        dao.upsert(AnalysisResultEntity("com.app.a", 1000L, "{}"))
        dao.upsert(AnalysisResultEntity("com.app.b", 3000L, "{}"))
        dao.upsert(AnalysisResultEntity("com.app.c", 2000L, "{}"))

        val results = dao.getAllSortedByDate()
        assertEquals(listOf("com.app.b", "com.app.c", "com.app.a"), results.map { it.packageName })
    }

    @Test
    fun deleteByPackageName_removesRecord() = runBlocking {
        dao.upsert(AnalysisResultEntity("com.example.app", 1000L, "{}"))
        dao.deleteByPackageName("com.example.app")

        assertNull(dao.getByPackageName("com.example.app"))
    }

    @Test
    fun trimToLimit_keepsOnlyMostRecentEntries() = runBlocking {
        repeat(5) { i ->
            dao.upsert(AnalysisResultEntity("com.app.$i", i.toLong() * 1000, "{}"))
        }
        dao.trimToLimit(3)
        val remaining = dao.getAllSortedByDate()
        assertEquals(3, remaining.size)
        assertEquals(listOf("com.app.4", "com.app.3", "com.app.2"), remaining.map { it.packageName })
    }
}
