package com.example.mobiletechstack.domain.model

data class PermissionInfo(
    val name: String,
    val granted: Boolean,
    val category: PermissionCategory
)

enum class PermissionCategory(val displayName: String) {
    CAMERA("Camera"),
    LOCATION("Location"),
    STORAGE("Storage"),
    MICROPHONE("Microphone"),
    CONTACTS("Contacts"),
    PHONE("Phone"),
    SMS("SMS & Messaging"),
    CALENDAR("Calendar"),
    SENSORS("Sensors"),
    NETWORK("Network"),
    BLUETOOTH("Bluetooth & NFC"),
    SYSTEM("System"),
    OTHER("Other")
}