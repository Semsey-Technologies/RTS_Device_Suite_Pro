package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.ui.components.FileViewMode

@Composable
fun StorageAnalyzerGridContent(
    uiState: StorageStats,
    viewMode: FileViewMode,
    groupedLargestFiles: Map<String, List<FileInfo>>,
    collapsedGroups: Map<String, Boolean>,
    categoriesCollapsed: Boolean,
    onToggleCategories: () -> Unit,
    selectedFiles: Set<String>,
    isSelectionMode: Boolean,
    onNavigateToCategory: (FileCategory) -> Unit,
    onToggleGroup: (String) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onEnterSelectionMode: (String) -> Unit,
    onFileClick: (FileInfo) -> Unit
) {
    val columnCount = when (viewMode) {
        FileViewMode.GRID_SMALL -> 5
        FileViewMode.GRID_MEDIUM -> 3
        FileViewMode.GRID_LARGE -> 2
        else -> 3
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item(span = { GridItemSpan(columnCount) }) { SummaryCard(uiState) }
        item(span = { GridItemSpan(columnCount) }) {
            SectionHeader(
                title = "CATEGORIES",
                isCollapsible = true,
                isCollapsed = categoriesCollapsed,
                onToggle = onToggleCategories
            )
        }

        if (!categoriesCollapsed) {
            items(
                uiState.categoryStats.toList(),
                span = { GridItemSpan(if (columnCount == 2) 2 else columnCount) }
            ) { (category, info) ->
                CategoryRow(category, info) { onNavigateToCategory(category) }
            }
        }

        item(span = { GridItemSpan(columnCount) }) {
            SectionHeader(
                title = "LARGEST FILES DETECTED",
                isCollapsible = true,
                isCollapsed = collapsedGroups[""] ?: true,
                onToggle = { onToggleGroup("") }
            )
        }
        groupedLargestFiles.forEach { (header, groupFiles) ->
            val isCollapsed = collapsedGroups[header] ?: header.isNotEmpty()
            if (header.isNotEmpty()) {
                item(span = { GridItemSpan(columnCount) }) {
                    GroupHeader(header, isCollapsed) { onToggleGroup(header) }
                }
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
