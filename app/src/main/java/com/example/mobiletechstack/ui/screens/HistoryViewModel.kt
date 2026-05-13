package com.example.mobiletechstack.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.AnalysisRepository
import com.example.mobiletechstack.data.repository.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn


class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository(
        AppDatabase.getInstance(application).analysisResultDao()
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val history: StateFlow<List<HistoryEntry>> = repository.observeHistory()
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) { false }
    }
}
