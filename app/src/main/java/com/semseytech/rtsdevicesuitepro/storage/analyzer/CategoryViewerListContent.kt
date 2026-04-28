package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun CategoryViewerListContent(
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        groupedFiles.forEach { (header, groupFiles) ->
            val isCollapsed = collapsedGroups[header] ?: false
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
        if (filesEmpty) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("NO FILES FOUND", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
