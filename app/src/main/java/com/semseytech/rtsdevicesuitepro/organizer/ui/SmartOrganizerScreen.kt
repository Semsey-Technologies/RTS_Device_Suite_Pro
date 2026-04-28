package com.semseytech.rtsdevicesuitepro.organizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerRule
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartOrganizerScreen(
    viewModel: OrganizerViewModel,
    onBack: () -> Unit
) {
    val currentTheme = LocalTheme.current
    val rules by viewModel.rules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SMART ORGANIZER",
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
                actions = {
                    IconButton(onClick = { viewModel.runRulesNow() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run All Now", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = currentTheme.accentColor,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (rules.isEmpty()) {
                EmptyState(currentTheme.accentColor)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules) { rule ->
                        RuleCard(
                            rule = rule,
                            accentColor = currentTheme.accentColor,
                            onDelete = { viewModel.deleteRule(rule) },
                            onToggle = { viewModel.toggleRule(rule) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { rule ->
                viewModel.addRule(rule)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EmptyState(accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Rule,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = accentColor.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No organization rules yet",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Tap + to create your first rule",
            color = Color.White.copy(alpha = 0.3f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun RuleCard(
    rule: OrganizerRule,
    accentColor: Color,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.02f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (rule.isEnabled) accentColor.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (rule.isEnabled) accentColor else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.name,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.isEnabled) Color.White else Color.Gray
                )
                Text(
                    "${rule.sourcePath} -> ${rule.targetPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White.copy(alpha = 0.5f))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}
