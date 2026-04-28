package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceMonitorScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    
    // Simulate real-time stats
    var cpuUsage by remember { mutableStateOf(0) }
    var ramUsage by remember { mutableStateOf(0) }
    var temp by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            cpuUsage = (10..90).random()
            ramUsage = (30..85).random()
            temp = (35..65).random()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RESOURCE MONITOR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = currentTheme.accentColor
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard("CPU Usage", "$cpuUsage%", cpuUsage / 100f, currentTheme.accentColor)
            }
            item {
                StatCard("RAM Usage", "$ramUsage%", ramUsage / 100f, Color(0xFF00FF99))
            }
            item {
                StatCard("Temperature", "$temp°C", temp / 100f, Color(0xFFFF0033))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, progress: Float, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.White.copy(alpha = 0.7f))
                Text(value, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}
