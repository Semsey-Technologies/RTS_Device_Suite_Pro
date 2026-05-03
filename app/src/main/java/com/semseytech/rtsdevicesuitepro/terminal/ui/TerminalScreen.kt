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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TerminalScreen(onBack: () -> Unit, viewModel: TerminalViewModel = viewModel()) {
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var showClipboard by remember { mutableStateOf(false) }
    val clipboard by viewModel.clipboard.collectAsState()

    LaunchedEffect(viewModel.emulator.lines.size) {
        if (viewModel.emulator.lines.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.emulator.lines.size - 1)
        }
    }

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
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = currentTheme.accentColor)
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp * scale),
                state = listState,
                contentPadding = PaddingValues(top = 20.dp * scale)
            ) {
                items(viewModel.emulator.lines) { line ->
                    Text(
                        text = line,
                        style = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (12 * scale).sp
                        ),
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { viewModel.saveToClipboard(line.text) }
                        )
                    )
                }
            }

            // Special Keys Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val keys = listOf("ESC", "TAB", "CTRL-C", "CTRL-D", "SSH", "UP", "DOWN", "LEFT", "RIGHT")
                keys.forEach { key ->
                    TextButton(
                        onClick = { 
                            if (key == "SSH") {
                                input = "ssh "
                            } else {
                                viewModel.sendSpecialKey(key) 
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(key, color = currentTheme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(8.dp * scale),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("> ", color = currentTheme.accentColor, fontFamily = FontFamily.Monospace, fontSize = (14 * scale).sp)
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color.White, 
                        fontFamily = FontFamily.Monospace, 
                        fontSize = (14 * scale).sp
                    ),
                    cursorBrush = SolidColor(currentTheme.accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) {
                            viewModel.sendInput(input + "\n")
                            input = ""
                        }
                    })
                )
                if (input.isNotBlank()) {
                    IconButton(onClick = { viewModel.saveToClipboard(input) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save to Clipboard", tint = currentTheme.accentColor, modifier = Modifier.size(20.dp * scale))
                    }
                }
            }
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
                                        input = snippet
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
                                        input = snippet
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
