package com.semseytech.rtsdevicesuitepro.restore

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.backup.model.*
import com.semseytech.rtsdevicesuitepro.restore.engine.RestoreEngine
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class RestoreViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RestoreViewModel"
    private val engine = RestoreEngine(application)

    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    fun loadArchive(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, status = "Loading archive...", report = null) }
            try {
                val context = getApplication<Application>()
                Log.d(TAG, "Loading archive from URI: $uri")

                // Try to take persistable permission (good practice for SAF)
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "Could not take persistable permission (normal for non-SAF URIs)")
                }

                // Get original filename safely - do this BEFORE opening stream as it might provide clues
                val fileName = try {
                    val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) cursor.getString(nameIndex) else null
                        } else null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not query display name, using default", e)
                    null
                } ?: "restore_temp_archive.zip"

                Log.d(TAG, "Opening input stream for: $fileName")

                // Robust stream opening for SAF/Samsung/Google Drive
                val inputStream = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    Log.w(TAG, "Primary openInputStream failed: ${e.message}")
                    
                    // Fallback 1: openAssetFileDescriptor (often handles cloud files better)
                    try {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
                    } catch (e2: Exception) {
                        Log.w(TAG, "AssetFileDescriptor fallback failed: ${e2.message}")
                        
                        // Fallback 2: openTypedAssetFileDescriptor (specifically for cloud files)
                        try {
                            context.contentResolver.openTypedAssetFileDescriptor(uri, "*/*", null)?.createInputStream()
                        } catch (e3: Exception) {
                            Log.w(TAG, "TypedAssetFileDescriptor fallback failed: ${e3.message}")
                            
                            // Fallback 3: openFileDescriptor with ParcelFileDescriptor
                            try {
                                context.contentResolver.openFileDescriptor(uri, "r")?.let { pfd ->
                                    android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)
                                }
                            } catch (e4: Exception) {
                                Log.e(TAG, "All opening methods failed", e4)
                                null
                            }
                        }
                    }
                } ?: throw java.io.IOException("Failed to open input stream for URI: $uri. If this is a cloud file (Google Drive), please ensure it is downloaded or try moving it to Internal Storage.")

                Log.d(TAG, "Saving to temp file: $fileName")
                val tempFile = File(context.cacheDir, fileName)
                
                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Archive copied to: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
                val tempDir = File(context.cacheDir, "restore_preview_${System.currentTimeMillis()}").apply { mkdirs() }
                val reader = com.semseytech.rtsdevicesuitepro.restore.engine.ArchiveReader(tempFile)
                
                // Optimized: Only extract manifest and data indices for preview
                val manifestFile = File(tempDir, "manifest.json")
                reader.extractFile("manifest.json", manifestFile)
                
                val callsFile = File(tempDir, "data/calls.json")
                val contactsFile = File(tempDir, "data/contacts.json")
                val settingsFile = File(tempDir, "data/settings.json")
                
                reader.extractFile("data/calls.json", callsFile)
                reader.extractFile("data/contacts.json", contactsFile)
                reader.extractFile("data/settings.json", settingsFile)
                
                Log.d(TAG, "Checking for manifest at: ${manifestFile.absolutePath}")
                if (manifestFile.exists()) {
                    val manifestJson = manifestFile.readText()
                    val gson = com.google.gson.Gson()
                    val manifest = gson.fromJson(manifestJson, BackupManifest::class.java)
                    
                    val categories = manifest.entries.groupBy { 
                        if (it.itemType == "JsonData") "JsonData_${it.category}" else it.itemType 
                    }.map { (groupKey, entries) ->
                        val first = entries.first()
                        val type = first.itemType
                        val catName = when (type) {
                            "SmsThread" -> "SMS/MMS Threads"
                            "CallLog" -> "Call Logs"
                            "Contact" -> "Contacts"
                            "Apk" -> "Installed APKs"
                            "UserFile" -> first.category
                            "JsonData" -> first.category.replaceFirstChar { it.uppercase() } ?: "Data"
                            else -> type
                        }

                        val items = if (type == "JsonData") {
                            val catId = first.category
                            when (catId) {
                                "calls" -> if (callsFile.exists()) {
                                    val callType = object : TypeToken<List<BackupItem.CallLogEntry>>() {}.type
                                    gson.fromJson<List<BackupItem.CallLogEntry>>(callsFile.readText(), callType)
                                } else emptyList()
                                "contacts" -> if (contactsFile.exists()) {
                                    val contactType = object : TypeToken<List<BackupItem.Contact>>() {}.type
                                    gson.fromJson<List<BackupItem.Contact>>(contactsFile.readText(), contactType)
                                } else emptyList()
                                "settings" -> if (settingsFile.exists()) {
                                    val settingType = object : TypeToken<List<BackupItem.SystemSetting>>() {}.type
                                    gson.fromJson<List<BackupItem.SystemSetting>>(settingsFile.readText(), settingType)
                                } else emptyList()
                                else -> entries.map { createBackupItemFromEntry(it) }
                            }
                        } else {
                            entries.map { createBackupItemFromEntry(it) }
                        }

                        BackupCategory(
                            id = groupKey.lowercase(),
                            name = catName,
                            items = items
                        )
                    }
                    
                    _uiState.update { it.copy(
                        isLoading = false, 
                        selectedUri = uri, 
                        selectedFileName = fileName,
                        categories = categories,
                        status = "Archive loaded: ${manifest.entries.size} items found"
                    ) }
                } else {
                    _uiState.update { it.copy(isLoading = false, status = "Invalid archive: manifest.json missing") }
                }
                
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                Log.e(TAG, "Load failed", e)
                val errorMessage = when {
                    e.message?.contains("Cello error 8") == true -> 
                        "Load failed (Samsung error 8). Please try selecting the file directly from 'Internal Storage' instead of 'Recent files'."
                    e.message?.contains("Cello error 2") == true ->
                        "Load failed (Samsung error 2: Access Denied). Please ensure the file is not in a protected folder or try copying it to Downloads first."
                    e.message?.contains("ope: Cello error 2") == true ->
                        "Load failed (Samsung error 2). Access was denied by the system. Try moving the file to your Downloads folder and picking it from there."
                    else -> "Load failed: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, status = errorMessage) }
            }
        }
    }

    private fun createBackupItemFromEntry(entry: ManifestEntry): BackupItem {
        return when (entry.itemType) {
            "SMS", "SmsThread" -> BackupItem.SmsMessage(
                id = entry.identifier,
                sender = entry.itemName.replace("Thread ", ""),
                snippet = "",
                date = entry.date,
                size = entry.size
            )
            "Call Log" -> BackupItem.CallLogEntry(
                id = entry.identifier,
                number = entry.itemName,
                latestType = "Unknown",
                date = entry.date,
                totalDuration = 0L,
                size = entry.size
            )
            "Contact" -> BackupItem.Contact(
                id = entry.identifier,
                name = entry.itemName,
                phoneNumbers = emptyList(),
                emails = emptyList(),
                size = entry.size,
                date = entry.date
            )
            "APK" -> BackupItem.Apk(
                id = entry.identifier,
                appName = entry.itemName,
                packageName = entry.identifier,
                version = "",
                size = entry.size,
                date = entry.date
            )
            "SmsIndex" -> BackupItem.UserFile(
                id = "sms_index",
                fileName = "SMS Index (Manifest)",
                size = entry.size,
                path = "",
                mimeType = "application/json",
                date = entry.date,
                type = "Metadata"
            )
            "JsonData" -> {
                // Determine specific type from category if it's generic JsonData
                when (entry.category) {
                    "calls" -> BackupItem.CallLogEntry(entry.identifier, entry.itemName, "History", entry.date, 0L, size = entry.size)
                    "contacts" -> BackupItem.Contact(entry.identifier, entry.itemName, emptyList(), emptyList(), size = entry.size, date = entry.date)
                    else -> BackupItem.UserFile(entry.identifier, entry.itemName, entry.size, "", "", entry.date, entry.category)
                }
            }
            else -> BackupItem.UserFile(
                id = entry.identifier,
                fileName = entry.itemName,
                size = entry.size,
                path = entry.originalPath ?: "",
                mimeType = "",
                date = entry.date,
                type = entry.itemType
            )
        }
    }

    fun startRestore() {
        val selectedItems = _uiState.value.categories.flatMap { it.items.filter { it.isSelected } }
        val fileName = _uiState.value.selectedFileName ?: "restore_temp_archive.zip"

        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, progress = 0f, status = "Starting restore...") }
            
            val context = getApplication<Application>()
            val archiveFile = File(context.cacheDir, fileName)
            
            val report = engine.runRestore(archiveFile, selectedItems) { progress, status ->
                _uiState.update { it.copy(progress = progress, status = status) }
            }

            _uiState.update { it.copy(isRestoring = false, report = report, status = "Restore completed") }
        }
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
                    category.copy(items = newItems, isAllSelected = newItems.isNotEmpty() && newItems.all { it.isSelected })
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

    fun toggleMasterSelection() {
        val newState = !_uiState.value.isMasterSelected
        _uiState.update { state ->
            state.copy(
                isMasterSelected = newState,
                categories = state.categories.map { category ->
                    category.copy(
                        isAllSelected = newState,
                        items = category.items.map { item ->
                            when (item) {
                                is BackupItem.SmsMessage -> item.copy(isSelected = newState)
                                is BackupItem.CallLogEntry -> item.copy(isSelected = newState)
                                is BackupItem.Contact -> item.copy(isSelected = newState)
                                is BackupItem.Apk -> item.copy(isSelected = newState)
                                is BackupItem.UserFile -> item.copy(isSelected = newState)
                                is BackupItem.SystemSetting -> item.copy(isSelected = newState)
                            }
                        }
                    )
                }
            )
        }
    }

    fun checkDefaultSmsStatus() {
        val context = getApplication<Application>()
        val isDefault = android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        _uiState.update { it.copy(isDefaultSmsApp = isDefault) }
    }

    fun clearRestoreCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            _uiState.value.selectedFileName?.let { name ->
                File(context.cacheDir, name).delete()
            }
            File(context.cacheDir, "restore_temp_archive").delete()
            _uiState.update { it.copy(categories = emptyList(), selectedUri = null, selectedFileName = null, report = null) }
        }
    }
}

data class RestoreUiState(
    val categories: List<BackupCategory> = emptyList(),
    val isLoading: Boolean = false,
    val isRestoring: Boolean = false,
    val status: String = "Select an archive",
    val progress: Float = 0f,
    val selectedUri: Uri? = null,
    val selectedFileName: String? = null,
    val isMasterSelected: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val report: RestoreReport? = null
)
