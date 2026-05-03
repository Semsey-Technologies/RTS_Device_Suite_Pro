package com.semseytech.rtsdevicesuitepro.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme

@Composable
fun SharedFileMenus(
    displaySettings: FileDisplaySettings,
    showSortMenu: Boolean, onSortMenuToggle: (Boolean) -> Unit,
    showViewMenu: Boolean, onViewMenuToggle: (Boolean) -> Unit,
    showGroupMenu: Boolean, onGroupMenuToggle: (Boolean) -> Unit,
    onSettingsChanged: (FileDisplaySettings) -> Unit
) {
    val currentTheme = LocalTheme.current
    val accentColor = currentTheme.accentColor
    val deepDark = Color(0xFF0D1321)

    Box {
        IconButton(onClick = { onSortMenuToggle(true) }) { 
            Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = Color.White) 
        }
        DropdownMenu(expanded = showSortMenu, onDismissRequest = { onSortMenuToggle(false) }, containerColor = deepDark) {
            Text("SORT BY", color = accentColor, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
            FileSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, 
                            color = if (displaySettings.sortOption == option) accentColor else Color.White, 
                            fontFamily = FontFamily.Monospace
                        ) 
                    },
                    onClick = { 
                        onSettingsChanged(displaySettings.copy(sortOption = option))
                        onSortMenuToggle(false) 
                    }
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Text("ORDER", color = accentColor, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
            FileSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            order.name.lowercase().replaceFirstChar { it.uppercase() }, 
                            color = if (displaySettings.sortOrder == order) accentColor else Color.White, 
                            fontFamily = FontFamily.Monospace
                        ) 
                    },
                    onClick = { 
                        onSettingsChanged(displaySettings.copy(sortOrder = order))
                        onSortMenuToggle(false) 
                    }
                )
            }
        }
    }
    
    Box {
        val viewIcon = if (displaySettings.viewMode == FileViewMode.LIST || displaySettings.viewMode == FileViewMode.DETAILS) 
            Icons.Default.ViewList else Icons.Default.GridView
            
        IconButton(onClick = { onViewMenuToggle(true) }) { 
            Icon(viewIcon, "View", tint = Color.White) 
        }
        DropdownMenu(expanded = showViewMenu, onDismissRequest = { onViewMenuToggle(false) }, containerColor = deepDark) {
            FileViewMode.entries.forEach { mode ->
                val label = mode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                DropdownMenuItem(
                    text = { 
                        Text(label, color = if (displaySettings.viewMode == mode) accentColor else Color.White, fontFamily = FontFamily.Monospace) 
                    },
                    onClick = { 
                        onSettingsChanged(displaySettings.copy(viewMode = mode))
                        onViewMenuToggle(false) 
                    }
                )
            }
        }
    }
    
    Box {
        IconButton(onClick = { onGroupMenuToggle(true) }) { 
            Icon(Icons.Default.Groups, "Group", tint = Color.White) 
        }
        DropdownMenu(expanded = showGroupMenu, onDismissRequest = { onGroupMenuToggle(false) }, containerColor = deepDark) {
            FileGroupByOption.entries.forEach { option ->
                val label = if (option == FileGroupByOption.NONE) "None" 
                            else "By " + option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                DropdownMenuItem(
                    text = { 
                        Text(label, color = if (displaySettings.groupBy == option) accentColor else Color.White, fontFamily = FontFamily.Monospace) 
                    },
                    onClick = { 
                        onSettingsChanged(displaySettings.copy(groupBy = option))
                        onGroupMenuToggle(false) 
                    }
                )
            }
        }
    }
}
