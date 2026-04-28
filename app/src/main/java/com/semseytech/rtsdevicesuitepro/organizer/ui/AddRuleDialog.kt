package com.semseytech.rtsdevicesuitepro.organizer.ui

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerOptions
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerRule
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTrigger
import com.semseytech.rtsdevicesuitepro.organizer.model.ArchiveOptions
import java.io.File

fun Uri.toFilePath(context: Context): String? {
    try {
        if (this.scheme == "file") return this.path
        if (DocumentsContract.isTreeUri(this)) {
            val documentId = DocumentsContract.getTreeDocumentId(this)
            val split = documentId.split(":")
            if (split.size >= 2) {
                val type = split[0]
                val path = split[1]
                val relativePath = if (path.startsWith("/")) path.substring(1) else path
                
                if ("primary".equals(type, ignoreCase = true)) {
                    return File(Environment.getExternalStorageDirectory(), relativePath).absolutePath
                } else {
                    // SD Card handle
                    val externalFilesDirs = context.getExternalFilesDirs(null)
                    for (file in externalFilesDirs) {
                        if (file != null) {
                            val absPath = file.absolutePath
                            if (absPath.contains(type)) {
                                val rootPath = absPath.split("/Android")[0]
                                return File(rootPath, relativePath).absolutePath
                            }
                        }
                    }
                }
            } else if (documentId == "downloads") {
                 return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (OrganizerRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sourcePath by remember { mutableStateOf("") }
    var targetPath by remember { mutableStateOf("") }
    
    val presetCategories = listOf("Audio", "Video", "Image", "Document", "Archive", "Custom")
    var selectedCategory by remember { mutableStateOf(presetCategories[0]) }
    var customExtensions by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val triggers = listOf("On Folder Modified", "On Idle", "On Power Connected", "Interval (Minutes)")
    var selectedTriggerName by remember { mutableStateOf(triggers[0]) }
    var triggerDropdownExpanded by remember { mutableStateOf(false) }
    var intervalMinutes by remember { mutableStateOf("60") }

    var moveEntireFolder by remember { mutableStateOf(false) }
    var ignoreSubfolders by remember { mutableStateOf(true) }
    var autoExtract by remember { mutableStateOf(false) }
    var deleteAfterExtraction by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val commonFolders = remember {
        val base = Environment.getExternalStorageDirectory()
        val downloads = File(base, "Download")
        val downloadsPlural = File(base, "Downloads")
        val downloadPath = when {
            downloads.exists() -> downloads.absolutePath
            downloadsPlural.exists() -> downloadsPlural.absolutePath
            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        
        mapOf(
            "Downloads" to downloadPath,
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath,
            "DCIM" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
        )
    }

    val sourcePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            val resolved = it.toFilePath(context)
            if (resolved != null) sourcePath = resolved
        }
    }

    val targetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            val resolved = it.toFilePath(context)
            if (resolved != null) targetPath = resolved
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Organization Rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Source Folder
                PathInputField(
                    label = "Source Folder",
                    value = sourcePath,
                    onValueChange = { sourcePath = it },
                    onPickClick = { sourcePickerLauncher.launch(null) },
                    commonFolders = commonFolders,
                    showDownloadsWarning = true
                )

                // Destination Folder
                PathInputField(
                    label = "Destination Folder",
                    value = targetPath,
                    onValueChange = { targetPath = it },
                    onPickClick = { targetPickerLauncher.launch(null) },
                    commonFolders = commonFolders
                )

                // File Types
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("File Types") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = categoryDropdownExpanded, onDismissRequest = { categoryDropdownExpanded = false }) {
                        presetCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = { selectedCategory = category; categoryDropdownExpanded = false }
                            )
                        }
                    }
                }

                if (selectedCategory == "Custom") {
                    OutlinedTextField(
                        value = customExtensions,
                        onValueChange = { customExtensions = it },
                        label = { Text("Extensions (csv, apk, etc.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Trigger
                ExposedDropdownMenuBox(
                    expanded = triggerDropdownExpanded,
                    onExpandedChange = { triggerDropdownExpanded = !triggerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTriggerName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Trigger Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerDropdownExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = triggerDropdownExpanded, onDismissRequest = { triggerDropdownExpanded = false }) {
                        triggers.forEach { trigger ->
                            DropdownMenuItem(
                                text = { Text(trigger) },
                                onClick = { selectedTriggerName = trigger; triggerDropdownExpanded = false }
                            )
                        }
                    }
                }

                if (selectedTriggerName == "Interval (Minutes)") {
                    OutlinedTextField(
                        value = intervalMinutes,
                        onValueChange = { intervalMinutes = it },
                        label = { Text("Interval") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                Text("Conditions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                
                ConditionOption("Move entire folder on match", moveEntireFolder) { moveEntireFolder = it }
                ConditionOption("Scan subfolders", !ignoreSubfolders) { ignoreSubfolders = !it }

                if (selectedCategory == "Archive" || selectedCategory == "Custom") {
                    HorizontalDivider()
                    Text("Archive Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    ConditionOption("Auto-extract", autoExtract) { autoExtract = it }
                    if (autoExtract) {
                        ConditionOption("Delete after extraction", deleteAfterExtraction, modifier = Modifier.padding(start = 16.dp)) { deleteAfterExtraction = it }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && sourcePath.isNotBlank() && targetPath.isNotBlank()) {
                    onConfirm(OrganizerRule(
                        name = name,
                        sourcePath = sourcePath,
                        targetPath = targetPath,
                        fileTypes = if (selectedCategory == "Custom") customExtensions.split(",").map { it.trim() } else listOf(selectedCategory.lowercase()),
                        trigger = when (selectedTriggerName) {
                            "On Folder Modified" -> RuleTrigger.OnFolderModified
                            "Interval (Minutes)" -> RuleTrigger.Interval(intervalMinutes.toIntOrNull() ?: 60)
                            else -> RuleTrigger.OnIdle
                        },
                        options = OrganizerOptions(
                            moveEntireFolderIfContainsMatch = moveEntireFolder,
                            ignoreSubfolders = ignoreSubfolders,
                            archiveOptions = ArchiveOptions(autoExtract = autoExtract, deleteAfterExtraction = deleteAfterExtraction)
                        )
                    ))
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PathInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPickClick: () -> Unit,
    commonFolders: Map<String, String>,
    showDownloadsWarning: Boolean = false
) {
    val exists = remember(value) { value.isNotEmpty() && File(value).exists() }
    
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("/storage/emulated/0/...") },
            isError = value.isNotEmpty() && !exists,
            trailingIcon = {
                IconButton(onClick = onPickClick) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Select Folder")
                }
            },
            supportingText = {
                if (value.isNotEmpty() && !exists) {
                    Text("Folder not found or inaccessible", color = MaterialTheme.colorScheme.error)
                } else if (showDownloadsWarning && value.isEmpty()) {
                    Text("Note: System picker blocks 'Downloads'. Use chips.")
                }
            }
        )
        FlowRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            commonFolders.forEach { (label, path) ->
                SuggestionChip(
                    onClick = { onValueChange(path) },
                    label = { Text(label, fontSize = 10.sp) }
                )
            }
        }
    }
}

@Composable
fun ConditionOption(label: String, checked: Boolean, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
