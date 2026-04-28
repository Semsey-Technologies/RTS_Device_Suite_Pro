package com.semseytech.rtsdevicesuitepro.tools.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExporterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentTheme = LocalTheme.current
    var logFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isGenerating = true
        logFile = LogExporter.generateLogMarkdown(context)
        isGenerating = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LOG EXPORTER",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = currentTheme.accentColor)
                    Text("Generating secure log...", color = Color.White, modifier = Modifier.padding(top = 16.dp))
                } else {
                    logFile?.let { file ->
                        Text(
                            "Secure log generated successfully. No personal data has been collected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        LogActionCard(
                            title = "Share / Upload",
                            description = "Send log via any app or upload to cloud",
                            icon = Icons.Default.Share,
                            color = currentTheme.accentColor,
                            onClick = { LogExporter.shareLog(context, file) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LogActionCard(
                            title = "Email Developer",
                            description = "Send directly to semseytechnologies@proton.me",
                            icon = Icons.Default.Email,
                            color = Color(0xFF00FF99),
                            onClick = { LogExporter.emailDeveloper(context, file) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LogActionCard(
                            title = "Save Locally",
                            description = "Choose a location to save the .md file",
                            icon = Icons.Default.Save,
                            color = Color(0xFFB58900),
                            onClick = { LogExporter.shareLog(context, file) } // Reuse share for file picker/save
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            "PREVIEW (Markdown)",
                            style = MaterialTheme.typography.labelLarge,
                            color = currentTheme.accentColor,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = file.readText().take(1000) + "...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogActionCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
