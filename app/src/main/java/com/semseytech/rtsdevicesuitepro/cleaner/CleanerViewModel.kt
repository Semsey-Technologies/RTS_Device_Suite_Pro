package com.semseytech.rtsdevicesuitepro.cleaner

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CleanerState.IDLE)
    val state = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<CleanupCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _apps = MutableStateFlow<List<AppCacheInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _showAppsToggle = MutableStateFlow(false)
    val showAppsToggle = _showAppsToggle.asStateFlow()

    private val _cleanupResult = MutableStateFlow(CleanupResult())
    val cleanupResult = _cleanupResult.asStateFlow()

    private val _progress = MutableStateFlow(CleanupProgress())
    val progress = _progress.asStateFlow()

    private val _currentAppProcessing = MutableStateFlow<AppCacheInfo?>(null)
    val currentAppProcessing = _currentAppProcessing.asStateFlow()

    val appTypeDefinitions = listOf(
        AppTypeInfo("Banking & Finance", "Apps used for banking, investments, or bill payments.", "Clearing cache might remove saved login methods or session tokens, requiring a full re-login or security verification."),
        AppTypeInfo("Social Media", "Facebook, Instagram, X (Twitter), etc.", "Clearing cache will remove temporary image and video data. While safe, it will significantly increase data usage and load times as the app re-downloads everything."),
        AppTypeInfo("2FA & Security", "Authenticator apps and security tokens.", "DANGER: Some apps may store temporary configuration or session data in cache. While rare, clearing it could interfere with active sessions."),
        AppTypeInfo("Gaming Apps", "Large games and online multiplayer titles.", "Cache often stores large asset files or temporary progress. Clearing it may force the app to re-download several GBs of data."),
        AppTypeInfo("Shopping Apps", "Amazon, eBay, and retailers.", "May clear your active shopping cart or 'recently viewed' items if they aren't synced to the server yet."),
        AppTypeInfo("Messaging Apps", "WhatsApp, Telegram, Signal.", "Clearing cache removes thumbnails and media previews. Your chats are safe, but scrolling back in history will be slower until media is re-cached.")
    )

    init {
        loadCategories()
    }

    private fun loadCategories() {
        _categories.value = listOf(
            CleanupCategory("dupes", "Duplicate Files", "Removes identical copies of files", Icons.Outlined.ContentCopy, 
                items = listOf(
                    CleanupItem("d1", "/sdcard/Downloads/IMG_001_copy.jpg", "IMG_001_copy.jpg", 1024 * 1024 * 2, extraInfo = "Duplicate of IMG_001.jpg"),
                    CleanupItem("d2", "/sdcard/DCIM/Video_02_1.mp4", "Video_02_1.mp4", 1024 * 1024 * 45, extraInfo = "Duplicate of Video_02.mp4")
                )),
            CleanupCategory("empty_folders", "Empty Folders", "Removes directories with no content", Icons.Outlined.Folder,
                items = listOf(
                    CleanupItem("f1", "/sdcard/Android/data/com.old.app/cache", "cache (com.old.app)"),
                    CleanupItem("f2", "/sdcard/Download/Temp_Folders/Old_Project", "Old_Project")
                )),
            CleanupCategory("residual", "Residual Data", "Leftover files from uninstalled apps", Icons.Outlined.DeleteSweep),
            CleanupCategory("downloads", "Failed Downloads", "Corrupted or incomplete downloads", Icons.Outlined.FileDownloadOff),
            CleanupCategory("temp", "Temp & Thumbnails", "Temporary cache and image previews", Icons.Outlined.Cached),
            CleanupCategory("recycle", "Recycle Bin", "Permanently empty deleted items", Icons.Outlined.DeleteOutline, isSelected = false),
            CleanupCategory("logs", "Call Logs", "Clear recent call history", Icons.Outlined.Call, isSelected = false),
            CleanupCategory("sms", "SMS Threads", "Clear selected text messages", Icons.Outlined.Sms, isSelected = false)
        )
    }

    fun toggleCategoryExpansion(id: String) {
        _categories.value = _categories.value.map {
            if (it.id == id) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    fun toggleCategorySelection(id: String) {
        _categories.value = _categories.value.map { category ->
            if (category.id == id) {
                val newState = !category.isSelected
                category.copy(
                    isSelected = newState,
                    items = category.items.map { it.copy(isSelected = newState) }
                )
            } else category
        }
    }

    fun toggleItemSelection(categoryId: String, itemId: String) {
        _categories.value = _categories.value.map { category ->
            if (category.id == categoryId) {
                val updatedItems = category.items.map {
                    if (it.id == itemId) it.copy(isSelected = !it.isSelected) else it
                }
                category.copy(
                    items = updatedItems,
                    isSelected = updatedItems.any { it.isSelected }
                )
            } else category
        }
    }

    fun setAllSelection(selected: Boolean) {
        _categories.value = _categories.value.map { category ->
            category.copy(
                isSelected = selected,
                items = category.items.map { it.copy(isSelected = selected) }
            )
        }
        _apps.value = _apps.value.map { it.copy(isSelected = selected) }
    }

    fun toggleShowApps(show: Boolean) {
        _showAppsToggle.value = show
        if (show && _apps.value.isEmpty()) {
            scanApps()
        }
    }

    fun toggleAppSelection(packageName: String) {
        _apps.value = _apps.value.map {
            if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    private fun scanApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            val appList = installedApps.filter { 
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 
            }.map { appInfo ->
                AppCacheInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    cacheSize = (10..500).random().toLong() * 1024 * 1024,
                    iconDrawable = pm.getApplicationIcon(appInfo),
                    warningReason = getWarningReason(appInfo.packageName),
                    isSelected = false // Default to false
                )
            }.sortedByDescending { it.cacheSize }
            
            _apps.value = appList
        }
    }

    private fun getWarningReason(packageName: String): String? {
        return when {
            packageName.contains("bank") || packageName.contains("finance") -> "Banking apps might lose session tokens."
            packageName.contains("whatsapp") || packageName.contains("messenger") -> "Messaging media previews will be removed."
            packageName.contains("gallery") || packageName.contains("music") || packageName.contains("spotify") -> "Increases loading time for media."
            packageName.contains("game") || packageName.contains("unity") -> "Might trigger large re-downloads of game assets."
            packageName.contains("amazon") || packageName.contains("shop") -> "May clear active cart or session data."
            packageName.contains("auth") || packageName.contains("security") || packageName.contains("2fa") -> "Risk of interfering with active security sessions."
            else -> null
        }
    }

    fun startCleanup() {
        viewModelScope.launch {
            _state.value = CleanerState.CLEANING
            
            val selectedItems = mutableListOf<Pair<String, CleanupItem>>()
            _categories.value.forEach { cat ->
                if (cat.isSelected) {
                    cat.items.filter { it.isSelected }.forEach { item ->
                        selectedItems.add(cat.id to item)
                    }
                }
            }

            val total = selectedItems.size
            var processed = 0
            var totalCleaned = 0L
            var dupesRemoved = 0
            var foldersCleared = 0

            if (total > 0) {
                selectedItems.forEach { (catId, item) ->
                    processed++
                    _progress.value = CleanupProgress(
                        currentItemName = item.path,
                        progress = processed.toFloat() / total,
                        itemsProcessed = processed,
                        totalItems = total
                    )
                    
                    delay(150) // Simulate file deletion delay
                    
                    totalCleaned += item.sizeBytes
                    if (catId == "dupes") dupesRemoved++
                    if (catId == "empty_folders") foldersCleared++
                }
            } else {
                // If no items selected, just a brief delay for system tasks
                _progress.value = CleanupProgress("System optimization...", 0.5f, 0, 0)
                delay(1000)
            }

            _cleanupResult.value = CleanupResult(
                cleanedSizeBytes = totalCleaned,
                duplicatesRemoved = dupesRemoved,
                foldersCleared = foldersCleared
            )

            if (_showAppsToggle.value) {
                val selectedApps = _apps.value.filter { it.isSelected }
                if (selectedApps.isNotEmpty()) {
                    _state.value = CleanerState.GUIDED_CACHE
                    _currentAppProcessing.value = selectedApps.first()
                } else {
                    _state.value = CleanerState.COMPLETED
                }
            } else {
                _state.value = CleanerState.COMPLETED
            }
        }
    }

    fun processNextApp() {
        val selectedApps = _apps.value.filter { it.isSelected }
        val currentIndex = selectedApps.indexOf(_currentAppProcessing.value)
        
        if (currentIndex < selectedApps.size - 1) {
            _currentAppProcessing.value = selectedApps[currentIndex + 1]
            openAppStorageSettings(_currentAppProcessing.value!!.packageName)
        } else {
            _state.value = CleanerState.COMPLETED
            _cleanupResult.value = _cleanupResult.value.copy(appsProcessed = selectedApps.size)
        }
    }

    fun openAppStorageSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        getApplication<Application>().startActivity(intent)
    }
}
