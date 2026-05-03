package com.semseytech.rtsdevicesuitepro.recovery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.storage.analyzer.FileCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecoveryViewModel(application: Application, private val repository: RecoveryRepository) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecoveryState())
    val uiState: StateFlow<RecoveryState> = _uiState.asStateFlow()

    private var allItems: List<RecoverableItem> = emptyList()

    fun startScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = 0f) }
            repository.performDeepScan().collect { update ->
                when (update) {
                    is RecoveryScanUpdate.Progress -> {
                        _uiState.update { it.copy(scanProgress = update.progress) }
                    }
                    is RecoveryScanUpdate.Finished -> {
                        allItems = update.items
                        processItems()
                        _uiState.update { it.copy(isScanning = false, itemsFound = allItems.size) }
                    }
                }
            }
        }
    }

    private fun processItems() {
        val filtered = allItems.filter { 
            it.name.contains(_uiState.value.searchQuery, ignoreCase = true) 
        }

        val sorted = when (_uiState.value.sortOption) {
            RecoverySortOption.NAME -> filtered.sortedBy { it.name }
            RecoverySortOption.SIZE -> filtered.sortedBy { it.size }
            RecoverySortOption.DATE_CREATED -> filtered.sortedBy { it.lastModified }
            RecoverySortOption.DATE_DELETED -> filtered.sortedBy { it.dateDeleted ?: it.lastModified }
            RecoverySortOption.FILE_TYPE -> filtered.sortedBy { it.category.name }
            RecoverySortOption.APP_SOURCE -> filtered.sortedBy { it.sourceApp ?: "" }
        }.let { if (_uiState.value.isDescending) it.reversed() else it }

        val categorized = FileCategory.values().map { cat ->
            val items = sorted.filter { it.category == cat }
            RecoveryCategory(
                category = cat,
                items = items,
                isExpanded = _uiState.value.categories.find { it.category == cat }?.isExpanded ?: false
            )
        }.filter { it.items.isNotEmpty() }

        _uiState.update { it.copy(categories = categorized) }
    }

    fun toggleCategoryExpansion(category: FileCategory) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { 
                if (it.category == category) it.copy(isExpanded = !it.isExpanded) else it 
            })
        }
    }

    fun toggleItemSelection(itemPath: String) {
        allItems = allItems.map { 
            if (it.path == itemPath) it.copy(isSelected = !it.isSelected) else it 
        }
        processItems()
    }

    fun selectAllInCategory(category: FileCategory, selected: Boolean) {
        allItems = allItems.map { 
            if (it.category == category) it.copy(isSelected = selected) else it 
        }
        processItems()
    }

    fun selectAll(selected: Boolean) {
        allItems = allItems.map { it.copy(isSelected = selected) }
        processItems()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        processItems()
    }

    fun updateSortOption(option: RecoverySortOption) {
        _uiState.update { it.copy(sortOption = option) }
        processItems()
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(isDescending = !it.isDescending) }
        processItems()
    }

    fun updateViewMode(mode: RecoveryViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun openItem(item: RecoverableItem) {
        repository.openItem(item)
    }

    fun recoverSelectedItems(destinationFolder: Uri) {
        viewModelScope.launch {
            val selected = allItems.filter { it.isSelected }
            selected.forEach { item ->
                repository.recoverItem(item, destinationFolder)
            }
            selectAll(false)
        }
    }

    fun permanentlyDeleteSelected() {
        viewModelScope.launch {
            val selected = allItems.filter { it.isSelected }
            selected.forEach { item ->
                repository.permanentlyDelete(item)
            }
            allItems = allItems.filter { !it.isSelected }
            processItems()
        }
    }
}
