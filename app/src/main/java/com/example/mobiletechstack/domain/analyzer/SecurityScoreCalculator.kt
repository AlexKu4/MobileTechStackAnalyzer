package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.PermissionCategory
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.RiskLevel
import com.example.mobiletechstack.domain.model.SecurityFlags
import com.example.mobiletechstack.domain.model.SecurityScore

// Считает балл безопасности — начинаем со 100 и вычитаем за каждый тревожный признак
object SecurityScoreCalculator {

    private val CRITICAL_CATEGORIES = setOf(
        PermissionCategory.CAMERA,
        PermissionCategory.MICROPHONE,
        PermissionCategory.LOCATION,
        PermissionCategory.CONTACTS
    )
    private val MODERATE_CATEGORIES = setOf(
        PermissionCategory.PHONE,
        PermissionCategory.SMS,
        PermissionCategory.STORAGE
    )
    private val LOW_CATEGORIES = setOf(
        PermissionCategory.BLUETOOTH,
        PermissionCategory.SENSORS,
        PermissionCategory.CALENDAR
    )

    fun calculate(
        securityFlags: SecurityFlags?,
        hasObfuscation: Boolean,
        permissions: List<PermissionInfo>
    ): SecurityScore {
        var score = 100f
        val reasons = mutableListOf<String>()

        // Позволяет подключиться к приложению через ADB и читать память
        if (securityFlags?.isDebuggable == true) {
            score -= 30f
            reasons.add("Debug mode enabled (debuggable) - critical risk -30 pts")
        }

        // Данные передаются без шифрования — перехват в любой WiFi сети
        if (securityFlags?.usesCleartextTraffic == true) {
            score -= 25f
            reasons.add("Unencrypted traffic allowed (cleartext) -25 pts")
        }

        // Без обфускации код читается как открытая книга
        if (!hasObfuscation) {
            score -= 15f
            reasons.add("Code is not obfuscated -15 pts")
        }

        // Резервная копия через ADB без root — риск при физическом доступе
        if (securityFlags?.allowBackup == true) {
            score -= 5f
            reasons.add("Backup via ADB allowed (allowBackup) -5 pts")
        }

        // Три уровня опасности разрешений суммируются в одну строку reasons
        val criticalCount = permissions.count { it.category in CRITICAL_CATEGORIES }
        val moderateCount = permissions.count { it.category in MODERATE_CATEGORIES }
        val lowCount = permissions.count { it.category in LOW_CATEGORIES }
        val totalCount = criticalCount + moderateCount + lowCount

        val totalPenalty = minOf(criticalCount * 2f + moderateCount * 1f + lowCount * 0.5f, 10f)

        if (totalPenalty > 0f) {
            val display = if (totalPenalty % 1f == 0f) totalPenalty.toInt().toString() else totalPenalty.toString()
            reasons.add("Dangerous permissions: $totalCount (-$display pts)")
        }

        val finalScore = maxOf(0f, score - totalPenalty)
        val riskLevel = when {
            finalScore >= 80f -> RiskLevel.LOW
            finalScore >= 60f -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }
        val verdict = when {
            finalScore >= 80f -> "Good security practices"
            finalScore >= 60f -> "Acceptable, minor concerns"
            finalScore >= 40f -> "Several risks detected"
            else -> "High risk"
        }

        if (reasons.isEmpty()) {
            reasons.add("Debug mode is disabled - OK")
            reasons.add("Cleartext traffic is not allowed - OK")
            reasons.add("Code obfuscation detected - OK")
            reasons.add("Backup via ADB is disabled - OK")
            reasons.add("No dangerous permissions detected - OK")
        }

        return SecurityScore(
            score = finalScore,
            riskLevel = riskLevel,
            reasons = reasons,
            verdict = verdict
        )
    }
}
