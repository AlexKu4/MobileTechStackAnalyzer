package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.PermissionCategory
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.RiskLevel
import com.example.mobiletechstack.domain.model.SecurityFlags
import com.example.mobiletechstack.domain.model.SecurityScore

// Считает балл безопасности — начинаем со 100 и вычитаем за каждый тревожный признак
object SecurityScoreCalculator {

    private val DANGEROUS_CATEGORIES = setOf(
        PermissionCategory.CAMERA,
        PermissionCategory.LOCATION,
        PermissionCategory.STORAGE,
        PermissionCategory.MICROPHONE,
        PermissionCategory.CONTACTS,
        PermissionCategory.PHONE,
        PermissionCategory.SMS,
        PermissionCategory.CALENDAR,
        PermissionCategory.SENSORS,
        PermissionCategory.BLUETOOTH
    )

    fun calculate(
        securityFlags: SecurityFlags?,
        hasObfuscation: Boolean,
        permissions: List<PermissionInfo>
    ): SecurityScore {
        var score = 100
        val reasons = mutableListOf<String>()

        // Позволяет подключиться к приложению через ADB и читать память
        if (securityFlags?.isDebuggable == true) {
            score -= 30
            reasons.add("Включён режим отладки (debuggable) — критический риск")
        }

        // Данные передаются без шифрования — перехват в любой WiFi сети
        if (securityFlags?.usesCleartextTraffic == true) {
            score -= 25
            reasons.add("Разрешена передача данных без шифрования (cleartext traffic)")
        }

        // Без обфускации код читается как открытая книга
        if (!hasObfuscation) {
            score -= 15
            reasons.add("Код не обфусцирован — упрощает реверс-инжиниринг")
        }

        // Резервная копия через ADB без root — риск при физическом доступе
        if (securityFlags?.allowBackup == true) {
            score -= 10
            reasons.add("Разрешено резервное копирование данных (allowBackup)")
        }

        // За каждое опасное разрешение -2 балла, максимум -20 суммарно
        val dangerousPermissions = permissions.filter { it.category in DANGEROUS_CATEGORIES }
        val permissionPenalty = minOf(dangerousPermissions.size * 2, 20)
        if (permissionPenalty > 0) {
            score -= permissionPenalty
            reasons.add("Опасных разрешений: ${dangerousPermissions.size} (−$permissionPenalty баллов)")
        }

        val finalScore = maxOf(0, score)
        val riskLevel = when {
            finalScore >= 80 -> RiskLevel.LOW
            finalScore >= 50 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        return SecurityScore(
            score = finalScore,
            riskLevel = riskLevel,
            reasons = reasons
        )
    }
}
