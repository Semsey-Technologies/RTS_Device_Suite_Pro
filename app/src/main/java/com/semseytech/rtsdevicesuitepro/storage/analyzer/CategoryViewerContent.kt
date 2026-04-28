package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CategoryViewerContent(
    padding: PaddingValues,
    viewMode: ViewMode,
    groupedFiles: Map<String, List<FileInfo>>,
    collapsedGroups: Map<String, Boolean>,
    selectedFiles: Set<String>,
    isSelectionMode: Boolean,
    filesEmpty: Boolean,
    onToggleGroup: (String) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onEnterSelectionMode: (String) -> Unit,
    onFileClick: (FileInfo) -> Unit
) {
    StorageAnalyzerBackground {
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (viewMode == ViewMode.LIST) {
                CategoryViewerListContent(
                    groupedFiles, collapsedGroups, selectedFiles, isSelectionMode,
                    filesEmpty, onToggleGroup, onToggleFileSelection, onEnterSelectionMode, onFileClick
                )
            } else {
                CategoryViewerGridContent(
                    viewMode, groupedFiles, collapsedGroups, selectedFiles, isSelectionMode,
                    onToggleGroup, onToggleFileSelection, onEnterSelectionMode, onFileClick
                )
            }
        }
    }
}
