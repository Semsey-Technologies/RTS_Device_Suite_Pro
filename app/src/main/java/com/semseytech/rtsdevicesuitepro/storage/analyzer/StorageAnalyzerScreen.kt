package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.Manifest
import android.os.Build
import com.semseytech.rtsdevicesuitepro.ui.components.FileDisplaySettings
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionGate

@Composable
fun StorageAnalyzerScreen(
    viewModel: StorageAnalyzerViewModel,
    onNavigateToCategory: (FileCategory) -> Unit,
    onBack: () -> Unit
) {
    val storagePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf("android.permission.MANAGE_EXTERNAL_STORAGE")
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    PermissionGate(
        permissions = storagePermissions,
        onPermissionGranted = { viewModel.runFullStorageScan() },
        onBack = onBack
    ) {
        val uiState by viewModel.uiState.collectAsState()
        val isSelectionMode by viewModel.isSelectionMode.collectAsState()
        val selectedFiles by viewModel.selectedFiles.collectAsState()
        val theme = LocalTheme.current
        
        val displaySettingsMap by viewModel.displaySettingsMap.collectAsState()
        val settings = displaySettingsMap["DASHBOARD"] ?: FileDisplaySettings()
        
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
            containerColor = theme.startColor,
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
                    displaySettings = settings,
                    onSettingsChanged = { viewModel.updateSettingsForScope("DASHBOARD") { _ -> it } },
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
}
