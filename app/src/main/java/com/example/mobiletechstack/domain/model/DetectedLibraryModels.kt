package com.example.mobiletechstack.domain.model

data class DetectedLibrary(
    val name: String,
    val packageName: String,
    val category: LibraryCategory
)

enum class LibraryCategory(val displayName: String) {
    ANALYTICS("Analytics & Tracking"),
    ADVERTISING("Advertising"),
    SOCIAL("Social Media"),
    PAYMENT("Payment"),
    UI("UI Libraries"),
    REACTIVE("Reactive Programming"),
    DATABASES("Databases & ORM"),
    NETWORKING("Networking"),
    OTHER("Other")
}
