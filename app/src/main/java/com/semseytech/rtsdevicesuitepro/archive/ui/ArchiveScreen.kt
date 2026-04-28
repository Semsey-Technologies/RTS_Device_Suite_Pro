package com.semseytech.rtsdevicesuitepro.archive.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.archive.logic.ArchiveViewModel
import com.semseytech.rtsdevicesuitepro.archive.model.FileItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    viewModel: ArchiveViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Archive Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                    }
                    IconButton(onClick = { viewModel.refreshFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                ToolbarAction(Icons.Default.Add, "Add") { viewModel.isArchiveDialogOpen = true }
                ToolbarAction(Icons.Default.Unarchive, "Extract") { 
                    if (viewModel.selectedFiles.isNotEmpty()) {
                        val firstFile = viewModel.selectedFiles.first()
                        viewModel.extractArchive(firstFile, viewModel.currentDirectory)
                        viewModel.selectedFiles.clear()
                    }
                }
                ToolbarAction(Icons.Default.BugReport, "Test") {
                    if (viewModel.selectedFiles.isNotEmpty()) {
                        viewModel.testArchive(viewModel.selectedFiles.first())
                    }
                }
                ToolbarAction(Icons.Default.ContentCopy, "Copy") {
                    viewModel.isCopyOperation = true
                    viewModel.isCopyMoveDialogOpen = true
                }
                ToolbarAction(Icons.Default.ContentCut, "Move") {
                    viewModel.isCopyOperation = false
                    viewModel.isCopyMoveDialogOpen = true
                }
                ToolbarAction(Icons.Default.Delete, "Delete") { viewModel.deleteSelected() }
                ToolbarAction(Icons.Default.Info, "Info") { viewModel.isInfoDialogOpen = true }
                
                var showMoreMenu by remember { mutableStateOf(false) }
                Box {
                    ToolbarAction(Icons.Default.MoreVert, "More") { showMoreMenu = true }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        Text("View Options", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                        DropdownMenuItem(text = { Text("Details") }, onClick = { viewModel.viewOption = ArchiveViewModel.ViewOption.DETAILS; showMoreMenu = false })
                        DropdownMenuItem(text = { Text("List") }, onClick = { viewModel.viewOption = ArchiveViewModel.ViewOption.LIST; showMoreMenu = false })
                        Divider()
                        Text("Sort By", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                        DropdownMenuItem(text = { Text("Name") }, onClick = { viewModel.setSort(ArchiveViewModel.SortOption.NAME); showMoreMenu = false })
                        DropdownMenuItem(text = { Text("Type") }, onClick = { viewModel.setSort(ArchiveViewModel.SortOption.TYPE); showMoreMenu = false })
                        DropdownMenuItem(text = { Text("Size") }, onClick = { viewModel.setSort(ArchiveViewModel.SortOption.SIZE); showMoreMenu = false })
                        DropdownMenuItem(text = { Text("Date") }, onClick = { viewModel.setSort(ArchiveViewModel.SortOption.DATE); showMoreMenu = false })
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ... (rest of the content)
            Text(
                text = viewModel.currentDirectory.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(viewModel.fileItems) { item ->
                    FileRow(
                        item = item,
                        isSelected = viewModel.selectedFiles.contains(item.file),
                        onClick = {
                            if (item.isDirectory) viewModel.navigateTo(item.file)
                            else viewModel.toggleSelection(item.file)
                        },
                        onLongClick = { viewModel.toggleSelection(item.file) }
                    )
                }
            }
        }
    }

    if (viewModel.isArchiveDialogOpen) {
        ArchiveDialog(
            initialDir = viewModel.currentDirectory.absolutePath,
            onDismiss = { viewModel.isArchiveDialogOpen = false },
            onConfirm = { file, options ->
                viewModel.addFilesToArchive(file, options)
            }
        )
    }

    if (viewModel.isInfoDialogOpen) {
        val selectedFile = viewModel.selectedFiles.firstOrNull() ?: viewModel.currentDirectory
        ArchiveInfoDialog(
            file = selectedFile,
            onDismiss = { viewModel.isInfoDialogOpen = false }
        )
    }

    if (viewModel.isCopyMoveDialogOpen) {
        CopyMoveDialog(
            isCopy = viewModel.isCopyOperation,
            onDismiss = { viewModel.isCopyMoveDialogOpen = false },
            onConfirm = { destDir ->
                viewModel.copyOrMoveSelected(destDir)
            }
        )
    }

    if (viewModel.isPasswordDialogOpen) {
        PasswordDialog(
            onDismiss = { 
                viewModel.isPasswordDialogOpen = false
                viewModel.pendingArchiveFile = null
            },
            onConfirm = { password ->
                viewModel.pendingArchiveFile?.let { file ->
                    viewModel.extractArchive(file, viewModel.currentDirectory, password)
                }
            }
        )
    }
}

@Composable
fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Required") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Enter Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(password) }) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ArchiveInfoDialog(file: java.io.File, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Information") },
        text = {
            Column {
                InfoItem("Name", file.name)
                InfoItem("Path", file.absolutePath)
                InfoItem("Size", if (file.isDirectory) "N/A" else formatSize(file.length()))
                InfoItem("Modified", formatDate(file.lastModified()))
                if (file.isDirectory) {
                    InfoItem("Contains", "${file.listFiles()?.size ?: 0} items")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun InfoItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label: ", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Text(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyMoveDialog(isCopy: Boolean, onDismiss: () -> Unit, onConfirm: (java.io.File) -> Unit) {
    var path by remember { mutableStateOf("/sdcard") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCopy) "Copy to..." else "Move to...") },
        text = {
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text("Destination Path") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(java.io.File(path)) }) {
                Text(if (isCopy) "Copy" else "Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ToolbarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(icon, contentDescription = label)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FileRow(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            Text("${if (item.isDirectory) "Folder" else formatSize(item.size)} | ${formatDate(item.lastModified)}")
        },
        leadingContent = {
            Icon(
                if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(time: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(time))
}
