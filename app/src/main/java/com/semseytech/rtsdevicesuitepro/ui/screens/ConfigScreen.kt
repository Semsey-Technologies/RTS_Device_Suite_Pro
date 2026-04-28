package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
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
import com.semseytech.rtsdevicesuitepro.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    var activeTab by remember { mutableIntStateOf(0) }

    Scaffold(
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
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> ThemeEngineList()
                    1 -> ScalingSettings()
                    2 -> TypographySettings()
                }
            }
        }
    }
}

@Composable
fun ThemeEngineList() {
    val currentTheme = LocalTheme.current
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ThemePresets.size) { index ->
            ThemeCategorySection(ThemePresets[index])
        }
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