package com.semseytech.rtsdevicesuitepro.storage.analyzer

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

@Composable
fun StorageAnalyzerMenus(
    showSortMenu: Boolean, onSortMenuToggle: (Boolean) -> Unit,
    showViewMenu: Boolean, onViewMenuToggle: (Boolean) -> Unit,
    showGroupMenu: Boolean, onGroupMenuToggle: (Boolean) -> Unit,
    viewMode: ViewMode,
    sortOption: SortOption,
    sortOrder: SortOrder,
    onSortOptionSelected: (SortOption) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit,
    onGroupBySelected: (GroupByOption) -> Unit
) {
    Box {
        IconButton(onClick = { onSortMenuToggle(true) }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = Color.White) }
        DropdownMenu(expanded = showSortMenu, onDismissRequest = { onSortMenuToggle(false) }, containerColor = DeepDark) {
            Text("SORT BY", color = NeonBlue, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = if (sortOption == option) NeonBlue else Color.White, fontFamily = FontFamily.Monospace) },
                    onClick = { onSortOptionSelected(option); onSortMenuToggle(false) }
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Text("ORDER", color = NeonBlue, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
            SortOrder.values().forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (sortOrder == order) NeonBlue else Color.White, fontFamily = FontFamily.Monospace) },
                    onClick = { onSortOrderSelected(order); onSortMenuToggle(false) }
                )
            }
        }
    }
    Box {
        IconButton(onClick = { onViewMenuToggle(true) }) { Icon(if (viewMode == ViewMode.LIST) Icons.Default.ViewList else Icons.Default.GridView, "View", tint = Color.White) }
        DropdownMenu(expanded = showViewMenu, onDismissRequest = { onViewMenuToggle(false) }, containerColor = DeepDark) {
            ViewMode.values().forEach { mode ->
                val label = when(mode) {
                    ViewMode.LIST -> "List"
                    ViewMode.GRID_SMALL -> "Small Icons"
                    ViewMode.GRID_MEDIUM -> "Medium Icons"
                    ViewMode.GRID_LARGE -> "Large Icons"
                }
                DropdownMenuItem(
                    text = { Text(label, color = if (viewMode == mode) NeonBlue else Color.White, fontFamily = FontFamily.Monospace) },
                    onClick = { onViewModeSelected(mode); onViewMenuToggle(false) }
                )
            }
        }
    }
    Box {
        IconButton(onClick = { onGroupMenuToggle(true) }) { Icon(Icons.Default.Groups, "Group", tint = Color.White) }
        DropdownMenu(expanded = showGroupMenu, onDismissRequest = { onGroupMenuToggle(false) }, containerColor = DeepDark) {
            GroupByOption.values().forEach { option ->
                val label = when(option) {
                    GroupByOption.NONE -> "None"
                    else -> "By " + option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }
                DropdownMenuItem(
                    text = { Text(label, color = Color.White, fontFamily = FontFamily.Monospace) },
                    onClick = { onGroupBySelected(option); onGroupMenuToggle(false) }
                )
            }
        }
    }
}
