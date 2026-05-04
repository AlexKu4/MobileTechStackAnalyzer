package com.example.mobiletechstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobiletechstack.ui.components.AppCard
import com.example.mobiletechstack.utils.formatSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Search
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    listState: LazyListState,
    onAppClick: (packageName: String, appName: String) -> Unit,
    selectionMode: Boolean = false,
    selectionStep: Int = 1,
    onAppSelected: ((packageName: String, appName: String) -> Unit)? = null,
    onCompareClick: (() -> Unit)? = null,
    viewModel: AppsListViewModel = viewModel()
) {
    val allApps by viewModel.allApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val title = when {
        selectionMode && selectionStep == 2 -> "Выберите приложение 2"
        selectionMode -> "Выберите приложение 1"
        else -> "Mobile TechStack"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by name or package") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        if (!selectionMode) {
                            IconButton(onClick = { onCompareClick?.invoke() }) {
                                Icon(Icons.Default.Compare, contentDescription = "Compare apps")
                            }
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
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
                        Text("Loading apps...")
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadApps() }) {
                            Text("Retry")
                        }
                    }
                }
                filteredApps.isEmpty() -> {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No apps match \"$searchQuery\"" else "No apps found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredApps) { app ->
                            AppCard(
                                icon = app.icon,
                                appName = app.appName,
                                packageName = app.packageName,
                                versionName = app.versionName,
                                apkSize = app.apkSize.formatSize(),
                                showSelectionBadge = selectionMode,
                                onClick = {
                                    if (selectionMode && onAppSelected != null) {
                                        onAppSelected(app.packageName, app.appName)
                                    } else {
                                        onAppClick(app.packageName, app.appName)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

