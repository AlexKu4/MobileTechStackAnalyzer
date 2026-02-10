package com.example.myapplication.domain.model

data class LibraryInfo(
    val name: String,
    val abi: String,
    val size: Long = 0
)