package com.semseytech.rtsdevicesuitepro.backup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.backup.model.SortType
import com.semseytech.rtsdevicesuitepro.backup.model.ViewMode
import com.semseytech.rtsdevicesuitepro.backup.model.GroupType

@Composable
fun SortingAndFilteringHeader(
    sortType: SortType,
    onSortSelected: (SortType) -> Unit,
    viewMode: ViewMode,
    onViewModeSelected: (ViewMode) -> Unit,
    groupType: GroupType,
    onGroupTypeSelected: (GroupType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        var showSortMenu by remember { mutableStateOf(false) }
        var showViewMenu by remember { mutableStateOf(false) }
        var showGroupMenu by remember { mutableStateOf(false) }

        // Sort Button
        Box {
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Sort")
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                SortType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onSortSelected(type)
                            showSortMenu = false
                        },
                        leadingIcon = { if (type == sortType) Icon(Icons.Default.Check, null) }
                    )
                }
            }
        }

        // View Mode Button
        Box {
            IconButton(onClick = { showViewMenu = true }) {
                Icon(Icons.Default.GridView, contentDescription = "View Mode")
            }
            DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                ViewMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name.replace("_", " ")) },
                        onClick = {
                            onViewModeSelected(mode)
                            showViewMenu = false
                        },
                        leadingIcon = { if (mode == viewMode) Icon(Icons.Default.Check, null) }
                    )
                }
            }
        }

        // Grouping Button
        Box {
            IconButton(onClick = { showGroupMenu = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Grouping")
            }
            DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                GroupType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onGroupTypeSelected(type)
                            showGroupMenu = false
                        },
                        leadingIcon = { if (type == groupType) Icon(Icons.Default.Check, null) }
                    )
                }
            }
        }
    }
}
