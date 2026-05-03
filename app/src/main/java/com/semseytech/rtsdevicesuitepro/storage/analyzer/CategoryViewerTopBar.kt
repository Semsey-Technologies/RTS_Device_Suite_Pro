package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.components.FileDisplaySettings
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewerTopBar(
    category: FileCategory,
    isSelectionMode: Boolean,
    selectedCount: Int,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    showViewMenu: Boolean,
    onViewMenuToggle: (Boolean) -> Unit,
    showGroupMenu: Boolean,
    onGroupMenuToggle: (Boolean) -> Unit,
    displaySettings: FileDisplaySettings,
    onSettingsChanged: (FileDisplaySettings) -> Unit,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onBack: () -> Unit
) {
    val currentTheme = LocalTheme.current
    TopAppBar(
        title = { 
            Text(
                text = if (isSelectionMode) "> SELECT MODE: $selectedCount" else "> ${category.name}", 
                color = currentTheme.accentColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                style = TextStyle(shadow = Shadow(color = currentTheme.accentColor.copy(alpha = 0.5f), blurRadius = 8f))
            ) 
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = currentTheme.startColor.copy(alpha = 0.8f)),
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onExitSelection) {
                    Icon(Icons.Default.Close, "Cancel", tint = currentTheme.textColor)
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = currentTheme.textColor)
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select All", tint = currentTheme.accentColor) }
                IconButton(onClick = onDeselectAll) { Icon(Icons.Default.Deselect, "Deselect All", tint = currentTheme.textColor.copy(alpha = 0.5f)) }
            } else {
                StorageAnalyzerMenus(
                    showSortMenu = showSortMenu,
                    onSortMenuToggle = onSortMenuToggle,
                    showViewMenu = showViewMenu,
                    onViewMenuToggle = onViewMenuToggle,
                    showGroupMenu = showGroupMenu,
                    onGroupMenuToggle = onGroupMenuToggle,
                    displaySettings = displaySettings,
                    onSettingsChanged = onSettingsChanged
                )
            }
        }
    )
}
