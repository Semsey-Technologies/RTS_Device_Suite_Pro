package com.semseytech.rtsdevicesuitepro.backup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.backup.engine.*
import com.semseytech.rtsdevicesuitepro.backup.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "BackupViewModel"
    private val scanner = BackupScanner(application)
    private val engine = BackupEngine(application)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun loadRealData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, backupStatus = "Scanning device...") }
            val categories = mutableListOf<BackupCategory>()

            try {
                // Communication Data
                categories.add(BackupCategory("sms", "SMS/MMS Threads", scanner.scanSmsThreads(), parentCategory = "Communication"))
                categories.add(BackupCategory("calls", "Call Logs", scanner.scanCallLogs(), parentCategory = "Communication"))
                categories.add(BackupCategory("contacts", "Contacts", scanner.scanContacts(), parentCategory = "Communication"))
                
                // App Data
                categories.add(BackupCategory("apks", "Installed APKs", scanner.scanApks(), parentCategory = "Apps"))
                
                // User Files
                listOf("Pictures", "Videos", "Audio", "Music", "Movies", "Documents", "Downloads", "Ringtones", "Notifications", "Alarms").forEach {
                    categories.add(BackupCategory(it.lowercase(), it, scanner.scanUserFiles(it), parentCategory = "User Files"))
                }
                
                // System Settings
                categories.add(BackupCategory("settings", "System Settings", scanner.scanSystemSettings(), parentCategory = "System"))
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
            }

            _uiState.update { it.copy(categories = categories, isLoading = false, backupStatus = "") }
            applySortingAndGrouping()
        }
    }

    private fun applySortingAndGrouping() {
        _uiState.update { state ->
            val sortedCategories = state.categories.map { category ->
                val sortedItems = when (state.sortType) {
                    SortType.NAME -> category.items.sortedBy { it.displayName }
                    SortType.DATE -> category.items.sortedByDescending { it.date }
                    SortType.SIZE -> category.items.sortedByDescending { it.size }
                    SortType.TYPE -> category.items.sortedBy { it.type }
                }
                category.copy(items = sortedItems)
            }
            state.copy(categories = sortedCategories)
        }
    }

    fun setSortType(type: SortType) {
        _uiState.update { it.copy(sortType = type) }
        applySortingAndGrouping()
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setGroupType(type: GroupType) {
        _uiState.update { it.copy(groupType = type) }
    }

    fun toggleCategoryExpansion(categoryId: String) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { 
                if (it.id == categoryId) it.copy(isExpanded = !it.isExpanded) else it 
            })
        }
    }

    fun toggleItemSelection(categoryId: String, itemId: String) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { category ->
                if (category.id == categoryId) {
                    val newItems = category.items.map { item ->
                        if (item.id == itemId) {
                            when (item) {
                                is BackupItem.SmsMessage -> item.copy(isSelected = !item.isSelected)
                                is BackupItem.CallLogEntry -> item.copy(isSelected = !item.isSelected)
                                is BackupItem.Contact -> item.copy(isSelected = !item.isSelected)
                                is BackupItem.Apk -> item.copy(isSelected = !item.isSelected)
                                is BackupItem.UserFile -> item.copy(isSelected = !item.isSelected)
                                is BackupItem.SystemSetting -> item.copy(isSelected = !item.isSelected)
                            }
                        } else item
                    }
                    category.copy(items = newItems, isAllSelected = newItems.all { it.isSelected })
                } else category
            })
        }
    }

    fun toggleCategorySelection(categoryId: String, isSelected: Boolean) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { category ->
                if (category.id == categoryId) {
                    category.copy(
                        isAllSelected = isSelected,
                        items = category.items.map { item ->
                            when (item) {
                                is BackupItem.SmsMessage -> item.copy(isSelected = isSelected)
                                is BackupItem.CallLogEntry -> item.copy(isSelected = isSelected)
                                is BackupItem.Contact -> item.copy(isSelected = isSelected)
                                is BackupItem.Apk -> item.copy(isSelected = isSelected)
                                is BackupItem.UserFile -> item.copy(isSelected = isSelected)
                                is BackupItem.SystemSetting -> item.copy(isSelected = isSelected)
                            }
                        }
                    )
                } else category
            })
        }
    }

    fun startBackup(onComplete: (String) -> Unit) {
        val selectedItems = _uiState.value.categories.flatMap { it.items.filter { it.isSelected } }
        if (selectedItems.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, backupProgress = 0f, backupStatus = "Preparing background task...") }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = _uiState.value.outputFileName.ifEmpty { "RTS_Backup_$timestamp" }
            
            // 1. Prepare data for worker
            val gson = com.google.gson.Gson()
            val itemsWithTypes = selectedItems.map { item ->
                val map = gson.fromJson(gson.toJson(item), MutableMap::class.java) as MutableMap<String, Any>
                map["itemClassType"] = item::class.java.simpleName
                map
            }
            
            val tempFile = File(getApplication<Application>().cacheDir, "backup_selection_${System.currentTimeMillis()}.json")
            tempFile.writeText(gson.toJson(itemsWithTypes))
            
            // 2. Prepare Worker input
            val inputData = androidx.work.Data.Builder()
                .putString("input_file_path", tempFile.absolutePath)
                .putString("output_file_name", fileName)
                .putString("archive_format", _uiState.value.selectedArchiveType.name)
                .putInt("compression_level", _uiState.value.compressionLevel)
                .putString("destination_json", gson.toJson(_uiState.value.selectedDestination))
                .build()
            
            // 3. Enqueue Worker
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.semseytech.rtsdevicesuitepro.backup.worker.BackupWorker>()
                .setInputData(inputData)
                .addTag("backup_work")
                .build()
            
            androidx.work.WorkManager.getInstance(getApplication())
                .enqueueUniqueWork("device_backup", androidx.work.ExistingWorkPolicy.REPLACE, workRequest)

            _uiState.update { it.copy(isLoading = false, backupStatus = "Backup started in background") }
            
            // Note: onComplete is usually for immediate results, 
            // but since it's now backgrounded, we can notify the user via WorkManager status if needed,
            // or just rely on the notification.
            withContext(Dispatchers.Main) {
                onComplete("Background process started. Check notifications for progress.")
            }
        }
    }

    fun setArchiveType(type: ArchiveFormat) {
        _uiState.update { it.copy(selectedArchiveType = type) }
    }
    
    fun setCompressionLevel(level: Int) {
        _uiState.update { it.copy(compressionLevel = level) }
    }

    fun setOutputFileName(name: String) {
        _uiState.update { it.copy(outputFileName = name) }
    }

    fun setDestination(destination: BackupDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }
}

data class BackupUiState(
    val categories: List<BackupCategory> = emptyList(),
    val isLoading: Boolean = false,
    val isMasterSelected: Boolean = false,
    val backupProgress: Float = 0f,
    val backupStatus: String = "",
    val sortType: SortType = SortType.NAME,
    val viewMode: ViewMode = ViewMode.LIST,
    val groupType: GroupType = GroupType.NONE,
    val selectedArchiveType: ArchiveFormat = ArchiveFormat.ZIP,
    val compressionLevel: Int = 5,
    val outputFileName: String = "",
    val selectedDestination: BackupDestination = BackupDestination(BackupDestinationType.INTERNAL, "Internal Storage"),
    val isDefaultSmsApp: Boolean = false
)
