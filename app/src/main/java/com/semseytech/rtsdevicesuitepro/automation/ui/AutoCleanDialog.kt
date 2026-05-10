package com.semseytech.rtsdevicesuitepro.automation.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoCleanDialog(
    onDismiss: () -> Unit,
    viewModel: AutomationViewModel
) {
    val currentTheme = LocalTheme.current
    val rules by viewModel.rules.collectAsState()
    val scope = rememberCoroutineScope()
    
    val autoCleanRule = remember(rules) {
        rules.find { it.actions.any { action -> action is Action.AutoClean } }
    }

    var selectedTriggerType by remember(autoCleanRule) {
        mutableStateOf(autoCleanRule?.trigger?.type ?: "TIME_OF_DAY")
    }

    var autoCleanHour by remember(autoCleanRule) {
        val trigger = autoCleanRule?.trigger as? Trigger.TimeOfDay
        mutableIntStateOf(trigger?.hour ?: 3)
    }
    
    var autoCleanMinute by remember(autoCleanRule) {
        val trigger = autoCleanRule?.trigger as? Trigger.TimeOfDay
        mutableIntStateOf(trigger?.minute ?: 0)
    }

    var selectedDaysStr by remember(autoCleanRule) {
        val trigger = autoCleanRule?.trigger as? Trigger.DaysOfWeek
        mutableStateOf(trigger?.parameters?.find { it.key == "days" }?.value as? String ?: "Mon,Wed,Fri")
    }
    
    val selectedCategories = remember(autoCleanRule) {
        val action = autoCleanRule?.actions?.find { it is Action.AutoClean } as? Action.AutoClean
        val list = mutableStateListOf<String>()
        val initialCategories = action?.categories?.split(",") ?: listOf("temp", "dupes", "empty_folders")
        list.addAll(initialCategories.filter { it.isNotBlank() })
        list
    }

    val availableCategories = listOf(
        "temp" to "Temp Files",
        "empty_folders" to "Empty Folders",
        "dupes" to "Duplicate Files",
        "residual" to "Residual Data",
        "logs" to "Call Logs",
        "sms" to "SMS Threads",
        "contact_dupes" to "Duplicate Contacts"
    )

    val triggerTypes = listOf(
        "TIME_OF_DAY" to "Specific Time",
        "DEVICE_IDLE" to "When Idle",
        "POWER_CONNECTED" to "Power Connected",
        "POWER_DISCONNECTED" to "Power Disconnected",
        "DAYS_OF_WEEK" to "Day of Week",
        "RECURRING" to "Frequency",
        "INTERVAL" to "Every X Days",
        "STORAGE_SIZE" to "Storage Size",
        "SENSOR_LIGHT" to "Light Level",
        "PROXIMITY_TRIGGERED" to "Proximity Sensor",
        "ACCELEROMETER_PATTERN" to "Movement Pattern",
        "GYROSCOPE_PATTERN" to "Rotation Pattern",
        "SENSOR_MAGNETIC" to "Magnetic Field",
        "SENSOR_PRESSURE" to "Barometer",
        "SENSOR_TEMP" to "Temperature",
        "SENSOR_HUMIDITY" to "Humidity",
        "SENSOR_HEART_RATE" to "Heart Rate",
        "STEP_COUNT_THRESHOLD" to "Step Counter",
        "SENSOR_NOISE" to "Ambient Noise",
        "TOUCH_GESTURE" to "Touch Gesture",
        "FILE_CREATED" to "File Created",
        "FILE_DELETED" to "File Deleted",
        "FILE_MODIFIED" to "File Modified",
        "FOLDER_CHANGED" to "Folder Contents Changed",
        "STORAGE_MOUNTED" to "Storage Mounted",
        "STORAGE_UNMOUNTED" to "Storage Unmounted",
        "DOWNLOAD_COMPLETED" to "Download Completed",
        "SCREENSHOT_TAKEN" to "Screenshot Saved"
    )

    var selectedInterval by remember { mutableIntStateOf(1) }
    var selectedFrequency by remember { mutableStateOf("WEEKLY") }
    var selectedComparison by remember { mutableStateOf("MORE_THAN") }
    var storageSizeGB by remember { mutableIntStateOf(10) }
    
    var sensorValue by remember { mutableIntStateOf(50) }
    var selectedPattern by remember { mutableStateOf("SHAKE") }
    var proximityNear by remember { mutableStateOf(true) }
    var selectedPath by remember { mutableStateOf("/storage/emulated/0/Download") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Schedule, null, tint = currentTheme.accentColor)
                Text("Auto Clean Scheduler", style = MaterialTheme.typography.titleMedium, color = currentTheme.accentColor)
            }
        },
        containerColor = currentTheme.startColor,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("CLEANUP CATEGORIES", style = MaterialTheme.typography.labelSmall, color = currentTheme.accentColor)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedCategories.contains(id),
                            onClick = {
                                if (selectedCategories.contains(id)) selectedCategories.remove(id)
                                else selectedCategories.add(id)
                            },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = currentTheme.accentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(alpha = 0.05f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Text("TRIGGER EVENT", style = MaterialTheme.typography.labelSmall, color = currentTheme.accentColor)
                var triggerExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { triggerExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(triggerTypes.find { it.first == selectedTriggerType }?.second ?: "Select Trigger", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = triggerExpanded,
                        onDismissRequest = { triggerExpanded = false },
                        modifier = Modifier.background(Color.DarkGray)
                    ) {
                        triggerTypes.forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    selectedTriggerType = type
                                    triggerExpanded = false
                                }
                            )
                        }
                    }
                }

                when (selectedTriggerType) {
                    "TIME_OF_DAY" -> {
                        var showPicker by remember { mutableStateOf(false) }
                        val displayTime = remember(autoCleanHour, autoCleanMinute) {
                            val h = if (autoCleanHour % 12 == 0) 12 else autoCleanHour % 12
                            val ampm = if (autoCleanHour < 12) "AM" else "PM"
                            String.format("%d:%02d %s", h, autoCleanMinute, ampm)
                        }

                        Text("Execution Time:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        OutlinedButton(
                            onClick = { showPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(displayTime, color = Color.White)
                        }

                        if (showPicker) {
                            TimePicker12h(
                                initialHour = autoCleanHour,
                                initialMinute = autoCleanMinute,
                                onTimeSelected = { h, m ->
                                    autoCleanHour = h
                                    autoCleanMinute = m
                                    showPicker = false
                                },
                                onDismiss = { showPicker = false }
                            )
                        }
                    }
                    "DAYS_OF_WEEK" -> {
                        var showPicker by remember { mutableStateOf(false) }
                        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        val selectedDays = selectedDaysStr.split(",").filter { it.isNotBlank() }.map { dayNames.indexOf(it) }.filter { it != -1 }

                        Text("Run on Days:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        OutlinedButton(
                            onClick = { showPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(if (selectedDaysStr.isBlank()) "Select Days" else selectedDaysStr, color = Color.White)
                        }

                        if (showPicker) {
                            DayOfWeekPicker(
                                selectedDays = selectedDays,
                                onDaysSelected = { days ->
                                    selectedDaysStr = days.sorted().joinToString(",") { dayNames[it] }
                                    showPicker = false
                                },
                                onDismiss = { showPicker = false }
                            )
                        }
                    }
                    "RECURRING" -> {
                        Text("Frequency:", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { freq ->
                                FilterChip(
                                    selected = selectedFrequency == freq,
                                    onClick = { selectedFrequency = freq },
                                    label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = currentTheme.accentColor,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }
                    "INTERVAL" -> {
                        Text("Every $selectedInterval Days", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Slider(
                            value = selectedInterval.toFloat(),
                            onValueChange = { selectedInterval = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.accentColor, activeTrackColor = currentTheme.accentColor)
                        )
                    }
                    "STORAGE_SIZE" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("When used size is", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            var compExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { compExpanded = true }) {
                                    Text(selectedComparison.replace("_", " "), color = currentTheme.accentColor)
                                }
                                DropdownMenu(expanded = compExpanded, onDismissRequest = { compExpanded = false }) {
                                    listOf("EQUALS", "MORE_THAN", "LESS_THAN").forEach { comp ->
                                        DropdownMenuItem(text = { Text(comp) }, onClick = { selectedComparison = comp; compExpanded = false })
                                    }
                                }
                            }
                        }
                        Text("$storageSizeGB GB", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            value = storageSizeGB.toFloat(),
                            onValueChange = { storageSizeGB = it.toInt() },
                            valueRange = 1f..512f,
                            colors = SliderDefaults.colors(thumbColor = currentTheme.accentColor, activeTrackColor = currentTheme.accentColor)
                        )
                    }
                    "SENSOR_LIGHT", "SENSOR_MAGNETIC", "SENSOR_PRESSURE", "SENSOR_TEMP", "SENSOR_HUMIDITY", "SENSOR_HEART_RATE", "SENSOR_NOISE", "STEP_COUNT_THRESHOLD" -> {
                        val (label, unit, min, max) = when(selectedTriggerType) {
                            "SENSOR_LIGHT" -> listOf("Lux Level", "lux", 0, 10000)
                            "SENSOR_MAGNETIC" -> listOf("Magnetic Strength", "µT", 0, 500)
                            "SENSOR_PRESSURE" -> listOf("Pressure", "hPa", 800, 1200)
                            "SENSOR_TEMP" -> listOf("Temperature", "°C", -20, 60)
                            "SENSOR_HUMIDITY" -> listOf("Humidity", "%", 0, 100)
                            "SENSOR_HEART_RATE" -> listOf("Heart Rate", "BPM", 40, 200)
                            "SENSOR_NOISE" -> listOf("Noise Level", "dB", 0, 120)
                            "STEP_COUNT_THRESHOLD" -> listOf("Step Count", "steps", 0, 50000)
                            else -> listOf("Value", "", 0, 100)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("When $label is", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            var compExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { compExpanded = true }) {
                                    Text(selectedComparison.replace("_", " "), color = currentTheme.accentColor)
                                }
                                DropdownMenu(expanded = compExpanded, onDismissRequest = { compExpanded = false }) {
                                    listOf("MORE_THAN", "LESS_THAN").forEach { comp ->
                                        DropdownMenuItem(text = { Text(comp.replace("_", " ")) }, onClick = { selectedComparison = comp; compExpanded = false })
                                    }
                                }
                            }
                        }
                        Text("$sensorValue ${unit as String}", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            value = sensorValue.toFloat().coerceIn((min as Int).toFloat(), (max as Int).toFloat()),
                            onValueChange = { sensorValue = it.toInt() },
                            valueRange = (min as Int).toFloat()..(max as Int).toFloat(),
                            colors = SliderDefaults.colors(thumbColor = currentTheme.accentColor, activeTrackColor = currentTheme.accentColor)
                        )
                    }
                    "PROXIMITY_TRIGGERED" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Trigger when object is:", color = Color.White)
                            FilterChip(
                                selected = proximityNear,
                                onClick = { proximityNear = true },
                                label = { Text("NEAR") }
                            )
                            FilterChip(
                                selected = !proximityNear,
                                onClick = { proximityNear = false },
                                label = { Text("FAR") }
                            )
                        }
                    }
                    "ACCELEROMETER_PATTERN", "GYROSCOPE_PATTERN", "TOUCH_GESTURE" -> {
                        val patterns = when(selectedTriggerType) {
                            "ACCELEROMETER_PATTERN" -> listOf("SHAKE", "TILT_LEFT", "TILT_RIGHT", "JOLT")
                            "GYROSCOPE_PATTERN" -> listOf("ROTATION", "FLIP")
                            "TOUCH_GESTURE" -> listOf("DOUBLE_TAP", "TRIPLE_TAP", "LONG_PRESS_TWO_FINGERS", "SWIPE_UP_THREE_FINGERS")
                            else -> emptyList()
                        }
                        Text("Select Pattern:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            patterns.forEach { p ->
                                FilterChip(
                                    selected = selectedPattern == p,
                                    onClick = { selectedPattern = p },
                                    label = { Text(p.replace("_", " "), fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                    "FILE_CREATED", "FILE_DELETED", "FILE_MODIFIED", "FOLDER_CHANGED" -> {
                        Text("Watch Folder Path:", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        OutlinedTextField(
                            value = selectedPath,
                            onValueChange = { selectedPath = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentTheme.accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                    "STORAGE_MOUNTED", "STORAGE_UNMOUNTED", "DOWNLOAD_COMPLETED", "SCREENSHOT_TAKEN" -> {
                        Text("Triggers immediately when event occurs.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trigger = when (selectedTriggerType) {
                        "TIME_OF_DAY" -> Trigger.TimeOfDay(autoCleanHour, autoCleanMinute)
                        "DAYS_OF_WEEK" -> {
                            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            val indices = selectedDaysStr.split(",").filter { it.isNotBlank() }.map { dayNames.indexOf(it) }.filter { it != -1 }
                            Trigger.DaysOfWeek(indices)
                        }
                        "DEVICE_IDLE" -> Trigger.DeviceIdle
                        "POWER_CONNECTED" -> Trigger.PowerConnected
                        "POWER_DISCONNECTED" -> Trigger.PowerDisconnected
                        "RECURRING" -> Trigger.RecurringTrigger(selectedFrequency)
                        "INTERVAL" -> Trigger.IntervalTrigger(selectedInterval)
                        "STORAGE_SIZE" -> Trigger.StorageSizeTrigger(storageSizeGB, selectedComparison)
                        "SENSOR_LIGHT" -> Trigger.LightSensorThreshold(sensorValue)
                        "PROXIMITY_TRIGGERED" -> Trigger.ProximityTriggered(proximityNear)
                        "ACCELEROMETER_PATTERN" -> Trigger.AccelerometerPattern(selectedPattern)
                        "GYROSCOPE_PATTERN" -> Trigger.GyroscopePattern(selectedPattern)
                        "SENSOR_MAGNETIC" -> Trigger.MagnetometerThreshold(sensorValue, selectedComparison)
                        "SENSOR_PRESSURE" -> Trigger.BarometerThreshold(sensorValue, selectedComparison)
                        "SENSOR_TEMP" -> Trigger.TemperatureThreshold(sensorValue, selectedComparison)
                        "SENSOR_HUMIDITY" -> Trigger.HumidityThreshold(sensorValue, selectedComparison)
                        "SENSOR_HEART_RATE" -> Trigger.HeartRateThreshold(sensorValue, selectedComparison)
                        "STEP_COUNT_THRESHOLD" -> Trigger.StepCountThreshold(sensorValue)
                        "SENSOR_NOISE" -> Trigger.AmbientNoiseThreshold(sensorValue, selectedComparison)
                        "TOUCH_GESTURE" -> Trigger.TouchGesturePattern(selectedPattern)
                        "FILE_CREATED" -> Trigger.FileCreated(selectedPath)
                        "FILE_DELETED" -> Trigger.FileDeleted(selectedPath)
                        "FILE_MODIFIED" -> Trigger.FileModified(selectedPath)
                        "FOLDER_CHANGED" -> Trigger.FolderChanged(selectedPath)
                        "STORAGE_MOUNTED" -> Trigger.ExternalStorageMounted
                        "STORAGE_UNMOUNTED" -> Trigger.ExternalStorageUnmounted
                        "DOWNLOAD_COMPLETED" -> Trigger.DownloadCompleted
                        "SCREENSHOT_TAKEN" -> Trigger.ScreenshotTaken
                        else -> Trigger.TimeOfDay(autoCleanHour, autoCleanMinute)
                    }
                    
                    if (autoCleanRule != null) {
                        viewModel.deleteRule(autoCleanRule)
                    }
                    
                    viewModel.addRule(
                        name = "Scheduled Auto Clean",
                        trigger = trigger,
                        conditions = emptyList(),
                        actions = listOf(Action.AutoClean(selectedCategories.joinToString(",")))
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
