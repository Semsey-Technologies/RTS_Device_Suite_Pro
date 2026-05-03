package com.semseytech.rtsdevicesuitepro.recovery

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.storage.analyzer.FileCategory
import java.util.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.draw.scale
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen(
    viewModel: RecoveryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToPreview by remember { mutableStateOf<RecoverableItem?>(null) }
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("RECOVERY MODULE PRO", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = currentTheme.textColor)
                    }
                },
                actions = {
                    val allSelected = uiState.categories.all { it.items.all { i -> i.isSelected } } && uiState.itemsFound > 0
                    IconButton(onClick = { viewModel.selectAll(!allSelected) }) {
                        Icon(
                            if (allSelected) Icons.Default.LibraryAddCheck else Icons.Default.LibraryAdd,
                            contentDescription = "Select All",
                            tint = if (allSelected) currentTheme.accentColor else currentTheme.textColor
                        )
                    }
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = currentTheme.textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = currentTheme.textColor,
                    navigationIconContentColor = currentTheme.textColor,
                    actionIconContentColor = currentTheme.textColor
                )
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            val context = LocalContext.current
            val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                uri?.let { viewModel.recoverSelectedItems(it) }
            }

            if (uiState.categories.any { it.items.any { item -> item.isSelected } }) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = { folderPicker.launch(null) },
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Download, null) },
                        text = { Text("RECOVER", style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(4.dp * scale),
                        modifier = Modifier.padding(bottom = 8.dp * scale)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showDeleteConfirm = true },
                        containerColor = Color(0xFFD50000),
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.DeleteForever, null) },
                        text = { Text("SECURE DELETE", style = MaterialTheme.typography.labelLarge) },
                        shape = RoundedCornerShape(4.dp * scale)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(currentTheme.startColor, currentTheme.endColor)
                    )
                )
        ) {
            Column(modifier = Modifier.padding(padding)) {
                RecoveryControlBar(uiState, viewModel, currentTheme, scale)
                
                if (uiState.isScanning) {
                    ScanningProgress(uiState.scanProgress, currentTheme, scale)
                } else if (uiState.categories.isEmpty() && !uiState.isScanning) {
                    EmptyState()
                } else {
                    RecoveryContent(uiState, viewModel, currentTheme, scale, onPreview = { itemToPreview = it })
                }
            }
        }

        if (showDeleteConfirm) {
            DeleteConfirmDialog(
                currentTheme = currentTheme,
                onConfirm = {
                    viewModel.permanentlyDeleteSelected()
                    showDeleteConfirm = false
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }

        itemToPreview?.let { item ->
            PreviewDialog(item = item, viewModel = viewModel, currentTheme = currentTheme, scale = scale, onDismiss = { itemToPreview = null })
        }
    }
}

@Composable
fun RecoveryControlBar(state: RecoveryState, viewModel: RecoveryViewModel, theme: ThemePreset, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.accentColor.copy(alpha = 0.1f))
            .padding(8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search recoverable...", color = theme.subtitleColor, fontSize = (12 * scale).sp) },
            modifier = Modifier.weight(1f).height(48.dp * scale),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.textColor),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.accentColor,
                unfocusedBorderColor = theme.subtitleColor,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp * scale))
        
        IconButton(onClick = { viewModel.toggleSortOrder() }) {
            Icon(
                if (state.isDescending) Icons.Default.SortByAlpha else Icons.Default.Sort,
                contentDescription = "Sort",
                tint = theme.textColor
            )
        }
        
        IconButton(onClick = { 
            val nextMode = when(state.viewMode) {
                RecoveryViewMode.LIST -> RecoveryViewMode.GRID
                else -> RecoveryViewMode.LIST
            }
            viewModel.updateViewMode(nextMode)
        }) {
            Icon(
                if (state.viewMode == RecoveryViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
                contentDescription = "View Mode",
                tint = theme.textColor
            )
        }
    }
}

@Composable
fun ScanningProgress(progress: Float, theme: ThemePreset, scale: Float) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = theme.accentColor, strokeWidth = 4.dp * scale)
        Spacer(modifier = Modifier.height(16.dp * scale))
        Text(
            "DEEP SCANNING STORAGE...",
            style = MaterialTheme.typography.bodyMedium,
            color = theme.textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${(progress * 100).toInt()}% COMPLETE",
            style = MaterialTheme.typography.bodySmall,
            color = theme.accentColor
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.padding(32.dp * scale).fillMaxWidth().height(4.dp * scale),
            color = theme.accentColor,
            trackColor = theme.textColor.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No recoverable items found", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RecoveryContent(state: RecoveryState, viewModel: RecoveryViewModel, theme: ThemePreset, scale: Float, onPreview: (RecoverableItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            SummaryHeader(state, theme, scale)
        }
        state.categories.forEach { category ->
            item {
                CategoryHeader(category, viewModel, theme, scale)
            }
            if (category.isExpanded) {
                if (state.viewMode == RecoveryViewMode.LIST) {
                    items(category.items) { item ->
                        RecoverableItemRow(item, theme, scale,
                            onClick = { viewModel.toggleItemSelection(item.path) },
                            onLongClick = { onPreview(item) }
                        )
                    }
                } else {
                    item {
                        val chunks = category.items.chunked(3)
                        chunks.forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp * scale)) {
                                rowItems.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        RecoverableGridItem(item, theme, scale,
                                            onClick = { viewModel.toggleItemSelection(item.path) },
                                            onLongClick = { onPreview(item) }
                                        )
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
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
fun SummaryHeader(state: RecoveryState, theme: ThemePreset, scale: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp * scale),
        colors = CardDefaults.cardColors(containerColor = theme.accentColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp * scale)
    ) {
        Row(modifier = Modifier.padding(16.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("TOTAL RECOVERABLE", color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
                Text("${state.itemsFound} Items", color = theme.textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TOTAL SIZE", color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
                val totalSize = state.categories.sumOf { it.totalSize }
                Text(formatSize(totalSize), color = theme.accentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CategoryHeader(category: RecoveryCategory, viewModel: RecoveryViewModel, theme: ThemePreset, scale: Float) {
    val allSelected = category.items.all { it.isSelected }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { viewModel.toggleCategoryExpansion(category.category) }
            .padding(horizontal = 16.dp * scale, vertical = 8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = allSelected,
            onCheckedChange = { viewModel.selectAllInCategory(category.category, it) },
            colors = CheckboxDefaults.colors(checkedColor = theme.accentColor)
        )
        Icon(
            getCategoryIcon(category.category),
            contentDescription = null,
            tint = theme.accentColor,
            modifier = Modifier.size(20.dp * scale)
        )
        Spacer(modifier = Modifier.width(12.dp * scale))
        Column(modifier = Modifier.weight(1f)) {
            Text(category.category.name, color = theme.textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text("${category.count} items | ${formatSize(category.totalSize)}", color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
        }
        Icon(
            if (category.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = theme.subtitleColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecoverableItemRow(item: RecoverableItem, theme: ThemePreset, scale: Float, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isSelected) theme.accentColor.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp * scale, vertical = 8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp * scale), contentAlignment = Alignment.Center) {
            ItemThumbnail(item)
            if (item.isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize().background(theme.accentColor.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp * scale))
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp * scale))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = theme.textColor, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatSize(item.size)} | ${item.sourceApp ?: "Unknown source"}", color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            "${(item.recoverabilityScore * 100).toInt()}%",
            color = if (item.recoverabilityScore > 0.8f) Color.Green else Color.Yellow,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecoverableGridItem(item: RecoverableItem, theme: ThemePreset, scale: Float, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(4.dp * scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp * scale))
            .background(Color.DarkGray.copy(alpha = 0.5f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ItemThumbnail(item, modifier = Modifier.fillMaxSize())
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 100f
                    )
                )
        )
        
        if (item.isSelected) {
            Box(
                modifier = Modifier.fillMaxSize().background(theme.accentColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp * scale))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp * scale)
        ) {
            Text(
                item.name,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontSize = (10 * scale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatSize(item.size),
                color = theme.accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = (8 * scale).sp
            )
        }
    }
}

@Composable
fun ItemThumbnail(item: RecoverableItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconPainter = rememberVectorPainter(getCategoryIcon(item.category))
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri ?: item.path)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = iconPainter,
            placeholder = iconPainter
        )
    }
}

@Composable
fun DeleteConfirmDialog(currentTheme: ThemePreset, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PERMANENT DELETION", style = MaterialTheme.typography.titleSmall, color = Color.Red) },
        text = { Text("WARNING: Selected items will be securely wiped and permanently deleted. This action is irreversible.", color = currentTheme.textColor) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("SECURE DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = currentTheme.subtitleColor)
            }
        },
        containerColor = currentTheme.endColor,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun PreviewDialog(item: RecoverableItem, viewModel: RecoveryViewModel, currentTheme: ThemePreset, scale: Float, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, style = MaterialTheme.typography.titleSmall, overflow = TextOverflow.Ellipsis, color = currentTheme.textColor) },
        text = {
            Column {
                Text("Path: ${item.path}", style = MaterialTheme.typography.labelSmall, color = currentTheme.subtitleColor)
                Text("Size: ${formatSize(item.size)}", style = MaterialTheme.typography.bodySmall, color = currentTheme.textColor)
                Text("Source: ${item.sourceApp}", style = MaterialTheme.typography.bodySmall, color = currentTheme.textColor)
                Text("Type: ${item.mimeType}", style = MaterialTheme.typography.bodySmall, color = currentTheme.textColor)
                Text("Recoverability: ${(item.recoverabilityScore * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = currentTheme.accentColor)
                
                Spacer(modifier = Modifier.height(16.dp * scale))
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp * scale).background(Color.Black, RoundedCornerShape(4.dp * scale)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconPainter = rememberVectorPainter(getCategoryIcon(item.category))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri ?: item.path)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        error = iconPainter
                    )
                    
                    if (item.category == FileCategory.VIDEOS) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(64.dp * scale))
                    } else if (item.category == FileCategory.AUDIO) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = currentTheme.accentColor, modifier = Modifier.size(64.dp * scale))
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { 
                    viewModel.openItem(item)
                    onDismiss()
                }) { 
                    Text("OPEN / VIEW", color = currentTheme.accentColor, fontWeight = FontWeight.Bold) 
                }
                TextButton(onClick = onDismiss) { Text("CLOSE", color = currentTheme.subtitleColor) }
            }
        },
        containerColor = currentTheme.endColor
    )
}

fun getCategoryIcon(category: FileCategory): ImageVector {
    return when(category) {
        FileCategory.IMAGES -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.Videocam
        FileCategory.AUDIO -> Icons.Default.MusicNote
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVES -> Icons.Default.Inventory2
        FileCategory.APKS -> Icons.Default.Android
        FileCategory.OTHERS -> Icons.Default.QuestionMark
    }
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1] + ""
    return String.format(Locale.US, "%.2f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
