package com.semseytech.rtsdevicesuitepro.adb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.adb.core.AdbManager

@Composable
fun AdbConsoleScreen(
    viewModel: AdbViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCommand by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ADB Command Console", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
                Text(
                    text = uiState.lastCommandOutput.ifBlank { "Console output will appear here..." },
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Prebuilt Safe Commands", fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(AdbManager.SAFE_COMMANDS) { cmd ->
                OutlinedButton(
                    onClick = { selectedCommand = cmd },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(cmd, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = selectedCommand,
            onValueChange = { selectedCommand = it },
            label = { Text("Command") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { viewModel.runCommand(selectedCommand) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                }
            }
        )
    }
}
