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
        
        onProgress(0.05f, "Reading manifest...")
        val manifestFile = File(tempDir, "manifest.json")
        reader.extractFile("manifest.json", manifestFile)
        
        if (!manifestFile.exists()) {
            return@withContext RestoreReport(0, 0, 1, listOf("Manifest missing in archive"))
        }

        val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
        
        val wantedPaths = mutableSetOf<String>()
        wantedPaths.add("manifest.json")
        wantedPaths.add("backup.json")
        wantedPaths.add("data/index.json") 
        
        selectedItems.forEach { item ->
            val entry = manifest.entries.find { it.identifier == item.id }
            entry?.filePath?.let { wantedPaths.add(it) }
            
            if (item is BackupItem.CallLogEntry || item.id == "calls") wantedPaths.add("data/calls.json")
            if (item is BackupItem.Contact || item.id == "contacts") wantedPaths.add("data/contacts.json")
            if (item is BackupItem.SystemSetting || item.id == "settings") wantedPaths.add("data/settings.json")
            if (item is BackupItem.SmsMessage) {
                wantedPaths.add("data/threads/${item.id}.json")
                wantedPaths.add("data/mms/${item.id}")
            }
        }

        onProgress(0.1f, "Extracting selected items...")
        reader.extractSelective(tempDir, wantedPaths) { progress, status ->
            onProgress(0.1f, status)
        }

        val details = mutableListOf<String>()
        var restored = 0
        var errors = 0

        val smsIndexFile = File(tempDir, "data/index.json")
        val smsThreads = if (smsIndexFile.exists()) {
            val type = object : TypeToken<List<SmsExtractor.SmsThread>>() {}.type
            gson.fromJson<List<SmsExtractor.SmsThread>>(smsIndexFile.readText(), type)
        } else emptyList()

        val callsToRestore = selectedItems.filter { it is BackupItem.CallLogEntry || it.id == "calls" }
        val contactsToRestore = selectedItems.filter { it is BackupItem.Contact || it.id == "contacts" }
        val otherItems = selectedItems.filter { 
            it !is BackupItem.CallLogEntry && it !is BackupItem.Contact && 
            it.id != "calls" && it.id != "contacts" 
        }

        val viewerDataDir = File(application.filesDir, "viewer_data").apply { 
            if (!exists()) mkdirs() 
        }
        val viewerDataSubDir = File(viewerDataDir, "data").apply { if (!exists()) mkdirs() }

        // 1. Process Call Logs (Bundled & Merged)
        if (callsToRestore.isNotEmpty()) {
            onProgress(0.2f, "Restoring Call Logs...")
            val sourceFile = File(tempDir, "data/calls.json")
            if (sourceFile.exists()) {
                val type = object : TypeToken<List<BackupItem.CallLogEntry>>() {}.type
                val allCalls: List<BackupItem.CallLogEntry> = gson.fromJson(sourceFile.readText(), type)
                val selectedIds = callsToRestore.map { it.id }.toSet()
                val restoreAll = selectedIds.contains("calls")
                
                val toRestore = allCalls.filter { restoreAll || selectedIds.contains(it.id) }
                toRestore.forEach {
                    restoreCallLogToSystem(it)
                    restored++
                }
                
                // Merge for viewer
                val viewerCallsFile = File(viewerDataSubDir, "calls.json")
                val existingCalls = if (viewerCallsFile.exists()) {
                    gson.fromJson<List<BackupItem.CallLogEntry>>(viewerCallsFile.readText(), type)
                } else emptyList()
                
                val mergedCalls = (existingCalls + toRestore).distinctBy { it.id }
                viewerCallsFile.writeText(gson.toJson(mergedCalls))
            } else {
                errors += callsToRestore.size
                details.add("Call log data file missing in archive")
            }
        }

        // 2. Process Contacts (Bundled & Merged)
        if (contactsToRestore.isNotEmpty()) {
            onProgress(0.3f, "Restoring Contacts...")
            val sourceFile = File(tempDir, "data/contacts.json")
            if (sourceFile.exists()) {
                val type = object : TypeToken<List<BackupItem.Contact>>() {}.type
                val allContacts: List<BackupItem.Contact> = gson.fromJson(sourceFile.readText(), type)
                val selectedIds = contactsToRestore.map { it.id }.toSet()
                val restoreAll = selectedIds.contains("contacts")
                
                val toRestore = allContacts.filter { restoreAll || selectedIds.contains(it.id) }
                toRestore.forEach {
                    restoreContactToSystem(it)
                    restored++
                }
                
                // Merge for viewer
                val viewerContactsFile = File(viewerDataSubDir, "contacts.json")
                val existingContacts = if (viewerContactsFile.exists()) {
                    gson.fromJson<List<BackupItem.Contact>>(viewerContactsFile.readText(), type)
                } else emptyList()
                
                // Extract photos for viewer
                val photosDir = File(viewerDataSubDir, "photos").apply { mkdirs() }
                val updatedToRestore = toRestore.map { contact ->
                    if (contact.photoUri != null && contact.photoUri.startsWith("content://")) {
                        val photoFile = File(photosDir, "${contact.id}.jpg")
                        try {
                            application.contentResolver.openInputStream(Uri.parse(contact.photoUri))?.use { input ->
                                photoFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            contact.copy(photoUri = "../../data/photos/${contact.id}.jpg")
                        } catch (e: Exception) {
                            contact
                        }
                    } else contact
                }

                val mergedContacts = (existingContacts + updatedToRestore).distinctBy { it.id }
                viewerContactsFile.writeText(gson.toJson(mergedContacts))
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
                        if (targetPath.isNotEmpty()) {
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
                        
                        // Copy to viewer
                        val source = File(tempDir, manifestEntry.filePath ?: "")
                        if (source.exists()) {
                            val target = File(viewerDataDir, manifestEntry.filePath ?: "")
                            target.parentFile?.mkdirs()
                            source.copyTo(target, overwrite = true)
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
                            
                            // Merge for viewer index
                            val viewerIndexFile = File(viewerDataSubDir, "index.json")
                            val indexType = object : TypeToken<List<SmsExtractor.SmsThread>>() {}.type
                            val existingIndex = if (viewerIndexFile.exists()) {
                                gson.fromJson<List<SmsExtractor.SmsThread>>(viewerIndexFile.readText(), indexType)
                            } else emptyList()
                            
                            threadInfo?.let { info ->
                                val mergedIndex = (existingIndex + info).distinctBy { it.thread_id }
                                viewerIndexFile.writeText(gson.toJson(mergedIndex))
                            }
                            
                            // Copy thread file
                            val viewerThreadDir = File(viewerDataSubDir, "threads").apply { mkdirs() }
                            sourceFile.copyTo(File(viewerThreadDir, "${item.id}.json"), overwrite = true)
                            
                            // Copy MMS attachments
                            val mmsSourceDir = File(tempDir, "data/mms/${item.id}")
                            if (mmsSourceDir.exists()) {
                                val mmsTargetDir = File(viewerDataSubDir, "mms/${item.id}").apply { mkdirs() }
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

        // Finalize Manifest merge for viewer
        val viewerManifestFile = File(viewerDataDir, "manifest.json")
        val existingManifest = if (viewerManifestFile.exists()) {
            gson.fromJson(viewerManifestFile.readText(), BackupManifest::class.java)
        } else null
        
        val mergedEntries = if (existingManifest != null) {
            (existingManifest.entries + manifest.entries.filter { newEntry -> 
                selectedItems.any { it.id == newEntry.identifier } 
            }).distinctBy { it.identifier }
        } else {
            manifest.entries.filter { newEntry -> 
                selectedItems.any { it.id == newEntry.identifier } 
            }
        }
        
        val mergedManifest = manifest.copy(entries = mergedEntries)
        viewerManifestFile.writeText(gson.toJson(mergedManifest))

        tempDir.deleteRecursively()
        RestoreReport(restored, 0, errors, details)
    }

    private fun restoreSmsToSystem(messages: List<BackupItem.MessageDetail>, address: String) {
        messages.forEach { msg ->
            if (msg.isMms) return@forEach 
            
            // Check for duplicates
            val selection = "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} = ? AND ${Telephony.Sms.BODY} = ?"
            val selectionArgs = arrayOf(address, msg.date.toString(), msg.body)
            val cursor = application.contentResolver.query(Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms._ID), selection, selectionArgs, null)
            val exists = cursor?.use { it.count > 0 } ?: false
            if (exists) {
                Log.d(TAG, "SMS already exists, skipping: $address at ${msg.date}")
                return@forEach
            }

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
        val callsToRestore = if (item.calls.isNotEmpty()) item.calls else listOf(
            BackupItem.CallDetail(item.id, item.latestType.toIntOrNull() ?: 1, item.date, item.totalDuration)
        )

        callsToRestore.forEach { call ->
            // Check for duplicates
            val selection = "${CallLog.Calls.NUMBER} = ? AND ${CallLog.Calls.DATE} = ? AND ${CallLog.Calls.TYPE} = ?"
            val selectionArgs = arrayOf(item.number, call.date.toString(), call.type.toString())
            
            val cursor = application.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID),
                selection,
                selectionArgs,
                null
            )
            
            val exists = cursor?.use { it.count > 0 } ?: false
            if (exists) {
                Log.d(TAG, "Call log entry already exists, skipping: ${item.number} at ${call.date}")
            } else {
                val values = ContentValues().apply {
                    put(CallLog.Calls.NUMBER, item.number)
                    put(CallLog.Calls.DATE, call.date)
                    put(CallLog.Calls.TYPE, call.type)
                    put(CallLog.Calls.DURATION, call.duration)
                    put(CallLog.Calls.NEW, 1)
                }
                try {
                    application.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert call log", e)
                }
            }
        }
    }

    private fun restoreContactToSystem(item: BackupItem.Contact) {
        // Simple duplicate check by name
        val cursor = application.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
            arrayOf(item.name),
            null
        )
        val exists = cursor?.use { it.count > 0 } ?: false
        if (exists) {
            Log.d(TAG, "Contact already exists with name ${item.name}, skipping.")
            return
        }

        val values = ContentValues().apply {
            put(ContactsContract.RawContacts.ACCOUNT_TYPE, null as String?)
            put(ContactsContract.RawContacts.ACCOUNT_NAME, null as String?)
        }

        try {
            val rawContactUri = application.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
            val rawContactId = android.content.ContentUris.parseId(rawContactUri!!)

            val nameValues = ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, item.name)
            }
            application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, nameValues)

            item.phoneNumbers.forEach { phone ->
                val phoneValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                }
                application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneValues)
            }
            
            item.emails.forEach { email ->
                val emailValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                }
                application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, emailValues)
            }
            
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
            val pduValues = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.DATE, msg.date / 1000)
                put(Telephony.Mms.MESSAGE_BOX, msg.type)
                put(Telephony.Mms.SUBJECT, msg.subject)
                put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related")
                put(Telephony.Mms.MESSAGE_TYPE, 132) 
                put(Telephony.Mms.READ, 1)
            }
            val pduUri = application.contentResolver.insert(Telephony.Mms.CONTENT_URI, pduValues)
            val mmsId = ContentUris.parseId(pduUri!!)

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

            msg.addresses.forEach { addr ->
                val addrValues = ContentValues().apply {
                    put("address", addr.address)
                    put("type", addr.type)
                    put("charset", 106) 
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
                "Settings", "WiFi", "Bluetooth", "Network", "Display" -> {
                    if (setting.id == "accessibility") {
                        android.provider.Settings.Secure.putInt(
                            application.contentResolver,
                            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                            setting.value.toIntOrNull() ?: 0
                        )
                    } else if (setting.id.contains("wifi") || setting.id.contains("bluetooth")) {
                        android.provider.Settings.Global.putString(
                            application.contentResolver,
                            setting.id,
                            setting.value
                        )
                    } else {
                        android.provider.Settings.System.putString(
                            application.contentResolver,
                            setting.id,
                            setting.value
                        )
                    }
                }
                else -> {
                    android.provider.Settings.System.putString(
                        application.contentResolver,
                        setting.id,
                        setting.value
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore setting: ${setting.displayName} in category ${setting.category}", e)
        }
    }
}
