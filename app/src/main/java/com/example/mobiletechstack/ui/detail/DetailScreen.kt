package com.example.mobiletechstack.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.domain.model.AppVersionInfo
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryCategory
import com.example.mobiletechstack.domain.model.PermissionCategory
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.SecurityFlags
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
            result.versionInfo?.let { versionInfo ->
                VersionInfoSection(versionInfo = versionInfo)
            }
        }

        item {
            result.securityFlags?.let { securityFlags ->
                SecurityFlagsSection(securityFlags = securityFlags)
            }
        }

        item {
            OutsideLibrariesSection(libraries = result.detectedLibraries)
        }

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
private fun OutsideLibrariesSection(libraries: List<DetectedLibrary>) {
    SectionCard(title = "Outside Libraries") {
        if (libraries.isEmpty()) {
            Text(
                text = "No outside libraries detected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val groupedLibraries = libraries.groupBy { it.category }

                groupedLibraries.forEach { (category, libs) ->
                    LibraryCategorySection(
                        category = category,
                        libraries = libs
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCategorySection(
    category: LibraryCategory,
    libraries: List<DetectedLibrary>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = libraries.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            libraries.forEach { library ->
                LibraryItem(library = library)
            }
        }
    }
}

@Composable
private fun LibraryItem(library: DetectedLibrary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = library.name,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = library.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VersionInfoSection(versionInfo: AppVersionInfo) {
    SectionCard(title = "Version & SDK") {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InfoRow("Version", "${versionInfo.versionName} (${versionInfo.versionCode})")
            InfoRow("Min SDK", formatSdkVersion(versionInfo.minSdkVersion))
            InfoRow("Target SDK", formatSdkVersion(versionInfo.targetSdkVersion))

            versionInfo.compileSdkVersion?.let { compileSdk ->
                InfoRow("Compile SDK", formatSdkVersion(compileSdk))
            }
        }
    }
}

@Composable
private fun SecurityFlagsSection(securityFlags: SecurityFlags) {
    SectionCard(title = "Security Flags") {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecurityFlagRow(
                label = "Debuggable",
                value = securityFlags.isDebuggable,
                description = if (securityFlags.isDebuggable)
                    "App can be debugged (security risk)"
                else
                    "Debugging disabled"
            )

            SecurityFlagRow(
                label = "Allow Backup",
                value = securityFlags.allowBackup,
                description = if (securityFlags.allowBackup)
                    "App data can be backed up via ADB"
                else
                    "Backup disabled"
            )

            SecurityFlagRow(
                label = "Cleartext Traffic",
                value = securityFlags.usesCleartextTraffic,
                description = if (securityFlags.usesCleartextTraffic)
                    "Unencrypted HTTP connections allowed"
                else
                    "Only HTTPS connections"
            )

            SecurityFlagRow(
                label = "Has Code",
                value = securityFlags.hasCode,
                description = if (securityFlags.hasCode)
                    "Contains executable code"
                else
                    "No executable code"
            )
        }
    }
}

@Composable
private fun SecurityFlagRow(
    label: String,
    value: Boolean,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (value) "Yes" else "No",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun formatSdkVersion(sdkVersion: Int): String {
    val androidVersion = when (sdkVersion) {
        35 -> "Android 15"
        34 -> "Android 14"
        33 -> "Android 13"
        32 -> "Android 12L"
        31 -> "Android 12"
        30 -> "Android 11"
        29 -> "Android 10"
        28 -> "Android 9"
        27 -> "Android 8.1"
        26 -> "Android 8.0"
        25 -> "Android 7.1"
        24 -> "Android 7.0"
        23 -> "Android 6.0"
        22 -> "Android 5.1"
        21 -> "Android 5.0"
        else -> if (sdkVersion > 35) "Android ${sdkVersion - 20}" else "API $sdkVersion"
    }

    return "API $sdkVersion ($androidVersion)"
}

enum class GrantedFilter {
    ALL,
    GRANTED,
    NOT_GRANTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSection(permissions: List<PermissionInfo>) {
    var grantedFilter by remember { mutableStateOf(GrantedFilter.ALL) }

    SectionCard(title = "Permissions (${permissions.size})") {
        if (permissions.isEmpty()) {
            Text(
                text = "No permissions requested",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GrantedFilterRow(
                    selectedFilter = grantedFilter,
                    onFilterChange = { grantedFilter = it },
                    permissions = permissions
                )

                Divider()

                val filteredPermissions = when (grantedFilter) {
                    GrantedFilter.ALL -> permissions
                    GrantedFilter.GRANTED -> permissions.filter { it.granted }
                    GrantedFilter.NOT_GRANTED -> permissions.filter { !it.granted }
                }

                if (filteredPermissions.isEmpty()) {
                    Text(
                        text = "No permissions match the filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    val groupedPermissions = filteredPermissions.groupBy { it.category }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrantedFilterRow(
    selectedFilter: GrantedFilter,
    onFilterChange: (GrantedFilter) -> Unit,
    permissions: List<PermissionInfo>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Filter by status:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == GrantedFilter.ALL,
                onClick = { onFilterChange(GrantedFilter.ALL) },
                label = { Text("All (${permissions.size})") }
            )

            val grantedCount = permissions.count { it.granted }
            FilterChip(
                selected = selectedFilter == GrantedFilter.GRANTED,
                onClick = { onFilterChange(GrantedFilter.GRANTED) },
                label = { Text("Granted ($grantedCount)") }
            )

            val notGrantedCount = permissions.count { !it.granted }
            FilterChip(
                selected = selectedFilter == GrantedFilter.NOT_GRANTED,
                onClick = { onFilterChange(GrantedFilter.NOT_GRANTED) },
                label = { Text("Not Granted ($notGrantedCount)") }
            )
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
                imageVector = if (permission.granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (permission.granted) "Granted" else "Not granted",
                tint = if (permission.granted)
                    Color(0xFF4CAF50)
                else
                    Color(0xFF9E9E9E),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = if (permission.granted) "Granted" else "Not granted",
                style = MaterialTheme.typography.bodySmall,
                color = if (permission.granted)
                    Color(0xFF4CAF50)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
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