package com.semseytech.rtsdevicesuitepro.wipe.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.wipe.WipeViewModel
import com.semseytech.rtsdevicesuitepro.wipe.WipeUiState
import com.semseytech.rtsdevicesuitepro.wipe.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WipeSuiteScreen(
    onNavigate: (String) -> Unit,
    viewModel: WipeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalTheme.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.startColor, theme.endColor)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SECURE WIPE SUITE",
                            color = theme.accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.Default.QuestionMark, "Help", tint = theme.accentColor)
                        }
                    }
                }

                // Model-Specific Intelligence Card
                item {
                    ModelIntelligenceCard(uiState.modelSpec)
                }

                // Readiness Section
                item {
                    ReadinessCard(uiState.readiness) { viewModel.runReadinessScan() }
                }

                // Global Simulation Mode Toggle
                item {
                    SimulationModeCard(
                        isEnabled = uiState.isSimulationMode,
                        onToggle = { viewModel.toggleSimulationMode(it) }
                    )
                }

                // Wipe Categories
                items(uiState.categories) { category ->
                    WipeCategoryCard(
                        category = category,
                        onExpandToggle = { viewModel.toggleCategoryExpansion(category.id) },
                        onModeChange = { viewModel.setCategoryControlMode(category.id, it) },
                        onItemToggle = { itemId -> viewModel.toggleItemSelection(category.id, itemId) },
                        onItemModeChange = { itemId, mode -> viewModel.setItemControlMode(category.id, itemId, mode) }
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isSimulationMode) Color(0xFF00BFFF) else Color.Red
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isRunning && uiState.categories.any { cat -> cat.items.any { it.isSelected } }
                        ) {
                            Text(
                                text = if (uiState.isSimulationMode) "RUN SIMULATION" else "RUN SECURE WIPE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        if (uiState.lastReport != null) {
                            OutlinedButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.5f))
                            ) {
                                Text("VIEW LAST REPORT", color = theme.accentColor)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // Progress Overlay
        if (uiState.isRunning) {
            WipeProgressOverlay(uiState)
        }
    }

    if (showConfirmDialog) {
        WipeConfirmationDialog(
            isSimulation = uiState.isSimulationMode,
            onConfirm = {
                showConfirmDialog = false
                viewModel.runWipe()
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
    
    if (showHelpDialog) {
        WipeHelpDialog(onDismiss = { showHelpDialog = false })
    }
    
    if (showReportDialog) {
        uiState.lastReport?.let {
            WipeReportDialog(report = it, onDismiss = { showReportDialog = false })
        }
    }
}

@Composable
fun ModelIntelligenceCard(spec: com.semseytech.rtsdevicesuitepro.model.ModelIntelligence.DeviceSpec) {
    val theme = LocalTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = theme.accentColor.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, null, tint = theme.accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Model Intelligence: ${spec.manufacturer} ${spec.model}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(spec.wipeGuidance, color = theme.accentColor, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ReadinessCard(readiness: WipeReadiness?, onRefresh: () -> Unit) {
    val theme = LocalTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wipe Readiness Scan", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                HealthBadge(readiness?.score ?: 0)
            }
            
            if (readiness != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ReadinessDetailRow(Icons.Default.BatteryChargingFull, "Battery: ${readiness.batteryLevel}% (${if (readiness.isCharging) "Charging" else "Not Charging"})")
                ReadinessDetailRow(Icons.Default.Storage, "Storage Health: ${readiness.storageHealth}")
                ReadinessDetailRow(Icons.Default.Backup, "Backup Status: ${readiness.backupStatus}")
                
                if (readiness.recommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    readiness.recommendations.forEach { rec ->
                        Text("• $rec", color = theme.accentColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadinessDetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
fun HealthBadge(score: Int) {
    val color = when {
        score >= 90 -> Color(0xFF00FF99)
        score >= 70 -> Color(0xFFFFD700)
        else -> Color(0xFFFF4500)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            "$score/100",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SimulationModeCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFF00BFFF).copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) Color(0xFF00BFFF) else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Visibility, null, tint = if (isEnabled) Color(0xFF00BFFF) else Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Simulation Mode", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Preview wipe actions without deleting files",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00BFFF))
            )
        }
    }
}

@Composable
fun WipeCategoryCard(
    category: WipeCategory,
    onExpandToggle: () -> Unit,
    onModeChange: (WipeControlMode) -> Unit,
    onItemToggle: (String) -> Unit,
    onItemModeChange: (String, WipeControlMode) -> Unit
) {
    val theme = LocalTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.15f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(category.icon, null, tint = theme.accentColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(category.description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Icon(
                    if (category.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }

            AnimatedVisibility(visible = category.isExpanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ControlModeSelector(
                        selectedMode = category.controlMode,
                        onModeSelected = onModeChange
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    category.items.forEach { item ->
                        WipeItemRow(
                            item = item,
                            onToggle = { onItemToggle(item.id) },
                            onModeChange = { onItemModeChange(item.id, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlModeSelector(
    selectedMode: WipeControlMode,
    onModeSelected: (WipeControlMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WipeControlMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onModeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun WipeItemRow(
    item: WipeItem,
    onToggle: () -> Unit,
    onModeChange: (WipeControlMode) -> Unit
) {
    val theme = LocalTheme.current
    var showInfo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = theme.accentColor)
            )
            Column(modifier = Modifier.weight(1f).clickable { onToggle() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, color = Color.White, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusIndicator(item.status)
                }
                Text(item.description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            IconButton(onClick = { showInfo = !showInfo }) {
                Icon(Icons.Default.Info, null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
            }
        }
        
        if (showInfo) {
            Text(
                text = item.infoText,
                color = theme.accentColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun StatusIndicator(status: WipeStatus) {
    val color = when (status) {
        WipeStatus.COMPLETED -> Color.Green
        WipeStatus.SIMULATED -> Color(0xFF00BFFF)
        WipeStatus.ERROR -> Color.Red
        WipeStatus.ACTIVE -> Color.Yellow
        else -> return
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun WipeProgressOverlay(uiState: WipeUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (uiState.isSimulationMode) "SIMULATING WIPE..." else "SECURING DATA...",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(
                    progress = { uiState.currentProgress?.progress ?: 0f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                    color = if (uiState.isSimulationMode) Color(0xFF00BFFF) else Color.Red,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${((uiState.currentProgress?.progress ?: 0f) * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pass ${uiState.currentProgress?.passes ?: 1}/${uiState.currentProgress?.totalPasses ?: 1}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = uiState.currentProgress?.currentItem ?: "Initializing...",
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiState.currentProgress?.detail ?: "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WipeConfirmationDialog(isSimulation: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
        title = {
            Text(if (isSimulation) "Confirm Simulation" else "DANGER: DESTRUCTIVE ACTION")
        },
        text = {
            Text(
                if (isSimulation) 
                    "This will preview the secure wipe process. No files will be deleted."
                else 
                    "You are about to PERMANENTLY delete and overwrite selected data. This CANNOT be undone. Are you absolutely sure?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (isSimulation) Color(0xFF00BFFF) else Color.Red)
            ) {
                Text("PROCEED")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun WipeHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Secure Wipe Guide") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Step 1: Run Readiness Scan to check battery and permissions.", fontWeight = FontWeight.Bold)
                Text("Step 2: Use Simulation Mode first to see what will happen.", fontWeight = FontWeight.Bold)
                Text("Step 3: Select categories or individual items to wipe.", fontWeight = FontWeight.Bold)
                Text("Step 4: Review model-specific guidance at the top.", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Modes Explanation:", fontWeight = FontWeight.Bold)
                Text("• Automated: Suite chooses best methods.")
                Text("• Guided: Step-by-step confirmation.")
                Text("• Manual: Full user control over passes.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("GOT IT") }
        }
    )
}

@Composable
fun WipeReportDialog(report: WipeReport, onDismiss: () -> Unit) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (report.isSimulation) "Simulation Report" else "Wipe Verification Report") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Start: ${sdf.format(Date(report.startTime))}")
                Text("End: ${sdf.format(Date(report.endTime))}")
                Text("Items Processed: ${report.totalItems}")
                Text("Status: ${if (report.errors > 0) "Completed with Errors" else "Success"}")
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                report.logEntries.forEach { entry ->
                    Text("• ${entry.itemName}: ${entry.status}", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}
