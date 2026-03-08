package com.example.mobiletechstack.domain.model


data class FrameworkInfo(
    val type: FrameworkType
)

enum class FrameworkType(val displayName: String) {
    NATIVE_ANDROID("Native Android"),
    FLUTTER("Flutter"),
    REACT_NATIVE("React Native"),
    XAMARIN("Xamarin"),
    CORDOVA("Cordova/PhoneGap"),
    IONIC("Ionic"),
    UNITY("Unity"),
    KOTLIN_MULTIPLATFORM("Kotlin Multiplatform"),
    NATIVE_SCRIPT("NativeScript"),
    UNKNOWN("Unknown")
}