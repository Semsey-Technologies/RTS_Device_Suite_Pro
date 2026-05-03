package com.semseytech.rtsdevicesuitepro.restore.engine

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.semseytech.rtsdevicesuitepro.backup.model.*
import com.semseytech.rtsdevicesuitepro.sms.logic.SmsExtractor
import kotlinx.coroutines.withContext
import java.io.File

class RestoreEngine(private val application: Application) {
    private val TAG = "RestoreEngine"
    private val gson = Gson()

    suspend fun runRestore(
        archiveFile: File,
        selectedItems: List<BackupItem>,
        onProgress: (Float, String) -> Unit
    ): RestoreReport = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val tempDir = File(application.cacheDir, "restore_work_${System.currentTimeMillis()}").apply { mkdirs() }
        val reader = ArchiveReader(archiveFile)
        
        onProgress(0.1f, "Extracting archive...")
        reader.extractTo(tempDir) { progress, status ->
            onProgress(0.1f, status)
        }

        val manifestFile = File(tempDir, "manifest.json")
        if (!manifestFile.exists()) {
            return@withContext RestoreReport(0, 0, 1, listOf("Manifest missing in archive"))
        }

        val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
        val details = mutableListOf<String>()
        var restored = 0
        var errors = 0

        // Load SMS index if it exists for thread mapping
        val smsIndexFile = File(tempDir, "data/index.json")
        val smsThreads = if (smsIndexFile.exists()) {
            val type = object : TypeToken<List<SmsExtractor.SmsThread>>() {}.type
            gson.fromJson<List<SmsExtractor.SmsThread>>(smsIndexFile.readText(), type)
        } else emptyList()

        // Group selected items to handle bundled data (Calls, Contacts) efficiently
        // Note: Check itemType or ID since they might be grouped under JsonData in some cases
        val callsToRestore = selectedItems.filter { it is BackupItem.CallLogEntry || it.id == "calls" }
        val contactsToRestore = selectedItems.filter { it is BackupItem.Contact || it.id == "contacts" }
        val otherItems = selectedItems.filter { 
            it !is BackupItem.CallLogEntry && it !is BackupItem.Contact && 
            it.id != "calls" && it.id != "contacts" 
        }

        val viewerDataDir = File(application.filesDir, "viewer_data").apply { 
            deleteRecursively()
            mkdirs() 
        }

        // 1. Process Call Logs (Bundled)
        if (callsToRestore.isNotEmpty()) {
            onProgress(0.2f, "Restoring Call Logs...")
            val sourceFile = File(tempDir, "data/calls.json")
            if (sourceFile.exists()) {
                val type = object : TypeToken<List<BackupItem.CallLogEntry>>() {}.type
                val allCalls: List<BackupItem.CallLogEntry> = gson.fromJson(sourceFile.readText(), type)
                val selectedIds = callsToRestore.map { it.id }.toSet()
                
                allCalls.filter { call -> selectedIds.contains(call.id) }.forEach {
                    restoreCallLogToSystem(it)
                    restored++
                }
                // Copy to viewer
                val viewerDir = File(viewerDataDir, "data").apply { mkdirs() }
                sourceFile.copyTo(File(viewerDir, "calls.json"), overwrite = true)
            } else {
                errors += callsToRestore.size
                details.add("Call log data file missing in archive")
            }
        }

        // 2. Process Contacts (Bundled)
        if (contactsToRestore.isNotEmpty()) {
            onProgress(0.3f, "Restoring Contacts...")
            val sourceFile = File(tempDir, "data/contacts.json")
            if (sourceFile.exists()) {
                val type = object : TypeToken<List<BackupItem.Contact>>() {}.type
                val allContacts: List<BackupItem.Contact> = gson.fromJson(sourceFile.readText(), type)
                val selectedIds = contactsToRestore.map { it.id }.toSet()
                
                allContacts.filter { contact -> selectedIds.contains(contact.id) }.forEach {
                    restoreContactToSystem(it)
                    restored++
                }
                // Copy to viewer
                val viewerDir = File(viewerDataDir, "data").apply { mkdirs() }
                sourceFile.copyTo(File(viewerDir, "contacts.json"), overwrite = true)
            } else {
                errors += contactsToRestore.size
                details.add("Contacts data file missing in archive")
            }
        }

        // 3. Process Other Items (Files, SMS, etc.)
        otherItems.forEachIndexed { index, item ->
            val progress = 0.4f + (index.toFloat() / otherItems.size * 0.6f)
            onProgress(progress, "Restoring: ${item.displayName}")
            
            val manifestEntry = manifest.entries.find { it.identifier == item.id }
            
            try {
                when (item) {
                    is BackupItem.UserFile -> {
                        if (manifestEntry == null) {
                            errors++
                            details.add("No manifest entry for ${item.displayName}")
                            return@forEachIndexed
                        }
                        val targetPath = manifestEntry.originalPath ?: item.path
                        if (targetPath.isEmpty()) {
                            errors++
                            details.add("Skipping ${item.fileName}: No original path preserved")
                            return@forEachIndexed
                        }
                        val sourceFile = File(tempDir, manifestEntry.filePath ?: "")
                        if (sourceFile.exists()) {
                            val targetFile = File(targetPath)
                            targetFile.parentFile?.mkdirs()
                            sourceFile.copyTo(targetFile, overwrite = true)
                            restored++
                        } else {
                            errors++
                            details.add("File missing in archive: ${item.fileName}")
                        }
                    }
                    is BackupItem.SmsMessage -> {
                        val sourcePath = manifestEntry?.filePath ?: "data/threads/${item.id}.json"
                        val sourceFile = File(tempDir, sourcePath)
                        if (sourceFile.exists()) {
                            val json = sourceFile.readText()
                            val type = object : TypeToken<List<BackupItem.MessageDetail>>() {}.type
                            val messages: List<BackupItem.MessageDetail> = gson.fromJson(json, type)
                            
                            val threadInfo = smsThreads.find { it.thread_id == item.id }
                            val address = threadInfo?.phone ?: item.sender
                            
                            messages.forEach { msg ->
                                if (msg.isMms) {
                                    restoreMmsToSystem(msg, item.id, tempDir)
                                } else {
                                    restoreSmsToSystem(listOf(msg), address)
                                }
                            }
                            restored++
                            
                            // Also copy to viewer folder
                            val viewerThreadDir = File(viewerDataDir, "data/threads").apply { mkdirs() }
                            sourceFile.copyTo(File(viewerThreadDir, "${item.id}.json"), overwrite = true)
                            
                            // Copy MMS attachments if they exist
                            val mmsSourceDir = File(tempDir, "data/mms/${item.id}")
                            if (mmsSourceDir.exists()) {
                                val mmsTargetDir = File(viewerDataDir, "data/mms/${item.id}").apply { mkdirs() }
                                mmsSourceDir.copyRecursively(mmsTargetDir, overwrite = true)
                            }
                        } else {
                            errors++
                            details.add("Thread file missing: $sourcePath")
                        }
                    }
                    is BackupItem.SystemSetting -> {
                        restoreSystemSetting(item)
                        restored++
                    }
                    else -> restored++
                }
            } catch (e: Exception) {
                errors++
                details.add("Error restoring ${item.displayName}: ${e.message}")
            }
        }

        // Copy viewer index and index.json
        val indexFile = File(tempDir, "data/index.json")
        if (indexFile.exists()) {
            val viewerDir = File(viewerDataDir, "data").apply { mkdirs() }
            indexFile.copyTo(File(viewerDir, "index.json"), overwrite = true)
        }

        // Copy all user files to viewer data directory so they show up in viewer gallery
        val manifestEntries = manifest.entries.filter { it.itemType == "UserFile" }
        manifestEntries.forEach { entry ->
            val source = File(tempDir, entry.filePath)
            if (source.exists()) {
                val target = File(viewerDataDir, entry.filePath)
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
            }
        }
        
        // Copy manifest for gallery metadata
        manifestFile.copyTo(File(viewerDataDir, "manifest.json"), overwrite = true)

        tempDir.deleteRecursively()
        RestoreReport(restored, 0, errors, details)
    }

    private fun restoreSmsToSystem(messages: List<BackupItem.MessageDetail>, address: String) {
        messages.forEach { msg ->
            if (msg.isMms) return@forEach // MMS restoration is complex and requires part insertion
            
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, msg.body)
                put(Telephony.Sms.DATE, msg.date)
                put(Telephony.Sms.DATE_SENT, msg.dateSent)
                put(Telephony.Sms.TYPE, msg.type)
                put(Telephony.Sms.READ, msg.read)
            }
            try {
                application.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert SMS", e)
            }
        }
    }

    private fun restoreCallLogToSystem(item: BackupItem.CallLogEntry) {
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, item.number)
            put(CallLog.Calls.DATE, item.date)
            put(CallLog.Calls.TYPE, item.latestType.toIntOrNull() ?: CallLog.Calls.INCOMING_TYPE)
            put(CallLog.Calls.DURATION, item.totalDuration)
            put(CallLog.Calls.NEW, 1)
        }
        try {
            application.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert call log", e)
        }
    }

    private fun restoreContactToSystem(item: BackupItem.Contact) {
        // Basic contact restoration (name, phone numbers, emails, and photo)
        val values = ContentValues().apply {
            put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
            put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
        }
        
        try {
            val rawContactUri = application.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
            val rawContactId = android.content.ContentUris.parseId(rawContactUri!!)

            // Name
            val nameValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, item.name)
            }
            application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

            // Phone numbers
            item.phoneNumbers.forEach { phone ->
                val phoneValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                }
                application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
            }
            
            // Emails
            item.emails.forEach { email ->
                val emailValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                }
                application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
            }
            
            // Photo URI (if available)
            item.photoUri?.let { uriStr ->
                try {
                    val photoUri = Uri.parse(uriStr)
                    application.contentResolver.openInputStream(photoUri)?.use { input ->
                        val bytes = input.readBytes()
                        val photoValues = ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                        }
                        application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, photoValues)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore contact photo: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert contact", e)
        }
    }

    private fun restoreMmsToSystem(msg: BackupItem.MessageDetail, threadId: String, tempDir: File) {
        try {
            // 1. Insert into PDU table
            val pduValues = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.DATE, msg.date / 1000)
                put(Telephony.Mms.MESSAGE_BOX, msg.type)
                put(Telephony.Mms.SUBJECT, msg.subject)
                put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related")
                put(Telephony.Mms.MESSAGE_TYPE, 132) // m-retrieve-conf
                put(Telephony.Mms.READ, 1)
            }
            val pduUri = application.contentResolver.insert(Telephony.Mms.CONTENT_URI, pduValues)
            val mmsId = ContentUris.parseId(pduUri!!)

            // 2. Insert Parts (Text and Media)
            msg.attachments.forEach { att ->
                val partValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", att.contentType)
                    put("fn", att.fileName)
                    put("name", att.fileName)
                }

                if (att.contentType == "text/plain") {
                    partValues.put("text", att.text)
                    application.contentResolver.insert(Uri.parse("content://mms/part"), partValues)
                } else {
                    val partUri = application.contentResolver.insert(Uri.parse("content://mms/part"), partValues)
                    val attachmentFile = File(tempDir, "data/mms/$threadId/${att.fileName}")
                    if (attachmentFile.exists()) {
                        application.contentResolver.openOutputStream(partUri!!)?.use { output ->
                            attachmentFile.inputStream().use { input -> input.copyTo(output) }
                        }
                    }
                }
            }

            // 3. Insert Addresses
            msg.addresses.forEach { addr ->
                val addrValues = ContentValues().apply {
                    put("address", addr.address)
                    put("type", addr.type)
                    put("charset", 106) // utf-8
                }
                application.contentResolver.insert(Uri.parse("content://mms/$mmsId/addr"), addrValues)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore MMS", e)
        }
    }

    private fun restoreSystemSetting(setting: BackupItem.SystemSetting) {
        try {
            when (setting.category) {
                "Settings" -> {
                    if (setting.id == "accessibility") {
                        android.provider.Settings.Secure.putInt(
                            application.contentResolver,
                            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                            setting.value.toIntOrNull() ?: 0
                        )
                    }
                }
                "WiFi" -> {
                    // WiFi restoration is limited by Android security, but we can try to add suggestons
                    // if it's an SSID we previously knew.
                }
                "Bluetooth" -> {
                    // Direct bonding from apps is restricted, but we can log that it should be paired
                }
            }
            // Fallback: Try System.Global or System.Secure for general settings
            android.provider.Settings.System.putString(
                application.contentResolver,
                setting.id,
                setting.value
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore setting: ${setting.displayName}", e)
        }
    }
}
