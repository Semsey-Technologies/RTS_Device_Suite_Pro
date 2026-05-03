package com.semseytech.rtsdevicesuitepro.adb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun AdbSetupScreen(
    viewModel: AdbViewModel = viewModel(),
    onNavigateToConsole: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    // Permission state
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Use a Side-Effect to re-check permissions whenever the screen is resumed
    var permissionsGranted by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                permissionsGranted = requiredPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                android.util.Log.d("AdbSetup", "Re-checked permissions on resume: $permissionsGranted")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        permissionsGranted = allGranted
        android.util.Log.d("AdbSetup", "Permission result: $result - allGranted: $allGranted")
        if (allGranted) {
            viewModel.toggleAdb(true)
        }
    }

    var showDisclaimer by remember { mutableStateOf(!uiState.isEnabled) }
    var pairingPort by remember { mutableStateOf("") }
    var pairingHost by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var adbPort by remember { mutableStateOf("") }
    
    // Debug log to trace what's happening
    LaunchedEffect(permissionsGranted) {
        android.util.Log.d("AdbSetup", "Permissions granted status: $permissionsGranted")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... header content ...
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Local ADB Client",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Status: ${uiState.connectionStatus}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (uiState.connectionStatus == "Connected") Color.Green else Color.Red
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (showDisclaimer) {
            SafetyDisclaimer(onAccept = {
                showDisclaimer = false
                if (!permissionsGranted) {
                    android.util.Log.d("AdbSetup", "Launching permissions from disclaimer")
                    launcher.launch(requiredPermissions)
                } else {
                    viewModel.toggleAdb(true)
                }
            })
        } else if (!permissionsGranted) {
            val shouldShowRationale = requiredPermissions.any {
                activity?.let { act -> androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(act, it) } ?: false
            }
            
            PermissionRationaleCard(
                shouldShowSettingsLink = !shouldShowRationale,
                onGrant = { 
                    android.util.Log.d("AdbSetup", "Grant button tapped. Rationale shown: $shouldShowRationale")
                    launcher.launch(requiredPermissions) 
                },
                onOpenSettings = {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        } else {
            // Re-check if ADB is enabled when we have permissions and no disclaimer
            LaunchedEffect(Unit) {
                if (!uiState.isEnabled) viewModel.toggleAdb(true)
            }

            DiscoveryPanel(
                services = uiState.discoveredServices,
                onRefresh = { viewModel.toggleAdb(true) },
                onConnect = { service -> 
                    if (service.type.contains("pairing")) {
                        pairingPort = service.port.toString()
                        pairingHost = service.host
                    } else {
                        adbPort = service.port.toString()
                        viewModel.connect(service.port.toString(), service.host)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            PairingPanel(
                pairingPort = pairingPort,
                pairingCode = pairingCode,
                onPortChange = { pairingPort = it },
                onCodeChange = { pairingCode = it },
                onPair = { port, code -> viewModel.pair(port, code, pairingHost) },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConnectionPanel(
                portText = adbPort,
                onPortChange = { adbPort = it },
                onConnect = { viewModel.connect(adbPort) },
                isLoading = uiState.isLoading
            )
            
            if (uiState.connectionStatus == "Connected") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToConsole,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open ADB Console")
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleCard(
    shouldShowSettingsLink: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permission Required", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "To automatically find your device's ADB service, the app needs permission to scan nearby Wi-Fi devices. " +
                "\n\nWithout this, you will have to find and enter the Port numbers manually every time they change."
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (shouldShowSettingsLink) {
                Text(
                    "It looks like permissions were permanently denied. Please enable 'Nearby devices' in app settings to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open App Settings")
                }
            } else {
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun SafetyDisclaimer(onAccept: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Safety Disclaimer", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enabling Local ADB allows this app to execute system-level commands. " +
                "This is used for advanced maintenance tasks like cache clearing and permission management. " +
                "\n\n- No root required.\n- You can revoke access at any time.\n- All commands are logged."
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("I Understand, Enable Local ADB")
            }
        }
    }
}

@Composable
fun DiscoveryPanel(
    services: List<com.semseytech.rtsdevicesuitepro.adb.core.AdbService>,
    onRefresh: () -> Unit,
    onConnect: (com.semseytech.rtsdevicesuitepro.adb.core.AdbService) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-Discovered Services", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (services.isEmpty()) {
                Text(
                    "Searching for local ADB services... Ensure Wireless Debugging is ON.\n\n" +
                    "To pair: Tap 'Pair device with pairing code' to make the pairing service visible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            } else {
                services.forEach { service ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val typeLabel = if (service.type.contains("pairing")) "Pairing Service" else "ADB Service"
                            Text(typeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("${service.host}:${service.port}", style = MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick = { onConnect(service) }) {
                            Text(if (service.type.contains("pairing")) "Auto-Fill" else "Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PairingPanel(
    pairingPort: String,
    pairingCode: String,
    onPortChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPair: (String, String) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Step 1: Initial Pairing", fontWeight = FontWeight.Bold)
            Text(
                "Tap 'Pair device with pairing code' in Wireless Debugging settings.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pairingPort,
                    onValueChange = onPortChange,
                    label = { Text("Pairing Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = onCodeChange,
                    label = { Text("Pairing Code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onPair(pairingPort, pairingCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && pairingPort.isNotBlank() && pairingCode.isNotBlank()
            ) {
                Text("Pair Device")
            }
        }
    }
}

@Composable
fun ConnectionPanel(
    portText: String,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    isLoading: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Wireless Debugging Setup", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "1. Open Settings > Developer Options\n" +
                "2. Enable 'Wireless Debugging'\n" +
                "3. Tap on 'Wireless Debugging' to see the IP and Port\n" +
                "4. Enter the PORT below (NOT the pairing port)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = portText,
                onValueChange = onPortChange,
                label = { Text("ADB Port (e.g. 38445)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Look for IP:Port in settings") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && portText.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Connect to Local ADB")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Note: If this is the first time, you may need to 'Always allow' when the authorization prompt appears on your screen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
