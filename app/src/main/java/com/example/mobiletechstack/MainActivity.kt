package com.example.mobiletechstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mobiletechstack.ui.screens.AppListScreen
import com.example.mobiletechstack.ui.detail.DetailScreen
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

    when (val screen = currentScreen) {
        is Screen.AppList -> {
            AppListScreen(
                onAppClick = { packageName, appName ->
                    currentScreen = Screen.Detail(packageName, appName)
                }
            )
        }

        is Screen.Detail -> {
            DetailScreen(
                packageName = screen.packageName,
                appName = screen.appName,
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
}