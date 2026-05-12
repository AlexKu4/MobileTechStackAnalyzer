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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    listState: LazyListState,
    onAppClick: (packageName: String, appName: String) -> Unit,
    selectionMode: Boolean = false,
    onBothSelected: ((String, String, String, String) -> Unit)? = null,
    onCompareClick: (() -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    onAnalyzeExternalClick: (() -> Unit)? = null,
    viewModel: AppsListViewModel = viewModel()
) {
    val allApps by viewModel.allApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var firstPkg by remember { mutableStateOf<String?>(null) }
    var firstName by remember { mutableStateOf<String?>(null) }
    var secondPkg by remember { mutableStateOf<String?>(null) }
    var secondName by remember { mutableStateOf<String?>(null) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val title = if (selectionMode) "Выберите два приложения" else "Mobile TechStack"

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MobileTechStack",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Compare, contentDescription = null) },
                    label = { Text("Сравнение приложений") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCompareClick?.invoke()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("История анализов") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onHistoryClick?.invoke()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Link, contentDescription = null) },
                    label = { Text("Analyze external APK") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAnalyzeExternalClick?.invoke()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
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
                        } else if (!selectionMode) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Меню")
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
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
            },
            floatingActionButton = {
                if (selectionMode && firstPkg != null && secondPkg != null) {
                    ExtendedFloatingActionButton(
                        onClick = { onBothSelected?.invoke(firstPkg!!, firstName!!, secondPkg!!, secondName!!) },
                        text = { Text("Сравнить") },
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
                    )
                }
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
                                    isSelected = app.packageName == firstPkg || app.packageName == secondPkg,
                                    onClick = {
                                        if (selectionMode) {
                                            when (app.packageName) {
                                                firstPkg -> { firstPkg = null; firstName = null }
                                                secondPkg -> { secondPkg = null; secondName = null }
                                                else -> {
                                                    if (firstPkg == null) {
                                                        firstPkg = app.packageName
                                                        firstName = app.appName
                                                    } else {
                                                        // Заменяем второй выбор (или устанавливаем, если пуст)
                                                        secondPkg = app.packageName
                                                        secondName = app.appName
                                                    }
                                                }
                                            }
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
}
