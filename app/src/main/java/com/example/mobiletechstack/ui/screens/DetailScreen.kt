package com.example.mobiletechstack.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.Locale
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
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    packageName: String,
    onBackClick: () -> Unit,
    cacheOnly: Boolean = false,
    viewModel: DetailViewModel = viewModel()
) {
    val analysisState by viewModel.analysisState.collectAsState()
    val lastAnalyzedAt by viewModel.lastAnalyzedAt.collectAsState()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        if (cacheOnly) viewModel.showCached(packageName)
        else viewModel.analyzeApp(packageName)
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
                actions = {
                    val successState = analysisState as? AnalysisState.Success
                    IconButton(
                        onClick = { showShareMenu = true },
                        enabled = successState != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        if (successState != null) {
                            DropdownMenuItem(
                                text = { Text("Share as Text") },
                                onClick = {
                                    showShareMenu = false
                                    shareAsText(context, buildTextReport(successState.result))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Save as HTML") },
                                onClick = {
                                    showShareMenu = false
                                    shareAsHtml(context, buildHtmlReport(successState.result), successState.result.appName)
                                }
                            )
                        }
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.fromCache && lastAnalyzedAt != null) {
                            val dateStr = remember(lastAnalyzedAt) {
                                SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                    .format(lastAnalyzedAt!!)
                            }
                            Text(
                                text = "Из кэша: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .align(Alignment.End)
                            )
                        }
                        DetailTabs(result = state.result, packageName = packageName)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTabs(result: AnalysisResult, packageName: String) {
    val tabs = listOf("Overview", "Security", "Permissions", "Libraries", "Native", "Unknown")
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
            5 -> UnknownTab(result.unknownPackages)
        }
    }
}

@Composable
private fun OverviewTab(result: AnalysisResult, packageName: String) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var showObfuscationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                appIcon = appInfo.loadIcon(pm)
            } catch (_: Exception) {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = result.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (result.hasObfuscation) {
                            IconButton(onClick = { showObfuscationDialog = true }) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Obfuscation detected",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
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
    if (showObfuscationDialog) {
        AlertDialog(
            onDismissRequest = { showObfuscationDialog = false },
            title = { Text("Obfuscation Detected") },
            text = {
                Text(
                    "This app appears to be obfuscated (ProGuard/R8). " +
                            "The analysis may contain mistakes"
                )
            },
            confirmButton = {
                Button(onClick = { showObfuscationDialog = false }) {
                    Text("OK")
                }
            }
        )
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
private fun UnknownTab(unknownPackages: List<String>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Unknown Packages (${unknownPackages.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (unknownPackages.isEmpty()) {
                        Text("No unknown packages found. All libraries detected.")
                    } else {
                        Text("The following package prefixes were not recognized by any pattern.")
                    }
                }
            }
        }
        items(unknownPackages) { pkg ->
            Text(
                text = pkg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
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
    var grantedFilter by remember { mutableStateOf(GrantedFilter.ALL) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Permissions (${permissions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GrantedFilterRow(
                    selectedFilter = grantedFilter,
                    onFilterChange = { grantedFilter = it },
                    permissions = permissions
                )
                HorizontalDivider()
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

fun buildTextReport(result: AnalysisResult): String {
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
    val versionInfo = result.versionInfo
    val security = result.securityFlags

    val librariesSection = if (result.detectedLibraries.isEmpty()) {
        "No libraries detected"
    } else {
        result.detectedLibraries
            .groupBy { it.category }
            .entries
            .sortedBy { it.key.displayName }
            .joinToString("\n") { (category, libs) ->
                "${category.displayName}: ${libs.joinToString(", ") { it.name }}"
            }
    }

    val dangerousCategories = setOf(
        PermissionCategory.CAMERA,
        PermissionCategory.LOCATION,
        PermissionCategory.CONTACTS,
        PermissionCategory.PHONE,
        PermissionCategory.STORAGE,
        PermissionCategory.MICROPHONE
    )
    val grantedCount = result.permissions.count { it.granted }
    val notGrantedCount = result.permissions.size - grantedCount
    val dangerousPerms = result.permissions.filter { it.category in dangerousCategories }
    val dangerousSection = if (dangerousPerms.isEmpty()) {
        "None detected"
    } else {
        dangerousPerms.joinToString("\n") { it.name.substringAfterLast('.') }
    }

    return buildString {
        appendLine("MobileTechStack Analysis")
        appendLine("========================")
        appendLine()
        appendLine("App: ${result.appName}")
        appendLine("Package: ${result.packageName}")
        appendLine("Size: ${result.apkSize.formatSize()}")
        appendLine("Date: $date")
        appendLine()
        appendLine("Tech Stack")
        appendLine("----------")
        appendLine("Framework: ${result.frameworkInfo?.type?.displayName ?: result.framework}")
        appendLine("Language: ${result.languageInfo?.primary?.displayName ?: result.language}")
        appendLine("Primary ABI: ${result.primaryAbi}")
        appendLine("64-bit: ${if (result.is64Bit) "Yes" else "No"}")
        appendLine("Obfuscation: ${if (result.hasObfuscation) "Detected" else "Not detected"}")
        appendLine()
        appendLine("Version")
        appendLine("-------")
        appendLine("Version: ${versionInfo?.versionName ?: "Unknown"} (${versionInfo?.versionCode ?: "Unknown"})")
        appendLine("Min SDK: ${versionInfo?.minSdkVersion ?: "Unknown"}")
        appendLine("Target SDK: ${versionInfo?.targetSdkVersion ?: "Unknown"}")
        appendLine()
        appendLine("Security")
        appendLine("--------")
        appendLine("Debuggable: ${if (security?.isDebuggable == true) "Yes" else "No"}")
        appendLine("Allow Backup: ${if (security?.allowBackup == true) "Yes" else "No"}")
        appendLine("Cleartext Traffic: ${if (security?.usesCleartextTraffic == true) "Yes" else "No"}")
        appendLine()
        appendLine("Libraries (${result.detectedLibraries.size})")
        appendLine("-".repeat(12 + result.detectedLibraries.size.toString().length))
        appendLine(librariesSection)
        appendLine()
        appendLine("Permissions (${result.permissions.size})")
        appendLine("-".repeat(14 + result.permissions.size.toString().length))
        appendLine("Granted: $grantedCount")
        appendLine("Not granted: $notGrantedCount")
        appendLine()
        appendLine("Dangerous permissions:")
        appendLine(dangerousSection)
        appendLine()
        appendLine("---")
        append("Analyzed by MobileTechStack")
    }
}

fun buildHtmlReport(result: AnalysisResult): String {
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
    val versionInfo = result.versionInfo
    val security = result.securityFlags

    val dangerousCategories = setOf(
        PermissionCategory.CAMERA,
        PermissionCategory.LOCATION,
        PermissionCategory.CONTACTS,
        PermissionCategory.PHONE,
        PermissionCategory.STORAGE,
        PermissionCategory.MICROPHONE
    )
    val grantedCount = result.permissions.count { it.granted }
    val notGrantedCount = result.permissions.size - grantedCount
    val dangerousPerms = result.permissions.filter { it.category in dangerousCategories }

    val librariesHtml = if (result.detectedLibraries.isEmpty()) {
        "<p class=\"empty\">No libraries detected</p>"
    } else {
        result.detectedLibraries
            .groupBy { it.category }
            .entries
            .sortedBy { it.key.displayName }
            .joinToString("\n") { (category, libs) ->
                """<div class="lib-group">
                  <span class="lib-category">${category.displayName}:</span>
                  ${libs.joinToString(", ") { "<span class=\"lib-name\">${it.name}</span>" }}
                </div>"""
            }
    }

    val dangerousHtml = if (dangerousPerms.isEmpty()) {
        "<p class=\"empty\">None detected</p>"
    } else {
        dangerousPerms.joinToString("\n") { perm ->
            "<div class=\"perm-dangerous\">${perm.name.substringAfterLast('.')}</div>"
        }
    }

    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>MobileTechStack — ${result.appName}</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: #fff; font-family: sans-serif; color: #212121; padding: 16px; }
  h1 { font-size: 1.4rem; color: #1976D2; margin-bottom: 4px; }
  .subtitle { font-size: 0.85rem; color: #757575; margin-bottom: 16px; }
  .card { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.15); padding: 16px; margin-bottom: 12px; }
  .card h2 { font-size: 1rem; color: #1976D2; margin-bottom: 12px; border-bottom: 1px solid #E3F2FD; padding-bottom: 6px; }
  .row { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #F5F5F5; }
  .row:last-child { border-bottom: none; }
  .label { font-size: 0.8rem; color: #757575; }
  .value { font-size: 0.85rem; color: #212121; font-weight: 500; text-align: right; max-width: 60%; word-break: break-all; }
  .lib-group { margin-bottom: 8px; font-size: 0.85rem; }
  .lib-category { font-weight: 600; color: #1976D2; }
  .lib-name { color: #424242; }
  .perm-stats { display: flex; gap: 16px; margin-bottom: 12px; }
  .stat-badge { background: #E3F2FD; border-radius: 4px; padding: 4px 10px; font-size: 0.8rem; color: #1976D2; font-weight: 600; }
  .perm-dangerous { color: #D32F2F; font-size: 0.85rem; padding: 3px 0; }
  .danger-label { font-size: 0.85rem; font-weight: 600; color: #D32F2F; margin-bottom: 6px; }
  .empty { font-size: 0.85rem; color: #9E9E9E; font-style: italic; }
  .footer { text-align: center; font-size: 0.75rem; color: #9E9E9E; margin-top: 8px; }
  @media (max-width: 480px) { .row { flex-direction: column; gap: 2px; } .value { text-align: left; max-width: 100%; } }
</style>
</head>
<body>

<h1>MobileTechStack Analysis</h1>
<p class="subtitle">Analyzed by MobileTechStack</p>

<div class="card">
  <h2>App Info</h2>
  <div class="row"><span class="label">App</span><span class="value">${result.appName}</span></div>
  <div class="row"><span class="label">Package</span><span class="value">${result.packageName}</span></div>
  <div class="row"><span class="label">APK Size</span><span class="value">${result.apkSize.formatSize()}</span></div>
  <div class="row"><span class="label">Analyzed</span><span class="value">$date</span></div>
</div>

<div class="card">
  <h2>Tech Stack</h2>
  <div class="row"><span class="label">Framework</span><span class="value">${result.frameworkInfo?.type?.displayName ?: result.framework}</span></div>
  <div class="row"><span class="label">Language</span><span class="value">${result.languageInfo?.primary?.displayName ?: result.language}</span></div>
  <div class="row"><span class="label">Primary ABI</span><span class="value">${result.primaryAbi}</span></div>
  <div class="row"><span class="label">64-bit</span><span class="value">${if (result.is64Bit) "Yes" else "No"}</span></div>
  <div class="row"><span class="label">Obfuscation</span><span class="value">${if (result.hasObfuscation) "Detected" else "Not detected"}</span></div>
</div>

<div class="card">
  <h2>Version</h2>
  <div class="row"><span class="label">Version</span><span class="value">${versionInfo?.versionName ?: "Unknown"} (${versionInfo?.versionCode ?: "Unknown"})</span></div>
  <div class="row"><span class="label">Min SDK</span><span class="value">${versionInfo?.minSdkVersion ?: "Unknown"}</span></div>
  <div class="row"><span class="label">Target SDK</span><span class="value">${versionInfo?.targetSdkVersion ?: "Unknown"}</span></div>
</div>

<div class="card">
  <h2>Security</h2>
  <div class="row"><span class="label">Debuggable</span><span class="value">${if (security?.isDebuggable == true) "Yes" else "No"}</span></div>
  <div class="row"><span class="label">Allow Backup</span><span class="value">${if (security?.allowBackup == true) "Yes" else "No"}</span></div>
  <div class="row"><span class="label">Cleartext Traffic</span><span class="value">${if (security?.usesCleartextTraffic == true) "Yes" else "No"}</span></div>
</div>

<div class="card">
  <h2>Libraries (${result.detectedLibraries.size})</h2>
  $librariesHtml
</div>

<div class="card">
  <h2>Permissions (${result.permissions.size})</h2>
  <div class="perm-stats">
    <span class="stat-badge">Granted: $grantedCount</span>
    <span class="stat-badge">Not granted: $notGrantedCount</span>
  </div>
  <div class="danger-label">Dangerous permissions:</div>
  $dangerousHtml
</div>

<p class="footer">Analyzed by MobileTechStack</p>

</body>
</html>"""
}

fun shareAsText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

fun shareAsHtml(context: Context, html: String, appName: String) {
    val safeName = appName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val file = File(context.cacheDir, "${safeName}_analysis.html")
    file.writeText(html)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
