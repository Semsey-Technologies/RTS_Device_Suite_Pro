package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.semseytech.rtsdevicesuitepro.ui.components.*

class StorageAnalyzerViewModel(application: Application, private val repository: StorageAnalyzerRepository) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StorageStats())
    val uiState: StateFlow<StorageStats> = _uiState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _displaySettingsMap = MutableStateFlow<Map<String, FileDisplaySettings>>(
        mapOf("DASHBOARD" to FileDisplaySettings())
    )
    val displaySettingsMap: StateFlow<Map<String, FileDisplaySettings>> = _displaySettingsMap.asStateFlow()

    fun getSettingsForScope(scope: String): FileDisplaySettings {
        return _displaySettingsMap.value[scope] ?: FileDisplaySettings()
    }

    fun updateSettingsForScope(scope: String, update: (FileDisplaySettings) -> FileDisplaySettings) {
        _displaySettingsMap.update { current ->
            val existing = current[scope] ?: FileDisplaySettings()
            current + (scope to update(existing))
        }
    }

    // Legacy setters for compatibility during transition, can be removed after UI update
    fun setSortOption(option: FileSortOption, scope: String = "DASHBOARD") {
        updateSettingsForScope(scope) { it.copy(sortOption = option) }
    }

    fun setSortOrder(order: FileSortOrder, scope: String = "DASHBOARD") {
        updateSettingsForScope(scope) { it.copy(sortOrder = order) }
    }

    fun setViewMode(mode: FileViewMode, scope: String = "DASHBOARD") {
        updateSettingsForScope(scope) { it.copy(viewMode = mode) }
    }

    fun setGroupBy(option: FileGroupByOption, scope: String = "DASHBOARD") {
        updateSettingsForScope(scope) { it.copy(groupBy = option) }
    }

    fun runFullStorageScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanProgress = "Scanning MediaStore and File System...") }
            val stats = repository.getStorageStats()
            _uiState.value = stats
        }
    }

    fun toggleFileSelection(path: String) {
        _selectedFiles.update { current ->
            if (current.contains(path)) current - path else current + path
        }
        if (_selectedFiles.value.isEmpty()) {
            _isSelectionMode.value = false
        } else {
            _isSelectionMode.value = true
        }
    }

    fun enterSelectionMode(path: String) {
        _isSelectionMode.value = true
        _selectedFiles.value = setOf(path)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedFiles.value = emptySet()
    }

    fun selectAll(files: List<FileInfo>) {
        _selectedFiles.value = files.map { it.path }.toSet()
        _isSelectionMode.value = _selectedFiles.value.isNotEmpty()
    }

    fun deselectAll() {
        _selectedFiles.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val allFiles = mutableListOf<FileInfo>()
            allFiles.addAll(_uiState.value.largestFiles)
            _uiState.value.categoryStats.values.forEach { allFiles.addAll(it.files) }
            
            val filesToDelete = allFiles.distinctBy { it.path }.filter { _selectedFiles.value.contains(it.path) }

            filesToDelete.forEach { file ->
                repository.deleteFile(file)
            }
            exitSelectionMode()
            runFullStorageScan() // Refresh after delete
        }
    }

    fun deleteFile(file: FileInfo) {
        viewModelScope.launch {
            repository.deleteFile(file)
            runFullStorageScan()
        }
    }

    fun openFile(file: FileInfo) {
        repository.openFile(file)
    }

    fun shareFile(file: FileInfo) {
        repository.shareFile(file)
    }

    fun renameFile(file: FileInfo, newName: String) {
        viewModelScope.launch {
            repository.renameFile(file, newName)
            runFullStorageScan()
        }
    }

    fun moveFile(file: FileInfo, destination: String) {
        viewModelScope.launch {
            repository.moveFile(file, destination)
            runFullStorageScan()
        }
    }

    fun copyFile(file: FileInfo, destination: String) {
        viewModelScope.launch {
            repository.copyFile(file, destination)
            runFullStorageScan()
        }
    }

    fun moveSelectedFiles(destination: String) {
        viewModelScope.launch {
            val filesToMove = _uiState.value.largestFiles.filter { _selectedFiles.value.contains(it.path) }
            filesToMove.forEach { repository.moveFile(it, destination) }
            exitSelectionMode()
            runFullStorageScan()
        }
    }

    fun copySelectedFiles(destination: String) {
        viewModelScope.launch {
            val filesToCopy = _uiState.value.largestFiles.filter { _selectedFiles.value.contains(it.path) }
            filesToCopy.forEach { repository.copyFile(it, destination) }
            exitSelectionMode()
            runFullStorageScan()
        }
    }

    fun getFilesByCategory(category: FileCategory): List<FileInfo> {
        val files = _uiState.value.categoryStats[category]?.files ?: emptyList()
        val settings = getSettingsForScope(category.name)
        return sortFiles(files, settings.sortOption, settings.sortOrder)
    }

    fun sortFiles(files: List<FileInfo>, option: FileSortOption, order: FileSortOrder): List<FileInfo> {
        val comparator = when (option) {
            FileSortOption.NAME -> compareBy<FileInfo> { it.name.lowercase() }
            FileSortOption.SIZE -> compareBy<FileInfo> { it.size }
            FileSortOption.DATE_MODIFIED -> compareBy<FileInfo> { it.lastModified }
            FileSortOption.DATE_CREATED -> compareBy<FileInfo> { it.lastModified } // Metadata not always available, falling back to lastModified
            FileSortOption.TYPE -> compareBy<FileInfo> { it.path.substringAfterLast('.', "").lowercase() }
            FileSortOption.AUTHORS -> compareBy<FileInfo> { "" } 
            FileSortOption.CATEGORIES -> compareBy<FileInfo> { it.category.name }
            FileSortOption.TAGS -> compareBy<FileInfo> { "" }
            FileSortOption.TITLE -> compareBy<FileInfo> { it.name }
            else -> compareBy<FileInfo> { it.size }
        }
        
        return if (order == FileSortOrder.ASCENDING) {
            files.sortedWith(comparator)
        } else {
            files.sortedWith(comparator.reversed())
        }
    }

    fun groupFiles(files: List<FileInfo>, option: FileGroupByOption): Map<String, List<FileInfo>> {
        return when (option) {
            FileGroupByOption.NONE -> mapOf("" to files)
            FileGroupByOption.NAME -> files.groupBy { file ->
                val firstChar = file.name.firstOrNull()?.uppercaseChar() ?: '?'
                when (firstChar) {
                    in 'A'..'E' -> "A - E"
                    in 'F'..'J' -> "F - J"
                    in 'K'..'O' -> "K - O"
                    in 'P'..'T' -> "P - T"
                    in 'U'..'Z' -> "U - Z"
                    else -> "Others"
                }
            }
            FileGroupByOption.DATE_MODIFIED -> files.groupBy {
                java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.lastModified))
            }
            FileGroupByOption.DATE_CREATED -> files.groupBy {
                java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it.lastModified))
            }
            FileGroupByOption.FOLDER -> files.groupBy { it.path.substringBeforeLast('/', "Root") }
            FileGroupByOption.TYPE -> files.groupBy { it.path.substringAfterLast('.', "Unknown").uppercase() }
            FileGroupByOption.AUTHOR -> files.groupBy { "Unknown Author" }
            FileGroupByOption.TAG -> files.groupBy { "No Tags" }
            FileGroupByOption.CATEGORY -> files.groupBy { it.category.name }
            FileGroupByOption.SIZE -> files.groupBy { file ->
                val mb = file.size / (1024 * 1024)
                when {
                    mb < 1 -> "Under 1 MB"
                    mb < 10 -> "1 MB - 10 MB"
                    mb < 100 -> "10 MB - 100 MB"
                    mb < 1000 -> "100 MB - 1 GB"
                    mb < 10000 -> "1 GB - 10 GB"
                    else -> "Over 10 GB"
                }
            }
            else -> mapOf("" to files)
        }
    }
}
