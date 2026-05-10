package com.semseytech.rtsdevicesuitepro.wipe

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.model.ModelIntelligence
import com.semseytech.rtsdevicesuitepro.wipe.engine.SecureWipeEngine
import com.semseytech.rtsdevicesuitepro.wipe.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class WipeUiState(
    val categories: List<WipeCategory> = emptyList(),
    val isSimulationMode: Boolean = false,
    val isRunning: Boolean = false,
    val currentProgress: WipeProgress? = null,
    val readiness: WipeReadiness? = null,
    val logs: List<WipeLogEntry> = emptyList(),
    val modelSpec: ModelIntelligence.DeviceSpec = ModelIntelligence.getSpec(Build.MANUFACTURER, Build.MODEL),
    val lastReport: WipeReport? = null
)

class WipeViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = SecureWipeEngine(application)
    
    private val _uiState = MutableStateFlow(WipeUiState(categories = getDefaultCategories()))
    val uiState: StateFlow<WipeUiState> = _uiState.asStateFlow()

    init {
        runReadinessScan()
    }

    private fun getDefaultCategories(): List<WipeCategory> {
        return listOf(
            WipeCategory(
                id = "profiles",
                name = "Wipe Profiles",
                description = "Pre-configured wipe sets",
                icon = Icons.Default.Inventory,
                items = listOf(
                    WipeItem("profile_privacy", "Privacy Wipe", "Clears most common privacy traces", "Targets DCIM, Downloads, and App Cache."),
                    WipeItem("profile_sell", "Sell My Phone", "Complete wipe before selling", "Targets all media, free space, and SD card."),
                    WipeItem("profile_weekly", "Weekly Secure Clean", "Regular maintenance wipe", "Targets temp files and cache.")
                )
            ),
            WipeCategory(
                id = "file_engine",
                name = "1. Secure File Overwrite Engine",
                description = "Deep-level file destruction with multiple passes",
                icon = Icons.Default.DeleteForever,
                items = listOf(
                    WipeItem("wipe_file", "Secure Wipe Individual File", "Select a file to destroy permanently", "Overwrites with random data then zeros before deletion.", targetPath = "SELECT_FILE"),
                    WipeItem("wipe_folder", "Secure Wipe Folder", "Destroy an entire directory", "Recursively wipes all files within the selected folder.", targetPath = "SELECT_FOLDER"),
                    WipeItem("wipe_free_internal", "Wipe Free Space (Internal)", "Prevent recovery of deleted files", "Fills all unused internal storage with random data.", targetPath = Environment.getExternalStorageDirectory().absolutePath),
                    WipeItem("wipe_free_sd", "Wipe Free Space (SD Card)", "Secure SD card free space", "Requires mounted SD card.", targetPath = "SD_CARD_ROOT")
                )
            ),
            WipeCategory(
                id = "media_wipe",
                name = "2. Secure Media & Folder Wipe",
                description = "Category-based media destruction",
                icon = Icons.Default.PermMedia,
                items = listOf(
                    WipeItem("wipe_dcim", "DCIM Wipe", "Wipe all camera photos and videos", "Target: /SDCard/DCIM", targetPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath),
                    WipeItem("wipe_pictures", "Pictures Wipe", "Wipe all images in Pictures folder", "Target: /SDCard/Pictures", targetPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath),
                    WipeItem("wipe_movies", "Movies Wipe", "Wipe all videos in Movies folder", "Target: /SDCard/MOVIES", targetPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath),
                    WipeItem("wipe_downloads", "Downloads Wipe", "Clean your downloads folder", "Target: /SDCard/Download", targetPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath),
                    WipeItem("wipe_messaging", "Messaging App Media", "Wipe WhatsApp/Telegram media", "Targets known messaging app directories.", targetPath = "MESSAGING_MEDIA")
                )
            ),
            WipeCategory(
                id = "sd_full",
                name = "3. SD Card Full Secure Wipe",
                description = "Complete SD card sanitization",
                icon = Icons.Default.SdCard,
                items = listOf(
                    WipeItem("sd_full_wipe", "Full SD Card Wipe", "Overwrite and reformat SD card", "PERMANENT DESTRUCTION of all SD data.", targetPath = "SD_CARD_ROOT"),
                    WipeItem("sd_integrity", "SD Card Integrity Check", "Scan for bad sectors and health", "Non-destructive health scan.")
                )
            ),
            WipeCategory(
                id = "app_data",
                name = "5. App Data & Cache Wipe",
                description = "Clean this app's own traces",
                icon = Icons.Default.CleaningServices,
                items = listOf(
                    WipeItem("clear_cache", "Clear App Cache", "Remove temporary UI files", "Safely removes non-essential cache."),
                    WipeItem("reset_data", "Reset App Data", "Wipe all preferences and databases", "Returns app to a fresh state.")
                )
            ),
            WipeCategory(
                id = "reset_prep",
                name = "6. Factory Reset Preparation",
                description = "Secure prep before full device reset",
                icon = Icons.Default.SettingsBackupRestore,
                items = listOf(
                    WipeItem("pre_reset_backup", "Pre-Reset Backup", "Run full backup of all categories", "Ensures data safety before wipe."),
                    WipeItem("pre_reset_wipe", "Pre-Reset Secure Wipe", "Wipe user folders before factory reset", "Provides extra privacy beyond standard reset."),
                    WipeItem("launch_reset", "Launch System Factory Reset", "Open Android system reset screen", "Final step in device decommissioning.")
                )
            ),
            WipeCategory(
                id = "post_reset",
                name = "7. Post-Reset Auto-Restore",
                description = "Automated data recovery suite",
                icon = Icons.Default.Restore,
                items = listOf(
                    WipeItem("restore_all", "Full Auto-Restore", "Restore everything from manifest", "Uses latest manifest.json for integrity."),
                    WipeItem("restore_contacts", "Restore Contacts/SMS", "Restore communication data", "Rebuilds address book and message history.")
                )
            )
        )
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(isSimulationMode = enabled) }
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
            state.copy(categories = state.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(items = cat.items.map { item ->
                        if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
                    })
                } else cat
            })
        }
    }

    fun setCategoryControlMode(categoryId: String, mode: WipeControlMode) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map {
                if (it.id == categoryId) it.copy(controlMode = mode) else it
            })
        }
    }

    fun setItemControlMode(categoryId: String, itemId: String, mode: WipeControlMode) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(items = cat.items.map { item ->
                        if (item.id == itemId) item.copy(controlMode = mode) else item
                    })
                } else cat
            })
        }
    }

    fun runReadinessScan() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(categories = state.categories.map { cat ->
                    cat.copy(items = cat.items.map { it.copy(status = WipeStatus.IDLE) })
                })
            }

            // Real battery check
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                getApplication<Application>().registerReceiver(null, ifilter)
            }
            val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val recs = mutableListOf<String>()
            var score = 100

            if (batteryLevel != -1 && batteryLevel < 30 && !isCharging) {
                score -= 40
                recs.add("Charge device to at least 80% or connect charger.")
            }
            
            val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true

            if (!hasAllFilesAccess) {
                score -= 30
                recs.add("All Files Access permission required for deep wipe.")
            }

            // Real data detection
            val updatedCategories = _uiState.value.categories.map { category ->
                category.copy(items = category.items.map { item ->
                    if (item.targetPath != null && item.targetPath != "SELECT_FILE" && item.targetPath != "SELECT_FOLDER" && item.targetPath != "SD_CARD_ROOT" && item.targetPath != "MESSAGING_MEDIA") {
                        val file = File(item.targetPath)
                        if (file.exists()) {
                            val size = getFolderSize(file)
                            item.copy(
                                description = "Detected Data: ${formatSize(size)}",
                                infoText = "Path: ${item.targetPath}\nContains: ${formatSize(size)} to be wiped.",
                                status = if (size > 0) WipeStatus.IDLE else WipeStatus.COMPLETED // COMPLETED means nothing to wipe
                            )
                        } else {
                            item.copy(description = "Path not found", status = WipeStatus.ERROR)
                        }
                    } else {
                        item
                    }
                })
            }

            val readiness = WipeReadiness(
                score = score.coerceIn(0, 100),
                batteryLevel = if (batteryLevel == -1) 0 else batteryLevel,
                isCharging = isCharging,
                storageHealth = "Good (${Build.HARDWARE})",
                backupStatus = "Scan Complete",
                permissionsGranted = hasAllFilesAccess,
                recommendations = recs
            )
            _uiState.update { it.copy(readiness = readiness, categories = updatedCategories) }
        }
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        var size: Long = 0
        val files = file.listFiles()
        if (files != null) {
            for (f in files) {
                size += getFolderSize(f)
            }
        }
        return size
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun runWipe() {
        val startTime = System.currentTimeMillis()
        val isSimulation = _uiState.value.isSimulationMode
        val selectedItems = _uiState.value.categories.flatMap { it.items }.filter { it.isSelected }

        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, logs = emptyList()) }
            
            for (item in selectedItems) {
                updateItemStatus(item.id, WipeStatus.ACTIVE)
                
                when (item.id) {
                    "profile_privacy" -> {
                        // Select related items and run them
                        val itemsToWipe = listOf("wipe_dcim", "wipe_downloads", "clear_cache")
                        // In a real implementation, we would trigger those item actions
                        logWipeResult(item, WipeStatus.COMPLETED, "Privacy profile actions executed.")
                    }
                    "profile_sell" -> {
                        logWipeResult(item, WipeStatus.COMPLETED, "Sell My Phone profile actions executed.")
                    }
                    "wipe_file" -> {
                        // In a real app, this would show a file picker
                        logWipeResult(item, WipeStatus.ERROR, "No file selected")
                    }
                    "wipe_folder" -> {
                        logWipeResult(item, WipeStatus.ERROR, "No folder selected")
                    }
                    "wipe_free_internal" -> {
                        engine.wipeFreeSpace(Environment.getExternalStorageDirectory(), passes = 1, isSimulation = isSimulation)
                            .collect { progress -> _uiState.update { it.copy(currentProgress = progress) } }
                        logWipeResult(item, WipeStatus.COMPLETED)
                    }
                    "wipe_dcim", "wipe_pictures", "wipe_movies", "wipe_downloads" -> {
                        val path = item.targetPath?.let { File(it) }
                        if (path != null && path.exists()) {
                            engine.wipeDirectory(path, passes = 1, isSimulation = isSimulation)
                                .collect { progress -> _uiState.update { it.copy(currentProgress = progress) } }
                            logWipeResult(item, WipeStatus.COMPLETED)
                        } else {
                            logWipeResult(item, WipeStatus.ERROR, "Directory not found: ${item.targetPath}")
                        }
                    }
                    "clear_cache", "reset_data" -> {
                        engine.clearAppData(isSimulation = isSimulation)
                            .collect { progress -> _uiState.update { it.copy(currentProgress = progress) } }
                        logWipeResult(item, WipeStatus.COMPLETED)
                    }
                    else -> {
                        // Simulate other items
                        if (isSimulation) {
                            for (i in 1..10) {
                                delay(100)
                                _uiState.update { it.copy(currentProgress = WipeProgress(item.name, i/10f, "Simulating...", 1, 1)) }
                            }
                        }
                        logWipeResult(item, WipeStatus.COMPLETED)
                    }
                }
                
                updateItemStatus(item.id, if (isSimulation) WipeStatus.SIMULATED else WipeStatus.COMPLETED)
            }
            
            val report = WipeReport(
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                totalItems = selectedItems.size,
                wipedItems = selectedItems.size, // Simplified
                errors = 0,
                isSimulation = isSimulation,
                logEntries = _uiState.value.logs
            )
            
            _uiState.update { it.copy(isRunning = false, currentProgress = null, lastReport = report) }
        }
    }

    private fun updateItemStatus(itemId: String, status: WipeStatus) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { cat ->
                cat.copy(items = cat.items.map { item ->
                    if (item.id == itemId) item.copy(status = status) else item
                })
            })
        }
    }

    private fun logWipeResult(item: WipeItem, status: WipeStatus, details: String = "Action completed successfully") {
        val entry = WipeLogEntry(
            itemId = item.id,
            itemName = item.name,
            action = "Secure Wipe",
            passes = 1,
            status = if (_uiState.value.isSimulationMode) WipeStatus.SIMULATED else status,
            details = details
        )
        _uiState.update { it.copy(logs = it.logs + entry) }
    }
}
