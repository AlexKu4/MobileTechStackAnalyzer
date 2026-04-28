package com.example.mobiletechstack.domain.analyzer

class ObfuscationDetector {

    fun hasObfuscation(dexClasses: Set<String>): Boolean {
        val pattern = Regex("""\b[a-z]\.[a-z]\b""")
        return dexClasses.any { className ->
            pattern.containsMatchIn(className)
        }
    }

}