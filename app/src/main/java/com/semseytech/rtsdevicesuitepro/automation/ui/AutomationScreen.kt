package com.semseytech.rtsdevicesuitepro.automation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionRegistry
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionPrefs
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionRationaleDialog
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.automation.data.RuleEntity
import com.semseytech.rtsdevicesuitepro.automation.data.RuleGroup
import com.semseytech.rtsdevicesuitepro.automation.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AutomationViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val flows by viewModel.flows.collectAsState()
    val runningRules by viewModel.runningRules.collectAsState()
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<RuleEntity?>(null) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val permissionPrefs = remember { PermissionPrefs(context) }
    var permissionsToRequest by remember { mutableStateOf<List<String>>(emptyList()) }
    var showRationale by remember { mutableStateOf(false) }
    var onPermissionsGranted by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            val missing = pendingPermissions.filter {
                !PermissionUtils.isPermissionGranted(context, it)
            }
            if (missing.isEmpty()) {
                onPermissionsGranted?.invoke()
                onPermissionsGranted = null
                pendingPermissions = emptyList()
            }
        } else {
            onPermissionsGranted = null
            pendingPermissions = emptyList()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPermissionsGranted?.let { callback ->
                    val missing = pendingPermissions.filter {
                        !PermissionUtils.isPermissionGranted(context, it)
                    }
                    if (missing.isEmpty()) {
                        callback()
                        onPermissionsGranted = null
                        pendingPermissions = emptyList()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun checkAndRequestAll(trigger: Trigger, conditions: List<Condition>, actions: List<Action>, onGranted: () -> Unit) {
        val allPermissions = (listOf(trigger) + conditions + actions).flatMap { it.requiredPermissions }.distinct()
        val missing = allPermissions.filter { !PermissionUtils.isPermissionGranted(context, it) }
        
        if (missing.isEmpty()) {
            onGranted()
        } else {
            val unshownPermissions = missing.filter { !permissionPrefs.isExplanationShown(it) }
            pendingPermissions = missing
            onPermissionsGranted = onGranted
            if (unshownPermissions.isNotEmpty()) {
                permissionsToRequest = missing
                showRationale = true
            } else {
                PermissionUtils.requestPermissions(context, missing, permissionLauncher)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Modular Automation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddRuleDialog = true }) {
                        Icon(Icons.Default.AddBox, "Standard Rule", tint = Color.White)
                    }
                    IconButton(onClick = { showAddGroupDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, "Add Group", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigate(com.semseytech.rtsdevicesuitepro.navigation.Screen.FlowEditor.route) },
                containerColor = Color(0xFF00E5FF)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Grouped Rules
            groups.forEach { group ->
                item(key = group.id) {
                    GroupHeader(
                        group = group,
                        onDelete = { viewModel.deleteGroup(group) }
                    )
                }
                val groupRules = rules.filter { it.groupId == group.id }
                items(groupRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        groups = groups,
                        isRunning = runningRules.contains(rule.id),
                        onToggle = { enabled ->
                            if (enabled) {
                                checkAndRequestAll(rule.trigger, rule.conditions, rule.actions) {
                                    viewModel.toggleRule(rule.id, true)
                                }
                            } else {
                                viewModel.toggleRule(rule.id, false)
                            }
                        },
                        onEdit = { 
                            android.util.Log.d("AutomationScreen", "Editing rule: ${rule.name}")
                            ruleToEdit = rule 
                        },
                        onDelete = { viewModel.deleteRule(rule) },
                        onMove = { viewModel.moveRuleToGroup(rule.id, it) },
                        onRun = { 
                            checkAndRequestAll(rule.trigger, rule.conditions, rule.actions) {
                                viewModel.runRule(rule)
                            }
                        },
                        onStop = { viewModel.stopRule(rule.id) }
                    )
                }
            }

            // Ungrouped Rules
            val ungroupedRules = rules.filter { it.groupId == null }
            if (ungroupedRules.isNotEmpty() || flows.isNotEmpty()) {
                item {
                    Text(
                        "Ungrouped & Flows",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(ungroupedRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        groups = groups,
                        isRunning = runningRules.contains(rule.id),
                        onToggle = { enabled ->
                            if (enabled) {
                                checkAndRequestAll(rule.trigger, rule.conditions, rule.actions) {
                                    viewModel.toggleRule(rule.id, true)
                                }
                            } else {
                                viewModel.toggleRule(rule.id, false)
                            }
                        },
                        onEdit = { 
                            android.util.Log.d("AutomationScreen", "Editing ungrouped rule: ${rule.name}")
                            ruleToEdit = rule 
                        },
                        onDelete = { viewModel.deleteRule(rule) },
                        onMove = { viewModel.moveRuleToGroup(rule.id, it) },
                        onRun = { 
                            checkAndRequestAll(rule.trigger, rule.conditions, rule.actions) {
                                viewModel.runRule(rule)
                            }
                        },
                        onStop = { viewModel.stopRule(rule.id) }
                    )
                }
                items(flows, key = { it.id }) { flow ->
                    FlowCard(
                        flow = flow,
                        onDelete = { viewModel.deleteFlow(flow) },
                        onEdit = { 
                            android.util.Log.d("AutomationScreen", "Editing flow: ${flow.name}")
                            onNavigate(com.semseytech.rtsdevicesuitepro.navigation.Screen.FlowEditor.route + "/${flow.id}") 
                        }
                    )
                }
            }
        }

        if (showAddGroupDialog) {
            AddGroupDialog(
                onDismiss = { showAddGroupDialog = false },
                onConfirm = { name ->
                    viewModel.addGroup(name)
                    showAddGroupDialog = false
                }
            )
        }

        if (showAddRuleDialog) {
            RuleEditorDialog(
                groups = groups,
                onDismiss = { showAddRuleDialog = false },
                onConfirm = { name, trigger, conditions, actions, groupId ->
                    viewModel.addRule(name, trigger, conditions, actions, groupId)
                    showAddRuleDialog = false
                }
            )
        }

        if (ruleToEdit != null) {
            RuleEditorDialog(
                rule = ruleToEdit,
                groups = groups,
                onDismiss = { ruleToEdit = null },
                onConfirm = { name, trigger, conditions, actions, groupId ->
                    val updatedRule = ruleToEdit!!.copy(
                        name = name,
                        trigger = trigger,
                        conditions = conditions,
                        actions = actions,
                        groupId = groupId
                    )
                    viewModel.updateRule(updatedRule)
                    ruleToEdit = null
                }
            )
        }

        if (showRationale) {
            PermissionRationaleDialog(
                permissions = permissionsToRequest,
                onConfirm = {
                    showRationale = false
                    permissionsToRequest.forEach { permissionPrefs.markExplanationShown(it) }
                    PermissionUtils.requestPermissions(context, permissionsToRequest, permissionLauncher)
                },
                onDismiss = {
                    showRationale = false
                    onPermissionsGranted = null
                },
                onNavigateToHelp = {
                    showRationale = false
                    onPermissionsGranted = null
                }
            )
        }
    }
}

@Composable
fun GroupHeader(group: RuleGroup, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete Group", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RuleCard(
    rule: RuleEntity,
    groups: List<RuleGroup>,
    isRunning: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (String?) -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRunning) {
                        IconButton(onClick = onStop) {
                            Icon(Icons.Default.Stop, "Stop Rule", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = onRun) {
                            Icon(Icons.Default.PlayArrow, "Run Rule", tint = Color(0xFF00FF99))
                        }
                    }

                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                    
                    Box {
                        IconButton(onClick = { showMoveMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Gray)
                        }
                        DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMoveMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to...") },
                                onClick = { /* Nested menu not easy here, just show list */ },
                                enabled = false
                            )
                            groups.filter { it.id != rule.groupId }.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        onMove(group.id)
                                        showMoveMenu = false
                                    }
                                )
                            }
                            if (rule.groupId != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove from Group") },
                                    onClick = {
                                        onMove(null)
                                        showMoveMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDelete()
                                    showMoveMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Trigger: ${rule.trigger.displayName}", color = Color(0xFF00E5FF), fontSize = 14.sp)
            if (rule.conditions.isNotEmpty()) {
                Text(text = "Conditions: ${rule.conditions.joinToString { it.displayName }}", color = Color.LightGray, fontSize = 12.sp)
            }
            Text(text = "Actions: ${rule.actions.joinToString { it.displayName }}", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun FlowCard(
    flow: com.semseytech.rtsdevicesuitepro.automation.data.FlowGraphEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AccountTree, null, tint = Color.Cyan)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(flow.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Flow Graph", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AddGroupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Group", color = Color.White) },
        containerColor = Color(0xFF1E1E1E),
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RuleEditorDialog(
    rule: RuleEntity? = null,
    groups: List<RuleGroup>,
    onDismiss: () -> Unit,
    onConfirm: (String, Trigger, List<Condition>, List<Action>, String?) -> Unit
) {
    var name by remember(rule) { mutableStateOf(rule?.name ?: "") }
    var selectedTrigger by remember(rule) { mutableStateOf<Trigger>(rule?.trigger ?: Trigger.WiFiConnected) }
    val selectedConditions = remember(rule) { 
        mutableStateListOf<Condition>().apply { 
            rule?.conditions?.let { addAll(it) } 
        } 
    }
    val selectedActions = remember(rule) { 
        mutableStateListOf<Action>().apply { 
            rule?.actions?.let { addAll(it) } 
        } 
    }
    var selectedGroupId by remember(rule) { mutableStateOf<String?>(rule?.groupId) }
    
    var editingComponent by remember(rule) { mutableStateOf<AutomationComponent?>(null) }

    val context = LocalContext.current
    val permissionPrefs = remember { PermissionPrefs(context) }
    var permissionsToRequest by remember { mutableStateOf<List<String>>(emptyList()) }
    var showRationale by remember { mutableStateOf(false) }
    var onPermissionsGranted by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingComponent by remember { mutableStateOf<AutomationComponent?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            // Check if there are any special permissions still missing
            val missing = pendingComponent?.requiredPermissions?.filter {
                !PermissionUtils.isPermissionGranted(context, it)
            } ?: emptyList()
            
            if (missing.isEmpty()) {
                onPermissionsGranted?.invoke()
                onPermissionsGranted = null
                pendingComponent = null
            }
        } else {
            onPermissionsGranted = null
            pendingComponent = null
        }
    }

    // Re-check when returning to app (for special permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPermissionsGranted?.let { callback ->
                    val missing = pendingComponent?.requiredPermissions?.filter {
                        !PermissionUtils.isPermissionGranted(context, it)
                    } ?: emptyList()
                    
                    if (missing.isEmpty()) {
                        callback()
                        onPermissionsGranted = null
                        pendingComponent = null
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun checkAndRequest(component: AutomationComponent, onGranted: () -> Unit) {
        val missing = component.requiredPermissions.filter {
            !PermissionUtils.isPermissionGranted(context, it)
        }
        if (missing.isEmpty()) {
            onGranted()
        } else {
            val unshownPermissions = missing.filter { !permissionPrefs.isExplanationShown(it) }
            pendingComponent = component
            onPermissionsGranted = onGranted
            if (unshownPermissions.isNotEmpty()) {
                permissionsToRequest = missing
                showRationale = true
            } else {
                PermissionUtils.requestPermissions(context, missing, permissionLauncher)
            }
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            permissions = permissionsToRequest,
            onConfirm = {
                showRationale = false
                permissionsToRequest.forEach { permissionPrefs.markExplanationShown(it) }
                PermissionUtils.requestPermissions(context, permissionsToRequest, permissionLauncher)
            },
            onDismiss = {
                showRationale = false
                onPermissionsGranted = null
            },
            onNavigateToHelp = {
                showRationale = false
                onPermissionsGranted = null
                onDismiss()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "Build New Rule" else "Edit Rule", color = Color.White) },
        containerColor = Color(0xFF1E1E1E),
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF)
                    )
                )

                SectionHeader("0. Select Group (Optional)")
                var groupExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { groupExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = groups.find { it.id == selectedGroupId }?.name ?: "No Group",
                            color = Color.White
                        )
                    }
                    DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("No Group") },
                            onClick = {
                                selectedGroupId = null
                                groupExpanded = false
                            }
                        )
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    selectedGroupId = group.id
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }
                
                SectionHeader("1. Select Trigger")
                val triggerOptions = listOf(
                    Trigger.WiFiConnected,
                    Trigger.WiFiDisconnected,
                    Trigger.SpecificWiFiConnected(),
                    Trigger.WiFiSignalStrength(),
                    Trigger.MobileDataOn,
                    Trigger.MobileDataOff,
                    Trigger.MobileDataTypeChanged(),
                    Trigger.HotspotOn,
                    Trigger.HotspotOff,
                    Trigger.VpnConnected,
                    Trigger.VpnDisconnected,
                    Trigger.IpAddressChanged,
                    Trigger.NetworkAvailable,
                    Trigger.NetworkLost,
                    Trigger.BluetoothOn,
                    Trigger.BluetoothOff,
                    Trigger.BluetoothDeviceConnected(),
                    Trigger.BluetoothDeviceDisconnected(),
                    Trigger.PingStatus(),
                    Trigger.DomainReachability(),
                    Trigger.NetworkSpeedThreshold(),
                    Trigger.PowerConnected,
                    Trigger.PowerDisconnected,
                    Trigger.BatteryLevelAbove(),
                    Trigger.BatteryLevelBelow(),
                    Trigger.ScreenOn,
                    Trigger.ScreenOff,
                    Trigger.ScreenUnlocked,
                    Trigger.BootCompleted,
                    Trigger.TimeOfDay(),
                    Trigger.TimeRange(),
                    Trigger.DaysOfWeek(),
                    Trigger.DayOfMonth(),
                    Trigger.MonthOfYear(),
                    Trigger.RecurringTrigger(),
                    Trigger.IntervalTrigger(),
                    Trigger.Sunrise,
                    Trigger.Sunset,
                    Trigger.GoldenHour(),
                    Trigger.TimerFinished(),
                    Trigger.StopwatchThreshold(),
                    Trigger.CountdownReached(),
                    Trigger.CronSchedule(),
                    Trigger.AppOpened(),
                    Trigger.AppClosed(),
                    Trigger.AppInstalled(),
                    Trigger.AppUninstalled(),
                    Trigger.AppUpdated(),
                    Trigger.AppCrashed(),
                    Trigger.ForegroundAppChanged,
                    Trigger.NotificationPosted(),
                    Trigger.NotificationRemoved(),
                    Trigger.NotificationMatches(),
                    Trigger.SystemDialogOpened,
                    Trigger.KeyboardStateChanged(),
                    Trigger.AccessibilityEventDetected(),
                    Trigger.ToastDetected(),
                    Trigger.OverlayPermissionChanged(),
                    Trigger.FileCreated(),
                    Trigger.FileDeleted(),
                    Trigger.FileModified(),
                    Trigger.FolderChanged(),
                    Trigger.ExternalStorageMounted,
                    Trigger.ExternalStorageUnmounted,
                    Trigger.DownloadCompleted,
                    Trigger.ScreenshotTaken,
                    Trigger.StorageSizeTrigger(),
                    Trigger.LightSensorThreshold(),
                    Trigger.MagnetometerThreshold(),
                    Trigger.BarometerThreshold(),
                    Trigger.TemperatureThreshold(),
                    Trigger.HumidityThreshold(),
                    Trigger.HeartRateThreshold(),
                    Trigger.AmbientNoiseThreshold(),
                    Trigger.StepCountThreshold(),
                    Trigger.ProximityTriggered(),
                    Trigger.AccelerometerPattern(),
                    Trigger.GyroscopePattern(),
                    Trigger.TouchGesturePattern()
                )
                ComponentSelector(
                    current = selectedTrigger,
                    options = triggerOptions,
                    onSelected = { checkAndRequest(it) { selectedTrigger = it as Trigger } },
                    onEdit = { editingComponent = it }
                )
                
                SectionHeader("2. Add Conditions")
                selectedConditions.forEachIndexed { index, condition ->
                    ComponentRow(
                        component = condition,
                        onEdit = { editingComponent = condition },
                        onRemove = { selectedConditions.removeAt(index) }
                    )
                }
                var showConditionOptions by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showConditionOptions = true }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Condition")
                    }
                    DropdownMenu(expanded = showConditionOptions, onDismissRequest = { showConditionOptions = false }) {
                        val conditionOptions = listOf(
                            Condition.IsConnectedToWiFi,
                            Condition.WiFiSSIDIs(),
                            Condition.BatteryLevelBetween(),
                            Condition.IsCharging,
                            Condition.ScreenIsOn,
                            Condition.DeviceIsLocked,
                            Condition.StorageFreeAbove()
                        )
                        conditionOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName) },
                                onClick = {
                                    checkAndRequest(opt) {
                                        selectedConditions.add(opt)
                                        showConditionOptions = false
                                    }
                                }
                            )
                        }
                    }
                }

                SectionHeader("3. Add Actions")
                selectedActions.forEachIndexed { index, action ->
                    ComponentRow(
                        component = action,
                        onEdit = { editingComponent = action },
                        onRemove = { selectedActions.removeAt(index) }
                    )
                }
                var showActionOptions by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showActionOptions = true }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Action")
                    }
                    DropdownMenu(expanded = showActionOptions, onDismissRequest = { showActionOptions = false }) {
                        val actionOptions = listOf(
                            Action.RunDNSBenchmark,
                            Action.RefreshDNS,
                            Action.ToggleWiFi,
                            Action.SetVolume(),
                            Action.Speak(),
                            Action.Vibrate(),
                            Action.LaunchApp(),
                            Action.ShowNotification(),
                            Action.ShowToast(),
                            Action.AutoClean(),
                            Action.RunBackup(),
                            Action.RunRestore(),
                            Action.ToggleFlashlight,
                            Action.SetBrightness(),
                            Action.Delay(),
                            Action.RunAdbCommand()
                        )
                        actionOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName) },
                                onClick = {
                                    checkAndRequest(opt) {
                                        selectedActions.add(opt)
                                        showActionOptions = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedTrigger, selectedConditions.toList(), selectedActions.toList(), selectedGroupId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text(if (rule == null) "Create Rule" else "Save Changes", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )

    if (editingComponent != null) {
        ParameterPromptDialog(
            component = editingComponent!!,
            onDismiss = { editingComponent = null },
            onSave = { updatedComponent ->
                when (updatedComponent) {
                    is Trigger -> selectedTrigger = updatedComponent
                    is Condition -> {
                        val index = selectedConditions.indexOfFirst { it.type == updatedComponent.type }
                        if (index != -1) selectedConditions[index] = updatedComponent
                    }
                    is Action -> {
                        val index = selectedActions.indexOfFirst { it.type == updatedComponent.type }
                        if (index != -1) selectedActions[index] = updatedComponent
                    }
                }
                editingComponent = null
            }
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
fun ComponentSelector(
    current: AutomationComponent,
    options: List<AutomationComponent>,
    onSelected: (AutomationComponent) -> Unit,
    onEdit: (AutomationComponent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val groupedOptions = remember(options) { options.groupBy { it.category } }
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = getIconForName(current.icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(current.displayName, color = Color.White)
                }
                DropdownMenu(
                    expanded = expanded, 
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF252525)).fillMaxWidth(0.8f).heightIn(max = 400.dp)
                ) {
                    groupedOptions.forEach { (category, components) ->
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                        components.forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getIconForName(opt.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(opt.displayName)
                                    }
                                },
                                onClick = {
                                    onSelected(opt)
                                    expanded = false
                                }
                            )
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                    }
                }
            }
            if (current.parameters.isNotEmpty()) {
                IconButton(onClick = { onEdit(current) }) {
                    Icon(Icons.Default.Settings, "Configure", tint = Color(0xFF00E5FF))
                }
            }
        }
    }
}

@Composable
fun getIconForName(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "wifi" -> Icons.Default.Wifi
        "wifi_off" -> Icons.Default.WifiOff
        "network_wifi" -> Icons.Default.NetworkWifi
        "signal_wifi_4_bar" -> Icons.Default.SignalWifi4Bar
        "signal_cellular_4_bar" -> Icons.Default.SignalCellular4Bar
        "signal_cellular_connected_no_internet_4_bar" -> Icons.Default.SignalCellularConnectedNoInternet4Bar
        "network_check" -> Icons.Default.NetworkCheck
        "wifi_tethering" -> Icons.Default.WifiTethering
        "wifi_tethering_off" -> Icons.Default.WifiTetheringOff
        "vpn_lock" -> Icons.Default.VpnLock
        "lan" -> Icons.Default.Lan
        "public" -> Icons.Default.Public
        "public_off" -> Icons.Default.PublicOff
        "bluetooth" -> Icons.Default.Bluetooth
        "bluetooth_disabled" -> Icons.Default.BluetoothDisabled
        "bluetooth_connected" -> Icons.Default.BluetoothConnected
        "settings_ethernet" -> Icons.Default.SettingsEthernet
        "language" -> Icons.Default.Language
        "speed" -> Icons.Default.Speed
        "power" -> Icons.Default.Power
        "power_off" -> Icons.Default.PowerOff
        "battery_full" -> Icons.Default.BatteryFull
        "battery_alert" -> Icons.Default.BatteryAlert
        "smartphone" -> Icons.Default.Smartphone
        "screen_lock_portrait" -> Icons.Default.ScreenLockPortrait
        "lock_open" -> Icons.Default.LockOpen
        "restart_alt" -> Icons.Default.RestartAlt
        "schedule" -> Icons.Default.Schedule
        "apps" -> Icons.Default.Apps
        "close" -> Icons.Default.Close
        "install_mobile" -> Icons.Default.InstallMobile
        "delete_sweep" -> Icons.Default.DeleteSweep
        "update" -> Icons.Default.Update
        "error" -> Icons.Default.Error
        "api" -> Icons.Default.Api
        "notifications_active" -> Icons.Default.NotificationsActive
        "notifications_off" -> Icons.Default.NotificationsOff
        "notification_important" -> Icons.Default.NotificationImportant
        "picture_in_picture" -> Icons.Default.PictureInPicture
        "keyboard" -> Icons.Default.Keyboard
        "accessibility" -> Icons.Default.Accessibility
        "announcement" -> Icons.AutoMirrored.Filled.Announcement
        "layers" -> Icons.Default.Layers
        "date_range" -> Icons.Default.DateRange
        "calendar_today" -> Icons.Default.CalendarToday
        "calendar_view_month" -> Icons.Default.CalendarViewMonth
        "wb_twilight" -> Icons.Default.WbTwilight
        "timer" -> Icons.Default.Timer
        "timer_10" -> Icons.Default.Timer10
        "hourglass_bottom" -> Icons.Default.HourglassBottom
        "code" -> Icons.Default.Code
        "calendar_month" -> Icons.Default.CalendarMonth
        "event_repeat" -> Icons.Default.EventRepeat
        "wb_sunny" -> Icons.Default.WbSunny
        "nights_stay" -> Icons.Default.NightsStay
        "create_new_folder" -> Icons.Default.CreateNewFolder
        "delete_forever" -> Icons.Default.DeleteForever
        "edit_note" -> Icons.Default.EditNote
        "folder_zip" -> Icons.Default.FolderZip
        "sd_card" -> Icons.Default.SdCard
        "sd_card_alert" -> Icons.Default.SdCardAlert
        "download_done" -> Icons.Default.DownloadDone
        "screenshot" -> Icons.Default.Screenshot
        "storage" -> Icons.Default.Storage
        "cleaning_services" -> Icons.Default.CleaningServices
        "cloud_upload" -> Icons.Default.CloudUpload
        "settings_backup_restore" -> Icons.Default.SettingsBackupRestore
        "brightness_medium" -> Icons.Default.BrightnessMedium
        "explore" -> Icons.Default.Explore
        "compress" -> Icons.Default.Compress
        "thermostat" -> Icons.Default.Thermostat
        "water_drop" -> Icons.Default.WaterDrop
        "favorite" -> Icons.Default.Favorite
        "graphic_eq" -> Icons.Default.GraphicEq
        "touch_app" -> Icons.Default.TouchApp
        "vibration" -> Icons.Default.Vibration
        "sync" -> Icons.Default.Sync
        "visibility" -> Icons.Default.Visibility
        "directions_walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "landscape" -> Icons.Default.Landscape
        else -> Icons.AutoMirrored.Filled.Help
    }
}

@Composable
fun ComponentRow(
    component: AutomationComponent,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(component.displayName, color = Color.White, modifier = Modifier.weight(1f))
        if (component.parameters.isNotEmpty()) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Settings, "Edit", tint = Color.Gray)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, "Remove", tint = Color.Red)
        }
    }
}

@Composable
fun ParameterPromptDialog(
    component: AutomationComponent,
    onDismiss: () -> Unit,
    onSave: (AutomationComponent) -> Unit
) {
    val tempParams = remember { mutableStateMapOf<String, Any?>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(component) {
        component.parameters.forEach { param ->
            tempParams[param.key] = param.value
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${component.displayName}", color = Color.White) },
        containerColor = Color(0xFF252525),
        text = {
            Column {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    component.parameters.forEach { param ->
                        when (param) {
                            is Parameter.TextParameter -> {
                                OutlinedTextField(
                                    value = (tempParams[param.key] as? String) ?: "",
                                    onValueChange = { tempParams[param.key] = it },
                                    label = { Text(param.label) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is Parameter.NumberParameter -> {
                                Column {
                                    Text("${param.label}: ${tempParams[param.key] ?: param.value}", color = Color.White)
                                    Slider(
                                        value = (tempParams[param.key] as? Int ?: param.value).toFloat(),
                                        onValueChange = { tempParams[param.key] = it.toInt() },
                                        valueRange = param.min.toFloat()..param.max.toFloat()
                                    )
                                }
                            }
                            is Parameter.TimeParameter -> {
                                var showPicker by remember { mutableStateOf(false) }
                                val timeStr = (tempParams[param.key] as? String) ?: "12:00"
                                val parts = timeStr.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                
                                val displayTime = remember(hour, minute) {
                                    val h = if (hour % 12 == 0) 12 else hour % 12
                                    val ampm = if (hour < 12) "AM" else "PM"
                                    String.format("%d:%02d %s", h, minute, ampm)
                                }

                                OutlinedButton(
                                    onClick = { showPicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${param.label}: $displayTime")
                                }

                                if (showPicker) {
                                    TimePicker12h(
                                        initialHour = hour,
                                        initialMinute = minute,
                                        onTimeSelected = { h, m ->
                                            tempParams[param.key] = String.format("%02d:%02d", h, m)
                                            showPicker = false
                                        },
                                        onDismiss = { showPicker = false }
                                    )
                                }
                            }
                            is Parameter.SelectionParameter -> {
                                if (param.key == "days") {
                                    var showPicker by remember { mutableStateOf(false) }
                                    val daysStr = (tempParams[param.key] as? String) ?: ""
                                    val selectedDays = daysStr.split(",").filter { it.isNotBlank() }.map { 
                                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").indexOf(it) 
                                    }.filter { it != -1 }

                                    OutlinedButton(
                                        onClick = { showPicker = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${param.label}: ${if (daysStr.isBlank()) "None" else daysStr}")
                                    }

                                    if (showPicker) {
                                        DayOfWeekPicker(
                                            selectedDays = selectedDays,
                                            onDaysSelected = { days ->
                                                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                                tempParams[param.key] = days.sorted().joinToString(",") { dayNames[it] }
                                                showPicker = false
                                            },
                                            onDismiss = { showPicker = false }
                                        )
                                    }
                                } else {
                                    var expanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text("${param.label}: ${tempParams[param.key] ?: param.value}")
                                        }
                                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            param.options.forEach { opt ->
                                                DropdownMenuItem(text = { Text(opt) }, onClick = {
                                                    tempParams[param.key] = opt
                                                    expanded = false
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text("Parameter ${param.label} not yet implemented", color = Color.Red)
                            }
                        }
                    }
                }
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        confirmButton = {
            Button(onClick = {
                val updated = updateComponent(component, tempParams)
                onSave(updated)
                scope.launch {
                    snackbarHostState.showSnackbar("Configuration saved")
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun updateComponent(component: AutomationComponent, params: Map<String, Any?>): AutomationComponent {
    return when (component) {
        is Trigger.SpecificWiFiConnected -> component.copy(ssid = params["ssid"] as? String ?: "")
        is Trigger.WiFiSignalStrength -> component.copy(
            threshold = params["threshold"] as? Int ?: component.threshold,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.MobileDataTypeChanged -> component.copy(targetType = params["targetType"] as? String ?: "ANY")
        is Trigger.BluetoothDeviceDisconnected -> component.copy(address = params["address"] as? String ?: "")
        is Trigger.PingStatus -> component.copy(
            host = params["host"] as? String ?: "8.8.8.8",
            status = params["status"] as? String ?: "SUCCESS"
        )
        is Trigger.DomainReachability -> component.copy(
            domain = params["domain"] as? String ?: "google.com",
            status = params["status"] as? String ?: "REACHABLE"
        )
        is Trigger.NetworkSpeedThreshold -> component.copy(
            speedMbps = params["speedMbps"] as? Int ?: component.speedMbps,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.BatteryLevelAbove -> component.copy(level = params["level"] as? Int ?: component.level)
        is Trigger.BatteryLevelBelow -> component.copy(level = params["level"] as? Int ?: component.level)
        is Trigger.BluetoothDeviceConnected -> component.copy(address = params["address"] as? String ?: "")
        is Trigger.TimeOfDay -> {
            val time = params["time"] as? String ?: "12:00"
            val parts = time.split(":")
            component.copy(
                hour = parts.getOrNull(0)?.toIntOrNull() ?: 12,
                minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            )
        }
        is Trigger.TimeRange -> {
            val startParts = (params["start"] as? String ?: "09:00").split(":")
            val endParts = (params["end"] as? String ?: "17:00").split(":")
            component.copy(
                startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 9,
                startMin = startParts.getOrNull(1)?.toIntOrNull() ?: 0,
                endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 17,
                endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 0
            )
        }
        is Trigger.DayOfMonth -> component.copy(day = params["day"] as? Int ?: 1)
        is Trigger.MonthOfYear -> component.copy(month = params["month"] as? Int ?: 1)
        is Trigger.GoldenHour -> component.copy(isMorning = params["isMorning"] == "MORNING")
        is Trigger.TimerFinished -> component.copy(timerName = params["timerName"] as? String ?: "Default")
        is Trigger.StopwatchThreshold -> component.copy(
            stopwatchName = params["stopwatchName"] as? String ?: "Default",
            thresholdSeconds = params["threshold"] as? Int ?: 60
        )
        is Trigger.CountdownReached -> component.copy(countdownName = params["countdownName"] as? String ?: "Default")
        is Trigger.CronSchedule -> component.copy(expression = params["expression"] as? String ?: "0 0 * * *")
        is Trigger.AppOpened -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.AppClosed -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.AppInstalled -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.AppUninstalled -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.AppUpdated -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.AppCrashed -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.NotificationPosted -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.NotificationRemoved -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Trigger.NotificationMatches -> component.copy(pattern = params["pattern"] as? String ?: "")
        is Trigger.KeyboardStateChanged -> component.copy(opened = params["opened"] == "OPENED")
        is Trigger.AccessibilityEventDetected -> component.copy(eventType = params["eventType"] as? String ?: "VIEW_CLICKED")
        is Trigger.ToastDetected -> component.copy(message = params["message"] as? String ?: "")
        is Trigger.OverlayPermissionChanged -> component.copy(granted = params["granted"] == "GRANTED")
        
        is Trigger.FileCreated -> component.copy(path = params["path"] as? String ?: "")
        is Trigger.FileDeleted -> component.copy(path = params["path"] as? String ?: "")
        is Trigger.FileModified -> component.copy(path = params["path"] as? String ?: "")
        is Trigger.FolderChanged -> component.copy(path = params["path"] as? String ?: "")
        is Trigger.StorageSizeTrigger -> component.copy(
            sizeGB = params["sizeGB"] as? Int ?: component.sizeGB,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.LightSensorThreshold -> component.copy(lux = params["lux"] as? Int ?: component.lux)
        is Trigger.MagnetometerThreshold -> component.copy(
            strength = params["strength"] as? Int ?: component.strength,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.BarometerThreshold -> component.copy(
            pressure = params["pressure"] as? Int ?: component.pressure,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.TemperatureThreshold -> component.copy(
            temp = params["temp"] as? Int ?: component.temp,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.HumidityThreshold -> component.copy(
            humidity = params["humidity"] as? Int ?: component.humidity,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.HeartRateThreshold -> component.copy(
            bpm = params["bpm"] as? Int ?: component.bpm,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.AmbientNoiseThreshold -> component.copy(
            decibels = params["decibels"] as? Int ?: component.decibels,
            comparison = params["comparison"] as? String ?: component.comparison
        )
        is Trigger.StepCountThreshold -> component.copy(steps = params["steps"] as? Int ?: component.steps)
        is Trigger.ProximityTriggered -> component.copy(near = params["near"] == "NEAR")
        is Trigger.AccelerometerPattern -> component.copy(pattern = params["pattern"] as? String ?: "SHAKE")
        is Trigger.GyroscopePattern -> component.copy(pattern = params["pattern"] as? String ?: "ROTATION")
        is Trigger.TouchGesturePattern -> component.copy(pattern = params["pattern"] as? String ?: "DOUBLE_TAP")

        is Condition.WiFiSSIDIs -> component.copy(ssid = params["ssid"] as? String ?: "")
        is Condition.BatteryLevelBetween -> component.copy(
            min = params["min"] as? Int ?: component.min,
            max = params["max"] as? Int ?: component.max
        )
        is Condition.StorageFreeAbove -> component.copy(gb = params["gb"] as? Int ?: component.gb)

        is Action.SetVolume -> component.copy(volume = params["volume"] as? Int ?: component.volume)
        is Action.Speak -> component.copy(text = params["text"] as? String ?: component.text)
        is Action.Vibrate -> component.copy(durationMs = params["duration"] as? Int ?: component.durationMs)
        is Action.LaunchApp -> component.copy(packageName = params["packageName"] as? String ?: "")
        is Action.ShowNotification -> component.copy(
            title = params["title"] as? String ?: component.title,
            message = params["message"] as? String ?: component.message
        )
        is Action.ShowToast -> component.copy(message = params["message"] as? String ?: component.message)
        is Action.AutoClean -> component.copy(categories = params["categories"] as? String ?: component.categories)
        is Action.RunBackup -> component.copy(categories = params["categories"] as? String ?: component.categories)
        is Action.RunRestore -> component.copy(
            archivePath = params["archivePath"] as? String ?: component.archivePath,
            categories = params["categories"] as? String ?: component.categories
        )
        is Action.SetBrightness -> component.copy(level = params["level"] as? Int ?: component.level)
        is Action.Delay -> component.copy(seconds = params["seconds"] as? Int ?: component.seconds)
        is Action.RunAdbCommand -> component.copy(command = params["command"] as? String ?: component.command)
        else -> component
    }
}
