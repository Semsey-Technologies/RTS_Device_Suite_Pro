package com.semseytech.rtsdevicesuitepro.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit, viewModel: TerminalViewModel = viewModel()) {
    val currentTheme = LocalTheme.current
    var showClipboard by remember { mutableStateOf(false) }
    val clipboard by viewModel.clipboard.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADVANCED TERMINAL", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { showClipboard = true }) {
                        Icon(Icons.Default.Assignment, contentDescription = "Clipboard", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black, 
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            MultiTabTerminalUI(viewModel)
        }
    }


    if (showClipboard) {
        AlertDialog(
            onDismissRequest = { showClipboard = false },
            title = { Text("Terminal Clipboard", color = currentTheme.accentColor) },
            text = {
                if (clipboard.isEmpty()) {
                    Text("No saved snippets", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(clipboard) { snippet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendInput(snippet)
                                        showClipboard = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = snippet,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2
                                )
                                Row {
                                    IconButton(onClick = { 
                                        viewModel.sendInput(snippet)
                                        showClipboard = false
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Inject", tint = currentTheme.accentColor)
                                    }
                                    IconButton(onClick = { viewModel.removeFromClipboard(snippet) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearClipboard() }) {
                    Text("CLEAR ALL", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboard = false }) {
                    Text("CLOSE", color = currentTheme.accentColor)
                }
            },
            containerColor = Color(0xFF121212)
        )
    }
}
