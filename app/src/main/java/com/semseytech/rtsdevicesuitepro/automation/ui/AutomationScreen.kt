package com.semseytech.rtsdevicesuitepro.automation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var showAddGroupDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                        onToggle = { viewModel.toggleRule(rule.id, it) },
                        onDelete = { viewModel.deleteRule(rule) },
                        onMove = { viewModel.moveRuleToGroup(rule.id, it) }
                    )
                }
            }

            // Ungrouped Rules
            val ungroupedRules = rules.filter { it.groupId == null }
            if (ungroupedRules.isNotEmpty()) {
                item {
                    Text(
                        "Ungrouped",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(ungroupedRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        groups = groups,
                        onToggle = { viewModel.toggleRule(rule.id, it) },
                        onDelete = { viewModel.deleteRule(rule) },
                        onMove = { viewModel.moveRuleToGroup(rule.id, it) }
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
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMove: (String?) -> Unit
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
fun AddRuleDialog(
    groups: List<RuleGroup>,
    onDismiss: () -> Unit,
    onAdd: (String, Trigger, List<Condition>, List<Action>, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf<Trigger>(Trigger.WiFiConnected) }
    val selectedConditions = remember { mutableStateListOf<Condition>() }
    val selectedActions = remember { mutableStateListOf<Action>() }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    
    var editingComponent by remember { mutableStateOf<AutomationComponent?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Build New Rule", color = Color.White) },
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
                ComponentSelector(
                    current = selectedTrigger,
                    options = listOf(
                        Trigger.WiFiConnected,
                        Trigger.WiFiDisconnected,
                        Trigger.SpecificWiFiConnected(),
                        Trigger.WiFiSignalLow(),
                        Trigger.MobileDataActive,
                        Trigger.PowerConnected,
                        Trigger.PowerDisconnected,
                        Trigger.BatteryLevelAbove(),
                        Trigger.BatteryLevelBelow(),
                        Trigger.ScreenOn,
                        Trigger.ScreenOff,
                        Trigger.ScreenUnlocked,
                        Trigger.DeviceBootCompleted,
                        Trigger.BluetoothOn,
                        Trigger.BluetoothDeviceConnected(),
                        Trigger.TimeOfDay(),
                        Trigger.AppOpened()
                    ),
                    onSelected = { selectedTrigger = it as Trigger },
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
                                    selectedConditions.add(opt)
                                    showConditionOptions = false
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
                            Action.ToggleFlashlight,
                            Action.SetBrightness(),
                            Action.Delay()
                        )
                        actionOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.displayName) },
                                onClick = {
                                    selectedActions.add(opt)
                                    showActionOptions = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, selectedTrigger, selectedConditions.toList(), selectedActions.toList(), selectedGroupId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Create Rule", color = Color.Black)
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
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(current.displayName, color = Color.White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.displayName) },
                            onClick = {
                                onSelected(opt)
                                expanded = false
                            }
                        )
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
        is Trigger.WiFiSignalLow -> component.copy(threshold = params["threshold"] as? Int ?: component.threshold)
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
        is Trigger.AppOpened -> component.copy(packageName = params["packageName"] as? String ?: "")
        
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
        is Action.SetBrightness -> component.copy(level = params["level"] as? Int ?: component.level)
        is Action.Delay -> component.copy(seconds = params["seconds"] as? Int ?: component.seconds)
        else -> component
    }
}
