package com.semseytech.rtsdevicesuitepro.filemanager

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    path: String,
    viewModel: FileExplorerViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalTheme.current
    val scale = ThemeManager.uiScale
    var showSystemAccessWarning by remember { mutableStateOf(false) }

    // On first load, check if the initial path is SMB and load it into the primary pane
    LaunchedEffect(path) {
        if (path.startsWith("smb://") || path.startsWith("/storage")) {
            viewModel.loadPath(path, isLeftPane = true)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = theme.startColor,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Row 1: Title and Global Actions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "FILE EXPLORER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = theme.accentColor,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                if (!uiState.isSystemAccessEnabled) {
                                    showSystemAccessWarning = true
                                } else {
                                    viewModel.setSystemAccess(false)
                                }
                            }) {
                                Icon(
                                    if (uiState.isSystemAccessEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                    "System Access",
                                    tint = if (uiState.isSystemAccessEnabled) Color.Yellow else theme.accentColor
                                )
                            }
                            IconButton(onClick = { viewModel.toggleSplitScreen() }) {
                                Icon(
                                    if (uiState.isSplitScreen) Icons.Default.Fullscreen else Icons.Default.VerticalSplit,
                                    "Toggle Split",
                                    tint = theme.accentColor
                                )
                            }
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, "Exit", tint = Color.Gray)
                            }
                        }
                    }

                    // Address Bar for Single Pane
                    if (!uiState.isSplitScreen) {
                        SinglePaneNavigation(viewModel, uiState.leftPane, theme, true)
                    }
                }
            }
        },
        containerColor = theme.startColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isSplitScreen) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) { 
                        Column {
                            SinglePaneNavigation(viewModel, uiState.leftPane, theme, true)
                            ExplorerPane(true, viewModel, theme, scale) 
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Box(modifier = Modifier.weight(1f)) { 
                        Column {
                            SinglePaneNavigation(viewModel, uiState.rightPane, theme, false)
                            ExplorerPane(false, viewModel, theme, scale) 
                        }
                    }
                }
            } else {
                ExplorerPane(true, viewModel, theme, scale)
            }
        }
    }

    if (showSystemAccessWarning) {
        AlertDialog(
            onDismissRequest = { showSystemAccessWarning = false },
            icon = { Icon(Icons.Default.Warning, null, tint = Color.Yellow) },
            title = { Text("System Access Warning", color = Color.Yellow) },
            text = {
                Text(
                    "You are about to enable System Access Mode. This allows you to view and alter files normally hidden or restricted by the system.\n\n" +
                    "⚠️ WARNING: Viewing files is safe, but modifying, moving, or deleting system files can be EXTREMELY DANGEROUS and may cause your device to malfunction if you do not know exactly what you are doing.\n\n" +
                    "Do you wish to proceed?",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSystemAccess(true)
                        showSystemAccessWarning = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                ) {
                    Text("ENABLE ACCESS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemAccessWarning = false }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
fun SinglePaneNavigation(
    viewModel: FileExplorerViewModel,
    pane: PaneState,
    theme: ThemePreset,
    isLeft: Boolean
) {
    val clipboard by viewModel.clipboard.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { viewModel.navigateBack(isLeft) },
            enabled = pane.historyIndex > 0,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = if (pane.historyIndex > 0) theme.accentColor else Color.Gray
            )
        }
        IconButton(
            onClick = { viewModel.navigateForward(isLeft) },
            enabled = pane.historyIndex < pane.history.size - 1,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = if (pane.historyIndex < pane.history.size - 1) theme.accentColor else Color.Gray
            )
        }
        
        Spacer(Modifier.width(4.dp))
        
        BreadcrumbBar(pane.currentPath, modifier = Modifier.weight(1f)) { path ->
            viewModel.loadPath(path, isLeft)
        }

        if (clipboard != null) {
            IconButton(onClick = { viewModel.paste(pane.currentPath, isLeft) }) {
                Icon(Icons.Default.ContentPaste, "Paste", tint = theme.accentColor)
            }
        }
    }
}

@Composable
fun ExplorerPane(
    isLeftPane: Boolean,
    viewModel: FileExplorerViewModel,
    theme: ThemePreset,
    scale: Float
) {
    val uiState by viewModel.uiState.collectAsState()
    val pane = if (isLeftPane) uiState.leftPane else uiState.rightPane

    var selectedItemForMenu by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var itemToRename by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var showPropertiesFor by remember { mutableStateOf<ExplorerFileItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (pane.isScanning) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = theme.accentColor)
        } else if (pane.errorMessage != null) {
            ErrorMessage(pane.errorMessage) { viewModel.loadPath(pane.currentPath, isLeftPane) }
        } else if (pane.items.isEmpty()) {
            Text(
                "Empty Directory",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pane.items) { item ->
                    FileItemRow(
                        item = item,
                        accentColor = theme.accentColor,
                        scale = scale,
                        onMoreClick = { selectedItemForMenu = item }
                    ) {
                        if (item.isDirectory) {
                            viewModel.loadPath(item.path, isLeftPane)
                        }
                    }
                }
            }
        }
    }

    if (selectedItemForMenu != null) {
        val item = selectedItemForMenu!!
        FileActionBottomSheet(item, onAction = { action ->
            when (action) {
                "copy" -> viewModel.copyToClipboard(item, false)
                "move" -> viewModel.copyToClipboard(item, true)
                "rename" -> itemToRename = item
                "delete" -> itemToDelete = item
                "properties" -> showPropertiesFor = item
            }
            selectedItemForMenu = null
        })
    }

    if (itemToRename != null) {
        RenameDialog(itemToRename!!, onDismiss = { itemToRename = null }) { newName ->
            viewModel.renameFile(itemToRename!!, newName, isLeftPane)
            itemToRename = null
        }
    }

    if (itemToDelete != null) {
        DeleteDialog(itemToDelete!!, onDismiss = { itemToDelete = null }) {
            viewModel.deleteFile(itemToDelete!!, isLeftPane)
            itemToDelete = null
        }
    }

    if (showPropertiesFor != null) {
        PropertiesDialog(showPropertiesFor!!, onDismiss = { showPropertiesFor = null })
    }
}

@Composable
fun BreadcrumbBar(path: String, modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val isSmb = path.startsWith("smb://")
    val displayPath = if (isSmb) path.removePrefix("smb://") else path
    val parts = displayPath.split("/").filter { it.isNotEmpty() }
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onNavigate(if (isSmb) "smb://${displayPath.split("/").first()}/" else "/storage/emulated/0") }, 
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(if (isSmb) "SMB" else "INTERNAL", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        
        var currentAcc = if (isSmb) "smb://" else ""
        parts.forEachIndexed { index, part ->
            if (isSmb && index == 0) {
                currentAcc += part
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                currentAcc += "/$part"
                val targetPath = currentAcc
                TextButton(
                    onClick = { onNavigate(targetPath) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        part.uppercase(),
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(msg: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(msg, color = Color.White, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionBottomSheet(item: ExplorerFileItem, onAction: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onAction("none") }) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            FileActionItem(Icons.Default.ContentCopy, "Copy") { onAction("copy") }
            FileActionItem(Icons.Default.ContentCut, "Move") { onAction("move") }
            FileActionItem(Icons.Default.Edit, "Rename") { onAction("rename") }
            FileActionItem(Icons.Default.Delete, "Delete", Color.Red) { onAction("delete") }
            FileActionItem(Icons.Default.Info, "Properties") { onAction("properties") }
        }
    }
}

@Composable
fun FileItemRow(
    item: ExplorerFileItem, 
    accentColor: Color, 
    scale: Float,
    onMoreClick: () -> Unit, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (item.isDirectory) accentColor else Color.Gray,
            modifier = Modifier.size(24.dp * scale)
        )
        Spacer(modifier = Modifier.width(12.dp * scale))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontSize = 15.sp)
            if (!item.isDirectory) {
                Text(formatSize(item.size), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RenameDialog(item: ExplorerFileItem, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var newName by remember { mutableStateOf(item.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("New Name") }) },
        confirmButton = { Button(onClick = { onRename(newName) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } }
    )
}

@Composable
fun DeleteDialog(item: ExplorerFileItem, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete?") },
        text = { Text("Are you sure you want to delete '${item.name}'?") },
        confirmButton = { Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel") } }
    )
}

@Composable
fun PropertiesDialog(item: ExplorerFileItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column {
                PropertyRow("Name", item.name)
                PropertyRow("Path", item.path)
                PropertyRow("Size", formatSize(item.size))
                PropertyRow("Type", if (item.isDirectory) "Folder" else "File")
            }
        },
        confirmButton = { Button(onClick = { onDismiss() }) { Text("Close") } }
    )
}

@Composable
fun FileActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (color == Color.Unspecified) LocalContentColor.current else color)
        Spacer(Modifier.width(16.dp))
        Text(label, color = if (color == Color.Unspecified) Color.Unspecified else color)
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
