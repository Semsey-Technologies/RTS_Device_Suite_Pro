package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeCategory
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePreset
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemePresets
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeEngineScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "THEME ENGINE",
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(ThemePresets) { category ->
                    ThemeCategorySection(category)
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ThemeCategorySection(category: ThemeCategory) {
    var expanded by remember { mutableStateOf(false) }
    val firstPreset = category.presets.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.02f))
    ) {
        // Category Title Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .background(
                    Brush.linearGradient(
                        colors = listOf(firstPreset.startColor, firstPreset.endColor)
                    )
                )
                .border(
                    width = 1.dp,
                    color = firstPreset.accentColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = firstPreset.accentColor,
                            letterSpacing = 1.sp,
                            shadow = Shadow(
                                color = firstPreset.accentColor.copy(alpha = 0.5f),
                                blurRadius = 8f
                            )
                        )
                    )
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, firstPreset.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand/Collapse",
                        tint = firstPreset.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                category.presets.forEach { preset ->
                    ThemePresetCard(preset)
                }
            }
        }
    }
}

@Composable
fun ThemePresetCard(preset: ThemePreset) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = preset.accentColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = if (preset.isAnimated) preset.animatedColors else listOf(preset.startColor, preset.endColor)
                    )
                )
                .padding(16.dp)
        ) {
            if (preset.hasGridOverlay) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val gridSize = 20.dp.toPx()
                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = preset.accentColor.copy(alpha = 0.05f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 0.5f
                        )
                    }
                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = preset.accentColor.copy(alpha = 0.05f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 0.5f
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = preset.textColor
                        )
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .height(2.dp)
                            .width(40.dp)
                            .background(preset.accentColor)
                    )
                }

                Button(
                    onClick = { ThemeManager.applyTheme(preset) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = preset.accentColor.copy(alpha = 0.2f),
                        contentColor = preset.accentColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, preset.accentColor.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("APPLY", fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
