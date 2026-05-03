package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.automation.ui.*
import com.semseytech.rtsdevicesuitepro.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    initialTab: Int = 0,
    onBack: () -> Unit,
    onNavigateToThemes: () -> Unit,
    automationViewModel: AutomationViewModel = viewModel()
) {
    val currentTheme = LocalTheme.current
    var activeTab by remember { mutableIntStateOf(initialTab) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SYSTEM CONFIGURATION",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = currentTheme.accentColor
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .drawBehind {
                    val gridSize = 40.dp.toPx()
                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                }
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Black.copy(alpha = 0.3f),
                contentColor = currentTheme.accentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = currentTheme.accentColor
                    )
                }
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Box(Modifier.padding(16.dp)) { Icon(Icons.Default.Palette, null) }
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Box(Modifier.padding(16.dp)) { Icon(Icons.Default.FormatSize, null) }
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Box(Modifier.padding(16.dp)) { Icon(Icons.Default.FontDownload, null) }
                }
                Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                    Box(Modifier.padding(16.dp)) { Icon(Icons.Default.Schedule, null) }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> ThemeSettingsCard(onNavigateToThemes)
                    1 -> ScalingSettings()
                    2 -> TypographySettings()
                    3 -> AutomationSettings(automationViewModel, snackbarHostState)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationSettings(viewModel: AutomationViewModel, snackbarHostState: SnackbarHostState) {
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
        list.addAll(action?.categories ?: listOf("temp", "dupes", "empty_folders"))
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
        "STORAGE_SIZE" to "Storage Size"
    )

    var selectedInterval by remember { mutableIntStateOf(1) }
    var selectedFrequency by remember { mutableStateOf("WEEKLY") }
    var selectedComparison by remember { mutableStateOf("MORE_THAN") }
    var storageSizeGB by remember { mutableIntStateOf(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "AUTOMATION RULES",
            style = MaterialTheme.typography.titleMedium,
            color = currentTheme.accentColor,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, tint = currentTheme.accentColor)
                    Text("Auto Clean Scheduler", style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("CLEANUP CATEGORIES", style = MaterialTheme.typography.labelSmall, color = currentTheme.accentColor)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("TRIGGER EVENT", style = MaterialTheme.typography.labelSmall, color = currentTheme.accentColor)
                var triggerExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                }

                Spacer(modifier = Modifier.height(16.dp))
                
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
                            else -> Trigger.TimeOfDay(autoCleanHour, autoCleanMinute)
                        }
                        
                        if (autoCleanRule != null) {
                            viewModel.deleteRule(autoCleanRule)
                        }
                        
                        viewModel.addRule(
                            name = "Scheduled Auto Clean",
                            trigger = trigger,
                            conditions = emptyList(),
                            actions = listOf(Action.AutoClean(selectedCategories.toList()))
                        )

                        scope.launch {
                            snackbarHostState.showSnackbar("Auto Clean settings saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
                ) {
                    Text("Save Scheduler Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            "Rules configured here apply when 'Auto Clean' is enabled on the dashboard.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ThemeSettingsCard(onNavigateToThemes: () -> Unit) {
    val currentTheme = LocalTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToThemes() }
                .border(1.dp, currentTheme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Appearance & Theming",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Change app colors, themes, and styles",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = currentTheme.accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Current Theme: ${currentTheme.name}", color = currentTheme.accentColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ScalingSettings() {
    val currentTheme = LocalTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ScalingSlider("Global UI Scale", ThemeManager.uiScale, 0.8f, 2.0f) { ThemeManager.updateUiScale(it) }
        ScalingSlider("Title Text Scale", ThemeManager.titleSizeScale, 0.8f, 2.5f) { ThemeManager.updateTitleScale(it) }
        ScalingSlider("Subtitle Text Scale", ThemeManager.subtitleSizeScale, 0.8f, 2.0f) { ThemeManager.updateSubtitleScale(it) }
        ScalingSlider("Body Text Scale", ThemeManager.bodySizeScale, 0.8f, 2.0f) { ThemeManager.updateBodyScale(it) }
    }
}

@Composable
fun ScalingSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    val currentTheme = LocalTheme.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(String.format(Locale.getDefault(), "%.2fx", value), color = currentTheme.accentColor, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = currentTheme.accentColor,
                activeTrackColor = currentTheme.accentColor,
                inactiveTrackColor = currentTheme.accentColor.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun TypographySettings() {
    val currentTheme = LocalTheme.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Font Family Dropdown
        Text("Font Family", color = Color.White, style = MaterialTheme.typography.titleMedium)
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(ThemeManager.selectedFont.displayName, color = Color.White)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.DarkGray)
            ) {
                AppFont.entries.forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font.displayName, color = Color.White) },
                        onClick = {
                            ThemeManager.selectedFont = font
                            expanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Style Toggles
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Bold Text", color = Color.White)
            Switch(
                checked = ThemeManager.isBold,
                onCheckedChange = { ThemeManager.isBold = it },
                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.accentColor)
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Italic Text", color = Color.White)
            Switch(
                checked = ThemeManager.isItalic,
                onCheckedChange = { ThemeManager.isItalic = it },
                colors = SwitchDefaults.colors(checkedThumbColor = currentTheme.accentColor)
            )
        }

        // Preview Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sample Title", style = MaterialTheme.typography.titleLarge, color = currentTheme.accentColor)
                Text("Sample Subtitle", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                Text("This is a preview of the typography settings you've applied above. Adjust scaling, weight, and font to your preference.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        }
    }
}
