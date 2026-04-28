package com.semseytech.rtsdevicesuitepro.restore

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.semseytech.rtsdevicesuitepro.backup.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipInputStream

class RestoreViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "RestoreViewModel"
    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    fun loadArchive(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, status = "Reading archive...") }
            try {
                val categories = mutableListOf<BackupCategory>()
                val tempDir = File(getApplication<Application>().cacheDir, "restore_temp")
                if (tempDir.exists()) tempDir.deleteRecursively()
                tempDir.mkdirs()

                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) ?: return@launch
                val fileName = getFileName(uri)?.lowercase() ?: "archive"

                try {
                    when {
                        fileName.endsWith(".zip") -> unzip(inputStream, tempDir)
                        fileName.endsWith(".tar") -> untar(inputStream, tempDir, false)
                        fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> untar(inputStream, tempDir, true)
                        else -> unzip(inputStream, tempDir) // Fallback to zip
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to extract archive: $fileName", e)
                }

                val manifestFile = File(tempDir, "manifest.json")
                if (manifestFile.exists()) {
                    val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
                    
                    val smsMap = try {
                        val file = File(tempDir, "data/sms.json")
                        if (file.exists()) {
                            val type = object : TypeToken<List<BackupItem.SmsMessage>>() {}.type
                            gson.fromJson<List<BackupItem.SmsMessage>>(file.readText(), type).associateBy { it.id }
                        } else emptyMap()
                    } catch (e: Exception) { emptyMap() }

                    val callMap = try {
                        val file = File(tempDir, "data/calls.json")
                        if (file.exists()) {
                            val type = object : TypeToken<List<BackupItem.CallLogEntry>>() {}.type
                            gson.fromJson<List<BackupItem.CallLogEntry>>(file.readText(), type).associateBy { it.id }
                        } else emptyMap()
                    } catch (e: Exception) { emptyMap() }

                    val contactMap = try {
                        val file = File(tempDir, "data/contacts.json")
                        if (file.exists()) {
                            val type = object : TypeToken<List<BackupItem.Contact>>() {}.type
                            gson.fromJson<List<BackupItem.Contact>>(file.readText(), type).associateBy { it.id }
                        } else emptyMap()
                    } catch (e: Exception) { emptyMap() }

                    val settingsMap = try {
                        val file = File(tempDir, "data/settings.json")
                        if (file.exists()) {
                            val type = object : TypeToken<List<BackupItem.UserSetting>>() {}.type
                            gson.fromJson<List<BackupItem.UserSetting>>(file.readText(), type).associateBy { it.id }
                        } else emptyMap()
                    } catch (e: Exception) { emptyMap() }

                    val groupedEntries = manifest.entries.groupBy { it.category }
                    
                    groupedEntries.forEach { (catName, entries) ->
                        val items = entries.map { entry ->
                            when (entry.itemType) {
                                "SmsMessage" -> {
                                    val orig = smsMap[entry.identifier]
                                    BackupItem.SmsMessage(
                                        id = entry.identifier, 
                                        sender = entry.itemName, 
                                        snippet = orig?.snippet ?: "", 
                                        date = orig?.date ?: 0L, 
                                        messages = orig?.messages ?: emptyList()
                                    )
                                }
                                "CallLogEntry" -> {
                                    val orig = callMap[entry.identifier]
                                    BackupItem.CallLogEntry(
                                        id = entry.identifier, 
                                        number = entry.itemName, 
                                        latestType = orig?.latestType ?: "Unknown", 
                                        latestDate = orig?.latestDate ?: 0L, 
                                        totalDuration = orig?.totalDuration ?: 0L,
                                        calls = orig?.calls ?: emptyList()
                                    )
                                }
                                "Contact" -> {
                                    val orig = contactMap[entry.identifier]
                                    BackupItem.Contact(entry.identifier, entry.itemName, orig?.phoneNumbers ?: emptyList(), orig?.emails ?: emptyList())
                                }
                                "Apk" -> BackupItem.Apk(entry.identifier, entry.itemName, entry.identifier, "1.0")
                                "UserFile" -> {
                                    val storagePath = Environment.getExternalStorageDirectory().absolutePath
                                    val relativePath = if (entry.filePath?.startsWith(storagePath) == true) {
                                        entry.filePath.substring(storagePath.length).trimStart(File.separatorChar)
                                    } else entry.itemName
                                    val size = File(tempDir, "files/$relativePath").length()
                                    BackupItem.UserFile(entry.identifier, entry.itemName, size, entry.filePath ?: "", "")
                                }
                                "UserSetting" -> {
                                    val orig = settingsMap[entry.identifier]
                                    BackupItem.UserSetting(entry.identifier, entry.itemName, orig?.value ?: "")
                                }
                                "LauncherConfig" -> BackupItem.LauncherConfig(entry.identifier, entry.itemName)
                                else -> BackupItem.UserSetting(entry.identifier, entry.itemName, "")
                            }
                        }
                        val catId = when(catName) {
                            "Text Messages (SMS/MMS)" -> "sms"
                            "Call Log" -> "calls"
                            "Contacts" -> "contacts"
                            "APKs (User Apps)" -> "apks"
                            "User Files" -> "files"
                            "Launcher Backup" -> "launcher"
                            "User Settings" -> "settings"
                            else -> catName.lowercase().replace(" ", "_")
                        }
                        categories.add(BackupCategory(catId, catName, items))
                    }
                }

                _uiState.update { it.copy(categories = categories, isLoading = false, selectedUri = uri, status = "Archive loaded") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load archive", e)
                _uiState.update { it.copy(isLoading = false, status = "Error: ${e.message}") }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }
        return name ?: uri.path?.substringAfterLast('/')
    }

    private fun unzip(inputStream: InputStream, targetDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                }
                entry = zis.nextEntry
            }
        }
    }

    private fun untar(inputStream: InputStream, targetDir: File, gzip: Boolean) {
        val bis = if (gzip) GzipCompressorInputStream(inputStream) else inputStream
        TarArchiveInputStream(bis).use { tais ->
            var entry = tais.nextTarEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos -> tais.copyTo(fos) }
                }
                entry = tais.nextTarEntry
            }
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
                        if (item.id == itemId) updateItemSelection(item, !item.isSelected) else item
                    }
                    category.copy(
                        items = newItems,
                        isAllSelected = newItems.all { it.isSelected }
                    )
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
                        items = category.items.map { updateItemSelection(it, isSelected) }
                    )
                } else category
            })
        }
    }

    private fun updateItemSelection(item: BackupItem, isSelected: Boolean): BackupItem {
        return when (item) {
            is BackupItem.SmsMessage -> item.copy(isSelected = isSelected)
            is BackupItem.CallLogEntry -> item.copy(isSelected = isSelected)
            is BackupItem.Contact -> item.copy(isSelected = isSelected)
            is BackupItem.Apk -> item.copy(isSelected = isSelected)
            is BackupItem.UserFile -> item.copy(isSelected = isSelected)
            is BackupItem.LauncherConfig -> item.copy(isSelected = isSelected)
            is BackupItem.UserSetting -> item.copy(isSelected = isSelected)
        }
    }

    fun toggleMasterSelection() {
        _uiState.update { state ->
            val allSelected = !state.isMasterSelected
            state.copy(
                isMasterSelected = allSelected,
                categories = state.categories.map { category ->
                    category.copy(
                        isAllSelected = allSelected,
                        items = category.items.map { updateItemSelection(it, allSelected) }
                    )
                }
            )
        }
    }

    fun startRestore() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRestoring = true, progress = 0f, status = "Starting restore...", report = null) }
            val tempDir = File(getApplication<Application>().cacheDir, "restore_temp")
            
            val reportDetails = mutableListOf<String>()
            var restoredTotal = 0
            var skippedTotal = 0
            var errorsTotal = 0

            try {
                val selectedCategories = _uiState.value.categories
                val totalSelectedItems = selectedCategories.sumOf { it.items.count { item -> item.isSelected } }
                var processedItems = 0

                selectedCategories.forEach { category ->
                    val selectedItems = category.items.filter { it.isSelected }
                    if (selectedItems.isEmpty()) return@forEach

                    _uiState.update { it.copy(status = "Restoring ${category.name}...") }
                    
                    val result = when (category.id) {
                        "sms" -> restoreSms(tempDir, selectedItems.filterIsInstance<BackupItem.SmsMessage>()) { 
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        "calls" -> restoreCalls(tempDir, selectedItems.filterIsInstance<BackupItem.CallLogEntry>()) {
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        "contacts" -> restoreContacts(tempDir, selectedItems.filterIsInstance<BackupItem.Contact>()) {
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        "apks" -> restoreApks(tempDir, selectedItems.filterIsInstance<BackupItem.Apk>()) {
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        "files" -> restoreFiles(tempDir, selectedItems.filterIsInstance<BackupItem.UserFile>()) {
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        "settings" -> restoreSettings(selectedItems.filterIsInstance<BackupItem.UserSetting>()) {
                            processedItems++
                            _uiState.update { it.copy(progress = processedItems.toFloat() / totalSelectedItems) }
                        }
                        else -> {
                            processedItems += selectedItems.size
                            RestoreResult(0, 0, 0, listOf("Category ${category.name} not supported for restoration yet."))
                        }
                    }

                    restoredTotal += result.restored
                    skippedTotal += result.skipped
                    errorsTotal += result.errors
                    reportDetails.addAll(result.details)
                }

                val finalReport = RestoreReport(restoredTotal, skippedTotal, errorsTotal, reportDetails)
                _uiState.update { it.copy(isRestoring = false, status = "Restore completed", report = finalReport) }
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _uiState.update { it.copy(isRestoring = false, status = "Restore failed: ${e.message}") }
            }
        }
    }

    data class RestoreResult(val restored: Int, val skipped: Int, val errors: Int, val details: List<String>)

    private suspend fun restoreSms(tempDir: File, selectedItems: List<BackupItem.SmsMessage>, onProgress: () -> Unit): RestoreResult {
        var restored = 0
        var skipped = 0
        var errors = 0
        val details = mutableListOf<String>()

        selectedItems.forEach { thread ->
            thread.messages.forEach { msg ->
                onProgress()
                try {
                    if (msg.isMms) {
                        if (restoreMmsItem(tempDir, msg)) {
                            restored++
                            kotlinx.coroutines.delay(15) // Small delay to prevent system overload
                        } else {
                            errors++
                        }
                    } else {
                        if (restoreSmsItem(msg, thread.sender)) {
                            restored++
                        } else {
                            skipped++ // Counting duplicates as skipped
                        }
                    }
                } catch (e: Exception) {
                    errors++
                    details.add("Error restoring message: ${e.message}")
                }
            }
        }
        
        getApplication<Application>().contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
        getApplication<Application>().contentResolver.notifyChange(Telephony.Mms.CONTENT_URI, null)
        
        details.add("Restore completed: $restored restored, $skipped skipped, $errors errors")
        return RestoreResult(restored, skipped, errors, details)
    }

    private fun restoreSmsItem(msg: BackupItem.MessageDetail, address: String): Boolean {
        val contentResolver = getApplication<Application>().contentResolver
        
        // Check for duplicates
        val cursor = contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID),
            "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} = ?",
            arrayOf(address, msg.body, msg.date.toString()),
            null
        )
        
        val exists = cursor?.use { it.count > 0 } ?: false
        if (exists) return false

        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, msg.body)
            put(Telephony.Sms.DATE, msg.date)
            put(Telephony.Sms.DATE_SENT, msg.dateSent)
            put(Telephony.Sms.READ, msg.read)
            put(Telephony.Sms.TYPE, msg.type)
            if (msg.threadId != 0L) {
                put(Telephony.Sms.THREAD_ID, msg.threadId)
            }
        }

        val uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        return uri != null
    }

    private fun restoreMmsItem(tempDir: File, msg: BackupItem.MessageDetail): Boolean {
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val dateInSeconds = msg.date / 1000

            // Check for duplicates
            val cursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID),
                "${Telephony.Mms.DATE} = ? AND ${Telephony.Mms.MESSAGE_BOX} = ?",
                arrayOf(dateInSeconds.toString(), msg.type.toString()),
                null
            )
            val exists = cursor?.use { it.count > 0 } ?: false
            if (exists) return true

            val values = ContentValues().apply {
                put(Telephony.Mms.MESSAGE_BOX, msg.type)
                put(Telephony.Mms.DATE, dateInSeconds)
                put(Telephony.Mms.DATE_SENT, msg.dateSent / 1000)
                put(Telephony.Mms.READ, msg.read)
                put(Telephony.Mms.SEEN, 1)
                put(Telephony.Mms.SUBJECT, msg.subject)
                put("sub_cs", msg.subCs)
                put(Telephony.Mms.MESSAGE_TYPE, msg.mType)
                put(Telephony.Mms.CONTENT_TYPE, msg.contentType ?: "application/vnd.wap.multipart.related")
                put(Telephony.Mms.MESSAGE_CLASS, "personal")
                put(Telephony.Mms.MMS_VERSION, 18)
                if (msg.threadId != 0L) {
                    put(Telephony.Mms.THREAD_ID, msg.threadId)
                }
            }

            val mmsUri = contentResolver.insert(Telephony.Mms.CONTENT_URI, values) ?: return false
            val mmsId = ContentUris.parseId(mmsUri)

            // Insert Parts
            msg.attachments.forEach { att ->
                val partValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", att.contentType)
                    put("fn", att.fileName)
                    put("name", att.fileName)
                    put("cd", "inline")
                }
                
                val partUri = Uri.parse("content://mms/part")
                
                if (att.contentType == "text/plain") {
                    val textContent = att.text ?: msg.body
                    partValues.put("text", textContent)
                    contentResolver.insert(partUri, partValues)
                } else {
                    val insertedPartUri = contentResolver.insert(partUri, partValues)
                    if (insertedPartUri != null) {
                        val archivedFile = File(tempDir, "mms/part_${att.partId}")
                        if (archivedFile.exists()) {
                            contentResolver.openOutputStream(insertedPartUri)?.use { os ->
                                archivedFile.inputStream().use { it.copyTo(os) }
                            }
                        }
                    }
                }
            }

            // Insert Addresses
            msg.addresses.forEach { addr ->
                val addrValues = ContentValues().apply {
                    put("address", addr.address)
                    put("type", addr.type)
                    put("charset", addr.charset)
                }
                contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), addrValues)
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring MMS item", e)
            return false
        }
    }

    private fun restoreMmsAttachments(tempDir: File, messageUri: Uri, attachments: List<BackupItem.MmsAttachment>) {
        // Deprecated: merged into restoreMmsItem
    }

    private fun restoreCalls(tempDir: File, selectedItems: List<BackupItem.CallLogEntry>, onProgress: () -> Unit): RestoreResult {
        var restored = 0
        var skipped = 0
        var errors = 0
        val details = mutableListOf<String>()

        selectedItems.forEach { entry ->
            onProgress()
            entry.calls.forEach { call ->
                try {
                    val cursor = getApplication<Application>().contentResolver.query(
                        CallLog.Calls.CONTENT_URI,
                        arrayOf(CallLog.Calls._ID),
                        "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ? AND ${CallLog.Calls.TYPE} = ?",
                        arrayOf(entry.number, call.date.toString(), call.type.toString()),
                        null
                    )
                    val exists = cursor?.use { it.count > 0 } ?: false
                    if (exists) {
                        skipped++
                    } else {
                        val values = ContentValues().apply {
                            put(CallLog.Calls.NUMBER, entry.number)
                            put(CallLog.Calls.DATE, call.date)
                            put(CallLog.Calls.DURATION, call.duration)
                            put(CallLog.Calls.TYPE, call.type)
                        }
                        val uri = getApplication<Application>().contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
                        if (uri != null) restored++ else errors++
                    }
                } catch (e: Exception) { 
                    errors++
                    details.add("Call Error (${entry.number}): ${e.message}")
                }
            }
        }
        details.add("Calls: Restored $restored, Skipped $skipped, Errors $errors")
        return RestoreResult(restored, skipped, errors, details)
    }

    private fun restoreContacts(tempDir: File, selectedItems: List<BackupItem.Contact>, onProgress: () -> Unit): RestoreResult {
        var restored = 0
        var skipped = 0
        var errors = 0
        val details = mutableListOf<String>()

        selectedItems.forEach { contact ->
            onProgress()
            try {
                var alreadyExists = false
                if (contact.phoneNumbers.isNotEmpty()) {
                    for (phone in contact.phoneNumbers) {
                        val cursor = getApplication<Application>().contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                            arrayOf(contact.name, phone),
                            null
                        )
                        if (cursor?.use { it.count > 0 } == true) { alreadyExists = true; break }
                    }
                } else {
                    val cursor = getApplication<Application>().contentResolver.query(
                        ContactsContract.Contacts.CONTENT_URI,
                        arrayOf(ContactsContract.Contacts._ID),
                        "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
                        arrayOf(contact.name),
                        null
                    )
                    if (cursor?.use { it.count > 0 } == true) alreadyExists = true
                }

                if (alreadyExists) {
                    skipped++
                } else {
                    val values = ContentValues().apply {
                        put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
                        put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
                    }
                    val rawContactUri = getApplication<Application>().contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
                    val rawContactId = ContentUris.parseId(rawContactUri!!)
                    
                    val nameValues = ContentValues().apply {
                        put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    }
                    getApplication<Application>().contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)
                    
                    contact.phoneNumbers.forEach { phone ->
                        val phoneValues = ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        }
                        getApplication<Application>().contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
                    }
                    
                    contact.emails.forEach { email ->
                        val emailValues = ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                            put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        }
                        getApplication<Application>().contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
                    }
                    restored++
                }
            } catch (e: Exception) { 
                errors++
                details.add("Contact Error (${contact.name}): ${e.message}")
            }
        }
        details.add("Contacts: Restored $restored, Skipped $skipped, Errors $errors")
        return RestoreResult(restored, skipped, errors, details)
    }

    private fun restoreApks(tempDir: File, items: List<BackupItem.Apk>, onProgress: () -> Unit): RestoreResult {
        var restored = 0
        var errors = 0
        val details = mutableListOf<String>()
        items.forEach { apk ->
            onProgress()
            try {
                val apkFile = File(tempDir, "apks/${apk.packageName}.apk")
                if (apkFile.exists()) {
                    val apkUri = FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.fileprovider", apkFile)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    getApplication<Application>().startActivity(intent)
                    restored++
                } else {
                    errors++
                    details.add("APK missing: ${apk.packageName}")
                }
            } catch (e: Exception) { errors++; details.add("APK Error (${apk.appName}): ${e.message}") }
        }
        return RestoreResult(restored, 0, errors, details)
    }

    private fun restoreFiles(tempDir: File, items: List<BackupItem.UserFile>, onProgress: () -> Unit): RestoreResult {
        if (!checkStoragePermission()) return RestoreResult(0, 0, items.size, listOf("Storage permission denied"))
        var restored = 0
        var errors = 0
        val details = mutableListOf<String>()
        items.forEach { userFile ->
            onProgress()
            try {
                val storagePath = Environment.getExternalStorageDirectory().absolutePath
                val relativePath = if (userFile.path.startsWith(storagePath)) {
                    userFile.path.substring(storagePath.length).trimStart(File.separatorChar)
                } else userFile.fileName
                
                val archivedFile = File(tempDir, "files/$relativePath")
                if (archivedFile.exists()) {
                    val targetFile = File(userFile.path)
                    targetFile.parentFile?.mkdirs()
                    archivedFile.copyTo(targetFile, overwrite = true)
                    restored++
                } else { errors++; details.add("File missing: ${userFile.fileName}") }
            } catch (e: Exception) { errors++; details.add("File Error (${userFile.fileName}): ${e.message}") }
        }
        return RestoreResult(restored, 0, errors, details)
    }

    private fun restoreSettings(items: List<BackupItem.UserSetting>, onProgress: () -> Unit): RestoreResult {
        var restored = 0
        var errors = 0
        val details = mutableListOf<String>()
        items.forEach { setting ->
            onProgress()
            try {
                android.provider.Settings.System.putString(getApplication<Application>().contentResolver, setting.settingName, setting.value)
                restored++
            } catch (e: Exception) { errors++; details.add("Setting Error (${setting.settingName}): ${e.message}") }
        }
        return RestoreResult(restored, 0, errors, details)
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED
        }
    }

    fun cancelRestore() {
        _uiState.update { it.copy(isRestoring = false, status = "Restore cancelled") }
        clearRestoreCache()
    }

    fun clearRestoreCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(getApplication<Application>().cacheDir, "restore_temp")
                if (tempDir.exists()) {
                    tempDir.deleteRecursively()
                    Log.d(TAG, "Restore cache cleared")
                }
                _uiState.update { it.copy(categories = emptyList(), selectedUri = null, status = "Cache cleared") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear restore cache", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearRestoreCache()
    }
}

data class RestoreUiState(
    val categories: List<BackupCategory> = emptyList(),
    val isLoading: Boolean = false,
    val isRestoring: Boolean = false,
    val status: String = "Select an archive to begin",
    val progress: Float = 0f,
    val selectedUri: Uri? = null,
    val isMasterSelected: Boolean = false,
    val report: RestoreReport? = null
)

data class RestoreReport(
    val restoredCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val details: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
