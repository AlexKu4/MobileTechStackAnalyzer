package com.example.mobiletechstack.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.domain.analyzer.APKAnalyzer
import com.example.mobiletechstack.ui.detail.AnalysisState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// ViewModel для экрана сравнения двух приложений
class CompareViewModel(application: Application) : AndroidViewModel(application) {

    private val analyzer = APKAnalyzer(application)

    val firstState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val secondState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)

    fun analyze(firstPackage: String, secondPackage: String) {
        viewModelScope.launch {
            // Запускаем оба анализа параллельно до установки Loading, чтобы не терять время
            val firstDeferred = async(Dispatchers.IO) { analyzer.analyzeApp(firstPackage) }
            val secondDeferred = async(Dispatchers.IO) { analyzer.analyzeApp(secondPackage) }

            firstState.value = AnalysisState.Loading
            secondState.value = AnalysisState.Loading

            try {
                firstState.value = AnalysisState.Success(firstDeferred.await())
            } catch (e: Exception) {
                firstState.value = AnalysisState.Error(e.message ?: "Ошибка")
            }

            try {
                secondState.value = AnalysisState.Success(secondDeferred.await())
            } catch (e: Exception) {
                secondState.value = AnalysisState.Error(e.message ?: "Ошибка")
            }
        }
    }
}
