package com.semseytech.rtsdevicesuitepro.net.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.storage.analyzer.DeepDark
import com.semseytech.rtsdevicesuitepro.storage.analyzer.NeonBlue
import com.semseytech.rtsdevicesuitepro.storage.analyzer.GradientEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationControlsScreen(
    viewModel: NetworkAutomationViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var infoTitle by remember { mutableStateOf("") }
    var infoContent by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AUTOMATION CONTROLS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDark, titleContentColor = Color.White)
            )
        },
        containerColor = DeepDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                AutomationToggleRow(
                    title = "Auto DNS Benchmark",
                    checked = settings.autoDnsBenchmark,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_DNS_BENCHMARK, it) },
                    onInfoClick = {
                        infoTitle = "Auto DNS Benchmark"
                        infoContent = "Automatically checks which DNS provider is fastest for you and notifies you if switching could speed up your connection."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto DNS Refresh",
                    checked = settings.autoDnsRefresh,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_DNS_REFRESH, it) },
                    onInfoClick = {
                        infoTitle = "Auto DNS Refresh"
                        infoContent = "Refreshes your DNS in the background to fix slow loading, stuck apps, or websites that won’t open."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto Socket Flush",
                    checked = settings.autoSocketFlush,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_SOCKET_FLUSH, it) },
                    onInfoClick = {
                        infoTitle = "Auto Socket Flush"
                        infoContent = "Clears old or stuck network connections so apps can reconnect faster and more reliably."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto Network Rebind",
                    checked = settings.autoNetworkRebind,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_NETWORK_REBIND, it) },
                    onInfoClick = {
                        infoTitle = "Auto Network Rebind"
                        infoContent = "Rebuilds your device’s routing paths to fix slow or unstable connections."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto WiFi Quality Monitor",
                    checked = settings.autoWifiQualityMonitor,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_WIFI_QUALITY_MONITOR, it) },
                    onInfoClick = {
                        infoTitle = "Auto WiFi Quality Monitor"
                        infoContent = "Watches your WiFi signal and warns you if it becomes weak, crowded, or unstable."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto Latency Monitor",
                    checked = settings.autoLatencyMonitor,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_LATENCY_MONITOR, it) },
                    onInfoClick = {
                        infoTitle = "Auto Latency Monitor"
                        infoContent = "Checks your connection speed and stability in the background and alerts you if things get slow."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto Captive Portal Detection",
                    checked = settings.autoCaptivePortalDetection,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_CAPTIVE_PORTAL_DETECTION, it) },
                    onInfoClick = {
                        infoTitle = "Auto Captive Portal Detection"
                        infoContent = "Detects hotel, airport, or café WiFi that requires a login and alerts you instantly."
                        showBottomSheet = true
                    }
                )
                Divider(color = Color.White.copy(alpha = 0.1f))
            }
            item {
                AutomationToggleRow(
                    title = "Auto WiFi Reset (Safe)",
                    checked = settings.autoWifiReset,
                    onCheckedChange = { viewModel.toggleSetting(NetworkAutomationRepository.Keys.AUTO_WIFI_RESET, it) },
                    onInfoClick = {
                        infoTitle = "Auto WiFi Reset (Safe)"
                        infoContent = "Turns WiFi off and back on when your connection gets stuck, helping it reconnect cleanly."
                        showBottomSheet = true
                    }
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = GradientEnd
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = infoTitle,
                    color = NeonBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = infoContent,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("UNDERSTOOD", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AutomationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = NeonBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonBlue,
                checkedTrackColor = NeonBlue.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}
