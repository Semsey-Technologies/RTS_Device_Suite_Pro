package com.semseytech.rtsdevicesuitepro.cleaner

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.semseytech.rtsdevicesuitepro.automation.ui.AutoCleanDialog
import com.semseytech.rtsdevicesuitepro.ui.components.DashboardHeader
import com.semseytech.rtsdevicesuitepro.ui.components.FileDisplaySettings
import com.semseytech.rtsdevicesuitepro.ui.components.FileViewMode
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
    val displaySettings by viewModel.displaySettings.collectAsState()
    val showSortMenu by viewModel.showSortMenu.collectAsState()
    val showViewMenu by viewModel.showViewMenu.collectAsState()
    val showGroupMenu by viewModel.showGroupMenu.collectAsState()
    val scale = ThemeManager.uiScale
    var showAutoCleanDialog by remember { mutableStateOf(false) }

    // Start initial scan when screen is first opened
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    Column(modifier = Modifier.fillMaxSize().background(currentTheme.startColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
            }
            Text(
                "CLEANER & MAINTENANCE",
                style = MaterialTheme.typography.titleMedium,
                color = currentTheme.accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        if (state == CleanerState.IDLE || state == CleanerState.READY_TO_CLEAN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Automatic Button
                Button(
                    onClick = { showAutoCleanDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Schedule, null, tint = currentTheme.accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Automatic", color = currentTheme.accentColor, style = MaterialTheme.typography.labelMedium)
                }

                // Standard Menu Actions
                CleanerMenus(
                    showSortMenu = showSortMenu,
                    onSortMenuToggle = { viewModel.setShowSortMenu(it) },
                    showViewMenu = showViewMenu,
                    onViewMenuToggle = { viewModel.setShowViewMenu(it) },
                    showGroupMenu = showGroupMenu,
                    onGroupMenuToggle = { viewModel.setShowGroupMenu(it) },
                    displaySettings = displaySettings,
                    onSettingsChanged = { viewModel.updateDisplaySettings(it) }
                )

                TextButton(onClick = { viewModel.setAllSelection(true) }) {
                    Text("Select All", color = currentTheme.accentColor, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { viewModel.setAllSelection(false) }) {
                    Text("Clear", color = currentTheme.accentColor, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = { viewModel.refreshScan() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = currentTheme.accentColor, modifier = Modifier.size(20.dp))
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                CleanerState.IDLE, CleanerState.READY_TO_CLEAN -> {
                    CleanerSetupContent(
                        categories = categories,
                        showAppsToggle = showAppsToggle,
                        apps = apps,
                        displaySettings = displaySettings,
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

    if (showAutoCleanDialog) {
        val automationViewModel: com.semseytech.rtsdevicesuitepro.automation.ui.AutomationViewModel = viewModel()
        AutoCleanDialog(
            onDismiss = { showAutoCleanDialog = false },
            viewModel = automationViewModel
        )
    }
}

@Composable
fun CleanerSetupContent(
    categories: List<CleanupCategory>,
    showAppsToggle: Boolean,
    apps: List<AppCacheInfo>,
    displaySettings: FileDisplaySettings,
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
                CleanupCategoryCard(
                    category = category,
                    scale = scale,
                    accentColor = accentColor,
                    displaySettings = displaySettings,
                    onToggleCategory = onToggleCategory,
                    onToggleExpand = onToggleExpand,
                    onToggleItem = onToggleItem
                )
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
    displaySettings: FileDisplaySettings,
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
                if (category.items.isEmpty()) {
                    Text("No items found", color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp * scale))
                } else {
                    if (displaySettings.viewMode == FileViewMode.DETAILS || displaySettings.viewMode == FileViewMode.LIST) {
                        Column(modifier = Modifier.padding(start = 16.dp * scale, end = 16.dp * scale, bottom = 16.dp * scale)) {
                            category.items.forEach { item ->
                                CleanupItemRow(item, scale, accentColor) {
                                    onToggleItem(category.id, item.id)
                                }
                            }
                        }
                    } else {
                        val columns = when(displaySettings.viewMode) {
                            FileViewMode.GRID_SMALL -> 4
                            FileViewMode.GRID_MEDIUM -> 3
                            FileViewMode.GRID_LARGE -> 2
                            else -> 3
                        }
                        
                        // We use a Box with fixed height for grid to avoid nesting infinite height lists
                        // or better, just use a non-lazy grid for small categories
                        Column(modifier = Modifier.padding(horizontal = 16.dp * scale, vertical = 8.dp * scale)) {
                            category.items.chunked(columns).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp * scale)
                                ) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CleanupItemGrid(item, displaySettings.viewMode, scale, accentColor) {
                                                onToggleItem(category.id, item.id)
                                            }
                                        }
                                    }
                                    // Fill empty slots if last row isn't full
                                    repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp * scale))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanupItemRow(
    item: CleanupItem,
    scale: Float,
    accentColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = accentColor)
        )
        
        Box(modifier = Modifier.size(40.dp * scale).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f))) {
            AsyncImage(
                model = item.path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = null // Falls back to nothing or we could add icon
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp * scale))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.extraInfo != null) {
                Text(item.extraInfo, color = Color.Gray, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (item.sizeBytes > 0) {
                Text(formatSize(item.sizeBytes), color = accentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun CleanupItemGrid(
    item: CleanupItem,
    viewMode: FileViewMode,
    scale: Float,
    accentColor: Color,
    onToggle: () -> Unit
) {
    val size = when(viewMode) {
        FileViewMode.GRID_SMALL -> 60.dp
        FileViewMode.GRID_MEDIUM -> 90.dp
        FileViewMode.GRID_LARGE -> 130.dp
        else -> 90.dp
    } * scale

    Column(
        modifier = Modifier
            .width(size)
            .clickable { onToggle() }
            .padding(4.dp * scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f))) {
            AsyncImage(
                model = item.path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(modifier = Modifier.align(Alignment.TopStart).padding(2.dp)) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = accentColor),
                    modifier = Modifier.size(20.dp * scale)
                )
            }
            
            if (item.sizeBytes > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(formatSize(item.sizeBytes), color = Color.White, fontSize = 8.sp * scale)
                }
            }
        }
        
        if (viewMode != FileViewMode.GRID_SMALL) {
            Text(
                item.name,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp * scale)
            )
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
