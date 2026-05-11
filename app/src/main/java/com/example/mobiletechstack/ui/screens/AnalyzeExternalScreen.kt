package com.example.mobiletechstack.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeExternalScreen(
    onBackClick: () -> Unit,
    onAnalysisComplete: (com.example.mobiletechstack.domain.model.AnalysisResult) -> Unit,
    viewModel: AnalyzeExternalViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Системный пикер APK через SAF — отдаёт content:// URI, файл копируем в кэш сами
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val (filePath, displayName) = copyPickedApkToCache(context, uri)
                viewModel.analyzeFromFile(filePath, displayName)
            }
        }
    }

    LaunchedEffect(state) {
        val current = state
        if (current is AnalyzeExternalState.Success) {
            val result = current.result
            viewModel.reset()
            onAnalysisComplete(result)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyze External APK") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val current = state) {
                AnalyzeExternalState.Idle -> IdleContent(
                    onPickFileClick = { filePickerLauncher.launch("application/vnd.android.package-archive") },
                    onAnalyzeUrl = { url -> viewModel.analyzeFromUrl(url) }
                )

                is AnalyzeExternalState.Downloading -> ProgressContent(
                    text = "Downloading... ${current.progress}%",
                    showLinear = true,
                    progress = current.progress
                )

                AnalyzeExternalState.Analyzing -> ProgressContent(
                    text = "Analyzing APK...",
                    showLinear = false,
                    progress = 0
                )

                is AnalyzeExternalState.Error -> ErrorContent(
                    message = current.message,
                    onTryAgain = { viewModel.reset() }
                )

                // Success обрабатывается через LaunchedEffect — пока идёт навигация показываем плейсхолдер
                is AnalyzeExternalState.Success -> ProgressContent(
                    text = "Analyzing APK...",
                    showLinear = false,
                    progress = 0
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    onPickFileClick: () -> Unit,
    onAnalyzeUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Карточка-кнопка: системный file picker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickFileClick)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Pick APK file", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Select .apk file from your device",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        var url by remember { mutableStateOf("") }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Download by URL", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Paste a direct link to .apk file (APKPure, F-Droid, GitHub)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Use direct download links from APKPure, F-Droid or GitHub Releases",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onAnalyzeUrl(url.trim()) },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Analyze")
                }
            }
        }
    }
}

@Composable
private fun ProgressContent(text: String, showLinear: Boolean, progress: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text)
            if (showLinear) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onTryAgain: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onTryAgain) {
                Text("Try again")
            }
        }
    }
}

// SAF отдаёт content:// URI, который ZipFile открыть не может — копируем в cacheDir и работаем с обычным путём
private suspend fun copyPickedApkToCache(context: Context, uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
    val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: "picked_apk.apk"

    val pickedDir = File(context.cacheDir, "picked_apk").apply { if (!exists()) mkdirs() }
    val outFile = File(pickedDir, displayName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        outFile.outputStream().use { output -> input.copyTo(output) }
    }
    val readableName = displayName.substringBeforeLast('.', displayName)
    Pair(outFile.absolutePath, readableName)
}
