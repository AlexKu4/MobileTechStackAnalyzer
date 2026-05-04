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
    CompareRowDef("Фреймворк")   { it.frameworkInfo?.type?.displayName ?: it.framework },
    CompareRowDef("Язык")        { it.languageInfo?.primary?.displayName ?: it.language },
    CompareRowDef("Primary ABI") { it.primaryAbi },
    CompareRowDef("64-bit")      { if (it.is64Bit) "Yes" else "No" },
    CompareRowDef("Обфускация")  { if (it.hasObfuscation) "Yes" else "No" },
    CompareRowDef("Версия")      { it.versionInfo?.versionName ?: "-" },
    CompareRowDef("Min SDK")     { it.versionInfo?.minSdkVersion?.toString() ?: "-" },
    CompareRowDef("APK Size")    { it.apkSize.formatSize() },
    CompareRowDef("Библиотеки")  { it.detectedLibraries.size.toString() },
    CompareRowDef("Разрешения")  { it.permissions.size.toString() },
    CompareRowDef("Debuggable")  { if (it.securityFlags?.isDebuggable == true) "Yes" else "No" },
    CompareRowDef("Allow Backup") { if (it.securityFlags?.allowBackup == true) "Yes" else "No" },
)

private sealed class CompareCell {
    data class Value(val text: String) : CompareCell()
    object Loading : CompareCell()
    object Err : CompareCell()
}

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppNamesHeader(firstName, secondName)
            HorizontalDivider()

            val anyLoaded = firstState is AnalysisState.Success || firstState is AnalysisState.Error ||
                            secondState is AnalysisState.Success || secondState is AnalysisState.Error

            if (!anyLoaded) {
                // Оба ещё грузятся — показываем два индикатора рядом
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = ColorLeft)
                        CircularProgressIndicator(color = ColorRight)
                    }
                }
            } else {
                val firstResult = (firstState as? AnalysisState.Success)?.result
                val secondResult = (secondState as? AnalysisState.Success)?.result

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(rowDefs, key = { it.label }) { rowDef ->
                        val leftCell = when {
                            firstResult != null -> CompareCell.Value(rowDef.extract(firstResult))
                            firstState is AnalysisState.Error -> CompareCell.Err
                            else -> CompareCell.Loading
                        }
                        val rightCell = when {
                            secondResult != null -> CompareCell.Value(rowDef.extract(secondResult))
                            secondState is AnalysisState.Error -> CompareCell.Err
                            else -> CompareCell.Loading
                        }
                        CompareRow(label = rowDef.label, leftCell = leftCell, rightCell = rightCell)
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
private fun CompareRow(label: String, leftCell: CompareCell, rightCell: CompareCell) {
    val leftValue = (leftCell as? CompareCell.Value)?.text
    val rightValue = (rightCell as? CompareCell.Value)?.text
    // Выделяем цветом только если оба значения известны и различаются
    val valuesMatch = leftValue != null && rightValue != null && leftValue == rightValue

    val leftColor = when {
        leftCell !is CompareCell.Value || valuesMatch -> MaterialTheme.colorScheme.onSurface
        else -> ColorLeft
    }
    val rightColor = when {
        rightCell !is CompareCell.Value || valuesMatch -> MaterialTheme.colorScheme.onSurface
        else -> ColorRight
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левое значение
        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.CenterStart) {
            when (leftCell) {
                is CompareCell.Value -> Text(
                    text = leftCell.text,
                    color = leftColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!valuesMatch && leftValue != null) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                CompareCell.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                CompareCell.Err -> Text("-", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Название параметра по центру
        Text(
            text = label,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Правое значение
        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) {
            when (rightCell) {
                is CompareCell.Value -> Text(
                    text = rightCell.text,
                    color = rightColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!valuesMatch && rightValue != null) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                CompareCell.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                CompareCell.Err -> Text("-", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End)
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
