package com.semseytech.rtsdevicesuitepro.net.automation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationEngineScreen(
    viewModel: AutomationViewModel,
    onBack: () -> Unit,
    onNavigateToControls: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AUTOMATION ENGINE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.textColor)
                    }
                },
                actions = {
                    if (settings.rules.any { it.isEnabled }) {
                        IconButton(onClick = { viewModel.stopAllRules() }) {
                            Icon(Icons.Default.StopCircle, contentDescription = "Stop All", tint = Color.Red)
                        }
                    }
                    IconButton(onClick = onNavigateToControls) {
                        Icon(Icons.Default.SettingsSuggest, contentDescription = "Quick Toggles", tint = currentTheme.textColor)
                    }
                    IconButton(onClick = { showAddRuleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = currentTheme.textColor)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(currentTheme.startColor, currentTheme.endColor)))
        ) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp * scale),
                verticalArrangement = Arrangement.spacedBy(16.dp * scale)
            ) {
                item {
                    Text(
                        "Active Rules & Automation",
                        color = currentTheme.accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (settings.rules.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No rules created yet. Tap + to add one.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(settings.rules, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            accentColor = currentTheme.accentColor,
                            scale = scale,
                            onToggle = { viewModel.toggleRule(rule.id, it) },
                            onDelete = { viewModel.removeRule(rule.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            currentTheme = currentTheme,
            onDismiss = { showAddRuleDialog = false },
            onAdd = { rule ->
                viewModel.addRule(rule)
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: AutomationRule,
    accentColor: Color,
    scale: Float,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp * scale)),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp * scale).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.name,
                    color = if (rule.isEnabled) Color.White else Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "IF ${rule.conditionType.name.replace("_", " ")} ${rule.conditionValue} THEN ${rule.actionType.name.replace("_", " ")}",
                    color = if (rule.isEnabled) Color.LightGray else Color.DarkGray,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (10 * scale).sp
                )
                if (rule.actionType == ActionType.NOTIFY_USER) {
                    Text("Channel: ${rule.audioChannel.name}", color = accentColor.copy(alpha = 0.6f), fontSize = 9.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    currentTheme: com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset,
    onDismiss: () -> Unit,
    onAdd: (AutomationRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf(ConditionType.LATENCY_ABOVE) }
    var conditionValue by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(ActionType.NOTIFY_USER) }
    var selectedAudioChannel by remember { mutableStateOf(AudioChannel.MEDIA) }

    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    conditionValue = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            },
            containerColor = currentTheme.endColor
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Rule", color = currentTheme.accentColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Condition:", color = Color.White, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ConditionType.entries.forEach { type ->
                        FilterChip(
                            selected = condition == type,
                            onClick = { condition = type },
                            label = { Text(type.name, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                
                if (condition == ConditionType.TIME_INTERVAL) {
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (conditionValue.isEmpty()) "Select Time" else "Time: $conditionValue")
                    }
                } else {
                    OutlinedTextField(
                        value = conditionValue,
                        onValueChange = { conditionValue = it },
                        label = { Text("Value (e.g. 150ms or 20%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Action:", color = Color.White, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ActionType.entries.forEach { type ->
                        FilterChip(
                            selected = action == type,
                            onClick = { action = type },
                            label = { Text(type.name, fontSize = 10.sp) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                if (action == ActionType.NOTIFY_USER) {
                    Text("Audio Channel:", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        AudioChannel.entries.forEach { channel ->
                            FilterChip(
                                selected = selectedAudioChannel == channel,
                                onClick = { selectedAudioChannel = channel },
                                label = { Text(channel.name, fontSize = 10.sp) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onAdd(AutomationRule(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    conditionType = condition,
                    conditionValue = conditionValue,
                    actionType = action,
                    audioChannel = selectedAudioChannel,
                    isEnabled = true
                ))
            }, enabled = name.isNotBlank() && conditionValue.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = currentTheme.endColor
    )
}
