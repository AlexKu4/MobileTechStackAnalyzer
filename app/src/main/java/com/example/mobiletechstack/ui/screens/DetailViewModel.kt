package com.example.mobiletechstack.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.AnalysisRepository
import com.example.mobiletechstack.domain.analyzer.APKAnalyzer
import com.example.mobiletechstack.domain.analyzer.SecurityScoreCalculator
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.domain.model.SecurityScore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val analyzer = APKAnalyzer(application)
    private val repository = AnalysisRepository(
        AppDatabase.getInstance(application).analysisResultDao()
    )

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _lastAnalyzedAt = MutableStateFlow<Long?>(null)
    val lastAnalyzedAt: StateFlow<Long?> = _lastAnalyzedAt.asStateFlow()

    // Балл безопасности считается во ViewModel, APKAnalyzer не трогаем
    private val _securityScore = MutableStateFlow<SecurityScore?>(null)
    val securityScore: StateFlow<SecurityScore?> = _securityScore.asStateFlow()

    fun showCached(packageName: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading
            val cached = repository.getCached(packageName)
            if (cached != null) {
                _lastAnalyzedAt.value = repository.getLastAnalyzedAt(packageName)
                _analysisState.value = AnalysisState.Success(cached, fromCache = true)
                _securityScore.value = SecurityScoreCalculator.calculate(
                    securityFlags = cached.securityFlags,
                    hasObfuscation = cached.hasObfuscation,
                    permissions = cached.permissions
                )
            } else {
                _analysisState.value = AnalysisState.Error("Нет данных в кэше")
            }
        }
    }

    fun analyzeApp(packageName: String) {
        viewModelScope.launch {
            var cached: AnalysisResult? = null
            try {
                cached = repository.getCached(packageName)
            } catch (e: Exception) {
                // битый кэш, продолаем без него
            }

            if (cached != null) {
                _analysisState.value = AnalysisState.Success(cached, fromCache = true)
                _lastAnalyzedAt.value = repository.getLastAnalyzedAt(packageName)
                _securityScore.value = SecurityScoreCalculator.calculate(
                    securityFlags = cached.securityFlags,
                    hasObfuscation = cached.hasObfuscation,
                    permissions = cached.permissions
                )
            } else {
                _analysisState.value = AnalysisState.Loading
            }

            try {
                val result = analyzer.analyzeApp(packageName)
                repository.save(result)
                _analysisState.value = AnalysisState.Success(result, fromCache = false)
                _securityScore.value = SecurityScoreCalculator.calculate(
                    securityFlags = result.securityFlags,
                    hasObfuscation = result.hasObfuscation,
                    permissions = result.permissions
                )
            } catch (e: Exception) {
                if (cached == null) {
                    _analysisState.value = AnalysisState.Error(e.message ?: "Неизвестная ошибка")
                }
            }
        }
    }
}

sealed interface AnalysisState {
    object Idle : AnalysisState
    object Loading : AnalysisState
    data class Success(val result: AnalysisResult, val fromCache: Boolean = false) : AnalysisState
    data class Error(val message: String) : AnalysisState
}
