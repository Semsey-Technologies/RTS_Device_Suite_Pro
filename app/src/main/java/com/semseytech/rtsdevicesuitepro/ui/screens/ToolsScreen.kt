package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onNavigate: (String) -> Unit) {
    val currentTheme = LocalTheme.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TOOLS & UTILITIES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = currentTheme.accentColor
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        Box(
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
            val tools = listOf(
                ToolItem("Log Exporter", "Generate secure bug reports", Icons.Default.Terminal, Screen.LogExporter.route, Color(0xFF00D1FF)),
                ToolItem("System Config", "Advanced device settings", Icons.Default.Settings, Screen.Network.route, Color(0xFF00FF99)),
                ToolItem("Command Terminal", "Direct shell access", Icons.Default.Terminal, "terminal", Color(0xFFB58900)),
                ToolItem("Resource Monitor", "Real-time hardware stats", Icons.Outlined.Analytics, Screen.ResourceMonitor.route, Color(0xFFA020F0)),
                ToolItem("Smart Organizer", "Automated file management", Icons.Outlined.AutoFixHigh, Screen.SmartOrganizer.route, Color(0xFF00FF99)),
                ToolItem("Auto-Optimizer", "Automated system tuning", Icons.Outlined.AutoFixHigh, Screen.Automation.route, Color(0xFFFF0033))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tools) { tool ->
                    ToolCard(tool, currentTheme.endColor) { onNavigate(tool.route) }
                }
            }
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, backgroundColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
            .border(1.dp, tool.accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tool.accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, tool.accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.accentColor, modifier = Modifier.size(24.dp))
            }
            
            Column {
                Text(
                    tool.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        lineHeight = 14.sp
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

data class ToolItem(val name: String, val description: String, val icon: ImageVector, val route: String, val accentColor: Color)
