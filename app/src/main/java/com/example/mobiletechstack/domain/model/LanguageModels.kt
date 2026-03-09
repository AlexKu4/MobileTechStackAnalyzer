package com.example.mobiletechstack.domain.model

data class LanguageInfo(
    val languages: List<ProgrammingLanguage>,
    val primary: ProgrammingLanguage
)

enum class ProgrammingLanguage(val displayName: String) {
    KOTLIN("Kotlin"),
    JAVA("Java"),
    CPP("C++"),
    C("C"),
    JAVASCRIPT("JavaScript"),
    DART("Dart"),
    CSHARP("C#"),
    PYTHON("Python"),
    UNKNOWN("Unknown")
}