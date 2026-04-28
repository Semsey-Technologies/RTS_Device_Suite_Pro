package com.semseytech.rtsdevicesuitepro.backup

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Environment
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.backup.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.*
import android.util.Log
import android.app.role.RoleManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import org.apache.commons.compress.archivers.tar.*
import org.apache.commons.compress.compressors.gzip.*

enum class ArchiveType(val extension: String) {
    ZIP(".zip"),
    TAR(".tar"),
    TAR_GZ(".tar.gz")
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BackupViewModel"
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun updateDefaultSmsStatus(isDefault: Boolean) {
        _uiState.update { it.copy(isDefaultSmsApp = isDefault) }
    }

    init {
        checkDefaultSmsStatus()
    }

    fun checkDefaultSmsStatus() {
        val packageName = getApplication<Application>().packageName
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getApplication<Application>().getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            Telephony.Sms.getDefaultSmsPackage(getApplication()) == packageName
        }
        _uiState.update { it.copy(isDefaultSmsApp = isDefault) }
    }

    fun loadRealData() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting data load...")
            checkDefaultSmsStatus()
            _uiState.update { it.copy(isLoading = true) }
            val categories = mutableListOf<BackupCategory>()

            try {
                // 1. SMS
                val sms = fetchSms()
                Log.d(TAG, "Fetched ${sms.size} SMS")
                categories.add(BackupCategory("sms", "Text Messages (SMS/MMS)", sms))

                // 2. Call Logs
                val calls = fetchCallLogs()
                Log.d(TAG, "Fetched ${calls.size} calls")
                categories.add(BackupCategory("calls", "Call Log", calls))

                // 3. Contacts
                val contacts = fetchContacts()
                Log.d(TAG, "Fetched ${contacts.size} contacts")
                categories.add(BackupCategory("contacts", "Contacts", contacts))

                // 4. APKs
                val apks = fetchApks()
                Log.d(TAG, "Fetched ${apks.size} APKs")
                categories.add(BackupCategory("apks", "APKs (User Apps)", apks))

                // 5. User Files (grouped)
                val files = fetchFiles()
                Log.d(TAG, "Fetched ${files.size} files")
                categories.add(BackupCategory("files", "User Files", files))

                // 6. Launcher (Layout/Config)
                categories.add(BackupCategory("launcher", "Launcher Backup", fetchLauncherConfig()))
                // 7. Settings
                categories.add(BackupCategory("settings", "User Settings", fetchSettings()))

            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
            }

            _uiState.update { it.copy(categories = categories, isLoading = false) }
            Log.d(TAG, "Data load complete. Categories: ${categories.size}")
        }
    }

    private fun fetchLauncherConfig(): List<BackupItem.LauncherConfig> {
        return listOf(
            BackupItem.LauncherConfig("l1", "Home Screen Layout"),
            BackupItem.LauncherConfig("l2", "App Drawer Configuration")
        )
    }

    private fun fetchSettings(): List<BackupItem.UserSetting> {
        return listOf(
            BackupItem.UserSetting("st1", "Wallpaper", "Current System Wallpaper"),
            BackupItem.UserSetting("st2", "Wi-Fi Networks", "Known Networks (Config Only)")
        )
    }

    private val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()

    private fun fetchSms(): List<BackupItem.SmsMessage> {
        val threads = mutableMapOf<String, MutableList<BackupItem.MessageDetail>>()
        try {
            // Fetch SMS
            val smsCursor: Cursor? = getApplication<Application>().contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.DATE_SENT,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ,
                    Telephony.Sms.THREAD_ID
                ),
                null, null, Telephony.Sms.DATE + " DESC"
            )

            smsCursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val dateSentIdx = it.getColumnIndex(Telephony.Sms.DATE_SENT)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)
                val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)

                while (it.moveToNext()) {
                    val address = it.getString(addrIdx) ?: "Unknown"
                    val detail = BackupItem.MessageDetail(
                        id = it.getString(idIdx),
                        body = it.getString(bodyIdx) ?: "",
                        date = it.getLong(dateIdx),
                        dateSent = it.getLong(dateSentIdx),
                        type = it.getInt(typeIdx),
                        read = it.getInt(readIdx),
                        threadId = it.getLong(threadIdIdx),
                        isMms = false
                    )
                    threads.getOrPut(address) { mutableListOf() }.add(detail)
                }
            }

            // Fetch MMS
            val mmsCursor = getApplication<Application>().contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID, Telephony.Mms.MESSAGE_BOX),
                null, null, Telephony.Mms.DATE + " DESC"
            )

            mmsCursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Mms._ID)
                val dateIdx = it.getColumnIndex(Telephony.Mms.DATE)
                val threadIdIdx = it.getColumnIndex(Telephony.Mms.THREAD_ID)
                val boxIdx = it.getColumnIndex(Telephony.Mms.MESSAGE_BOX)

                while (it.moveToNext()) {
                    val mmsId = it.getString(idIdx)
                    val threadId = it.getLong(threadIdIdx)
                    val date = it.getLong(dateIdx) * 1000 // MMS date is in seconds
                    val type = it.getInt(boxIdx)
                    
                    val attachments = getMmsAttachments(mmsId)
                    val addresses = getMmsAddresses(mmsId)
                    
                    // Improved body extraction: try to find the text/plain part and read it
                    var body = "MMS Message"
                    attachments.firstOrNull { it.contentType == "text/plain" }?.let { textPart ->
                         val textBody = getMmsPartText(textPart.partId)
                         if (textBody.isNotEmpty()) {
                             body = textBody
                         }
                    }

                    val subIdx = it.getColumnIndex(Telephony.Mms.SUBJECT)
                    val subCsIdx = it.getColumnIndex("sub_cs")
                    val subject = if (subIdx != -1) it.getString(subIdx) else null
                    val subCs = if (subCsIdx != -1) it.getInt(subCsIdx) else 106

                    val detail = BackupItem.MessageDetail(
                        id = mmsId,
                        body = body,
                        date = date,
                        dateSent = date,
                        type = type,
                        threadId = threadId,
                        isMms = true,
                        subject = subject,
                        subCs = subCs,
                        mType = 132,
                        contentType = "application/vnd.wap.multipart.related",
                        attachments = attachments,
                        addresses = addresses
                    )
                    
                    val fromAddress = addresses.find { addr -> addr.type == 137 }?.address ?: "Unknown"
                    threads.getOrPut(fromAddress) { mutableListOf() }.add(detail)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SMS/MMS", e)
        }

        return threads.map { (address, messages) ->
            val sortedMessages = messages.sortedByDescending { it.date }
            val latest = sortedMessages.first()
            BackupItem.SmsMessage(
                id = address,
                sender = address,
                snippet = latest.body,
                date = latest.date,
                messageCount = messages.size,
                messages = sortedMessages
            )
        }
    }

    private fun getMmsAddresses(mmsId: String): List<BackupItem.MmsAddress> {
        val addresses = mutableListOf<BackupItem.MmsAddress>()
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val addrIdx = it.getColumnIndex("address")
            val typeIdx = it.getColumnIndex("type")
            val charsetIdx = it.getColumnIndex("charset")
            while (it.moveToNext()) {
                val address = it.getString(addrIdx) ?: continue
                val type = it.getInt(typeIdx)
                val charset = it.getInt(charsetIdx)
                addresses.add(BackupItem.MmsAddress(address, type, charset))
            }
        }
        return addresses
    }

    private fun getMmsPartText(partId: String): String {
        val partUri = Uri.parse("content://mms/part/$partId")
        return try {
            getApplication<Application>().contentResolver.openInputStream(partUri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error reading MMS part text for part $partId", e)
            ""
        }
    }

    private fun getMmsAddress(mmsId: String): String? {
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val projection = arrayOf("address")
        val cursor = getApplication<Application>().contentResolver.query(
            uri, 
            projection, 
            "type=?", 
            arrayOf("137"), 
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                val addrIdx = it.getColumnIndex("address")
                if (addrIdx != -1) it.getString(addrIdx) else null
            } else null
        }
    }

    private fun getMmsAttachments(mmsId: String): List<BackupItem.MmsAttachment> {
        val attachments = mutableListOf<BackupItem.MmsAttachment>()
        val uri = Uri.parse("content://mms/part")
        val projection = arrayOf("_id", "ct", "fn", "name", "text")
        val cursor = getApplication<Application>().contentResolver.query(
            uri, 
            projection, 
            "mid=?", 
            arrayOf(mmsId), 
            null
        )
        cursor?.use {
            val idIdx = it.getColumnIndex("_id")
            val ctIdx = it.getColumnIndex("ct")
            val fnIdx = it.getColumnIndex("fn")
            val nameIdx = it.getColumnIndex("name")
            val textIdx = it.getColumnIndex("text")
            
            while (it.moveToNext()) {
                val contentType = it.getString(ctIdx) ?: ""
                if (contentType == "application/smil") continue
                
                val partId = it.getString(idIdx)
                val fileName = it.getString(fnIdx) ?: it.getString(nameIdx) ?: "part_$partId"
                
                var text: String? = null
                if (contentType == "text/plain") {
                    text = if (textIdx != -1) it.getString(textIdx) else null
                    // Fallback to streaming if the 'text' column is empty
                    if (text == null) {
                        text = getMmsPartText(partId)
                    }
                }
                
                attachments.add(BackupItem.MmsAttachment(partId, contentType, fileName, partId, text))
            }
        }
        return attachments
    }

    private fun fetchCallLogs(): List<BackupItem.CallLogEntry> {
        val history = mutableMapOf<String, MutableList<BackupItem.CallDetail>>()
        try {
            val cursor: Cursor? = getApplication<Application>().contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
                null, null, CallLog.Calls.DATE + " DESC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val number = it.getString(numIdx) ?: "Unknown"
                    val detail = BackupItem.CallDetail(
                        id = it.getString(idIdx),
                        type = it.getInt(typeIdx),
                        date = it.getLong(dateIdx),
                        duration = it.getLong(durIdx)
                    )
                    history.getOrPut(number) { mutableListOf() }.add(detail)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching call logs", e)
        }

        return history.map { (number, calls) ->
            val latest = calls.first()
            BackupItem.CallLogEntry(
                id = number,
                number = number,
                latestType = when (latest.type) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                },
                latestDate = latest.date,
                totalDuration = calls.sumOf { it.duration },
                callCount = calls.size,
                calls = calls
            )
        }
    }

    private fun fetchContacts(): List<BackupItem.Contact> {
        val list = mutableListOf<BackupItem.Contact>()
        try {
            val contentResolver = getApplication<Application>().contentResolver
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                
                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else continue
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                    
                    val phoneNumbers = mutableListOf<String>()
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    phoneCursor?.use { pc ->
                        val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (pc.moveToNext()) {
                            if (numberIndex != -1) {
                                val number = pc.getString(numberIndex)
                                if (number != null) phoneNumbers.add(number)
                            }
                        }
                    }
                    val emails = mutableListOf<String>()
                    val emailCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    emailCursor?.use { ec ->
                        val emailIndex = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        while (ec.moveToNext()) {
                            if (emailIndex != -1) {
                                val email = ec.getString(emailIndex)
                                if (email != null) emails.add(email)
                            }
                        }
                    }
                    list.add(BackupItem.Contact(id, name, phoneNumbers, emails))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun fetchApks(): List<BackupItem.Apk> {
        val list = mutableListOf<BackupItem.Apk>()
        try {
            val pm = getApplication<Application>().packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            list.addAll(apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map {
                    val info = pm.getPackageInfo(it.packageName, 0)
                    BackupItem.Apk(
                        it.packageName,
                        it.loadLabel(pm).toString(),
                        it.packageName,
                        info.versionName ?: "1.0",
                        it.sourceDir
                    )
                })
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun fetchFiles(): List<BackupItem.UserFile> {
        val list = mutableListOf<BackupItem.UserFile>()
        try {
            val storagePath = Environment.getExternalStorageDirectory()
            val directoriesToScan = listOf(
                Environment.DIRECTORY_DOWNLOADS,
                Environment.DIRECTORY_DOCUMENTS,
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_MUSIC
            )

            directoriesToScan.forEach { dirName ->
                val dir = File(storagePath, dirName)
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryRecursive(dir, list, depth = 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching files", e)
        }
        return list
    }

    private fun scanDirectoryRecursive(directory: File, list: MutableList<BackupItem.UserFile>, depth: Int) {
        if (depth > 5) return 
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectoryRecursive(file, list, depth + 1)
            } else if (file.isFile) {
                list.add(BackupItem.UserFile(
                    file.absolutePath,
                    file.name,
                    file.length(),
                    file.absolutePath,
                    "application/octet-stream"
                ))
            }
        }
    }

    fun toggleMasterBackup() {
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

    fun toggleCategoryExpansion(categoryId: String) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { 
                if (it.id == categoryId) it.copy(isExpanded = !it.isExpanded) else it 
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

    fun setArchiveType(type: ArchiveType) {
        _uiState.update { it.copy(selectedArchiveType = type) }
    }

    fun isDefaultSmsApp(): Boolean {
        val packageName = getApplication<Application>().packageName
        return Telephony.Sms.getDefaultSmsPackage(getApplication()) == packageName
    }

    fun startBackup(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, backupStatus = "Preparing archive...", backupProgress = 0f) }
            
            val backupDir = File(getApplication<Application>().getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val archiveType = _uiState.value.selectedArchiveType
            val archiveFile = File(backupDir, "RTS_Backup_$timestamp${archiveType.extension}")
            
            val selectedCategories = _uiState.value.categories
            val totalItems = selectedCategories.sumOf { it.items.count { item -> item.isSelected } }
            var processedItems = 0
            
            try {
                val fos = FileOutputStream(archiveFile)
                val archiveHelper: ArchiveHelper = when (archiveType) {
                    ArchiveType.ZIP -> ZipArchiveHelper(fos)
                    ArchiveType.TAR -> TarArchiveHelper(fos, false)
                    ArchiveType.TAR_GZ -> TarArchiveHelper(fos, true)
                }

                archiveHelper.use { helper ->
                    selectedCategories.forEach { category ->
                        val selectedItems = category.items.filter { it.isSelected }
                        if (selectedItems.isEmpty()) return@forEach

                        when (category.id) {
                                "sms" -> {
                                    _uiState.update { it.copy(backupStatus = "Exporting SMS/MMS...") }
                                    val items = selectedItems.filterIsInstance<BackupItem.SmsMessage>()
                                    
                                    // Backup attachments first
                                    items.forEach { thread ->
                                        thread.messages.filter { it.isMms }.forEach { msg ->
                                            msg.attachments.forEach { att ->
                                                if (att.text != null) return@forEach // Skip binary backup for text-only parts
                                                val partUri = Uri.parse("content://mms/part/${att.partId}")
                                                try {
                                                    getApplication<Application>().contentResolver.openInputStream(partUri)?.use { isr ->
                                                        val tempFile = File(backupDir, "mms_${att.partId}")
                                                        tempFile.outputStream().use { osr -> isr.copyTo(osr) }
                                                        helper.addFile(tempFile, "mms/part_${att.partId}")
                                                        tempFile.delete()
                                                    }
                                                } catch (e: Exception) { 
                                                    // For text parts, we might not have a stream if it's stored in the 'text' column
                                                    // but getMmsPartText already handled reading from stream if available.
                                                    Log.e(TAG, "Failed to backup MMS binary part ${att.partId}", e) 
                                                }
                                            }
                                        }
                                    }

                                    val json = gson.toJson(items)
                                    val tempFile = File(backupDir, "sms.json")
                                    tempFile.writeText(json)
                                    helper.addFile(tempFile, "data/sms.json")
                                    tempFile.delete()
                                    processedItems += items.size
                                }
                            "calls" -> {
                                _uiState.update { it.copy(backupStatus = "Exporting Call Logs...") }
                                val items = selectedItems.filterIsInstance<BackupItem.CallLogEntry>()
                                val json = gson.toJson(items)
                                val tempFile = File(backupDir, "calls.json")
                                tempFile.writeText(json)
                                helper.addFile(tempFile, "data/calls.json")
                                tempFile.delete()
                                processedItems += items.size
                            }
                            "contacts" -> {
                                _uiState.update { it.copy(backupStatus = "Exporting Contacts...") }
                                val items = selectedItems.filterIsInstance<BackupItem.Contact>()
                                val json = gson.toJson(items)
                                val tempFile = File(backupDir, "contacts.json")
                                tempFile.writeText(json)
                                helper.addFile(tempFile, "data/contacts.json")
                                tempFile.delete()
                                processedItems += items.size
                            }
                            "launcher", "settings" -> {
                                _uiState.update { it.copy(backupStatus = "Exporting ${category.name}...") }
                                val json = gson.toJson(selectedItems)
                                val tempFile = File(backupDir, "${category.id}.json")
                                tempFile.writeText(json)
                                helper.addFile(tempFile, "data/${category.id}.json")
                                tempFile.delete()
                                processedItems += selectedItems.size
                            }
                            "apks" -> {
                                selectedItems.forEach { item ->
                                    val apk = item as BackupItem.Apk
                                    apk.sourceDir?.let { path ->
                                        val file = File(path)
                                        if (file.exists()) {
                                            _uiState.update { it.copy(backupStatus = "Backing up APK: ${apk.appName}", backupProgress = processedItems.toFloat() / totalItems) }
                                            helper.addFile(file, "apks/${apk.packageName}.apk")
                                        }
                                    }
                                    processedItems++
                                }
                            }
                            "files" -> {
                                selectedItems.forEach { item ->
                                    val userFile = item as BackupItem.UserFile
                                    val file = File(userFile.path)
                                    if (file.exists()) {
                                        _uiState.update { it.copy(backupStatus = "Archiving: ${file.name}", backupProgress = processedItems.toFloat() / totalItems) }
                                        
                                        // Maintain directory structure relative to the scanned root
                                        val storagePath = Environment.getExternalStorageDirectory().absolutePath
                                        val relativePath = if (file.absolutePath.startsWith(storagePath)) {
                                            file.absolutePath.substring(storagePath.length).trimStart(File.separatorChar)
                                        } else {
                                            file.name
                                        }
                                        
                                        helper.addFile(file, "files/$relativePath")
                                    }
                                    processedItems++
                                }
                            }
                        }
                    }
                    
                    val manifest = generateManifest()
                    val manifestJson = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(manifest)
                    val manifestFile = File(backupDir, "manifest.json")
                    manifestFile.writeText(manifestJson)
                    helper.addFile(manifestFile, "manifest.json")
                    manifestFile.delete()
                }

                _uiState.update { it.copy(isLoading = false, backupStatus = "Backup completed", backupProgress = 1f) }
                onComplete(archiveFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                _uiState.update { it.copy(isLoading = false, backupStatus = "Backup failed: ${e.message}") }
            }
        }
    }

    fun generateManifest(): BackupManifest {
        val selectedItems = _uiState.value.categories.flatMap { cat ->
            cat.items.filter { it.isSelected }.map { item ->
                ManifestEntry(
                    category = cat.name,
                    itemName = item.displayName,
                    itemType = item::class.simpleName ?: "Unknown",
                    identifier = item.id,
                    filePath = (item as? BackupItem.UserFile)?.path ?: (item as? BackupItem.Apk)?.sourceDir
                )
            }
        }
        return BackupManifest(
            timestamp = System.currentTimeMillis(),
            deviceName = android.os.Build.MODEL,
            entries = selectedItems
        )
    }

    interface ArchiveHelper : AutoCloseable {
        fun addFile(file: File, entryName: String)
    }

    private inner class ZipArchiveHelper(fos: FileOutputStream) : ArchiveHelper {
        private val zos = ZipOutputStream(fos)
        override fun addFile(file: File, entryName: String) {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
        override fun close() = zos.close()
    }

    private inner class TarArchiveHelper(fos: FileOutputStream, compress: Boolean) : ArchiveHelper {
        private val bos = if (compress) GzipCompressorOutputStream(fos) else fos
        private val tos = TarArchiveOutputStream(bos).apply {
            setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
        }
        override fun addFile(file: File, entryName: String) {
            val entry = tos.createArchiveEntry(file, entryName)
            tos.putArchiveEntry(entry)
            FileInputStream(file).use { it.copyTo(tos) }
            tos.closeArchiveEntry()
        }
        override fun close() = tos.close()
    }
}

data class BackupUiState(
    val categories: List<BackupCategory> = emptyList(),
    val isMasterSelected: Boolean = false,
    val isLoading: Boolean = false,
    val selectedArchiveType: ArchiveType = ArchiveType.ZIP,
    val backupProgress: Float = 0f,
    val backupStatus: String = "",
    val isDefaultSmsApp: Boolean = false
)
