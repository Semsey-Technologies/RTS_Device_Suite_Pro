package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryViewerGridContent(
    viewMode: ViewMode,
    groupedFiles: Map<String, List<FileInfo>>,
    collapsedGroups: Map<String, Boolean>,
    selectedFiles: Set<String>,
    isSelectionMode: Boolean,
    onToggleGroup: (String) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onEnterSelectionMode: (String) -> Unit,
    onFileClick: (FileInfo) -> Unit
) {
    val columnCount = when (viewMode) {
        ViewMode.GRID_SMALL -> 5
        ViewMode.GRID_MEDIUM -> 3
        ViewMode.GRID_LARGE -> 2
        else -> 3
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        groupedFiles.forEach { (header, groupFiles) ->
            val isCollapsed = collapsedGroups[header] ?: false
            if (header.isNotEmpty()) {
                item(span = { GridItemSpan(columnCount) }) { GroupHeader(header, isCollapsed) { onToggleGroup(header) } }
            }
            if (!isCollapsed) {
                items(groupFiles) { file ->
                    FileGridItem(
                        file = file,
                        viewMode = viewMode,
                        isSelected = selectedFiles.contains(file.path),
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = { onToggleFileSelection(file.path) },
                        onLongPress = { onEnterSelectionMode(file.path) },
                        onClick = { onFileClick(file) }
                    )
                }
            }
        }
    }
}
