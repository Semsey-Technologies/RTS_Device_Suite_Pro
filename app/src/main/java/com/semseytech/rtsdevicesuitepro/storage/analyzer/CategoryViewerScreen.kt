package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun CategoryViewerScreen(
    category: FileCategory,
    viewModel: StorageAnalyzerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    
    val displaySettingsMap by viewModel.displaySettingsMap.collectAsState()
    val settings = displaySettingsMap[category.name] ?: DisplaySettings()
    
    val sortOption = settings.sortOption
    val sortOrder = settings.sortOrder
    val viewMode = settings.viewMode
    val groupBy = settings.groupBy

    val dialogState = rememberStorageAnalyzerDialogState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>() }

    val files = remember(uiState, category, settings) {
        viewModel.getFilesByCategory(category)
    }
    val groupedFiles = remember(files, groupBy) {
        viewModel.groupFiles(files, groupBy)
    }

    Scaffold(
        containerColor = DeepDark,
        topBar = {
            CategoryViewerTopBar(
                category = category,
                isSelectionMode = isSelectionMode,
                selectedCount = selectedFiles.size,
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it },
                showViewMenu = showViewMenu,
                onViewMenuToggle = { showViewMenu = it },
                showGroupMenu = showGroupMenu,
                onGroupMenuToggle = { showGroupMenu = it },
                viewMode = viewMode,
                sortOption = sortOption,
                sortOrder = sortOrder,
                onSortOptionSelected = { viewModel.setSortOption(it, category.name) },
                onSortOrderSelected = { viewModel.setSortOrder(it, category.name) },
                onViewModeSelected = { viewModel.setViewMode(it, category.name) },
                onGroupBySelected = { viewModel.setGroupBy(it, category.name) },
                onSelectAll = { viewModel.selectAll(files) },
                onDeselectAll = { viewModel.deselectAll() },
                onMoveSelected = { dialogState.showMultiMoveCopyDialog = true },
                onCopySelected = { dialogState.showMultiMoveCopyDialog = false },
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isSelectionMode) DeleteSelectedFAB(onDelete = { viewModel.deleteSelectedFiles() })
        }
    ) { padding ->
        CategoryViewerContent(
            padding = padding,
            viewMode = viewMode,
            groupedFiles = groupedFiles,
            collapsedGroups = collapsedGroups,
            selectedFiles = selectedFiles,
            isSelectionMode = isSelectionMode,
            filesEmpty = files.isEmpty(),
            onToggleGroup = { collapsedGroups[it] = !(collapsedGroups[it] ?: false) },
            onToggleFileSelection = { viewModel.toggleFileSelection(it) },
            onEnterSelectionMode = { viewModel.enterSelectionMode(it) },
            onFileClick = { file ->
                if (isSelectionMode) viewModel.toggleFileSelection(file.path)
                else dialogState.showFileOptions = file
            }
        )
    }

    StorageAnalyzerDialogWrapper(dialogState, viewModel)
}
