package com.semseytech.rtsdevicesuitepro.backup.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.backup.BackupViewModel
import com.semseytech.rtsdevicesuitepro.backup.model.BackupCategory
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.provider.Telephony
import android.app.role.RoleManager
import androidx.compose.ui.window.Dialog
import com.semseytech.rtsdevicesuitepro.backup.ArchiveType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel = viewModel()) {
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
            Manifest.permission.READ_CONTACTS
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

    // Automatically load data if permissions are already granted
    LaunchedEffect(Unit) {
        viewModel.checkDefaultSmsStatus()
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

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Reload data after potentially becoming default app
        viewModel.checkDefaultSmsStatus()
        viewModel.loadRealData()
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
                        sourceFile.delete() // Clean up internal temp file
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
                title = { Text("Backup Module") },
                actions = {
                    if (!uiState.isDefaultSmsApp) {
                        TextButton(onClick = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                        defaultSmsLauncher.launch(intent)
                                    } else {
                                        // Fallback for Q if RoleManager is not available
                                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                        }
                                        defaultSmsLauncher.launch(intent)
                                    }
                                } else {
                                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                    }
                                    defaultSmsLauncher.launch(intent)
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not open SMS settings: ${e.message}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Set Default SMS")
                        }
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
                        .padding(16.dp),
                    enabled = uiState.categories.any { cat -> cat.items.any { it.isSelected } }
                ) {
                    Text("Backup Selected Items")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.backupStatus)
                    LinearProgressIndicator(
                        progress = uiState.backupProgress,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else if (uiState.categories.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No data found or permissions denied.")
                    Button(onClick = { permissionsLauncher.launch(permissions) }) {
                        Text("Retry Permissions")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MasterBackupButton(
                            isSelected = uiState.isMasterSelected,
                            onToggle = { viewModel.toggleMasterBackup() }
                        )
                    }

                    uiState.categories.forEach { category ->
                        item(key = "header_${category.id}") {
                            CategoryCard(
                                category = category,
                                onExpandToggle = { viewModel.toggleCategoryExpansion(category.id) },
                                onCategorySelect = { viewModel.toggleCategorySelection(category.id, it) }
                            )
                        }

                        if (category.isExpanded) {
                            if (category.items.isEmpty()) {
                                item(key = "empty_${category.id}") {
                                    Text(
                                        "No items found in this category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                }
                            } else {
                                item(key = "actions_${category.id}") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.toggleCategorySelection(category.id, true) }) {
                                            Text("Select All")
                                        }
                                        TextButton(onClick = { viewModel.toggleCategorySelection(category.id, false) }) {
                                            Text("Deselect All")
                                        }
                                    }
                                }
                                items(category.items, key = { "${category.id}_${it.id}" }) { item ->
                                    Box(modifier = Modifier.padding(start = 24.dp)) {
                                        BackupItemRow(item = item, onSelect = { viewModel.toggleItemSelection(category.id, item.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showArchiveOptions) {
            ArchiveOptionsDialog(
                selectedType = uiState.selectedArchiveType,
                onTypeSelected = { viewModel.setArchiveType(it) },
                onDismiss = { showArchiveOptions = false },
                onConfirm = {
                    showArchiveOptions = false
                    viewModel.startBackup { path ->
                        pendingBackupPath = path
                        val fileName = "RTS_Backup_${System.currentTimeMillis()}${uiState.selectedArchiveType.extension}"
                        saveFileLauncher.launch(fileName)
                    }
                }
            )
        }
    }
}

@Composable
fun ArchiveOptionsDialog(
    selectedType: ArchiveType,
    onTypeSelected: (ArchiveType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Options") },
        text = {
            Column {
                Text("Select Archive Type:")
                ArchiveType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeSelected(type) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == selectedType, onClick = { onTypeSelected(type) })
                        Text(type.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MasterBackupButton(isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Text(
                "Backup Everything",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: BackupCategory,
    onExpandToggle: () -> Unit,
    onCategorySelect: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = category.isAllSelected,
                onCheckedChange = { onCategorySelect(it) }
            )
            Text(
                text = category.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "(${category.items.size})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Icon(
                imageVector = if (category.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
}

@Composable
fun BackupItemRow(item: BackupItem, onSelect: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isSelected, onCheckedChange = { onSelect() })
            Column {
                Text(item.displayName, style = MaterialTheme.typography.bodyMedium)
                ItemSubtitle(item)
            }
        }
        
        AnimatedVisibility(visible = isExpanded) {
            ItemPreview(item)
        }
    }
}

@Composable
fun ItemSubtitle(item: BackupItem) {
    val text = when (item) {
        is BackupItem.SmsMessage -> "${item.messageCount} messages • ${formatDate(item.date)}"
        is BackupItem.CallLogEntry -> "${item.latestType} • ${item.callCount} calls • ${formatDate(item.latestDate)}"
        is BackupItem.Contact -> item.phoneNumbers.firstOrNull() ?: "No number"
        is BackupItem.Apk -> "${item.packageName} (v${item.version})"
        is BackupItem.UserFile -> formatSize(item.size)
        is BackupItem.LauncherConfig -> "System Layout"
        is BackupItem.UserSetting -> "Device Setting"
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Composable
fun ItemPreview(item: BackupItem) {
    Box(modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 8.dp)) {
        when (item) {
            is BackupItem.SmsMessage -> {
                Column {
                    Text("Latest: ${item.snippet}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    item.messages.take(3).forEach { msg ->
                        Text("- ${if (msg.type == 2) "Me" else item.sender}: ${msg.body}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.messages.size > 3) Text("... and ${item.messages.size - 3} more", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
            is BackupItem.CallLogEntry -> {
                Column {
                    Text("Total Duration: ${item.totalDuration}s", style = MaterialTheme.typography.bodySmall)
                    item.calls.take(3).forEach { call ->
                        val type = when(call.type) {
                            1 -> "Incoming"
                            2 -> "Outgoing"
                            3 -> "Missed"
                            else -> "Unknown"
                        }
                        Text("- $type: ${formatDate(call.date)} (${call.duration}s)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.calls.size > 3) Text("... and ${item.calls.size - 3} more", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
            is BackupItem.Contact -> Text("Emails: ${item.emails.joinToString()}\nPhones: ${item.phoneNumbers.joinToString()}", style = MaterialTheme.typography.bodySmall)
            is BackupItem.Apk -> Text("Package: ${item.packageName}\nVersion: ${item.version}", style = MaterialTheme.typography.bodySmall)
            is BackupItem.UserFile -> Text("Path: ${item.path}\nType: ${item.mimeType}", style = MaterialTheme.typography.bodySmall)
            else -> Text("No additional details available.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb > 1) "%.2f MB".format(mb) else "%.2f KB".format(kb)
}
