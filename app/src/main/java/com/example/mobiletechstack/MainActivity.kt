package com.example.mobiletechstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.mobiletechstack.ui.screens.AppListScreen
import com.example.mobiletechstack.ui.screens.CompareScreen
import com.example.mobiletechstack.ui.screens.DetailScreen
import com.example.mobiletechstack.ui.screens.HistoryScreen
import com.example.mobiletechstack.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.AppList) }
    val listState = rememberLazyListState()

    BackHandler(enabled = currentScreen !is Screen.AppList) {
        currentScreen = Screen.AppList
    }

    when (val screen = currentScreen) {
        is Screen.AppList -> {
            AppListScreen(
                listState = listState,
                onAppClick = { packageName, appName ->
                    currentScreen = Screen.Detail(packageName, appName)
                },
                onCompareClick = {
                    currentScreen = Screen.SelectFirst
                },
                onHistoryClick = {
                    currentScreen = Screen.History
                },
                onAnalyzeUrlClick = {
                    currentScreen = Screen.AnalyzeUrl
                }
            )
        }

        is Screen.SelectFirst -> {
            AppListScreen(
                listState = listState,
                onAppClick = { _, _ -> },
                selectionMode = true,
                onBothSelected = { pkg1, name1, pkg2, name2 ->
                    currentScreen = Screen.Compare(pkg1, name1, pkg2, name2)
                }
            )
        }

        is Screen.Detail -> {
            DetailScreen(
                packageName = screen.packageName,
                onBackClick = {
                    currentScreen = Screen.AppList
                }
            )
        }

        is Screen.AnalyzeUrl -> {
            Scaffold(
                topBar = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text("Анализ APK по ссылке") },
                        navigationIcon = {
                            IconButton(onClick = { currentScreen = Screen.AppList }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Скоро", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        is Screen.History -> {
            HistoryScreen(
                onBackClick = { currentScreen = Screen.AppList },
                onAppClick = { packageName, appName ->
                    currentScreen = Screen.Detail(packageName, appName)
                }
            )
        }

        is Screen.Compare -> {

            CompareScreen(
                firstPackage = screen.firstPackage,
                firstName = screen.firstName,
                secondPackage = screen.secondPackage,
                secondName = screen.secondName,
                onBackClick = {
                    currentScreen = Screen.AppList
                }
            )
        }
    }
}

sealed class Screen {
    data object AppList : Screen()
    data class Detail(val packageName: String, val appName: String) : Screen()
    data object SelectFirst : Screen()
    data class Compare(
        val firstPackage: String,
        val firstName: String,
        val secondPackage: String,
        val secondName: String
    ) : Screen()
    data object AnalyzeUrl : Screen()
    data object History : Screen()
}