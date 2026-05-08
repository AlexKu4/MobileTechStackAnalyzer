package com.example.mobiletechstack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LibraryPatternDao {

    @Query("SELECT * FROM library_patterns")
    suspend fun getAll(): List<LibraryPatternEntity>

    @Query("SELECT * FROM library_patterns WHERE category = :category")
    suspend fun getByCategory(category: String): List<LibraryPatternEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patterns: List<LibraryPatternEntity>)

    @Query("DELETE FROM library_patterns")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM library_patterns")
    suspend fun count(): Int
}
