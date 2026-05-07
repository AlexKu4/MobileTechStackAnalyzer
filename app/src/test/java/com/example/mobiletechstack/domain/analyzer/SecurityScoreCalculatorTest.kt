package com.example.mobiletechstack.domain.analyzer

import com.example.mobiletechstack.domain.model.PermissionCategory
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.RiskLevel
import com.example.mobiletechstack.domain.model.SecurityFlags
import org.junit.Assert.*
import org.junit.Test

class SecurityScoreCalculatorTest {

    private fun flags(
        isDebuggable: Boolean = false,
        allowBackup: Boolean = false,
        usesCleartextTraffic: Boolean = false
    ) = SecurityFlags(
        isDebuggable = isDebuggable,
        allowBackup = allowBackup,
        usesCleartextTraffic = usesCleartextTraffic,
        hasCode = true
    )

    private fun perm(category: PermissionCategory) =
        PermissionInfo("android.permission.${category.name}", granted = true, category = category)

    // ── штрафы ──────────────────────────────────────────────────────────────

    @Test
    fun perfectApp_scores100() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(100, result.score)
    }

    @Test
    fun debuggable_deducts30() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(70, result.score)
    }

    @Test
    fun cleartextTraffic_deducts25() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(usesCleartextTraffic = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(75, result.score)
    }

    @Test
    fun noObfuscation_deducts15() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = false,
            permissions = emptyList()
        )
        assertEquals(85, result.score)
    }

    @Test
    fun allowBackup_deducts10() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(allowBackup = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(90, result.score)
    }

    @Test
    fun oneDangerousPermission_deducts2() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = listOf(perm(PermissionCategory.CAMERA))
        )
        assertEquals(98, result.score)
    }

    @Test
    fun tenDangerousPermissions_deducts20() {
        val perms = listOf(
            perm(PermissionCategory.CAMERA),
            perm(PermissionCategory.LOCATION),
            perm(PermissionCategory.STORAGE),
            perm(PermissionCategory.MICROPHONE),
            perm(PermissionCategory.CONTACTS),
            perm(PermissionCategory.PHONE),
            perm(PermissionCategory.SMS),
            perm(PermissionCategory.CALENDAR),
            perm(PermissionCategory.SENSORS),
            perm(PermissionCategory.BLUETOOTH)
        )
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = perms
        )
        assertEquals(80, result.score)
    }

    @Test
    fun moreThan10DangerousPermissions_penaltyCappedAt20() {
        // 15 опасных разрешений — штраф не может превысить 20
        val perms = List(15) { perm(PermissionCategory.CAMERA) }
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = perms
        )
        assertEquals(80, result.score)
    }

    @Test
    fun allWorstCaseFlags_scoreIs0() {
        // 100 - 30 - 25 - 15 - 10 - 20 = 0
        val perms = listOf(
            perm(PermissionCategory.CAMERA),
            perm(PermissionCategory.LOCATION),
            perm(PermissionCategory.STORAGE),
            perm(PermissionCategory.MICROPHONE),
            perm(PermissionCategory.CONTACTS),
            perm(PermissionCategory.PHONE),
            perm(PermissionCategory.SMS),
            perm(PermissionCategory.CALENDAR),
            perm(PermissionCategory.SENSORS),
            perm(PermissionCategory.BLUETOOTH)
        )
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = perms
        )
        assertEquals(0, result.score)
    }

    @Test
    fun scoreNeverGoesBelowZero() {
        val perms = List(100) { perm(PermissionCategory.CAMERA) }
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = perms
        )
        assertTrue(result.score >= 0)
    }

    // ── null flags ───────────────────────────────────────────────────────────

    @Test
    fun nullFlags_noFlagPenaltiesApplied() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = null,
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(100, result.score)
    }

    @Test
    fun nullFlags_withNoObfuscation_only15deducted() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = null,
            hasObfuscation = false,
            permissions = emptyList()
        )
        assertEquals(85, result.score)
    }

    // ── безопасные разрешения не считаются ──────────────────────────────────

    @Test
    fun networkAndSystemPermissions_doNotAffectScore() {
        val perms = listOf(
            PermissionInfo("android.permission.INTERNET", granted = true, category = PermissionCategory.NETWORK),
            PermissionInfo("android.permission.VIBRATE", granted = true, category = PermissionCategory.SYSTEM),
            PermissionInfo("android.permission.UNKNOWN", granted = true, category = PermissionCategory.OTHER)
        )
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = perms
        )
        assertEquals(100, result.score)
    }

    // ── уровни риска ─────────────────────────────────────────────────────────

    @Test
    fun riskLevel_low_whenScore100() {
        val result = SecurityScoreCalculator.calculate(flags(), true, emptyList())
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun riskLevel_low_atBoundary80() {
        // ровно 10 опасных разрешений → 100 - 20 = 80
        val perms = listOf(
            perm(PermissionCategory.CAMERA), perm(PermissionCategory.LOCATION),
            perm(PermissionCategory.STORAGE), perm(PermissionCategory.MICROPHONE),
            perm(PermissionCategory.CONTACTS), perm(PermissionCategory.PHONE),
            perm(PermissionCategory.SMS), perm(PermissionCategory.CALENDAR),
            perm(PermissionCategory.SENSORS), perm(PermissionCategory.BLUETOOTH)
        )
        val result = SecurityScoreCalculator.calculate(flags(), true, perms)
        assertEquals(80, result.score)
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun riskLevel_medium_whenScore70() {
        // debuggable → 70
        val result = SecurityScoreCalculator.calculate(flags(isDebuggable = true), true, emptyList())
        assertEquals(70, result.score)
        assertEquals(RiskLevel.MEDIUM, result.riskLevel)
    }

    @Test
    fun riskLevel_high_whenScore45() {
        // debuggable + cleartext → 100 - 30 - 25 = 45
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, usesCleartextTraffic = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(45, result.score)
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    @Test
    fun riskLevel_high_whenScore0() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = List(10) { perm(PermissionCategory.CAMERA) }
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    // ── reasons ──────────────────────────────────────────────────────────────

    @Test
    fun noIssues_reasonsIsEmpty() {
        val result = SecurityScoreCalculator.calculate(flags(), true, emptyList())
        assertTrue(result.reasons.isEmpty())
    }

    @Test
    fun allPenalties_reasonsHasFiveEntries() {
        // debuggable + cleartext + allowBackup + no obfuscation + 1 dangerous perm = 5 причин
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = listOf(perm(PermissionCategory.CAMERA))
        )
        assertEquals(5, result.reasons.size)
    }

    @Test
    fun debuggable_reasonMentionsDebuggable() {
        val result = SecurityScoreCalculator.calculate(flags(isDebuggable = true), true, emptyList())
        assertTrue(result.reasons.any { "debuggable" in it.lowercase() })
    }

    @Test
    fun noObfuscation_reasonMentionsObfuscation() {
        val result = SecurityScoreCalculator.calculate(flags(), false, emptyList())
        assertTrue(result.reasons.any { "обфусц" in it })
    }

    @Test
    fun dangerousPermissions_reasonContainsCount() {
        val perms = listOf(perm(PermissionCategory.CAMERA), perm(PermissionCategory.LOCATION))
        val result = SecurityScoreCalculator.calculate(flags(), true, perms)
        assertTrue(result.reasons.any { "2" in it })
    }
}
