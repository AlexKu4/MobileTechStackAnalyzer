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
        assertEquals(100f, result.score, 0f)
    }

    @Test
    fun debuggable_deducts30() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(70f, result.score, 0f)
    }

    @Test
    fun cleartextTraffic_deducts25() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(usesCleartextTraffic = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(75f, result.score, 0f)
    }

    @Test
    fun noObfuscation_deducts15() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = false,
            permissions = emptyList()
        )
        assertEquals(85f, result.score, 0f)
    }

    @Test
    fun allowBackup_deducts5() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(allowBackup = true),
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(95f, result.score, 0f)
    }

    @Test
    fun oneCriticalPermission_deducts2() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = listOf(perm(PermissionCategory.CAMERA))
        )
        assertEquals(98f, result.score, 0f)
    }

    @Test
    fun oneModeratePermission_deducts1() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = listOf(perm(PermissionCategory.PHONE))
        )
        assertEquals(99f, result.score, 0f)
    }

    @Test
    fun oneLowPermission_deducts0point5() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = listOf(perm(PermissionCategory.BLUETOOTH))
        )
        assertEquals(99.5f, result.score, 0.001f)
    }

    @Test
    fun tenDangerousPermissions_deducts12point5() {
        // 4 critical×2 + 3 moderate×1 + 3 low×0.5 = 8+3+1.5 = 12.5
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
        assertEquals(87.5f, result.score, 0.001f)
    }

    @Test
    fun penaltyCappedAt15() {
        // 15 критических разрешений × 2 = 30, но лимит 15
        val perms = List(15) { perm(PermissionCategory.CAMERA) }
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(),
            hasObfuscation = true,
            permissions = perms
        )
        assertEquals(85f, result.score, 0f)
    }

    @Test
    fun allWorstCaseFlags_scoreIs12point5() {
        // 100 - 30 - 25 - 15 - 5 - 12.5 = 12.5
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
        assertEquals(12.5f, result.score, 0.001f)
    }

    @Test
    fun scoreNeverGoesBelowZero() {
        val perms = List(100) { perm(PermissionCategory.CAMERA) }
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = perms
        )
        assertTrue(result.score >= 0f)
    }

    // ── null flags ───────────────────────────────────────────────────────────

    @Test
    fun nullFlags_noFlagPenaltiesApplied() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = null,
            hasObfuscation = true,
            permissions = emptyList()
        )
        assertEquals(100f, result.score, 0f)
    }

    @Test
    fun nullFlags_withNoObfuscation_only15deducted() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = null,
            hasObfuscation = false,
            permissions = emptyList()
        )
        assertEquals(85f, result.score, 0f)
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
        assertEquals(100f, result.score, 0f)
    }

    // ── уровни риска ─────────────────────────────────────────────────────────

    @Test
    fun riskLevel_low_whenScore100() {
        val result = SecurityScoreCalculator.calculate(flags(), true, emptyList())
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun riskLevel_low_atBoundary87point5() {
        // 4 critical + 3 moderate + 3 low → 12.5 penalty → score = 87.5 → LOW
        val perms = listOf(
            perm(PermissionCategory.CAMERA), perm(PermissionCategory.LOCATION),
            perm(PermissionCategory.STORAGE), perm(PermissionCategory.MICROPHONE),
            perm(PermissionCategory.CONTACTS), perm(PermissionCategory.PHONE),
            perm(PermissionCategory.SMS), perm(PermissionCategory.CALENDAR),
            perm(PermissionCategory.SENSORS), perm(PermissionCategory.BLUETOOTH)
        )
        val result = SecurityScoreCalculator.calculate(flags(), true, perms)
        assertEquals(87.5f, result.score, 0.001f)
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun riskLevel_medium_whenScore70() {
        // debuggable → 70
        val result = SecurityScoreCalculator.calculate(flags(isDebuggable = true), true, emptyList())
        assertEquals(70f, result.score, 0f)
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
        assertEquals(45f, result.score, 0f)
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    @Test
    fun riskLevel_high_withAllPenalties() {
        val result = SecurityScoreCalculator.calculate(
            securityFlags = flags(isDebuggable = true, allowBackup = true, usesCleartextTraffic = true),
            hasObfuscation = false,
            permissions = List(10) { perm(PermissionCategory.CAMERA) }
        )
        assertEquals(RiskLevel.HIGH, result.riskLevel)
    }

    // ── reasons ──────────────────────────────────────────────────────────────

    @Test
    fun noIssues_reasonsContainOkMessages() {
        val result = SecurityScoreCalculator.calculate(flags(), true, emptyList())
        assertTrue(result.reasons.all { "OK" in it })
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
        assertTrue(result.reasons.any { "obfuscat" in it.lowercase() })
    }

    @Test
    fun dangerousPermissions_reasonContainsCount() {
        val perms = listOf(perm(PermissionCategory.CAMERA), perm(PermissionCategory.LOCATION))
        val result = SecurityScoreCalculator.calculate(flags(), true, perms)
        assertTrue(result.reasons.any { "2" in it })
    }
}
