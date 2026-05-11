package com.example.mobiletechstack.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.AnalysisRepository
import com.example.mobiletechstack.domain.analyzer.APKAnalyzer
import com.example.mobiletechstack.domain.analyzer.ApkDownloader
import com.example.mobiletechstack.domain.model.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ViewModel экрана анализа внешнего APK — из файловой системы или по URL.
class AnalyzeExternalViewModel(application: Application) : AndroidViewModel(application) {

    private val analyzer = APKAnalyzer(application)
    private val downloader = ApkDownloader(application)
    private val repository = AnalysisRepository(
        AppDatabase.getInstance(application).analysisResultDao()
    )

    private val _state = MutableStateFlow<AnalyzeExternalState>(AnalyzeExternalState.Idle)
    val state: StateFlow<AnalyzeExternalState> = _state.asStateFlow()

    // Отдельный поток прогресса — чтобы прогрессбар обновлялся без перерисовки всего экрана
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    fun reset() {
        _state.value = AnalyzeExternalState.Idle
        _downloadProgress.value = 0
    }

    fun analyzeFromFile(filePath: String, displayName: String) {
        viewModelScope.launch {
            _state.value = AnalyzeExternalState.Analyzing
            try {
                val result = analyzer.analyzeExternalApk(filePath, displayName)
                repository.save(result)
                _state.value = AnalyzeExternalState.Success(result)
            } catch (e: Exception) {
                _state.value = AnalyzeExternalState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun analyzeFromUrl(url: String) {
        viewModelScope.launch {
            _state.value = AnalyzeExternalState.Downloading(0)
            _downloadProgress.value = 0

            var downloadedFile: File? = null
            try {
                val downloadResult = downloader.download(url) { progress ->
                    _downloadProgress.value = progress
                    _state.value = AnalyzeExternalState.Downloading(progress)
                }

                downloadedFile = downloadResult.getOrElse { error ->
                    _state.value = AnalyzeExternalState.Error(error.message ?: "Failed to download APK")
                    return@launch
                }

                _state.value = AnalyzeExternalState.Analyzing
                val result = analyzer.analyzeExternalApk(downloadedFile.absolutePath, downloadedFile.nameWithoutExtension)
                repository.save(result)
                _state.value = AnalyzeExternalState.Success(result)
            } catch (e: Exception) {
                _state.value = AnalyzeExternalState.Error(e.message ?: "Unknown error")
            } finally {
                // Скачанный APK уже не нужен — кэш не должен расти бесконтрольно
                downloadedFile?.delete()
            }
        }
    }
}

sealed class AnalyzeExternalState {
    object Idle : AnalyzeExternalState()
    data class Downloading(val progress: Int) : AnalyzeExternalState()
    object Analyzing : AnalyzeExternalState()
    data class Success(val result: AnalysisResult) : AnalyzeExternalState()
    data class Error(val message: String) : AnalyzeExternalState()
}
