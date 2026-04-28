package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager

// Mockup Accurate Colors
val NeonBlue = Color(0xFF00BFFF)
val NeonGreen = Color(0xFF00FF99)
val AppBackground = Color(0xFF0A0F1A)
val SecondaryBackground = Color(0xFF0D1422)
val CardBackground = Color(0xFF111A2C)
val CardElevated = Color(0xFF162033)
val LightGray = Color(0xFF94A3B8)

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        currentTheme.startColor,
                        currentTheme.endColor
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, 0f) // 90 degrees (Horizontal)
                )
            )
            .drawBehind {
                val gridSize = (40.dp * scale).toPx()
                // Horizontal Grid
                for (y in 0..size.height.toInt() step gridSize.toInt()) {
                    drawLine(
                        color = currentTheme.accentColor.copy(alpha = 0.1f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
                // Vertical Grid
                for (x in 0..size.width.toInt() step gridSize.toInt()) {
                    drawLine(
                        color = currentTheme.accentColor.copy(alpha = 0.1f),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = 1f
                    )
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                BannerHeader(currentTheme.name)

                Column(modifier = Modifier.padding(horizontal = (16.dp * scale))) {
                    SystemStatusDivider()
                    StorageStatusCard(uiState, onNavigate)

                    Spacer(modifier = Modifier.height(24.dp * scale))

                    // 1. SYSTEM MODULES SECTION
                    SectionHeaderLabel("SYSTEM MODULES", currentTheme.accentColor)
                    SystemModulesGrid(onNavigate, currentTheme.accentColor)

                    Spacer(modifier = Modifier.height(24.dp * scale))

                    // 2. QUICK ACTIONS SECTION
                    SectionHeaderLabel("QUICK ACTIONS", currentTheme.accentColor)
                    QuickActionsRow(onNavigate, currentTheme.accentColor)

                    Spacer(modifier = Modifier.height(24.dp * scale))

                    // 3. TOOLS / EXTRA MODULES SECTION
                    SectionHeaderLabel("TOOLS", currentTheme.accentColor)
                    ToolsGrid(onNavigate, currentTheme.accentColor)

                    Spacer(modifier = Modifier.height(24.dp * scale))
                    
                    AutoTasksCard(uiState, currentTheme.accentColor)
                    Spacer(modifier = Modifier.height(12.dp * scale))
                    SmartOrganizerCard(currentTheme.accentColor)
                    Spacer(modifier = Modifier.height(24.dp * scale))
                }
            }

            // Bottom Navigation Bar is now handled in MainActivity's Scaffold
        }
    }
}

@Composable
fun SystemModulesGrid(onNavigate: (String) -> Unit, accentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val modules = listOf(
                ModuleItem(Screen.ViewBackups, Icons.Outlined.Public, "View Backups", accentColor),
                ModuleItem(Screen.Recovery, Icons.Outlined.History, "Restore Data", accentColor),
                ModuleItem(Screen.Recovery, Icons.Outlined.ManageSearch, "Deep Recovery", accentColor),
                ModuleItem(Screen.Cleaner, Icons.Outlined.Brush, "File Cleaner", accentColor),
                ModuleItem(Screen.Archive, Icons.Outlined.CloudSync, "NAS / SMB Sync", accentColor),
                ModuleItem(Screen.StorageAnalyzer, Icons.Outlined.BarChart, "Storage Analyzer", accentColor),
                ModuleItem(Screen.Network, Icons.Outlined.SignalCellularAlt, "Net Optimizer", accentColor)
            )

            modules.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { item ->
                        CompactModuleTile(item) { onNavigate(item.screen.route) }
                    }
                }
                if (modules.indexOf(row.last()) < modules.size - 1) {
                   Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ToolsGrid(onNavigate: (String) -> Unit, accentColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryBackground.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompactModuleTile(ModuleItem(Screen.PreReset, Icons.AutoMirrored.Outlined.FactCheck, "Pre-Reset", accentColor)) { onNavigate(Screen.PreReset.route) }
            CompactModuleTile(ModuleItem(Screen.Automation, Icons.Outlined.Schedule, "Snapshots", accentColor)) { onNavigate(Screen.Automation.route) }
            CompactModuleTile(ModuleItem(Screen.Config, Icons.Outlined.Settings, "Config", Color.Gray)) { onNavigate(Screen.Config.route) }
        }
    }
}

@Composable
fun CompactModuleTile(item: ModuleItem, onClick: () -> Unit) {
    val scale = ThemeManager.uiScale
    Column(
        modifier = Modifier
            .width(75.dp * scale)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(45.dp * scale)
                .background(SecondaryBackground, RoundedCornerShape(8.dp))
                .border(1.dp, item.accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, null, tint = item.accentColor, modifier = Modifier.size(22.dp * scale))
        }
        Spacer(modifier = Modifier.height(6.dp * scale))
        Text(
            item.displayName, 
            color = Color.White, 
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center, 
            lineHeight = 10.sp * scale
        )
    }
}

@Composable
fun QuickActionsRow(onNavigate: (String) -> Unit, accentColor: Color) {
    val scale = ThemeManager.uiScale
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp * scale)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickOrb("Deep Recovery Scan", Icons.Default.History, accentColor) { onNavigate(Screen.Recovery.route) }
            QuickOrb("Unused Apps", Icons.Default.Apps, accentColor) { onNavigate(Screen.Cleaner.route) }
            QuickOrb("Extract Archives", Icons.Default.FolderZip, accentColor) { onNavigate(Screen.Archive.route) }
        }
    }
}

@Composable
fun QuickOrb(label: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit) {
    val scale = ThemeManager.uiScale
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(40.dp * scale)
                .background(
                    Brush.radialGradient(listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)),
                    CircleShape
                )
                .border(1.dp, accentColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp * scale))
        }
        Text(label, color = LightGray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp * scale))
    }
}

@Composable
fun AutoTasksCard(uiState: DashboardUiState, accentColor: Color) {
    val scale = ThemeManager.uiScale
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp * scale)) {
            Text("Auto Tasks", color = accentColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp * scale), color = Color.White.copy(alpha = 0.1f))
            ToggleRow("Daily Backup", true, accentColor)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp * scale), color = Color.White.copy(alpha = 0.1f))
            ToggleRow("Auto Clean", uiState.isAutoCleanEnabled, accentColor)
        }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, accentColor: Color) {
    val scale = ThemeManager.uiScale
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp * scale),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked, 
            onCheckedChange = {}, 
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor, 
                checkedTrackColor = accentColor.copy(alpha = 0.3f),
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
            ),
            modifier = Modifier.scale(scale.coerceIn(0.8f, 1.5f))
        )
    }
}

@Composable
fun SmartOrganizerCard(accentColor: Color) {
    val scale = ThemeManager.uiScale
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp * scale)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart File Organizer", color = accentColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = CardElevated),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp * scale)
                ) {
                    Text("Manage", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp * scale), color = Color.White.copy(alpha = 0.1f))
            Text("Downloads Sorted | NAS Sync Active", color = LightGray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SectionHeaderLabel(title: String, accentColor: Color) {
    val scale = ThemeManager.uiScale
    Text(
        text = title,
        color = accentColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp * scale, bottom = 4.dp * scale),
        letterSpacing = 1.sp
    )
}

@Composable
fun BannerHeader(themeName: String) {
    val context = LocalContext.current
    val bannerPath = remember(themeName) {
        val extensions = listOf("png", "jpeg", "jpg", "webp")
        var foundPath: String? = null
        for (ext in extensions) {
            val path = "banner/$themeName.$ext"
            try {
                context.assets.open(path).close()
                foundPath = "file:///android_asset/$path"
                break
            } catch (e: Exception) {
                // Continue searching
            }
        }
        foundPath
    }

    if (bannerPath != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bannerPath)
                .crossfade(true)
                .build(),
            contentDescription = "Theme Banner",
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
            contentScale = ContentScale.FillWidth
        )
    } else {
        // Fallback to default banner
        Image(
            painter = painterResource(id = com.semseytech.rtsdevicesuitepro.R.drawable.banner),
            contentDescription = "Default Banner",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun SystemStatusDivider() {
    val scale = ThemeManager.uiScale
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
        Text(
            "System Status", 
            color = Color.White, 
            style = MaterialTheme.typography.bodyMedium, 
            modifier = Modifier.padding(horizontal = 16.dp * scale),
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
    }
}

@Composable
fun StorageStatusCard(uiState: DashboardUiState, onNavigate: (String) -> Unit) {
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp * scale)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(85.dp * scale)) {
                CircularProgressIndicator(
                    progress = { uiState.storageUsedPercent },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp * scale,
                    color = currentTheme.accentColor,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Butt
                )
                Text(
                    "${(uiState.storageUsedPercent * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(20.dp * scale))
            Column {
                Text("93.4 GB Free / 256 GB", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Last Backup: Today, 10:15 AM", color = LightGray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp * scale))
                Button(
                    onClick = { onNavigate(Screen.Backup.route) },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp * scale),
                    contentPadding = PaddingValues(horizontal = 24.dp * scale, vertical = 0.dp)
                ) {
                    Text("Create Backup", color = Color.Black, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class ModuleItem(val screen: Screen, val icon: ImageVector, val displayName: String, val accentColor: Color)
