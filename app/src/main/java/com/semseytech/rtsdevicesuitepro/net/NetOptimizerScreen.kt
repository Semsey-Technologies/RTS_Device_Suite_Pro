package com.semseytech.rtsdevicesuitepro.net

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetOptimizerScreen(
    viewModel: NetOptimizerViewModel,
    onBack: () -> Unit,
    onNavigateToAutomation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NET OPTIMIZER PRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = currentTheme.textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = currentTheme.textColor),
                actions = {
                    IconButton(onClick = onNavigateToAutomation) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = "Automation", tint = currentTheme.accentColor)
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(currentTheme.startColor, currentTheme.endColor)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoDashboard(uiState.networkInfo, currentTheme, scale)
                
                QualityMetricsSection(uiState.qualityMetrics, currentTheme, scale)
                
                OptimizationSection(
                    isOptimizing = uiState.isOptimizing,
                    log = uiState.optimizationLog,
                    accentColor = currentTheme.accentColor,
                    scale = scale,
                    onOptimize = { viewModel.runOptimization() }
                )
                
                if (uiState.dnsResults.isNotEmpty()) {
                    DnsResultsSection(uiState.dnsResults, currentTheme, scale)
                    
                    // Add a button to open DNS settings
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_VPN_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to main settings
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                            }
                        },
                        modifier = Modifier.padding(16.dp * scale).fillMaxWidth(),
                        border = BorderStroke(1.dp, currentTheme.accentColor),
                        shape = RoundedCornerShape(8.dp * scale)
                    ) {
                        Text("OPEN PRIVATE DNS SETTINGS", color = currentTheme.accentColor, style = MaterialTheme.typography.labelLarge)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp * scale))
            }
        }
    }
}

@Composable
fun InfoDashboard(info: NetworkInfo, theme: ThemePreset, scale: Float) {
    Card(
        modifier = Modifier.padding(16.dp * scale).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.endColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp * scale),
        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp * scale)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NetworkCheck, null, tint = theme.accentColor)
                Spacer(modifier = Modifier.width(8.dp * scale))
                Text("NETWORK STATUS", color = theme.textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.weight(1f))
                if (info.isCaptive) {
                    StatusBadge("CAPTIVE", Color.Yellow, scale)
                    Spacer(modifier = Modifier.width(4.dp * scale))
                }
                StatusBadge(if (info.isValidated) "ONLINE" else "OFFLINE", if (info.isValidated) Color(0xFF00FF99) else Color.Red, scale)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp * scale), color = Color.White.copy(alpha = 0.1f))
            
            InfoRow("Local IP", info.localIp)
            InfoRow("Subnet Mask", info.subnetMask)
            InfoRow("External IP", info.externalIp)
            InfoRow("Location", info.location)
            InfoRow("Gateway", info.gateway)
            InfoRow("DNS Servers", info.dnsServers.joinToString(", "))
            
            Spacer(modifier = Modifier.height(16.dp * scale))

            if (info.networkType == "WiFi") {
                WifiDetails(info, theme.accentColor, scale)
            } else if (info.networkType == "Cellular") {
                CellularDetails(info, theme.accentColor, scale)
            }
            
            Spacer(modifier = Modifier.height(8.dp * scale))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("Type", info.networkType, Icons.Default.NetworkCell, theme.accentColor, scale)
                MiniStat("VPN", if (info.isVpn) "YES" else "NO", Icons.Default.Security, theme.accentColor, scale)
                MiniStat("Data", if (info.mobileDataEnabled) "ON" else "OFF", Icons.Default.DataUsage, theme.accentColor, scale)
            }
        }
    }
}

@Composable
fun WifiDetails(info: NetworkInfo, accentColor: Color, scale: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("RSSI", "${info.rssi} dBm", Icons.Default.SignalWifi4Bar, accentColor, scale)
            MiniStat("Link Speed", "${info.linkSpeed} Mbps", Icons.Default.Speed, accentColor, scale)
            MiniStat("Band", "${info.frequency} MHz", Icons.Default.Wifi, accentColor, scale)
        }
        Spacer(modifier = Modifier.height(8.dp * scale))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("Channel", info.channel.toString(), Icons.Default.WifiTethering, accentColor, scale)
            MiniStat("BSSID", info.bssid, Icons.Default.LocationOn, accentColor, scale)
            MiniStat("SSID", info.ssid, Icons.Default.Info, accentColor, scale)
        }
    }
}

@Composable
fun CellularDetails(info: NetworkInfo, accentColor: Color, scale: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("Signal", "${info.cellDbm ?: "N/A"} dBm", Icons.Default.SignalCellularAlt, accentColor, scale)
            MiniStat("Carrier", info.carrierName, Icons.Default.Business, accentColor, scale)
            MiniStat("Network", info.cellNetworkType, Icons.Default.SettingsCell, accentColor, scale)
        }
        Spacer(modifier = Modifier.height(8.dp * scale))
        InfoRow("Roaming", if (info.isRoaming) "Yes" else "No")
    }
}

@Composable
fun QualityMetricsSection(metrics: QualityMetrics, theme: ThemePreset, scale: Float) {
    Text(
        "QUALITY METRICS",
        color = theme.accentColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp * scale, bottom = 8.dp * scale)
    )
    
    Column(modifier = Modifier.padding(horizontal = 16.dp * scale)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard("Latency", "${metrics.latencyAvg}ms", "Jitter: ${metrics.jitter}ms", theme, scale, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp * scale))
            MetricCard("Packet Loss", "${metrics.packetLoss}%", "DNS Res: ${metrics.dnsResolutionTime}ms", theme, scale, Modifier.weight(1f))
        }
        
        if (metrics.latencyHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp * scale))
            LatencyGraph(metrics.latencyHistory, theme.accentColor, scale)
        }
    }
}

@Composable
fun LatencyGraph(history: List<Long>, accentColor: Color, scale: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().height(60.dp * scale),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp * scale)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp * scale, vertical = 8.dp * scale),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val max = history.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            history.takeLast(20).forEach { latency ->
                val heightPercent = (latency.toFloat() / max).coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .width(4.dp * scale)
                        .fillMaxHeight(heightPercent)
                        .background(if (latency > 100) Color.Red else Color(0xFF00FF99), RoundedCornerShape(2.dp * scale))
                )
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, subValue: String, theme: ThemePreset, scale: Float, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(8.dp * scale)
    ) {
        Column(modifier = Modifier.padding(12.dp * scale)) {
            Text(title, color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
            Text(value, color = theme.textColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subValue, color = theme.accentColor, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun OptimizationSection(isOptimizing: Boolean, log: List<String>, accentColor: Color, scale: Float, onOptimize: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp * scale)) {
        Button(
            onClick = onOptimize,
            modifier = Modifier.fillMaxWidth().height(56.dp * scale),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(8.dp * scale),
            enabled = !isOptimizing
        ) {
            if (isOptimizing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp * scale), color = Color.Black)
            } else {
                Text("START NETWORK OPTIMIZATION", color = Color.Black, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        
        if (log.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp * scale))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp * scale)
            ) {
                Column(modifier = Modifier.padding(12.dp * scale)) {
                    log.forEach { line ->
                        Text("> $line", color = Color(0xFF00FF99), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun DnsResultsSection(results: List<DnsResult>, theme: ThemePreset, scale: Float) {
    Text(
        "DNS BENCHMARK RESULTS",
        color = theme.accentColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp * scale, bottom = 8.dp * scale, top = 16.dp * scale)
    )
    
    Column(modifier = Modifier.padding(horizontal = 16.dp * scale)) {
        results.sortedBy { it.latency }.forEach { res ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp * scale)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp * scale))
                    .padding(8.dp * scale),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(res.name, color = theme.textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text(res.ip, color = theme.subtitleColor, style = MaterialTheme.typography.labelSmall)
                }
                Text("${res.latency}ms", color = if (res.isSuccess) Color(0xFF00FF99) else Color.Red, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MiniStat(label: String, value: String, icon: ImageVector, accentColor: Color, scale: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp * scale))
        Spacer(modifier = Modifier.width(4.dp * scale))
        Column {
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontSize = (9 * scale).sp)
            Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, scale: Float) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp * scale))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp * scale))
            .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
