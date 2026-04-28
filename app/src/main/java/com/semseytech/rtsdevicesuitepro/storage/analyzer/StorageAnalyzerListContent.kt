package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StorageAnalyzerListContent(
    uiState: StorageStats,
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
    onFileClick: (FileInfo) -> Unit,
    onRunScan: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        item { SummaryCard(uiState) }
        item {
            SectionHeader(
                title = "CATEGORIES",
                isCollapsible = true,
                isCollapsed = categoriesCollapsed,
                onToggle = onToggleCategories
            )
        }
        if (!categoriesCollapsed) {
            items(uiState.categoryStats.toList()) { (category, info) ->
                CategoryRow(category, info) { onNavigateToCategory(category) }
            }
        }
        item {
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
                item { GroupHeader(header, isCollapsed) { onToggleGroup(header) } }
            }
            if (!isCollapsed) {
                items(groupFiles) { file ->
                    FileRow(
                        file = file,
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
