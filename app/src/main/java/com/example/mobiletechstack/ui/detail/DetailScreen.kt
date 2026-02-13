package com.example.mobiletechstack.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.ui.components.SectionCard
import com.example.mobiletechstack.utils.formatSize


private fun formatArchitecture(primaryAbi: String, is64Bit: Boolean): String {
    val bitness = if (is64Bit) "64-bit" else "32-bit"
    return "$primaryAbi ($bitness)"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    packageName: String,
    appName: String,
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val analysisState by viewModel.analysisState.collectAsState()

    LaunchedEffect(packageName) {
        viewModel.analyzeApp(packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(appName)
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = analysisState) {
                is AnalysisState.Idle -> { }

                is AnalysisState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("App analysis...")
                    }
                }

                is AnalysisState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.analyzeApp(packageName) }) {
                            Text("Retry")
                        }
                    }
                }

                is AnalysisState.Success -> {
                    DetailContent(result = state.result)
                }
            }
        }
    }
}

@Composable
private fun DetailContent(result: AnalysisResult) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SectionCard(title = "Tools:") {
                InfoRow("Framework", result.framework)
                InfoRow("Language", result.language)
                InfoRow("Architecture", formatArchitecture(result.primaryAbi, result.is64Bit))
                InfoRow("Package", result.packageName)
                InfoRow("APK size", result.apkSize.formatSize())
                InfoRow("Path", result.apkPath)
            }
        }

        item {
            SectionCard(title = "Native libraries (${result.nativeLibraries.size})") {
                if (result.nativeLibraries.isEmpty()) {
                    Text(
                        text = "Not found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val groupedLibs = result.nativeLibraries.groupBy { it.abi }

                    groupedLibs.forEach { (abi, libs) ->
                        Text(
                            text = "$abi (${libs.size} libs)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )

                        libs.forEach { lib ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = lib.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = lib.size.formatSize(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}