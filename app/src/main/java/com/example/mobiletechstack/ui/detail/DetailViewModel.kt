package com.example.mobiletechstack.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.AnalysisRepository
import com.example.mobiletechstack.domain.analyzer.APKAnalyzer
import com.example.mobiletechstack.domain.model.AnalysisResult
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

    fun analyzeApp(packageName: String) {
        viewModelScope.launch {
            val cached = repository.getCached(packageName)
            if (cached != null) {
                // Показываем кэш сразу, чтобы экран не был пустым пока идёт свежий анализ
                _analysisState.value = AnalysisState.Success(cached, fromCache = true)
                _lastAnalyzedAt.value = repository.getLastAnalyzedAt(packageName)
            } else {
                _analysisState.value = AnalysisState.Loading
            }

            try {
                val result = analyzer.analyzeApp(packageName)
                repository.save(result)
                _analysisState.value = AnalysisState.Success(result, fromCache = false)
            } catch (e: Exception) {
                // Если кэш уже показан — не перекрываем его ошибкой
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
