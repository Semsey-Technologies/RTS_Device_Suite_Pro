package com.semseytech.rtsdevicesuitepro.filemanager

import android.os.Environment
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FileExplorerViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedConnections by viewModel.savedConnections.collectAsState(emptyList())
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    var showConnectDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FILE EXPLORER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = currentTheme.textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = currentTheme.startColor, titleContentColor = currentTheme.textColor)
            )
        },
        containerColor = currentTheme.startColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp * scale),
            verticalArrangement = Arrangement.spacedBy(24.dp * scale)
        ) {
            item {
                ExplorerSectionLabel("STORAGE", currentTheme.accentColor)
                StorageGrid(scale, currentTheme.accentColor, onNavigate)
            }

            if (savedConnections.isNotEmpty()) {
                item {
                    ExplorerSectionLabel("SAVED CONNECTIONS", currentTheme.accentColor)
                    SavedConnectionsGrid(savedConnections, viewModel, scale, currentTheme.accentColor, onNavigate)
                }
            }

            item {
                ExplorerSectionLabel("NETWORK PROTOCOLS", currentTheme.accentColor)
                NetworkGrid(scale, currentTheme.accentColor) { showConnectDialog = it }
            }

            item {
                ExplorerSectionLabel("CLOUD SERVICES", currentTheme.accentColor)
                CloudGrid(scale, currentTheme.accentColor) { showConnectDialog = it }
            }

            item {
                ExplorerSectionLabel("TOOLS", currentTheme.accentColor)
                ToolsGrid(onNavigate, scale, currentTheme.accentColor)
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showConnectDialog != null) {
        ConnectDialog(
            type = showConnectDialog!!,
            viewModel = viewModel,
            onDismiss = { showConnectDialog = null },
            onConnected = { route -> onNavigate(Screen.FileList.createRoute(route)) }
        )
    }
}

@Composable
fun SavedConnectionsGrid(
    connections: List<com.semseytech.rtsdevicesuitepro.filemanager.data.SmbConnection>,
    viewModel: FileExplorerViewModel,
    scale: Float,
    accentColor: Color,
    onNavigate: (String) -> Unit
) {
    var connectionToEdit by remember { mutableStateOf<com.semseytech.rtsdevicesuitepro.filemanager.data.SmbConnection?>(null) }
    var showOptionsFor by remember { mutableStateOf<com.semseytech.rtsdevicesuitepro.filemanager.data.SmbConnection?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp * scale)) {
        connections.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
                row.forEach { conn ->
                    Box(modifier = Modifier.weight(1f)) {
                        ExplorerTile(
                            item = ExplorerItem(conn.name, Icons.Default.Dns, "SMB Server"),
                            accentColor = accentColor,
                            scale = scale,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            onNavigate(Screen.FileList.createRoute("smb://${conn.host}/"))
                        }
                        IconButton(
                            onClick = { showOptionsFor = conn },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp * scale)
                        ) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.Gray, modifier = Modifier.size(16.dp * scale))
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (showOptionsFor != null) {
        val conn = showOptionsFor!!
        AlertDialog(
            onDismissRequest = { showOptionsFor = null },
            title = { Text(conn.name, color = accentColor) },
            text = { Text("What would you like to do with this connection?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    connectionToEdit = conn
                    showOptionsFor = null
                }) { Text("Rename", color = accentColor) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.removeConnection(conn)
                    showOptionsFor = null
                }) { Text("Remove", color = Color.Red) }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (connectionToEdit != null) {
        var newName by remember { mutableStateOf(connectionToEdit!!.name) }
        AlertDialog(
            onDismissRequest = { connectionToEdit = null },
            title = { Text("Rename Connection", color = accentColor) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Connection Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameConnection(connectionToEdit!!, newName)
                    connectionToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { connectionToEdit = null }) { Text("Cancel") }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
fun ExplorerSectionLabel(title: String, accentColor: Color) {
    val scale = ThemeManager.uiScale
    Text(
        text = title,
        color = accentColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp * scale),
        letterSpacing = 1.sp
    )
}

@Composable
fun StorageGrid(scale: Float, accentColor: Color, onNavigate: (String) -> Unit) {
    val internalPath = Environment.getExternalStorageDirectory().absolutePath
    val items = listOf(
        ExplorerItem("Internal Storage", Icons.Default.Smartphone, "Local", internalPath),
        ExplorerItem("SD Card", Icons.Default.SdCard, "External", "/storage") 
    )
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
        items.forEach { item ->
            ExplorerTile(item, accentColor, scale, Modifier.weight(1f)) {
                item.route?.let { path ->
                    onNavigate(Screen.FileList.createRoute(path))
                }
            }
        }
    }
}

@Composable
fun NetworkGrid(scale: Float, accentColor: Color, onConnect: (String) -> Unit) {
    val networkItems = listOf(
        ExplorerItem("SMB", Icons.Default.Dns, "Network"),
        ExplorerItem("FTP", Icons.Default.CloudUpload, "Network"),
        ExplorerItem("SFTP", Icons.Default.Security, "Network"),
        ExplorerItem("WebDAV", Icons.Default.Public, "Network")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp * scale)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            networkItems.take(2).forEach { item ->
                ExplorerTile(item, accentColor, scale, Modifier.weight(1f)) { onConnect(item.name) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            networkItems.drop(2).forEach { item ->
                ExplorerTile(item, accentColor, scale, Modifier.weight(1f)) { onConnect(item.name) }
            }
        }
    }
}

@Composable
fun CloudGrid(scale: Float, accentColor: Color, onConnect: (String) -> Unit) {
    val cloudItems = listOf(
        ExplorerItem("Google Drive", Icons.Default.Cloud, "Cloud"),
        ExplorerItem("Dropbox", Icons.Default.CloudQueue, "Cloud"),
        ExplorerItem("OneDrive", Icons.Default.CloudCircle, "Cloud"),
        ExplorerItem("Box", Icons.Default.AllInbox, "Cloud")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp * scale)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            cloudItems.take(3).forEach { item ->
                ExplorerTile(item, accentColor, scale, Modifier.weight(1f)) { onConnect(item.name) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            cloudItems.drop(3).forEach { item ->
                ExplorerTile(item, accentColor, scale, Modifier.weight(1f)) { onConnect(item.name) }
            }
            repeat(3 - (cloudItems.size - 3)) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
fun ConnectDialog(type: String, viewModel: FileExplorerViewModel, onDismiss: () -> Unit, onConnected: (String) -> Unit) {
    val theme = LocalTheme.current
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    
    var host by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var saveCredentials by remember { mutableStateOf(true) }

    val isCloud = type in listOf("Google Drive", "Dropbox", "OneDrive", "Box")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to $type", color = theme.accentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isCloud) {
                    Text("Redirecting to $type for secure authentication...", color = Color.White)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.accentColor)
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        val url = when(type) {
                            "Google Drive" -> "https://drive.google.com"
                            "Dropbox" -> "https://www.dropbox.com/login"
                            "OneDrive" -> "https://onedrive.live.com/about/signin"
                            "Box" -> "https://app.box.com/login"
                            else -> "https://google.com"
                        }
                        uriHandler.openUri(url)
                        onDismiss()
                    }
                } else if (type == "SMB") {
                    if (uiState.isScanningNetwork) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(color = theme.accentColor)
                            Text("Scanning Local Network...", color = Color.Gray, fontSize = 10.sp)
                        }
                    } else if (uiState.networkNodes.isNotEmpty()) {
                        Text("Available Devices:", color = theme.accentColor, fontWeight = FontWeight.Bold)
                        uiState.networkNodes.forEach { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { host = node.ip }
                                    .padding(8.dp)
                                    .background(if (host == node.ip) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Computer, null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(node.name, color = Color.White, fontSize = 12.sp)
                                    Text(node.ip, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        Button(onClick = { viewModel.startSmbScan() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Scan Local Network")
                        }
                    }
                    
                    if (!uiState.isScanningNetwork) {
                        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host / IP") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = saveCredentials, onCheckedChange = { saveCredentials = it })
                            Text("Save credentials for auto-reconnect", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host / IP") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = saveCredentials, onCheckedChange = { saveCredentials = it })
                        Text("Remember me", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!isCloud && !uiState.isScanningNetwork) {
                Button(onClick = {
                    viewModel.connectToNetwork(type, host, user, pass, saveCredentials) { route ->
                        onConnected(route)
                    }
                    onDismiss()
                }) { Text("Connect") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = theme.endColor
    )
}

@Composable
fun ToolsGrid(onNavigate: (String) -> Unit, scale: Float, accentColor: Color) {
    val tools = listOf(
        ExplorerItem("Storage Analyzer", Icons.Outlined.PieChart, "Tool", Screen.StorageAnalyzer.route),
        ExplorerItem("Archive Manager", Icons.Outlined.FolderZip, "Tool", Screen.Archive.route),
        ExplorerItem("File Cleaner", Icons.Outlined.AutoFixHigh, "Tool", Screen.Cleaner.route),
        ExplorerItem("Smart Organizer", Icons.Outlined.Rule, "Tool", Screen.SmartOrganizer.route)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp * scale)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            ExplorerTile(tools[0], accentColor, scale, Modifier.weight(1f)) { onNavigate(tools[0].route!!) }
            ExplorerTile(tools[1], accentColor, scale, Modifier.weight(1f)) { onNavigate(tools[1].route!!) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp * scale)) {
            ExplorerTile(tools[2], accentColor, scale, Modifier.weight(1f)) { onNavigate(tools[2].route!!) }
            ExplorerTile(tools[3], accentColor, scale, Modifier.weight(1f)) { onNavigate(tools[3].route!!) }
        }
    }
}

@Composable
fun ExplorerTile(
    item: ExplorerItem,
    accentColor: Color,
    scale: Float,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp * scale)
            .clickable { onClick() }
            .border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp * scale)),
        shape = RoundedCornerShape(12.dp * scale),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp * scale),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(item.icon, null, tint = accentColor, modifier = Modifier.size(24.dp * scale))
            Column {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.category, color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontSize = (9 * scale).sp)
            }
        }
    }
}

data class ExplorerItem(val name: String, val icon: ImageVector, val category: String, val route: String? = null)
