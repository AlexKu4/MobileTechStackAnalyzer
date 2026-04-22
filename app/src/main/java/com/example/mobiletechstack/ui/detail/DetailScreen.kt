package com.example.mobiletechstack.ui.detail

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletechstack.domain.model.AnalysisResult
import com.example.mobiletechstack.domain.model.AppVersionInfo
import com.example.mobiletechstack.domain.model.DetectedLibrary
import com.example.mobiletechstack.domain.model.LibraryInfo
import com.example.mobiletechstack.domain.model.PermissionCategory
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.SecurityFlags
import com.example.mobiletechstack.ui.components.SectionCard
import com.example.mobiletechstack.utils.formatSize
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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
                title = { Text("Mobile TechStack") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                is AnalysisState.Idle -> {}
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
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.analyzeApp(packageName) }) {
                            Text("Retry")
                        }
                    }
                }
                is AnalysisState.Success -> {
                    DetailTabs(result = state.result, packageName = packageName)
                }
            }
        }
    }
}

@Composable
private fun DetailTabs(result: AnalysisResult, packageName: String) {
    val tabs = listOf("Overview", "Security", "Permissions", "Libraries", "Native")
    var selectedTab by remember { mutableStateOf(0) }

    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> OverviewTab(result, packageName)
            1 -> SecurityTab(result.versionInfo, result.securityFlags)
            2 -> PermissionsTab(result.permissions)
            3 -> LibrariesTab(result.detectedLibraries)
            4 -> NativeLibrariesTab(result.nativeLibraries)
        }
    }
}

@Composable
private fun OverviewTab(result: AnalysisResult, packageName: String) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                appIcon = appInfo.loadIcon(pm)
            } catch (e: Exception) {
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon!!.toBitmap(width = 48, height = 48).asImageBitmap(),
                                contentDescription = "App icon",
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = result.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = result.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val versionInfo = result.versionInfo
                    val security = result.securityFlags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoRowCompact("Version", "${versionInfo?.versionName ?: "Unknown"} (${versionInfo?.versionCode ?: "?"})")
                            InfoRowCompact("Min SDK", versionInfo?.minSdkVersion?.let { formatSdkVersion(it) } ?: "Unknown")
                            InfoRowCompact("Target SDK", versionInfo?.targetSdkVersion?.let { formatSdkVersion(it) } ?: "Unknown")
                            InfoRowCompact("APK size", result.apkSize.formatSize())
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            InfoRowCompact("Path", result.apkPath.takeLast(30))
                            InfoRowCompact("Debuggable", if (security?.isDebuggable == true) "Yes" else "No")
                            InfoRowCompact("Allow Backup", if (security?.allowBackup == true) "Yes" else "No")
                            InfoRowCompact("Cleartext", if (security?.usesCleartextTraffic == true) "Yes" else "No")
                        }
                    }
                }
            }
        }


        item {
            SectionCard(title = "Framework & Languages") {
                result.frameworkInfo?.let {
                    InfoRow("Framework", it.type.displayName)
                }
                result.languageInfo?.let {
                    InfoRow("Primary Language", it.primary.displayName)
                    if (it.languages.size > 1) {
                        InfoRow("All Languages", it.languages.joinToString(", ") { lang -> lang.displayName })
                    }
                }
            }
        }
        item {
            SectionCard(title = "Architecture") {
                InfoRow("Primary ABI", result.primaryAbi)
                InfoRow("64-bit", if (result.is64Bit) "Yes" else "No")
                InfoRow("Supported ABIs", result.supportedAbis.joinToString(", "))
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityTab(versionInfo: AppVersionInfo?, securityFlags: SecurityFlags?) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SectionCard(title = "Version & SDK") {
                if (versionInfo != null) {
                    InfoRow("Version", "${versionInfo.versionName} (${versionInfo.versionCode})")
                    InfoRow("Min SDK", formatSdkVersion(versionInfo.minSdkVersion))
                    InfoRow("Target SDK", formatSdkVersion(versionInfo.targetSdkVersion))
                    versionInfo.compileSdkVersion?.let {
                        InfoRow("Compile SDK", formatSdkVersion(it))
                    }
                } else {
                    Text("No version info available")
                }
            }
        }
        item {
            SectionCard(title = "Security Flags") {
                if (securityFlags != null) {
                    SecurityFlagRow("Debuggable", securityFlags.isDebuggable,
                        if (securityFlags.isDebuggable) "App can be debugged (security risk)" else "Debugging disabled")
                    SecurityFlagRow("Allow Backup", securityFlags.allowBackup,
                        if (securityFlags.allowBackup) "App data can be backed up via ADB" else "Backup disabled")
                    SecurityFlagRow("Cleartext Traffic", securityFlags.usesCleartextTraffic,
                        if (securityFlags.usesCleartextTraffic) "Unencrypted HTTP allowed" else "Only HTTPS")
                    SecurityFlagRow("Has Code", securityFlags.hasCode,
                        if (securityFlags.hasCode) "Contains executable code" else "No executable code")
                } else {
                    Text("No security flags available")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionsTab(permissions: List<PermissionInfo>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            PermissionsSection(permissions)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LibrariesTab(libraries: List<DetectedLibrary>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            OutsideLibrariesSection(libraries)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NativeLibrariesTab(nativeLibs: List<LibraryInfo>) {
    if (nativeLibs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No native libraries found")
        }
        return
    }

    val grouped = nativeLibs.groupBy { it.abi }
    val abis = grouped.keys.sortedWith(compareByDescending<String> { it.contains("64") }.thenBy { it })
    var selectedAbiIndex by remember { mutableStateOf(0) }

    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedAbiIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp
        ) {
            abis.forEachIndexed { index, abi ->
                Tab(
                    text = { Text("$abi (${grouped[abi]?.size ?: 0})") },
                    selected = selectedAbiIndex == index,
                    onClick = { selectedAbiIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val selectedAbi = abis.getOrNull(selectedAbiIndex)
        if (selectedAbi != null) {
            val libs = grouped[selectedAbi] ?: emptyList()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                libs.forEach { lib ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lib.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
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

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SecurityFlagRow(label: String, value: Boolean, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (value) "Yes" else "No", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionsSection(permissions: List<PermissionInfo>) {
    var expanded by remember { mutableStateOf(false) }
    var grantedFilter by remember { mutableStateOf(GrantedFilter.ALL) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Permissions (${permissions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GrantedFilterRow(selectedFilter = grantedFilter, onFilterChange = { grantedFilter = it }, permissions = permissions)
                    Divider()
                    val filtered = when (grantedFilter) {
                        GrantedFilter.ALL -> permissions
                        GrantedFilter.GRANTED -> permissions.filter { it.granted }
                        GrantedFilter.NOT_GRANTED -> permissions.filter { !it.granted }
                    }
                    if (filtered.isEmpty()) {
                        Text("No permissions match the filter", style = MaterialTheme.typography.bodySmall)
                    } else {
                        val grouped = filtered.groupBy { it.category }
                        grouped.keys.sortedBy { it.displayName }.forEach { category ->
                            CategoryHeader(category, grouped[category]?.size ?: 0)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                grouped[category]?.forEach { permission ->
                                    PermissionItem(permission)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutsideLibrariesSection(libraries: List<DetectedLibrary>) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Libraries (${libraries.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            if (libraries.isEmpty()) {
                Text("No outside libraries detected", style = MaterialTheme.typography.bodySmall)
            } else {
                val grouped = libraries.groupBy { it.category }
                grouped.keys.sortedBy { it.displayName }.forEach { category ->
                    Text(category.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        grouped[category]?.forEach { lib ->
                            Text("• ${lib.name}", style = MaterialTheme.typography.bodyMedium)
                            Text(lib.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
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
private fun InfoRowCompact(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
