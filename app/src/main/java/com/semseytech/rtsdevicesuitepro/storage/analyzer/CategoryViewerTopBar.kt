package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

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
    viewMode: ViewMode,
    onSortOptionSelected: (SortOption) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
    onViewModeSelected: (ViewMode) -> Unit,
    onGroupBySelected: (GroupByOption) -> Unit,
    sortOption: SortOption,
    sortOrder: SortOrder,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onMoveSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isSelectionMode) "> SELECT MODE: $selectedCount" else "> ${category.name}",
                color = NeonBlue,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                style = TextStyle(shadow = Shadow(color = NeonBlue.copy(alpha = 0.5f), blurRadius = 8f))
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onMoveSelected) { Icon(Icons.Default.DriveFileMove, "Move Selected", tint = NeonGreen) }
                IconButton(onClick = onCopySelected) { Icon(Icons.Default.ContentCopy, "Copy Selected", tint = NeonGreen) }
                IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, "Select All", tint = NeonGreen) }
                IconButton(onClick = onDeselectAll) { Icon(Icons.Default.Deselect, "Deselect All", tint = Color.Gray) }
            } else {
                StorageAnalyzerMenus(
                    showSortMenu = showSortMenu,
                    onSortMenuToggle = onSortMenuToggle,
                    showViewMenu = showViewMenu,
                    onViewMenuToggle = onViewMenuToggle,
                    showGroupMenu = showGroupMenu,
                    onGroupMenuToggle = onGroupMenuToggle,
                    viewMode = viewMode,
                    sortOption = sortOption,
                    sortOrder = sortOrder,
                    onSortOptionSelected = onSortOptionSelected,
                    onSortOrderSelected = onSortOrderSelected,
                    onViewModeSelected = onViewModeSelected,
                    onGroupBySelected = onGroupBySelected
                )
            }
        }
    )
}
