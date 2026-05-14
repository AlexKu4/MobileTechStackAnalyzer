package com.example.mobiletechstack.domain.model

// Итоговая оценка безопасности приложения
enum class RiskLevel { LOW, MEDIUM, HIGH }

data class SecurityScore(
    val score: Float,
    val riskLevel: RiskLevel,
    val reasons: List<String>,
    val verdict: String
)
