package com.example.mobiletechstack.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.domain.analyzer.APKAnalyzer
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val analyzer = APKAnalyzer(application)

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    fun analyzeApp(packageName: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading

            try {
                val result = analyzer.analyzeApp(packageName)
                _analysisState.value = AnalysisState.Success(result)
            } catch (e: Exception) {
                _analysisState.value = AnalysisState.Error(
                    e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }
}

sealed interface AnalysisState {
    object Idle : AnalysisState
    object Loading : AnalysisState
    data class Success(val result: AnalysisResult) : AnalysisState
    data class Error(val message: String) : AnalysisState
}