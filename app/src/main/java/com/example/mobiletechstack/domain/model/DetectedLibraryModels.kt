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
    DI("Dependency Injection"),
    IMAGE_LOADING("Image Loading"),
    SERIALIZATION("Serialization"),
    MAPS("Maps & Location"),
    MEDIA("Media & Player"),
    PUSH("Push Notifications"),
    CAMERA("Camera"),
    SECURITY("Security"),
    OTHER("Other")
}
