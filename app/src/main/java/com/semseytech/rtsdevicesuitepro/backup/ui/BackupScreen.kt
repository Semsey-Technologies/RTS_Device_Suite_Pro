package com.semseytech.rtsdevicesuitepro.backup.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.backup.BackupViewModel
import com.semseytech.rtsdevicesuitepro.backup.BackupUiState
import com.semseytech.rtsdevicesuitepro.backup.model.*
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.backup.ui.components.SortingAndFilteringHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = viewModel(),
    onNavigateToRestore: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showArchiveOptions by remember { mutableStateOf(false) }
    var pendingBackupPath by remember { mutableStateOf<String?>(null) }

    val permissions = remember {
        val list = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.toTypedArray()
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.loadRealData()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.loadRealData()
        }
    }

    var pendingDestType by remember { mutableStateOf<BackupDestinationType?>(null) }
    var startBackupAfterPicker by remember { mutableStateOf(false) }

    val safLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            val type = pendingDestType ?: BackupDestinationType.SAF
            viewModel.setDestination(BackupDestination(
                type = type,
                displayName = "Selected ${type.name.replace("_", " ")}",
                uri = it.toString()
            ))
            
            if (startBackupAfterPicker) {
                viewModel.startBackup { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
                showArchiveOptions = false
            }
        }
        pendingDestType = null
        startBackupAfterPicker = false
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { destinationUri ->
            pendingBackupPath?.let { path ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val sourceFile = java.io.File(path)
                        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                            sourceFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        sourceFile.delete() 
                        snackbarHostState.showSnackbar("Backup saved successfully!")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save backup: ${e.message}")
                    } finally {
                        pendingBackupPath = null
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup Suite Pro") },
                actions = {
                    IconButton(onClick = onNavigateToRestore) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.categories.isNotEmpty()) {
                Button(
                    onClick = { showArchiveOptions = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = uiState.categories.any { cat -> cat.items.any { it.isSelected } },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Configure & Backup")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                LoadingOverlay(uiState.backupStatus, uiState.backupProgress)
            } else if (uiState.categories.isEmpty()) {
                EmptyState(onRetry = { permissionsLauncher.launch(permissions) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SortingAndFilteringHeader(
                            sortType = uiState.sortType,
                            onSortSelected = { viewModel.setSortType(it) },
                            viewMode = uiState.viewMode,
                            onViewModeSelected = { viewModel.setViewMode(it) },
                            groupType = uiState.groupType,
                            onGroupTypeSelected = { viewModel.setGroupType(it) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Master Selection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.toggleMasterSelection() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isMasterSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (uiState.isMasterSelected) "Deselect All" else "Select All")
                            }
                        }
                    }

                    val groupedCategories = uiState.categories.groupBy { it.parentCategory ?: "Other" }
                    groupedCategories.forEach { (parent, categories) ->
                        item {
                            Text(
                                text = parent.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        categories.forEach { category ->
                            item(key = category.id) {
                                CategoryCard(
                                    category = category,
                                    onExpandToggle = { viewModel.toggleCategoryExpansion(category.id) },
                                    onCategorySelect = { viewModel.toggleCategorySelection(category.id, it) }
                                )
                            }
                            if (category.isExpanded) {
                                items(category.items, key = { "${category.id}_${it.id}" }) { item ->
                                    BackupItemRow(
                                        item = item, 
                                        viewMode = uiState.viewMode,
                                        onSelect = { viewModel.toggleItemSelection(category.id, item.id) }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showArchiveOptions) {
            AdvancedArchiveDialog(
                uiState = uiState,
                onArchiveTypeSelected = { viewModel.setArchiveType(it) },
                onCompressionLevelChanged = { viewModel.setCompressionLevel(it) },
                onFileNameChanged = { viewModel.setOutputFileName(it) },
                onDestinationSelected = { viewModel.setDestination(it) },
                onLaunchSAF = { type ->
                    pendingDestType = type
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val name = uiState.outputFileName.ifEmpty { "RTS_Backup_$timestamp" }
                    val extension = uiState.selectedArchiveType.extension
                    safLauncher.launch("$name$extension")
                },
                onDismiss = { showArchiveOptions = false },
                onConfirm = {
                    val dest = uiState.selectedDestination
                    val requiresPicker = dest.type != BackupDestinationType.WEBDAV && 
                                         dest.type != BackupDestinationType.DROPBOX &&
                                         dest.type != BackupDestinationType.MEGA
                    
                    if (requiresPicker && dest.uri == null) {
                        startBackupAfterPicker = true
                        pendingDestType = dest.type
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val name = uiState.outputFileName.ifEmpty { "RTS_Backup_$timestamp" }
                        val extension = uiState.selectedArchiveType.extension
                        safLauncher.launch("$name$extension")
                    } else {
                        showArchiveOptions = false
                        viewModel.startBackup { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedArchiveDialog(
    uiState: BackupUiState,
    onArchiveTypeSelected: (ArchiveFormat) -> Unit,
    onCompressionLevelChanged: (Int) -> Unit,
    onFileNameChanged: (String) -> Unit,
    onDestinationSelected: (BackupDestination) -> Unit,
    onLaunchSAF: (BackupDestinationType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Backup Configuration") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = uiState.outputFileName,
                    onValueChange = onFileNameChanged,
                    label = { Text("Output Filename") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                Text("Archive Format:", style = MaterialTheme.typography.labelLarge)
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    ArchiveFormat.values().forEach { format ->
                        FilterChip(
                            selected = uiState.selectedArchiveType == format,
                            onClick = { onArchiveTypeSelected(format) },
                            label = { Text(format.displayName) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Compression Level: ${uiState.compressionLevel}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = uiState.compressionLevel.toFloat(),
                    onValueChange = { onCompressionLevelChanged(it.toInt()) },
                    valueRange = 0f..9f,
                    steps = 8
                )

                Spacer(Modifier.height(16.dp))
                Text("Destination:", style = MaterialTheme.typography.labelLarge)
                BackupDestinationType.values().forEach { destType ->
                    val requiresPicker = destType != BackupDestinationType.WEBDAV && 
                                         destType != BackupDestinationType.DROPBOX &&
                                         destType != BackupDestinationType.MEGA
                    
                    val isSelected = uiState.selectedDestination.type == destType
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onDestinationSelected(BackupDestination(destType, destType.name))
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected, 
                            onClick = { 
                                onDestinationSelected(BackupDestination(destType, destType.name))
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(destType.name.replace("_", " "))
                            if (isSelected && uiState.selectedDestination.uri != null) {
                                Text(
                                    "URI: ${uiState.selectedDestination.uri}", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Start Backup") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CategoryCard(category: BackupCategory, onExpandToggle: () -> Unit, onCategorySelect: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onExpandToggle() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = category.isAllSelected, onCheckedChange = { onCategorySelect(it) })
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${category.items.size} items", style = MaterialTheme.typography.bodySmall)
            }
            Icon(if (category.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
    }
}

@Composable
fun BackupItemRow(item: BackupItem, viewMode: ViewMode, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.isSelected, onCheckedChange = { onSelect() })
        Spacer(Modifier.width(8.dp))
        Column {
            Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
            ItemSubtitle(item)
        }
    }
}

@Composable
fun ItemSubtitle(item: BackupItem) {
    val text = when (item) {
        is BackupItem.SmsMessage -> "${item.messageCount} messages"
        is BackupItem.UserFile -> formatSize(item.size)
        is BackupItem.Apk -> item.packageName
        is BackupItem.SystemSetting -> item.category
        else -> formatDate(item.date)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Composable
fun LoadingOverlay(status: String, progress: Float) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(status, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.width(200.dp))
        }
    }
}

@Composable
fun EmptyState(onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text("No backup categories found", style = MaterialTheme.typography.titleLarge)
        Text("Please grant permissions to scan your device", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Grant Permissions") }
    }
}

fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
