package com.semseytech.rtsdevicesuitepro.prereset.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.prereset.logic.PreResetPdfGenerator
import com.semseytech.rtsdevicesuitepro.prereset.logic.PreResetScanner
import com.semseytech.rtsdevicesuitepro.prereset.model.*
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.widget.Toast

class PreResetViewModel(private val scanner: PreResetScanner, private val pdfGenerator: PreResetPdfGenerator) : ViewModel() {
    private val _uiState = MutableStateFlow<PreResetUiState>(PreResetUiState.Loading)
    val uiState: StateFlow<PreResetUiState> = _uiState

    fun loadData() {
        _uiState.value = PreResetUiState.Success(scanner.scanDevice())
    }

    fun exportPdf(context: android.content.Context, data: PreResetGuideData) {
        val uri = pdfGenerator.generatePdf(data)
        if (uri != null) {
            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }
}

sealed class PreResetUiState {
    object Loading : PreResetUiState()
    data class Success(val data: PreResetGuideData) : PreResetUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreResetGuideScreen(
    onNavigate: (String) -> Unit,
    viewModel: PreResetViewModel = viewModel(factory = PreResetViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Reset Safety Guide", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Screen.Dashboard.route) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(currentTheme.startColor)
        ) {
            when (val state = uiState) {
                is PreResetUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = accentColorOrDefault(currentTheme.accentColor))
                }
                is PreResetUiState.Success -> {
                    GuideContent(state.data, onNavigate, viewModel, currentTheme.accentColor, scale)
                }
            }
        }
    }
}

fun accentColorOrDefault(color: Color?): Color = color ?: Color(0xFF00BFFF)

@Composable
fun GuideContent(
    data: PreResetGuideData,
    onNavigate: (String) -> Unit,
    viewModel: PreResetViewModel,
    accentColor: Color,
    scale: Float
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp * scale),
        verticalArrangement = Arrangement.spacedBy(16.dp * scale)
    ) {
        item {
            SummaryCard(data, accentColor, scale)
        }

        item {
            val context = LocalContext.current
            ExportButton(onClick = { viewModel.exportPdf(context, data) }, accentColor, scale)
        }

        item {
            CollapsibleSection(
                title = "Accounts (${data.accounts.size})",
                icon = Icons.Default.AccountCircle,
                accentColor = accentColor,
                scale = scale
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                    data.accounts.forEach { account ->
                        AccountItem(account, scale)
                    }
                }
            }
        }

        item {
            val criticalApps = data.apps.filter { it.isTwoFactorApp || it.category == AppCategory.BANKING || it.category == AppCategory.CRYPTO }
            CollapsibleSection(
                title = "Critical Apps (${criticalApps.size})",
                icon = Icons.Default.Warning,
                accentColor = Color.Red,
                scale = scale
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                    criticalApps.forEach { app ->
                        AppItem(app, SafetyStatus.CRITICAL, scale)
                    }
                }
            }
        }

        item {
            val localDataApps = data.apps.filter { it.storesLocalData && !it.isTwoFactorApp }
            CollapsibleSection(
                title = "Local Data Apps (${localDataApps.size})",
                icon = Icons.Default.Storage,
                accentColor = Color.Yellow,
                scale = scale
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                    localDataApps.forEach { app ->
                        AppItem(app, SafetyStatus.WARNING, scale)
                    }
                }
            }
        }

        item {
            BackupIntegrationCard(onNavigate, accentColor, scale)
        }
    }
}

@Composable
fun SummaryCard(data: PreResetGuideData, accentColor: Color, scale: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp * scale)) {
            Text("Safety Summary", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp * scale)
            Spacer(modifier = Modifier.height(8.dp * scale))
            Text("Accounts: ${data.accounts.size}", color = Color.White)
            Text("Critical Apps: ${data.apps.count { it.isTwoFactorApp }}", color = Color.White)
            Text("Manual Export Needed: ${data.apps.count { it.requiresManualExport }}", color = Color.White)
        }
    }
}

@Composable
fun AccountItem(account: AccountInfo, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp * scale))
        Spacer(modifier = Modifier.width(8.dp * scale))
        Column {
            Text(account.name, color = Color.White, fontSize = 14.sp * scale)
            Text(account.type, color = Color.Gray, fontSize = 12.sp * scale)
        }
    }
}

@Composable
fun AppItem(app: AppSafetyInfo, status: SafetyStatus, scale: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp * scale)
            .background(status.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, status.color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Apps, contentDescription = null, tint = status.color, modifier = Modifier.size(24.dp * scale))
        Spacer(modifier = Modifier.width(12.dp * scale))
        Column {
            Text(app.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp * scale)
            Text(app.packageName, color = Color.Gray, fontSize = 11.sp * scale)
            if (app.requiresManualExport) {
                Text("Manual Export Recommended", color = status.color, fontSize = 11.sp * scale, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    scale: Float,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp * scale)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp * scale))
                Spacer(modifier = Modifier.width(12.dp * scale))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.White
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp * scale)) {
                content()
            }
        }
    }
}

@Composable
fun ExportButton(onClick: () -> Unit, accentColor: Color, scale: Float) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp * scale))
        Text("Export Safety Guide (PDF)", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BackupIntegrationCard(onNavigate: (String) -> Unit, accentColor: Color, scale: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp * scale)) {
            Text("Next Steps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp * scale)
            Spacer(modifier = Modifier.height(12.dp * scale))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                Button(
                    onClick = { onNavigate(Screen.Backup.route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Full Backup", color = Color.Black)
                }
                Button(
                    onClick = { onNavigate(Screen.Restore.route) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Text("Test Restore", color = Color.White)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class PreResetViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PreResetViewModel(PreResetScanner(context), PreResetPdfGenerator(context)) as T
    }
}
