package com.semseytech.rtsdevicesuitepro.cleaner

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.*
import com.semseytech.rtsdevicesuitepro.ui.components.*

class CleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CleanerViewModel"
    private val _state = MutableStateFlow(CleanerState.IDLE)
    val state = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<CleanupCategory>>(emptyList())
    
    private val _displaySettings = MutableStateFlow(FileDisplaySettings())
    val displaySettings = _displaySettings.asStateFlow()

    val categories = combine(_categories, _displaySettings) { cats, settings ->
        applyDisplaySettings(cats, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _showSortMenu = MutableStateFlow(false)
    val showSortMenu = _showSortMenu.asStateFlow()

    private val _showViewMenu = MutableStateFlow(false)
    val showViewMenu = _showViewMenu.asStateFlow()

    private val _showGroupMenu = MutableStateFlow(false)
    val showGroupMenu = _showGroupMenu.asStateFlow()

    val appTypeDefinitions = listOf(
        AppTypeInfo("Banking & Finance", "Apps used for banking, investments, or bill payments.", "Clearing cache might remove saved login methods or session tokens, requiring a full re-login or security verification."),
        AppTypeInfo("Social Media", "Facebook, Instagram, X (Twitter), etc.", "Clearing cache will remove temporary image and video data. While safe, it will significantly increase data usage and load times as the app re-downloads everything."),
        AppTypeInfo("2FA & Security", "Authenticator apps and security tokens.", "DANGER: Some apps may store temporary configuration or session data in cache. While rare, clearing it could interfere with active sessions."),
        AppTypeInfo("Gaming Apps", "Large games and online multiplayer titles.", "Cache often stores large asset files or temporary progress. Clearing it may force the app to re-download several GBs of data."),
        AppTypeInfo("Shopping Apps", "Amazon, eBay, and retailers.", "May clear your active shopping cart or 'recently viewed' items if they aren't synced to the server yet."),
        AppTypeInfo("Messaging Apps", "WhatsApp, Telegram, Signal.", "Clearing cache removes thumbnails and media previews. Your chats are safe, but scrolling back in history will be slower until media is re-cached.")
    )

    init {
        // Only initialize categories with empty items, don't trigger heavy scan automatically
        _categories.value = listOf(
            CleanupCategory("dupes", "Duplicate Files", "Removes identical copies of files", Icons.Outlined.ContentCopy),
            CleanupCategory("contact_dupes", "Duplicate Contacts", "Identical contact entries", Icons.Outlined.Person),
            CleanupCategory("empty_folders", "Empty Folders", "Removes directories with no content", Icons.Outlined.Folder),
            CleanupCategory("temp", "Temp & Thumbnails", "Temporary cache and image previews", Icons.Outlined.Cached),
            CleanupCategory("residual", "Residual Data", "Leftover files from uninstalled apps", Icons.Outlined.DeleteSweep),
            CleanupCategory("downloads", "Failed Downloads", "Corrupted or incomplete downloads", Icons.Outlined.FileDownloadOff),
            CleanupCategory("recycle", "Recycle Bin", "Permanently empty deleted items", Icons.Outlined.DeleteOutline, isSelected = false),
            CleanupCategory("logs", "Call Logs", "Clear recent call history", Icons.Outlined.Call, isSelected = false),
            CleanupCategory("sms", "SMS Threads", "Clear selected text messages", Icons.Outlined.Sms, isSelected = false)
        )
    }

    fun startScan() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_state.value == CleanerState.SCANNING) return@launch
            
            _state.value = CleanerState.SCANNING
            val externalStorage = Environment.getExternalStorageDirectory()
            
            // Perform all file-based scans in one pass to avoid multiple walks
            val dupes = mutableListOf<CleanupItem>()
            val emptyFolders = mutableListOf<CleanupItem>()
            val tempFiles = mutableListOf<CleanupItem>()
            val fileMap = mutableMapOf<String, String>() // Key -> Path (save memory)
            val tempPatterns = listOf(".tmp", ".temp", ".cache", "thumbnails", ".log")

            externalStorage.walkTopDown().onEnter { !it.name.startsWith(".") }.forEach { file ->
                kotlinx.coroutines.yield()
                if (file.isFile) {
                    // Check Temp
                    if (tempPatterns.any { file.name.contains(it, ignoreCase = true) || file.path.contains(it, ignoreCase = true) }) {
                        tempFiles.add(CleanupItem(UUID.randomUUID().toString(), file.absolutePath, file.name, file.length()))
                    } else {
                        // Check Dupes
                        val size = file.length()
                        if (size > 0) {
                            val hash = FileHasher.calculateMD5(file)
                            if (hash != null) {
                                val key = "${hash}_${size}"
                                if (fileMap.containsKey(key)) {
                                    dupes.add(CleanupItem(UUID.randomUUID().toString(), file.absolutePath, file.name, size, extraInfo = "Duplicate of file in ${fileMap[key]}"))
                                } else {
                                    fileMap[key] = file.parent ?: "root"
                                }
                            }
                        }
                    }
                }
            }
            
            // Empty folders need bottom-up
            externalStorage.walkBottomUp().onEnter { !it.name.startsWith(".") }.forEach { file ->
                kotlinx.coroutines.yield()
                if (file.isDirectory) {
                    val children = file.list()
                    if (children != null && children.isEmpty()) {
                        emptyFolders.add(CleanupItem(UUID.randomUUID().toString(), file.absolutePath, file.name, 0))
                    }
                }
            }

            val duplicateContacts = findDuplicateContacts()

            _categories.value = _categories.value.map { cat ->
                when (cat.id) {
                    "dupes" -> cat.copy(items = dupes)
                    "contact_dupes" -> cat.copy(items = duplicateContacts)
                    "empty_folders" -> cat.copy(items = emptyFolders)
                    "temp" -> cat.copy(items = tempFiles)
                    else -> cat
                }
            }
            _state.value = CleanerState.IDLE
        }
    }

    fun refreshScan() {
        startScan()
    }

    private fun findDuplicateContacts(): List<CleanupItem> {
        val context = getApplication<Application>()
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing READ_CONTACTS permission for duplicate scan")
            return emptyList()
        }

        val contentResolver = context.contentResolver
        val dupes = mutableListOf<CleanupItem>()
        val seenByNumber = mutableMapOf<String, String>() // NormalizedNumber -> ContactID
        val seenByName = mutableMapOf<String, String>()   // Name -> ContactID

        // 1. Scan Phone Numbers
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        phoneCursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val name = it.getString(nameIdx) ?: "Unknown"
                val rawNumber = it.getString(numIdx) ?: ""
                val normalizedNumber = rawNumber.replace(Regex("[^0-9]"), "")
                
                if (normalizedNumber.length < 7) continue
                
                if (seenByNumber.containsKey(normalizedNumber)) {
                    val originalId = seenByNumber[normalizedNumber]!!
                    if (originalId != id) {
                        dupes.add(CleanupItem(
                            id = id,
                            path = "contact://$id",
                            name = name,
                            sizeBytes = 0,
                            extraInfo = "Same number as another contact ($rawNumber)"
                        ))
                    }
                } else {
                    seenByNumber[normalizedNumber] = id
                }
            }
        }

        // 2. Scan Names (for contacts without numbers)
        val nameCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )

        nameCursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getString(idIdx)
                val name = it.getString(nameIdx) ?: continue
                if (name.isBlank() || name == "Unknown") continue
                
                val lowerName = name.lowercase().trim()
                if (seenByName.containsKey(lowerName)) {
                    val originalId = seenByName[lowerName]!!
                    if (originalId != id) {
                        // Check if we already flagged this ID via phone scan
                        if (dupes.none { it.id == id }) {
                            dupes.add(CleanupItem(
                                id = id,
                                path = "contact://$id",
                                name = name,
                                sizeBytes = 0,
                                extraInfo = "Identical name to another contact"
                            ))
                        }
                    }
                } else {
                    seenByName[lowerName] = id
                }
            }
        }
        
        Log.d(TAG, "Found ${dupes.size} duplicate contacts")
        return dupes.distinctBy { it.id }
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
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            val appList = installedApps.filter { 
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 
            }.map { appInfo ->
                // Note: Getting real cache size requires specific permissions or usage stats API.
                // For now, we use the APK size as a proxy or 0 if we can't get it, 
                // but we inform the user they will clear it manually.
                val file = File(appInfo.sourceDir)
                AppCacheInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    cacheSize = if (file.exists()) file.length() else 0L, 
                    iconDrawable = pm.getApplicationIcon(appInfo),
                    warningReason = getWarningReason(appInfo.packageName),
                    isSelected = false
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
        viewModelScope.launch(Dispatchers.IO) {
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

            selectedItems.forEach { (catId, item) ->
                processed++
                _progress.value = CleanupProgress(
                    currentItemName = if (item.path.startsWith("contact://")) "Contact: ${item.name}" else item.path,
                    progress = processed.toFloat() / total,
                    itemsProcessed = processed,
                    totalItems = total
                )
                
                if (item.path.startsWith("contact://")) {
                    if (deleteContact(item.id)) {
                        totalCleaned += 0 // Size unknown for contacts
                        if (catId == "contact_dupes") dupesRemoved++
                    }
                } else {
                    val file = File(item.path)
                    if (file.exists()) {
                        val size = file.length()
                        if (file.delete()) {
                            totalCleaned += size
                            if (catId == "dupes") dupesRemoved++
                            if (catId == "empty_folders") foldersCleared++
                        }
                    }
                }
            }

            _cleanupResult.value = CleanupResult(
                cleanedSizeBytes = totalCleaned,
                duplicatesRemoved = dupesRemoved,
                foldersCleared = foldersCleared
            )

            withContext(Dispatchers.Main) {
                if (_showAppsToggle.value) {
                    val selectedApps = _apps.value.filter { it.isSelected }
                    if (selectedApps.isNotEmpty()) {
                        _state.value = CleanerState.GUIDED_CACHE
                        _currentAppProcessing.value = selectedApps.first()
                        openAppStorageSettings(_currentAppProcessing.value!!.packageName)
                    } else {
                        _state.value = CleanerState.COMPLETED
                    }
                } else {
                    _state.value = CleanerState.COMPLETED
                }
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

    private fun deleteContact(contactId: String): Boolean {
        return try {
            val contentResolver = getApplication<Application>().contentResolver
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            val rowsDeleted = contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            false
        }
    }

    fun updateDisplaySettings(settings: FileDisplaySettings) {
        _displaySettings.value = settings
    }

    fun setShowSortMenu(show: Boolean) { _showSortMenu.value = show }
    fun setShowViewMenu(show: Boolean) { _showViewMenu.value = show }
    fun setShowGroupMenu(show: Boolean) { _showGroupMenu.value = show }

    fun setSortOption(option: FileSortOption) {
        _displaySettings.update { it.copy(sortOption = option) }
    }

    fun setSortOrder(order: FileSortOrder) {
        _displaySettings.update { it.copy(sortOrder = order) }
    }

    fun setViewMode(mode: FileViewMode) {
        _displaySettings.update { it.copy(viewMode = mode) }
    }

    fun setGroupBy(option: FileGroupByOption) {
        _displaySettings.update { it.copy(groupBy = option) }
    }

    private fun applyDisplaySettings(categories: List<CleanupCategory>, settings: FileDisplaySettings): List<CleanupCategory> {
        // First sort items within each category
        val updatedCategories = categories.map { category ->
            val sortedItems = sortItems(category.items, settings.sortOption, settings.sortOrder)
            category.copy(items = sortedItems)
        }

        // Then handle grouping if needed (simplistic approach for now)
        return when (settings.groupBy) {
            FileGroupByOption.SIZE_RANGE -> groupByDimensions(updatedCategories) { it.sizeBytes }
            else -> updatedCategories
        }
    }

    private fun sortItems(items: List<CleanupItem>, option: FileSortOption, order: FileSortOrder): List<CleanupItem> {
        val comparator = when (option) {
            FileSortOption.NAME -> compareBy<CleanupItem> { it.name.lowercase() }
            FileSortOption.SIZE -> compareBy<CleanupItem> { it.sizeBytes }
            FileSortOption.DATE -> compareBy<CleanupItem> { File(it.path).lastModified() }
            FileSortOption.TYPE -> compareBy<CleanupItem> { it.name.substringAfterLast(".", "").lowercase() }
            else -> compareBy<CleanupItem> { it.sizeBytes }
        }
        
        return if (order == FileSortOrder.DESCENDING) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
    }

    private fun groupByDimensions(categories: List<CleanupCategory>, selector: (CleanupItem) -> Long): List<CleanupCategory> {
        // For now, we keep original categories but we could flatten and regroup
        // Let's just sort the categories themselves by total size for now as a "grouping" effect
        return categories.sortedByDescending { it.items.sumOf { item -> item.sizeBytes } }
    }
}
