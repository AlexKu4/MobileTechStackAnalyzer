package com.example.mobiletechstack.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.ui.components.SectionCard
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.PermissionCategory
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
            PermissionsSection(permissions = result.permissions)
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

@Composable
private fun PermissionsSection(permissions: List<PermissionInfo>) {
    SectionCard(title = "Permissions (${permissions.size})") {
        if (permissions.isEmpty()) {
            Text(
                text = "No permissions requested",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val groupedPermissions = permissions.groupBy { it.category }

            val sortedCategories = groupedPermissions.keys.sortedBy { it.displayName }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                sortedCategories.forEach { category ->
                    val categoryPermissions = groupedPermissions[category] ?: emptyList()

                    CategoryHeader(
                        category = category,
                        count = categoryPermissions.size
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        categoryPermissions.forEach { permission ->
                            PermissionItem(permission = permission)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: PermissionCategory,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PermissionItem(permission: PermissionInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = permission.name.substringAfterLast('.'),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (permission.granted) {
                    Icons.Default.Check
                } else {
                    Icons.Default.Close
                },
                contentDescription = if (permission.granted) "Granted" else "Not granted",
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = if (permission.granted) "Granted" else "Not granted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
