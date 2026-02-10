package com.example.mobiletechstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mobiletechstack.domain.model.AppInfo
import com.example.mobiletechstack.ui.components.AppCard
import com.example.mobiletechstack.utils.formatSize
import com.example.mobiletechstack.utils.getInstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                apps = withContext(Dispatchers.IO) {
                    context.packageManager.getInstalledApps()
                }

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Load error:: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mobile TechStack",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!isLoading && apps.isNotEmpty()) {
                            Text(
                                text = "${apps.size} apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Load apps list")
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    apps = withContext(Dispatchers.IO) {
                                        context.packageManager.getInstalledApps()
                                    }
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }

                apps.isEmpty() -> {
                    Text(
                        text = "Apps not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(apps) { app ->
                            AppCard(
                                icon = app.icon,
                                appName = app.appName,
                                packageName = app.packageName,
                                versionName = app.versionName,
                                apkSize = app.apkSize.formatSize(),
                                onClick = {
                                    // TODO: detail analysis
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}