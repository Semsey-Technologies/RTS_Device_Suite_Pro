package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun StorageAnalyzerScreen(
    viewModel: StorageAnalyzerViewModel,
    onNavigateToCategory: (FileCategory) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    
    val displaySettingsMap by viewModel.displaySettingsMap.collectAsState()
    val settings = displaySettingsMap["DASHBOARD"] ?: DisplaySettings()
    
    val sortOption = settings.sortOption
    val sortOrder = settings.sortOrder
    val viewMode = settings.viewMode
    val groupBy = settings.groupBy
    
    val dialogState = rememberStorageAnalyzerDialogState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

    val largestFiles = remember(uiState.largestFiles, sortOption, sortOrder) {
        viewModel.sortFiles(uiState.largestFiles, sortOption, sortOrder)
    }
    val groupedLargestFiles = remember(largestFiles, groupBy) {
        viewModel.groupFiles(largestFiles, groupBy)
    }
    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>("" to true) }
    var categoriesCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.largestFiles.isEmpty() && !uiState.isScanning) viewModel.runFullStorageScan()
    }

    Scaffold(
        containerColor = DeepDark,
        topBar = {
            StorageAnalyzerTopBar(
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
                onSortOptionSelected = { viewModel.setSortOption(it, "DASHBOARD") },
                onSortOrderSelected = { viewModel.setSortOrder(it, "DASHBOARD") },
                onViewModeSelected = { viewModel.setViewMode(it, "DASHBOARD") },
                onGroupBySelected = { viewModel.setGroupBy(it, "DASHBOARD") },
                onRefresh = { viewModel.runFullStorageScan() },
                onExitSelection = { viewModel.exitSelectionMode() },
                onSelectAll = { viewModel.selectAll(uiState.largestFiles) },
                onDeselectAll = { viewModel.deselectAll() },
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isSelectionMode) DeleteSelectedFAB(onDelete = { viewModel.deleteSelectedFiles() })
        }
    ) { padding ->
        StorageAnalyzerContent(
            padding = padding,
            uiState = uiState,
            viewMode = viewMode,
            groupedLargestFiles = groupedLargestFiles,
            collapsedGroups = collapsedGroups,
            categoriesCollapsed = categoriesCollapsed,
            onToggleCategories = { categoriesCollapsed = !categoriesCollapsed },
            selectedFiles = selectedFiles,
            isSelectionMode = isSelectionMode,
            onNavigateToCategory = onNavigateToCategory,
            onToggleGroup = { header -> 
                val defaultCollapsed = header.isNotEmpty()
                collapsedGroups[header] = !(collapsedGroups[header] ?: defaultCollapsed)
            },
            onToggleFileSelection = { viewModel.toggleFileSelection(it) },
            onEnterSelectionMode = { viewModel.enterSelectionMode(it) },
            onFileClick = { file ->
                if (isSelectionMode) viewModel.toggleFileSelection(file.path)
                else dialogState.showFileOptions = file
            },
            onRunScan = { viewModel.runFullStorageScan() }
        )
    }

    StorageAnalyzerDialogWrapper(dialogState, viewModel)
}
