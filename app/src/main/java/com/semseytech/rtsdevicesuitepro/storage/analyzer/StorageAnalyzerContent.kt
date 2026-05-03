package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.semseytech.rtsdevicesuitepro.ui.components.FileViewMode

@Composable
fun StorageAnalyzerContent(
    padding: PaddingValues,
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
    onFileClick: (FileInfo) -> Unit,
    onRunScan: () -> Unit
) {
    StorageAnalyzerBackground {
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (uiState.isScanning) {
                ScanningOverlay(uiState.scanProgress)
            } else {
                if (viewMode == FileViewMode.LIST) {
                    StorageAnalyzerListContent(
                        uiState, groupedLargestFiles, collapsedGroups, categoriesCollapsed, onToggleCategories, selectedFiles,
                        isSelectionMode, onNavigateToCategory, onToggleGroup,
                        onToggleFileSelection, onEnterSelectionMode, onFileClick, onRunScan
                    )
                } else {
                    StorageAnalyzerGridContent(
                        uiState, viewMode, groupedLargestFiles, collapsedGroups, categoriesCollapsed, onToggleCategories, selectedFiles,
                        isSelectionMode, onNavigateToCategory, onToggleGroup,
                        onToggleFileSelection, onEnterSelectionMode, onFileClick
                    )
                }
            }
        }
    }
}
