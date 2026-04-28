package com.semseytech.rtsdevicesuitepro.cleaner

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    onBack: () -> Unit,
    viewModel: CleanerViewModel = viewModel()
) {
    val currentTheme = LocalTheme.current
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val showAppsToggle by viewModel.showAppsToggle.collectAsState()
    val cleanupResult by viewModel.cleanupResult.collectAsState()
    val currentApp by viewModel.currentAppProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val scale = ThemeManager.uiScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CLEANER & MAINTENANCE", style = MaterialTheme.typography.titleMedium, color = currentTheme.accentColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
                    }
                },
                actions = {
                    if (state == CleanerState.IDLE || state == CleanerState.READY_TO_CLEAN) {
                        TextButton(onClick = { viewModel.setAllSelection(true) }) {
                            Text("Select All", color = currentTheme.accentColor)
                        }
                        TextButton(onClick = { viewModel.setAllSelection(false) }) {
                            Text("Clear", color = currentTheme.accentColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        },
        containerColor = currentTheme.startColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                CleanerState.IDLE, CleanerState.READY_TO_CLEAN -> {
                    CleanerSetupContent(
                        categories = categories,
                        showAppsToggle = showAppsToggle,
                        apps = apps,
                        scale = scale,
                        accentColor = currentTheme.accentColor,
                        appTypes = viewModel.appTypeDefinitions,
                        onToggleCategory = { viewModel.toggleCategorySelection(it) },
                        onToggleExpand = { viewModel.toggleCategoryExpansion(it) },
                        onToggleItem = { cat, item -> viewModel.toggleItemSelection(cat, item) },
                        onToggleShowApps = { viewModel.toggleShowApps(it) },
                        onToggleApp = { viewModel.toggleAppSelection(it) },
                        onClean = { viewModel.startCleanup() }
                    )
                }
                CleanerState.CLEANING -> {
                    CleaningProgressContent(
                        progress = progress,
                        accentColor = currentTheme.accentColor,
                        scale = scale
                    )
                }
                CleanerState.GUIDED_CACHE -> {
                    GuidedCacheContent(
                        currentApp = currentApp,
                        accentColor = currentTheme.accentColor,
                        scale = scale,
                        onOpenSettings = { viewModel.openAppStorageSettings(it) },
                        onNext = { viewModel.processNextApp() }
                    )
                }
                CleanerState.COMPLETED -> {
                    CleanupSummaryContent(
                        result = cleanupResult,
                        accentColor = currentTheme.accentColor,
                        scale = scale,
                        onFinish = onBack
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun CleanerSetupContent(
    categories: List<CleanupCategory>,
    showAppsToggle: Boolean,
    apps: List<AppCacheInfo>,
    scale: Float,
    accentColor: Color,
    appTypes: List<AppTypeInfo>,
    onToggleCategory: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleItem: (String, String) -> Unit,
    onToggleShowApps: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onClean: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp * scale),
            verticalArrangement = Arrangement.spacedBy(8.dp * scale)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp * scale)) }
            
            items(categories) { category ->
                CleanupCategoryCard(category, scale, accentColor, onToggleCategory, onToggleExpand, onToggleItem)
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp * scale), color = Color.White.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp * scale),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Apps to Clear Cache", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = showAppsToggle,
                        onCheckedChange = onToggleShowApps,
                        colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                    )
                }
            }

            if (showAppsToggle) {
                item {
                    CacheSafetyGuidanceCard(accentColor, scale, appTypes)
                }
                items(apps) { app ->
                    AppCacheItem(app, scale, accentColor, onToggleApp)
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp * scale)) }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp * scale).background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onClean,
                modifier = Modifier.fillMaxWidth().height(56.dp * scale),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("START CLEANUP", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun CleanupCategoryCard(
    category: CleanupCategory,
    scale: Float,
    accentColor: Color,
    onToggleCategory: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleItem: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand(category.id) }
                    .padding(16.dp * scale),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = category.isSelected,
                    onCheckedChange = { onToggleCategory(category.id) },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                )
                Icon(category.icon, null, tint = accentColor, modifier = Modifier.size(24.dp * scale))
                Spacer(modifier = Modifier.width(12.dp * scale))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(category.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    if (category.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = category.isExpanded) {
                Column(modifier = Modifier.padding(start = 48.dp * scale, end = 16.dp * scale, bottom = 16.dp * scale)) {
                    if (category.items.isEmpty()) {
                        Text("No items found", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp * scale))
                    } else {
                        category.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onToggleItem(category.id, item.id) }.padding(vertical = 8.dp * scale),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { onToggleItem(category.id, item.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                                Column {
                                    Text(item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    if (item.extraInfo != null) {
                                        Text(item.extraInfo, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (item.sizeBytes > 0) {
                                        Text(formatSize(item.sizeBytes), color = accentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                    }
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
fun CacheSafetyGuidanceCard(accentColor: Color, scale: Float, appTypes: List<AppTypeInfo>) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<AppTypeInfo?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp * scale),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp * scale)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, tint = accentColor, modifier = Modifier.size(24.dp * scale))
                Spacer(modifier = Modifier.width(12.dp * scale))
                Text("Safety Guidance", color = accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (isExpanded) "LESS" else "MORE", color = accentColor, style = MaterialTheme.typography.labelLarge)
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp * scale)) {
                    Text(
                        "Click an app category below to understand the impact of clearing its cache:",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp * scale))
                    
                    appTypes.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                            row.forEach { type ->
                                OutlinedButton(
                                    onClick = { selectedType = type },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(type.typeName, color = Color.White, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedType != null) {
        Dialog(onDismissRequest = { selectedType = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, accentColor)
            ) {
                Column(modifier = Modifier.padding(24.dp * scale)) {
                    Text(selectedType!!.typeName, color = accentColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(16.dp * scale))
                    Text(selectedType!!.description, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp * scale))
                    Text("IMPACT:", color = Color.Yellow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(selectedType!!.consequences, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(24.dp * scale))
                    Button(
                        onClick = { selectedType = null },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("UNDERSTOOD", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun AppCacheItem(app: AppCacheInfo, scale: Float, accentColor: Color, onToggle: (String) -> Unit) {
    var showWarning by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = app.isSelected,
                onCheckedChange = { onToggle(app.packageName) },
                colors = CheckboxDefaults.colors(checkedColor = accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp * scale))
            Text(app.appName, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            
            if (app.warningReason != null) {
                IconButton(onClick = { showWarning = !showWarning }) {
                    Icon(Icons.Outlined.Info, null, tint = Color.Yellow.copy(alpha = 0.7f), modifier = Modifier.size(18.dp * scale))
                }
            }
            
            Text(formatSize(app.cacheSize), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        
        AnimatedVisibility(visible = showWarning && app.warningReason != null) {
            Text(
                text = "⚠️ ${app.warningReason}",
                color = Color.Yellow.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 48.dp * scale, bottom = 8.dp * scale)
            )
        }
    }
}

@Composable
fun CleaningProgressContent(progress: CleanupProgress, accentColor: Color, scale: Float) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp * scale),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.size(120.dp * scale),
                color = accentColor,
                strokeWidth = 8.dp * scale,
                trackColor = accentColor.copy(alpha = 0.1f)
            )
            Text(
                "${(progress.progress * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp * scale))
        
        Text(
            "CLEANING IN PROGRESS",
            color = accentColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp * scale))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp * scale)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Processing:", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${progress.itemsProcessed} / ${progress.totalItems}",
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp * scale))
                
                Text(
                    text = progress.currentItemName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp * scale))
                
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp * scale).clip(CircleShape),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp * scale))
        
        Text(
            "Please do not close the app",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun GuidedCacheContent(
    currentApp: AppCacheInfo?,
    accentColor: Color,
    scale: Float,
    onOpenSettings: (String) -> Unit,
    onNext: () -> Unit
) {
    if (currentApp == null) return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp * scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("GUIDED CACHE CLEAR", color = accentColor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp * scale))
        
        Box(
            modifier = Modifier.size(80.dp * scale).background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Apps, null, tint = accentColor, modifier = Modifier.size(40.dp * scale))
        }
        
        Spacer(modifier = Modifier.height(16.dp * scale))
        Text(currentApp.appName, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp * scale))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp * scale)) {
                StepRow(1, "Click 'STORAGE & CACHE'", scale, accentColor)
                Spacer(modifier = Modifier.height(12.dp * scale))
                StepRow(2, "Click 'CLEAR CACHE'", scale, accentColor)
                Spacer(modifier = Modifier.height(12.dp * scale))
                StepRow(3, "Return here for next app", scale, accentColor)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp * scale))
        
        Button(
            onClick = { onOpenSettings(currentApp.packageName) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("OPEN APP INFO", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp * scale))
        
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, accentColor)
        ) {
            Text("NEXT APP", color = accentColor)
        }
    }
}

@Composable
fun StepRow(number: Int, text: String, scale: Float, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(24.dp * scale).background(accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp * scale))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CleanupSummaryContent(result: CleanupResult, accentColor: Color, scale: Float, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp * scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(80.dp * scale))
        Spacer(modifier = Modifier.height(24.dp * scale))
        Text("CLEANUP COMPLETE", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp * scale))
        
        SummaryRow("Space Reclaimed", formatSize(result.cleanedSizeBytes), accentColor, scale)
        SummaryRow("Duplicates Removed", result.duplicatesRemoved.toString(), accentColor, scale)
        SummaryRow("Folders Cleared", result.foldersCleared.toString(), accentColor, scale)
        SummaryRow("Apps Processed", result.appsProcessed.toString(), accentColor, scale)
        
        Spacer(modifier = Modifier.height(48.dp * scale))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, accentColor: Color, scale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp * scale),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = accentColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        else -> String.format(Locale.getDefault(), "%.2f KB", kb)
    }
}
