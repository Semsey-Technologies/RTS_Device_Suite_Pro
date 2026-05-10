package com.semseytech.rtsdevicesuitepro.restore.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.semseytech.rtsdevicesuitepro.restore.RestoreViewModel
import com.semseytech.rtsdevicesuitepro.backup.model.RestoreReport
import com.semseytech.rtsdevicesuitepro.backup.ui.CategoryCard
import com.semseytech.rtsdevicesuitepro.backup.ui.BackupItemRow
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    viewModel: RestoreViewModel = viewModel(),
    onBack: () -> Unit = {},
    onViewResults: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    val scope = rememberCoroutineScope()

    val pickArchiveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.loadArchive(it) }
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.checkDefaultSmsStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.checkDefaultSmsStatus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "RESTORE MODULE", 
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        color = currentTheme.accentColor
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.categories.any { it.id == "sms" } && !uiState.isDefaultSmsApp) {
                        TextButton(onClick = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                        defaultSmsLauncher.launch(intent)
                                    } else {
                                        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                        }
                                        defaultSmsLauncher.launch(intent)
                                    }
                                } else {
                                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                    }
                                    defaultSmsLauncher.launch(intent)
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Could not open SMS settings: ${e.message}")
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = currentTheme.accentColor)
                            Spacer(Modifier.width(4.dp))
                            Text("Set Default SMS", color = currentTheme.accentColor)
                        }
                    }
                    if (uiState.categories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearRestoreCache() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Cache", tint = currentTheme.accentColor)
                        }
                    }
                    IconButton(onClick = { pickArchiveLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Open Archive", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        bottomBar = {
            if (uiState.categories.isNotEmpty() && uiState.report == null) {
                Surface(
                    color = Color(0xFF111A2C).copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Button(
                        onClick = { viewModel.startRestore() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(56.dp * scale),
                        enabled = !uiState.isRestoring && uiState.categories.any { it.items.any { item -> item.isSelected } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (uiState.isRestoring) "RESTORING DATA..." else "RESTORE SELECTED ITEMS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(currentTheme.startColor, Color.Black)
                    )
                )
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = currentTheme.accentColor)
                    Spacer(Modifier.height(16.dp))
                    Text(uiState.status, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (uiState.isRestoring) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = currentTheme.accentColor)
                    Spacer(Modifier.height(16.dp))
                    Text(uiState.status, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = currentTheme.accentColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            } else if (uiState.report != null) {
                RestoreReportView(
                    report = uiState.report!!,
                    onViewResults = onViewResults,
                    onDismiss = { viewModel.loadArchive(uiState.selectedUri!!) } // Reload to reset state
                )
            } else if (uiState.categories.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isError = uiState.status.contains("failed", ignoreCase = true)
                    Icon(
                        if (isError) Icons.Default.ErrorOutline else Icons.Default.CloudDownload, 
                        contentDescription = null, 
                        modifier = Modifier.size(80.dp * scale),
                        tint = if (isError) Color.Red.copy(alpha = 0.6f) else currentTheme.accentColor.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (uiState.status != "Select an archive") uiState.status else "Select a backup archive to begin restoration.", 
                        color = if (isError) Color.Red.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 48.dp),
                        style = if (isError) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { pickArchiveLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.height(48.dp * scale).padding(horizontal = 32.dp)
                    ) {
                        Text(if (isError) "Try Again" else "Pick Archive", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    uiState.categories.forEach { category ->
                        if (category == uiState.categories.first()) {
                            item(key = "master_restore") {
                                MasterRestoreButton(
                                    isSelected = uiState.isMasterSelected,
                                    onToggle = { viewModel.toggleMasterSelection() }
                                )
                            }
                        }

                        item(key = "header_${category.id}") {
                            CategoryCard(
                                category = category,
                                onExpandToggle = { viewModel.toggleCategoryExpansion(category.id) },
                                onCategorySelect = { viewModel.toggleCategorySelection(category.id, it) }
                            )
                        }

                        if (category.isExpanded) {
                            if (category.items.isEmpty()) {
                                item(key = "empty_${category.id}") {
                                    Text(
                                        "No items found in this archive.",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                                    )
                                }
                            } else {
                                item(key = "actions_${category.id}") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewModel.toggleCategorySelection(category.id, true) }) {
                                            Text("Select All", color = currentTheme.accentColor)
                                        }
                                        TextButton(onClick = { viewModel.toggleCategorySelection(category.id, false) }) {
                                            Text("Deselect All", color = currentTheme.accentColor)
                                        }
                                    }
                                }
                                items(category.items, key = { "${category.id}_${it.id}" }) { item ->
                                    Box(modifier = Modifier.padding(start = 24.dp)) {
                                        BackupItemRow(
                                            item = item,
                                            viewMode = uiState.viewMode,
                                            onSelect = { viewModel.toggleItemSelection(category.id, item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterRestoreButton(isSelected: Boolean, onToggle: () -> Unit) {
    val currentTheme = LocalTheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) currentTheme.accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accentColor) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = currentTheme.accentColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Restore Everything",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RestoreReportView(report: RestoreReport, onViewResults: () -> Unit, onDismiss: () -> Unit) {
    val currentTheme = LocalTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = currentTheme.accentColor
        )
        Spacer(Modifier.height(16.dp))
        Text("Restore Complete", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ReportRow("Items Restored", report.restoredCount.toString(), currentTheme.accentColor)
                ReportRow("Items Skipped", report.skippedCount.toString(), Color.Gray)
                ReportRow("Errors", report.errorCount.toString(), Color.Red)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(report.details) { detail ->
                Text(detail, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = onViewResults,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
        ) {
            Text("VIEW RESTORED DATA", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, currentTheme.accentColor.copy(alpha = 0.5f))
        ) {
            Text("Done", color = Color.White)
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}
