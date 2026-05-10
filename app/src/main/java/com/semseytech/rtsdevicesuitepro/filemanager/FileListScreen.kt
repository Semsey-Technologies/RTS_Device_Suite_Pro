package com.semseytech.rtsdevicesuitepro.filemanager

import android.webkit.MimeTypeMap
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scale = ThemeManager.uiScale
    val scope = rememberCoroutineScope()
    var showSystemAccessWarning by remember { mutableStateOf(false) }
    var itemToFavorite by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreateDialog by remember { mutableStateOf<String?>(null) } // "folder", "text", "script"
    var targetPathForCreate by remember { mutableStateOf(path) }
    var isLeftPaneForCreate by remember { mutableStateOf(true) }

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
                            
                            var showGlobalMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showGlobalMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "Global Menu", tint = theme.accentColor)
                                }
                                DropdownMenu(
                                    expanded = showGlobalMenu,
                                    onDismissRequest = { showGlobalMenu = false },
                                    containerColor = Color(0xFF1A1A1A)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New Folder", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, tint = theme.accentColor) },
                                        onClick = {
                                            showCreateDialog = "folder"
                                            targetPathForCreate = uiState.leftPane.currentPath
                                            isLeftPaneForCreate = true
                                            showGlobalMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("New Text Document", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.NoteAdd, null, tint = theme.accentColor) },
                                        onClick = {
                                            scope.launch {
                                                val name = viewModel.generateUniqueName(uiState.leftPane.currentPath, "New Document", "txt")
                                                viewModel.createFile(uiState.leftPane.currentPath, name, true) { filePath ->
                                                    onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                                }
                                            }
                                            showGlobalMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("New Script (.sh)", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Code, null, tint = theme.accentColor) },
                                        onClick = {
                                            scope.launch {
                                                val name = viewModel.generateUniqueName(uiState.leftPane.currentPath, "New Script", "sh")
                                                viewModel.createFile(uiState.leftPane.currentPath, name, true) { filePath ->
                                                    onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                                }
                                            }
                                            showGlobalMenu = false
                                        }
                                    )
                                }
                            }

                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, "Exit", tint = Color.Gray)
                            }
                        }
                    }

                    // Address Bar for Single Pane
                    if (!uiState.isSplitScreen) {
                        SinglePaneNavigation(viewModel, uiState.leftPane, theme, true) { path: String, name: String ->
                            itemToFavorite = path to name
                        }
                    }
                }
            }
        },
        containerColor = theme.startColor,
        floatingActionButton = {
            var showFabMenu by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showCreateDialog = "folder"
                            targetPathForCreate = uiState.leftPane.currentPath
                            isLeftPaneForCreate = true
                            showFabMenu = false
                        },
                        containerColor = theme.accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.CreateNewFolder, "New Folder") }
                    
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val name = viewModel.generateUniqueName(uiState.leftPane.currentPath, "New Document", "txt")
                                viewModel.createFile(uiState.leftPane.currentPath, name, true) { filePath ->
                                    onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                }
                            }
                            showFabMenu = false
                        },
                        containerColor = theme.accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.NoteAdd, "New Text") }

                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                val name = viewModel.generateUniqueName(uiState.leftPane.currentPath, "New Script", "sh")
                                viewModel.createFile(uiState.leftPane.currentPath, name, true) { filePath ->
                                    onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                                }
                            }
                            showFabMenu = false
                        },
                        containerColor = theme.accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) { Icon(Icons.Default.Code, "New Script") }
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = theme.accentColor,
                    contentColor = Color.Black
                ) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, "Create New")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isSplitScreen) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) { 
                        Column {
                            SinglePaneNavigation(viewModel, uiState.leftPane, theme, true) { path: String, name: String ->
                                itemToFavorite = path to name
                            }
                            ExplorerPane(true, viewModel, theme, scale, onFavorite = { path: String, name: String ->
                                itemToFavorite = path to name
                            }, onNavigate = onNavigate)
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Box(modifier = Modifier.weight(1f)) { 
                        Column {
                            SinglePaneNavigation(viewModel, uiState.rightPane, theme, false) { path: String, name: String ->
                                itemToFavorite = path to name
                            }
                            ExplorerPane(false, viewModel, theme, scale, onFavorite = { path: String, name: String ->
                                itemToFavorite = path to name
                            }, onNavigate = onNavigate)
                        }
                    }
                }
            } else {
                ExplorerPane(true, viewModel, theme, scale, onFavorite = { path: String, name: String ->
                    itemToFavorite = path to name
                }, onNavigate = onNavigate)
            }
        }
    }

    if (showCreateDialog == "folder") {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = null },
            title = { Text("New Folder", color = theme.accentColor) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val finalName = if (folderName.isBlank()) "New Folder" else folderName
                        val uniqueName = viewModel.generateUniqueName(targetPathForCreate, finalName)
                        viewModel.createFolder(targetPathForCreate, uniqueName, isLeftPaneForCreate)
                        showCreateDialog = null
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = null }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (itemToFavorite != null) {
        FavoriteNamingDialog(
            defaultName = itemToFavorite!!.second,
            onDismiss = { itemToFavorite = null },
            onConfirm = { customName ->
                viewModel.addFavorite(itemToFavorite!!.first, customName)
                itemToFavorite = null
            }
        )
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
    isLeft: Boolean,
    onFavorite: (String, String) -> Unit
) {
    val clipboard by viewModel.clipboard.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isFavorited = uiState.favorites.any { it.path == pane.currentPath }
    
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

        IconButton(onClick = { 
            if (isFavorited) {
                viewModel.removeFavoriteByPath(pane.currentPath)
            } else {
                val name = pane.currentPath.trimEnd('/').split("/").lastOrNull()?.takeIf { it.isNotEmpty() } ?: "Folder"
                onFavorite(pane.currentPath, name) 
            }
        }) {
            Icon(
                if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorite",
                tint = if (isFavorited) Color.Red else theme.accentColor
            )
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
    scale: Float,
    onFavorite: (String, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pane = if (isLeftPane) uiState.leftPane else uiState.rightPane
    val scope = rememberCoroutineScope()

    var selectedItemForMenu by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var itemToRename by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<ExplorerFileItem?>(null) }
    var showPropertiesFor by remember { mutableStateOf<ExplorerFileItem?>(null) }
    
    var showCreateDialogInPane by remember { mutableStateOf<String?>(null) }
    var targetPathForCreateInPane by remember { mutableStateOf(pane.currentPath) }

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
                        } else {
                            val ext = item.name.substringAfterLast('.', "").lowercase()
                            if (listOf("txt", "sh", "py", "js").contains(ext)) {
                                onNavigate("text_editor?path=${java.net.URLEncoder.encode(item.path, "UTF-8")}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedItemForMenu != null) {
        val item = selectedItemForMenu!!
        val isFavorited = uiState.favorites.any { it.path == item.path }
        
        FileActionBottomSheet(
            item = item, 
            isFavorited = isFavorited,
            onAction = { action ->
                when (action) {
                    "copy" -> viewModel.copyToClipboard(item, false)
                    "move" -> viewModel.copyToClipboard(item, true)
                    "rename" -> itemToRename = item
                    "delete" -> itemToDelete = item
                    "properties" -> showPropertiesFor = item
                    "favorite" -> {
                        if (isFavorited) {
                            viewModel.removeFavoriteByPath(item.path)
                        } else {
                            onFavorite(item.path, item.name)
                        }
                    }
                    "new_folder" -> {
                        targetPathForCreateInPane = item.path
                        showCreateDialogInPane = "folder"
                    }
                    "new_text" -> {
                        scope.launch {
                            val name = viewModel.generateUniqueName(item.path, "New Document", "txt")
                            viewModel.createFile(item.path, name, isLeftPane) { filePath ->
                                onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                            }
                        }
                    }
                    "new_script" -> {
                        scope.launch {
                            val name = viewModel.generateUniqueName(item.path, "New Script", "sh")
                            viewModel.createFile(item.path, name, isLeftPane) { filePath ->
                                onNavigate("text_editor?path=${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                            }
                        }
                    }
                    "open_with" -> {
                        viewModel.openWith(context, item)
                    }
                }
                selectedItemForMenu = null
            }
        )
    }

    if (showCreateDialogInPane == "folder") {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialogInPane = null },
            title = { Text("New Folder", color = theme.accentColor) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val finalName = if (folderName.isBlank()) "New Folder" else folderName
                        val uniqueName = viewModel.generateUniqueName(targetPathForCreateInPane, finalName)
                        viewModel.createFolder(targetPathForCreateInPane, uniqueName, isLeftPane)
                        showCreateDialogInPane = null
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialogInPane = null }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1A1A1A)
        )
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
fun FileActionBottomSheet(
    item: ExplorerFileItem, 
    isFavorited: Boolean = false,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = { onAction("none") }) {
        val theme = LocalTheme.current
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            
            if (item.isDirectory) {
                FileActionItem(
                    if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    if (isFavorited) "Remove from Favorites" else "Add to Favorites"
                ) { onAction("favorite") }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
                
                FileActionItem(Icons.Default.CreateNewFolder, "New Folder", theme.accentColor) { onAction("new_folder") }
                FileActionItem(Icons.Default.NoteAdd, "New Text Document", theme.accentColor) { onAction("new_text") }
                FileActionItem(Icons.Default.Code, "New Script (.sh)", theme.accentColor) { onAction("new_script") }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.1f))
            }

            FileActionItem(Icons.Default.ContentCopy, "Copy") { onAction("copy") }
            FileActionItem(Icons.Default.ContentCut, "Move") { onAction("move") }
            FileActionItem(Icons.Default.OpenInNew, "Open with...") { onAction("open_with") }
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
fun FavoriteNamingDialog(defaultName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Favorite") },
        text = { 
            Column {
                Text("Enter a name for this favorite location:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Favorite Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                ) 
            }
        },
        confirmButton = { 
            Button(onClick = { onConfirm(name) }) { Text("Save Favorite") } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Cancel") } 
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

@Composable
fun DeleteDialog(item: ExplorerFileItem, onDismiss: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete?") },
        text = { Text("Are you sure you want to delete \u0027${item.name}\u0027?") },
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
