package com.semseytech.rtsdevicesuitepro.archive.ui

import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.semseytech.rtsdevicesuitepro.archive.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveDialog(
    initialDir: String,
    onDismiss: () -> Unit,
    onConfirm: (File, ArchiveOptions) -> Unit
) {
    var archiveName by remember { mutableStateOf("archive") }
    var options by remember { mutableStateOf(ArchiveOptions()) }
    var showExplanation by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var reenterPassword by remember { mutableStateOf("") }

    val formats = ArchiveFormat.values()
    val levels = CompressionLevel.values()
    val methods = CompressionMethod.values()
    val pathModes = PathMode.values()
    val encryptionMethods = EncryptionMethod.values()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Create Archive", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    label = { Text("Archive Name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Text(".${options.format.extension}", modifier = Modifier.padding(end = 8.dp)) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Location: $initialDir", style = MaterialTheme.typography.bodySmall)

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                SelectableOption("Archive Format", options.format.name) {
                    formats.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.name) },
                            onClick = { options = options.copy(format = format); it() }
                        )
                    }
                }

                SelectableOption("Compression Level", options.level.name) {
                    levels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.name) },
                            onClick = { options = options.copy(level = level); it() }
                        )
                    }
                }

                SelectableOption("Compression Method", options.method.name) {
                    methods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name) },
                            onClick = { options = options.copy(method = method); it() }
                        )
                    }
                }

                SelectableOption("Dictionary Size", "${options.dictionarySize} MB") {
                    listOf(2, 4, 8, 16, 32, 64, 128).forEach { size ->
                        DropdownMenuItem(
                            text = { Text("$size MB") },
                            onClick = { options = options.copy(dictionarySize = size); it() }
                        )
                    }
                }

                SelectableOption("Word Size", options.wordSize.toString()) {
                    listOf(8, 16, 24, 32, 48, 64, 96, 128).forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.toString()) },
                            onClick = { options = options.copy(wordSize = size); it() }
                        )
                    }
                }

                SelectableOption("Solid Block Size", if (options.solidBlockSize == -1L) "Solid" else if (options.solidBlockSize == 0L) "None" else "${options.solidBlockSize} MB") {
                    listOf(0L, 1L, 2L, 4L, 8L, 16L, 32L, 64L, -1L).forEach { size ->
                        DropdownMenuItem(
                            text = { Text(if (size == -1L) "Solid" else if (size == 0L) "None" else "$size MB") },
                            onClick = { options = options.copy(solidBlockSize = size); it() }
                        )
                    }
                }

                SelectableOption("Number of CPU Threads", if (options.threads == 0) "Auto" else options.threads.toString()) {
                    (0..Runtime.getRuntime().availableProcessors()).forEach { threadCount ->
                        DropdownMenuItem(
                            text = { Text(if (threadCount == 0) "Auto" else threadCount.toString()) },
                            onClick = { options = options.copy(threads = threadCount); it() }
                        )
                    }
                }

                OutlinedTextField(
                    value = if (options.splitSize == 0L) "" else options.splitSize.toString(),
                    onValueChange = { options = options.copy(splitSize = it.toLongOrNull() ?: 0L) },
                    label = { Text("Split to Volumes (bytes)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        var splitMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { splitMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = splitMenuExpanded, onDismissRequest = { splitMenuExpanded = false }) {
                            val presets = listOf(
                                "10 MB" to 10 * 1024 * 1024L,
                                "100 MB" to 100 * 1024 * 1024L,
                                "700 MB" to 700 * 1024 * 1024L,
                                "4 GB" to 4294967296L
                            )
                            presets.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { options = options.copy(splitSize = value); splitMenuExpanded = false }
                                )
                            }
                        }
                    }
                )

                SelectableOption("Path Mode", options.pathMode.name) {
                    pathModes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.name) },
                            onClick = { options = options.copy(pathMode = mode); it() }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Options", style = MaterialTheme.typography.titleMedium)

                CheckboxOption("Create SFX Archive", options.createSfx) { options = options.copy(createSfx = it) }
                CheckboxOption("Compress Shared Files", options.compressShared) { options = options.copy(compressShared = it) }
                CheckboxOption("Delete Files After Compression", options.deleteAfter) { options = options.copy(deleteAfter = it) }

                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Encryption", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reenterPassword,
                    onValueChange = { reenterPassword = it },
                    label = { Text("Re-enter Password") },
                    visualTransformation = if (showPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = password != reenterPassword && reenterPassword.isNotEmpty()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
                    Text("Show Password")
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(checked = options.encryptFileNames, onCheckedChange = { options = options.copy(encryptFileNames = it) })
                    Text("Encrypt Names")
                    IconButton(onClick = { showExplanation = "Encrypt File Names" }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(18.dp))
                    }
                }

                SelectableOption("Encryption Method", options.encryptionMethod.name) {
                    encryptionMethods.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name) },
                            onClick = { options = options.copy(encryptionMethod = method); it() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { 
                            val file = java.io.File(initialDir, "$archiveName.${options.format.extension}")
                            onConfirm(file, options.copy(password = password)) 
                        },
                        enabled = archiveName.isNotBlank() && (password == reenterPassword)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    if (showExplanation != null) {
        AlertDialog(
            onDismissRequest = { showExplanation = null },
            title = { Text(showExplanation!!) },
            text = { Text(ArchiveExplanations.getExplanation(showExplanation!!)) },
            confirmButton = { TextButton(onClick = { showExplanation = null }) { Text("OK") } }
        )
    }
}

@Composable
fun SelectableOption(label: String, value: String, menuContent: @Composable (dismiss: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(18.dp))
            }
        }
        Box {
            OutlinedCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(value, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                menuContent { expanded = false }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label) },
            text = { Text(ArchiveExplanations.getExplanation(label)) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } }
        )
    }
}

@Composable
fun CheckboxOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { showInfo = true }) {
            Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(18.dp))
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(label) },
            text = { Text(ArchiveExplanations.getExplanation(label)) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } }
        )
    }
}
