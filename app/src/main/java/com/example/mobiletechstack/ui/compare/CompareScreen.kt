package com.example.mobiletechstack.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.ui.detail.AnalysisState
import com.example.mobiletechstack.ui.screens.CompareViewModel
import com.example.mobiletechstack.utils.formatSize

private val ColorLeft = Color(0xFF4CAF50)
private val ColorRight = Color(0xFFEF5350)

// Описание одной строки сравнения: метка и функция извлечения значения из результата
private class CompareRowDef(val label: String, val extract: (AnalysisResult) -> String)

private val rowDefs = listOf(
    CompareRowDef("Фреймворк")    { it.frameworkInfo?.type?.displayName ?: it.framework },
    CompareRowDef("Язык")         { it.languageInfo?.primary?.displayName ?: it.language },
    CompareRowDef("Primary ABI")  { it.primaryAbi },
    CompareRowDef("64-bit")       { if (it.is64Bit) "Yes" else "No" },
    CompareRowDef("Обфускация")   { if (it.hasObfuscation) "Yes" else "No" },
    CompareRowDef("Версия")       { it.versionInfo?.versionName ?: "-" },
    CompareRowDef("Min SDK")      { it.versionInfo?.minSdkVersion?.toString() ?: "-" },
    CompareRowDef("APK Size")     { it.apkSize.formatSize() },
    CompareRowDef("Библиотеки")   { it.detectedLibraries.size.toString() },
    CompareRowDef("Разрешения")   { it.permissions.size.toString() },
    CompareRowDef("Debuggable")   { if (it.securityFlags?.isDebuggable == true) "Yes" else "No" },
    CompareRowDef("Allow Backup") { if (it.securityFlags?.allowBackup == true) "Yes" else "No" },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    firstPackage: String,
    firstName: String,
    secondPackage: String,
    secondName: String,
    onBackClick: () -> Unit,
    viewModel: CompareViewModel = viewModel()
) {
    val firstState by viewModel.firstState.collectAsState()
    val secondState by viewModel.secondState.collectAsState()

    LaunchedEffect(firstPackage, secondPackage) {
        viewModel.analyze(firstPackage, secondPackage)
    }

    val bothLoaded = firstState is AnalysisState.Success && secondState is AnalysisState.Success
    val hasError = firstState is AnalysisState.Error || secondState is AnalysisState.Error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сравнение") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when {
            hasError -> {
                val errorMsg = (firstState as? AnalysisState.Error)?.message
                    ?: (secondState as? AnalysisState.Error)?.message
                    ?: "Ошибка анализа"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            !bothLoaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Анализ приложений...")
                }
            }
            else -> {
                val firstResult = (firstState as AnalysisState.Success).result
                val secondResult = (secondState as AnalysisState.Success).result

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppNamesHeader(firstName, secondName)
                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(rowDefs, key = { it.label }) { rowDef ->
                            CompareRow(
                                label = rowDef.label,
                                leftValue = rowDef.extract(firstResult),
                                rightValue = rowDef.extract(secondResult)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNamesHeader(firstName: String, secondName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = firstName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ColorLeft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = secondName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = ColorRight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompareRow(label: String, leftValue: String, rightValue: String) {
    val valuesMatch = leftValue == rightValue

    val leftColor = if (valuesMatch) MaterialTheme.colorScheme.onSurface else ColorLeft
    val rightColor = if (valuesMatch) MaterialTheme.colorScheme.onSurface else ColorRight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftValue,
            modifier = Modifier.weight(2f),
            color = leftColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (!valuesMatch) FontWeight.Medium else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = label,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = rightValue,
            modifier = Modifier.weight(2f),
            color = rightColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (!valuesMatch) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
