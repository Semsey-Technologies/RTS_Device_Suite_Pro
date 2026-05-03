package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.*
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val theme = LocalTheme.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "cog_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(theme.startColor, theme.endColor)
                )
            )
    ) {
        // Background Cog Wheel
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .graphicsLayer(rotationZ = rotation)
                .alpha(0.05f),
            tint = theme.accentColor
        )

        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Active Alerts
            uiState.activeAlerts.forEach { alert ->
                AlertCard(alert, theme) { viewModel.dismissAlert(alert.id) }
                Spacer(modifier = Modifier.height(12.dp))
            }

            SystemStatusSection(uiState, onNavigate)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            MaintenanceSection(uiState) { viewModel.completeMaintenanceTask(it) }

            Spacer(modifier = Modifier.height(24.dp))
            
            NetworkAnalyzerSection(
                uiState = uiState,
                onNavigate = onNavigate,
                onRunDiagnostic = { viewModel.runNetworkDiagnostic() },
                onExportLog = { viewModel.exportDiagnosticLog() }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            QuickAccessSection(onNavigate, uiState)

            Spacer(modifier = Modifier.height(24.dp))

            AutoTasksSection(
                uiState = uiState,
                onDailyBackupToggle = { viewModel.toggleDailyBackup(it) },
                onAutoCleanToggle = { viewModel.toggleAutoClean(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SystemStatusSection(uiState: DashboardUiState, onNavigate: (String) -> Unit) {
    val theme = LocalTheme.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("System Status")
            HealthBadge(uiState.healthScore, uiState.healthStatus)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device Info Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${uiState.deviceInfo.manufacturer} ${uiState.deviceInfo.model} • Android ${uiState.deviceInfo.androidVersion}",
                        color = theme.textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                if (uiState.modelSpec.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        uiState.modelSpec.notes,
                        color = theme.accentColor.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusGauge(
                        label = "Storage",
                        value = (uiState.storageUsedPercent * 100).toInt(),
                        subValue = "${uiState.storageFreeSpace} Free",
                        color = Color(0xFF00BFFF), // Blue
                        onClick = { onNavigate(Screen.StorageAnalyzer.route) }
                    )
                    StatusGauge(
                        label = "Battery",
                        value = (uiState.batteryLevel * 100).toInt(),
                        color = Color(0xFF00FF99), // Green
                        onClick = { onNavigate(Screen.BatteryEstimation.route) }
                    )
                    StatusGauge(
                        label = "CPU Temp",
                        value = (uiState.cpuTemp * 100).toInt(),
                        unit = "°C",
                        color = Color(0xFFFF8C00), // Orange
                        onClick = { onNavigate(Screen.ResourceMonitor.route) }
                    )
                    StatusGauge(
                        label = "Uptime",
                        valueString = formatUptime(uiState.uptimeSeconds),
                        progress = 1.0f,
                        color = Color(0xFF00FFFF), // Cyan
                        onClick = { onNavigate(Screen.ResourceMonitor.route) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Last Backup: ${uiState.lastBackupDate}",
                        color = theme.textColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Last Sync: ${uiState.lastSyncDate}",
                        color = theme.textColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatusGauge(
    label: String,
    value: Int? = null,
    valueString: String? = null,
    progress: Float? = null,
    unit: String = "%",
    subValue: String? = null,
    color: Color,
    onClick: () -> Unit
) {
    val displayProgress = progress ?: (value?.toFloat()?.div(100f) ?: 0f)
    val animatedProgress by animateFloatAsState(
        targetValue = displayProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueString ?: "$value$unit",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (valueString != null) 10.sp else 14.sp,
                    textAlign = TextAlign.Center
                )
                if (subValue != null) {
                    Text(
                        text = subValue,
                        color = color.copy(alpha = 0.8f),
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun NetworkAnalyzerSection(
    uiState: DashboardUiState,
    onNavigate: (String) -> Unit,
    onRunDiagnostic: () -> Unit,
    onExportLog: () -> Unit
) {
    val theme = LocalTheme.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.Network.route) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Network Analyzer")
            Icon(Icons.Default.ChevronRight, null, tint = theme.accentColor)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DiagnosticButton(
                    isRunning = uiState.isDiagnosticRunning,
                    onClick = onRunDiagnostic
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                NetworkGrid(uiState.networkDiagnostics)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onExportLog,
                    modifier = Modifier.fillMaxWidth(0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Export Log")
                }
            }
        }
    }
}

@Composable
fun DiagnosticButton(isRunning: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .scale(pulseScale),
        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(if (isRunning) "Scanning..." else "Run Diagnostic", color = Color.White)
    }
}

@Composable
fun NetworkGrid(diag: NetworkDiagnostics) {
    val theme = LocalTheme.current
    val borderColor = theme.accentColor.copy(alpha = 0.1f)
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            NetworkItem("Local IP", diag.localIp, Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(40.dp), color = borderColor)
            NetworkItem("External IP", diag.externalIp, Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(40.dp), color = borderColor)
            NetworkItem("Ping", diag.ping, Modifier.weight(1f))
        }
        HorizontalDivider(color = borderColor, modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            NetworkItem("Download", diag.download, Modifier.weight(1f), highlightColor = Color(0xFF00BFFF))
            VerticalDivider(modifier = Modifier.height(40.dp), color = borderColor)
            NetworkItem("Upload", diag.upload, Modifier.weight(1f))
            VerticalDivider(modifier = Modifier.height(40.dp), color = borderColor)
            NetworkItem("Wi-Fi Signal", diag.wifiSignal, Modifier.weight(1f), highlightColor = Color(0xFF00FF99))
        }
        HorizontalDivider(color = borderColor, modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            NetworkItem("Packet Loss", diag.packetLoss, Modifier.weight(1f))
        }
    }
}

@Composable
fun NetworkItem(label: String, value: String, modifier: Modifier = Modifier, highlightColor: Color? = null) {
    val theme = LocalTheme.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(
            value, 
            color = highlightColor ?: Color.White, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QuickAccessSection(onNavigate: (String) -> Unit, uiState: DashboardUiState) {
    Column {
        SectionHeader("Quick Access")
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile("Auto Organizer", Icons.Default.Folder, "", Modifier.weight(1f)) { onNavigate(Screen.SmartOrganizer.route) }
            QuickAccessTile("Archive Manager", Icons.Default.Archive, "", Modifier.weight(1f)) { onNavigate(Screen.Archive.route) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile(
                label = "Cleaner", 
                icon = Icons.Default.CleaningServices, 
                status = if (uiState.isAutoCleanEnabled) "Auto Active" else "Manual", 
                modifier = Modifier.weight(1f)
            ) { onNavigate(Screen.Cleaner.route) }
            QuickAccessTile("File Explorer", Icons.Default.FolderOpen, "", Modifier.weight(1f)) { onNavigate(Screen.FileExplorer.route) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile("System Tools", Icons.Default.Build, "Utilities", Modifier.weight(1f)) { onNavigate(Screen.Tools.route) }
            QuickAccessTile("Automation", Icons.Default.AutoFixHigh, "Rules", Modifier.weight(1f)) { onNavigate(Screen.Automation.route) }
        }
    }
}

@Composable
fun QuickAccessTile(
    label: String,
    icon: ImageVector,
    status: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by animateFloatAsState(if (isPressed) 0.6f else 0f)

    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = theme.accentColor.copy(alpha = glowAlpha),
                        size = size,
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = theme.accentColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (status.isNotEmpty()) {
                Text(status, color = theme.accentColor, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun AutoTasksSection(
    uiState: DashboardUiState,
    onDailyBackupToggle: (Boolean) -> Unit,
    onAutoCleanToggle: (Boolean) -> Unit
) {
    val theme = LocalTheme.current
    Column {
        SectionHeader("Auto Tasks")
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ToggleRow("Daily Backup", uiState.isDailyBackupEnabled, onDailyBackupToggle)
                HorizontalDivider(color = theme.accentColor.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
                Column {
                    ToggleRow("Auto Clean", uiState.isAutoCleanEnabled, onAutoCleanToggle)
                    if (uiState.isAutoCleanEnabled) {
                        Text(
                            text = uiState.autoCleanSummary,
                            color = theme.accentColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.accentColor,
                checkedTrackColor = theme.accentColor.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    val theme = LocalTheme.current
    Text(
        text = title,
        color = theme.accentColor,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun HealthBadge(score: Int, status: String) {
    val color = when {
        score >= 90 -> Color(0xFF00FF99)
        score >= 75 -> Color(0xFFADFF2F)
        score >= 50 -> Color(0xFFFFD700)
        score >= 25 -> Color(0xFFFF8C00)
        else -> Color(0xFFFF4500)
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "$status ($score)",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AlertCard(alert: MaintenanceAlert, theme: ThemePreset, onDismiss: () -> Unit) {
    val color = when (alert.severity) {
        AlertSeverity.CRITICAL -> Color.Red
        AlertSeverity.WARNING -> Color(0xFFFF8C00)
        AlertSeverity.INFO -> theme.accentColor
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (alert.severity == AlertSeverity.CRITICAL) Icons.Default.Error else Icons.Default.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(alert.message, color = theme.textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MaintenanceSection(uiState: DashboardUiState, onCompleteTask: (String) -> Unit) {
    val theme = LocalTheme.current
    Column {
        SectionHeader("Physical Maintenance")
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                uiState.maintenanceTasks.forEach { task ->
                    MaintenanceItem(task, theme, onCompleteTask)
                }
            }
        }
    }
}

@Composable
fun MaintenanceItem(task: MaintenanceTask, theme: ThemePreset, onComplete: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = if (task.isDue) Color.Red.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = if (task.isDue) Icons.Default.PriorityHigh else Icons.Default.Check,
                contentDescription = null,
                tint = if (task.isDue) Color.Red else Color.Green,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, color = theme.textColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(task.description, color = theme.textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
        
        if (task.isDue) {
            TextButton(onClick = { onComplete(task.id) }) {
                Text("MARK DONE", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                "COMPLETED",
                color = Color.Green.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "${h}h ${m}m"
}
