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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleEditorDialog(
    rule: OrganizerRule? = null,
    onDismiss: () -> Unit,
    onConfirm: (OrganizerRule) -> Unit
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var sourcePaths by remember { mutableStateOf(rule?.sourcePaths ?: listOf("")) }
    var targetPath by remember { mutableStateOf(rule?.targetPath ?: "") }
    
    val presetCategories = listOf("Audio", "Video", "Image", "Document", "Archive", "Custom")
    val initialCategory = remember(rule) {
        if (rule == null) presetCategories[0]
        else {
            val type = rule.fileTypes.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Custom"
            if (presetCategories.contains(type)) type else "Custom"
        }
    }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var customExtensions by remember { mutableStateOf(if (initialCategory == "Custom") rule?.fileTypes?.joinToString(",") ?: "" else "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val triggers = listOf("On Folder Modified", "On Idle", "On Power Connected", "Interval (Minutes)")
    val initialTriggerName = remember(rule) {
        when (rule?.trigger) {
            is RuleTrigger.OnFolderModified -> "On Folder Modified"
            is RuleTrigger.Interval -> "Interval (Minutes)"
            is RuleTrigger.OnPowerConnected -> "On Power Connected"
            else -> "On Idle"
        }
    }
    var selectedTriggerName by remember { mutableStateOf(initialTriggerName) }
    var triggerDropdownExpanded by remember { mutableStateOf(false) }
    var intervalMinutes by remember { mutableStateOf(if (rule?.trigger is RuleTrigger.Interval) (rule.trigger as RuleTrigger.Interval).minutes.toString() else "60") }

    var moveEntireFolder by remember { mutableStateOf(rule?.options?.moveEntireFolderIfContainsMatch ?: false) }
    var ignoreSubfolders by remember { mutableStateOf(rule?.options?.ignoreSubfolders ?: true) }
    var autoExtract by remember { mutableStateOf(rule?.options?.archiveOptions?.autoExtract ?: false) }
    var deleteAfterExtraction by remember { mutableStateOf(rule?.options?.archiveOptions?.deleteAfterExtraction ?: false) }

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

    var activePickerIndex by remember { mutableStateOf(-1) }
    val sourcePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { 
            val resolved = it.toFilePath(context)
            if (resolved != null && activePickerIndex != -1) {
                val newPaths = sourcePaths.toMutableList()
                newPaths[activePickerIndex] = resolved
                sourcePaths = newPaths
            }
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
        title = { Text(if (rule == null) "Create Organization Rule" else "Edit Organization Rule", fontWeight = FontWeight.Bold) },
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

                // Source Folders
                Text("Source Folders", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                sourcePaths.forEachIndexed { index, path ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PathInputField(
                                label = "Source Folder ${index + 1}",
                                value = path,
                                onValueChange = { newPath ->
                                    val newPaths = sourcePaths.toMutableList()
                                    newPaths[index] = newPath
                                    sourcePaths = newPaths
                                },
                                onPickClick = { 
                                    activePickerIndex = index
                                    sourcePickerLauncher.launch(null) 
                                },
                                commonFolders = commonFolders,
                                showDownloadsWarning = index == 0
                            )
                        }
                        if (sourcePaths.size > 1) {
                            IconButton(
                                onClick = {
                                    sourcePaths = sourcePaths.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Source", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = { sourcePaths = sourcePaths + "" },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ADD ANOTHER SOURCE")
                }

                HorizontalDivider()

                // Destination Folder
                PathInputField(
                    label = "Destination Folder",
                    value = targetPath,
                    onValueChange = { targetPath = it },
                    onPickClick = { targetPickerLauncher.launch(null) },
                    commonFolders = commonFolders
                )

                HorizontalDivider()

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
                val validSources = sourcePaths.filter { it.isNotBlank() }
                if (name.isNotBlank() && validSources.isNotEmpty() && targetPath.isNotBlank()) {
                    onConfirm(OrganizerRule(
                        id = rule?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name,
                        sourcePaths = validSources,
                        targetPath = targetPath,
                        fileTypes = if (selectedCategory == "Custom") customExtensions.split(",").map { it.trim() } else listOf(selectedCategory.lowercase()),
                        trigger = when (selectedTriggerName) {
                            "On Folder Modified" -> RuleTrigger.OnFolderModified
                            "Interval (Minutes)" -> RuleTrigger.Interval(intervalMinutes.toIntOrNull() ?: 60)
                            "On Power Connected" -> RuleTrigger.OnPowerConnected
                            else -> RuleTrigger.OnIdle
                        },
                        options = OrganizerOptions(
                            moveEntireFolderIfContainsMatch = moveEntireFolder,
                            ignoreSubfolders = ignoreSubfolders,
                            archiveOptions = ArchiveOptions(autoExtract = autoExtract, deleteAfterExtraction = deleteAfterExtraction)
                        ),
                        isEnabled = rule?.isEnabled ?: true
                    ))
                }
            }) { Text(if (rule == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
