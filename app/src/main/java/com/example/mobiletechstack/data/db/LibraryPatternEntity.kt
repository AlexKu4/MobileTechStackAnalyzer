package com.example.mobiletechstack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

// Хранит один паттерн библиотеки из JSON или Kotlin-fallback
@Entity(tableName = "library_patterns")
data class LibraryPatternEntity(
    @PrimaryKey val packagePattern: String,
    val libraryName: String,
    val category: String
)
