package com.semseytech.rtsdevicesuitepro.battery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleBatteryStatus
import com.semseytech.rtsdevicesuitepro.battery.data.OptimizationSuggestion
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Usage Estimator") },
                actions = {
                    IconButton(onClick = { viewModel.refreshUsage() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Estimated Module Impact (Last 24h)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "These values are estimates based on module activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            items(uiState.moduleStatuses) { status ->
                ModuleBatteryCard(status, onToggle = { viewModel.toggleModule(status.type, it) })
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Power Optimization Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.suggestions) { suggestion ->
                OptimizationCard(suggestion)
            }

            item {
                Button(
                    onClick = { viewModel.exportReport() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Battery Report (PDF)")
                }
            }

            if (uiState.exportedFile != null) {
                item {
                    Text(
                        "Report exported to: ${uiState.exportedFile?.absolutePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleBatteryCard(status: ModuleBatteryStatus, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(status.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (status.isEnabled) "Enabled" else "Disabled",
                        color = if (status.isEnabled) Color.Green else Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(checked = status.isEnabled, onCheckedChange = onToggle)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = (status.batteryPercent / 10f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = if (status.batteryPercent > 5f) Color.Red else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${status.batteryPercent.format(1)}%", fontWeight = FontWeight.Bold)
            }

            Text(
                "${status.estimatedMah.format(0)} mAh · Reduces battery life by ~${status.timeImpactHours.format(1)}h",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Info, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    status.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun OptimizationCard(suggestion: OptimizationSuggestion) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.title, fontWeight = FontWeight.Bold)
                Text(suggestion.description, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { /* Action */ }) {
                Text(suggestion.actionLabel)
            }
        }
    }
}

private fun Float.format(digits: Int) = "%.${digits}f".format(this)
