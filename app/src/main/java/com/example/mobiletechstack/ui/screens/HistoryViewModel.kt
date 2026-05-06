package com.example.mobiletechstack.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.AnalysisRepository
import com.example.mobiletechstack.data.repository.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Показывает список всех ранее проанализированных приложений из Room
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(
        AppDatabase.getInstance(application).analysisResultDao()
    )

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _history.value = withContext(Dispatchers.IO) {
                repository.getHistory()
            }
            _isLoading.value = false
        }
    }

    // Проверяет установлено ли приложение — нужно для пометки External APK
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) { false }
    }
}
