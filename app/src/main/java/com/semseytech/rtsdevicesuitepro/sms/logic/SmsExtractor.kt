package com.semseytech.rtsdevicesuitepro.sms.logic

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import java.io.File
import java.io.FileOutputStream

class SmsExtractor(private val context: Context) {
    private val TAG = "SmsExtractor"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    data class SmsThread(
        val thread_id: String,
        val contact: String,
        val phone: String,
        val last_message: String,
        val timestamp: Long
    )

    fun extractAll(outputDir: File): List<ManifestEntry> {
        val manifestEntries = mutableListOf<ManifestEntry>()
        val threadsDir = File(outputDir, "data/threads").apply { mkdirs() }
        val mmsDir = File(outputDir, "data/mms").apply { mkdirs() }
        
        val threadsMap = mutableMapOf<String, MutableList<BackupItem.MessageDetail>>()
        val threadInfoMap = mutableMapOf<String, SmsThread>()

        try {
            // 1. Fetch SMS
            fetchSms(threadsMap, threadInfoMap)
            
            // 2. Fetch MMS
            fetchMms(threadsMap, threadInfoMap, mmsDir)

            // 3. Write individual thread JSONs
            threadsMap.forEach { (threadId, messages) ->
                val threadFile = File(threadsDir, "$threadId.json")
                val sortedMessages = messages.sortedBy { it.date }
                threadFile.writeText(gson.toJson(sortedMessages))
                manifestEntries.add(ManifestEntry("sms_thread", "Thread $threadId", "SmsThread", threadId, "data/threads/$threadId.json", size = threadFile.length(), date = sortedMessages.lastOrNull()?.date ?: 0L))
            }

            // 4. Write master index
            val indexFile = File(outputDir, "data/index.json")
            val indexData = threadInfoMap.values.toList()
            indexFile.writeText(gson.toJson(indexData))
            manifestEntries.add(ManifestEntry("sms_index", "SMS Index", "SmsIndex", "index", "data/index.json", size = indexFile.length(), date = System.currentTimeMillis()))

        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
        }

        return manifestEntries
    }

    private fun fetchSms(
        threadsMap: MutableMap<String, MutableList<BackupItem.MessageDetail>>,
        threadInfoMap: MutableMap<String, SmsThread>
    ) {
        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                Telephony.Sms.DATE, Telephony.Sms.DATE_SENT, Telephony.Sms.TYPE,
                Telephony.Sms.READ, Telephony.Sms.THREAD_ID
            ),
            null, null, Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val dateSentIdx = it.getColumnIndex(Telephony.Sms.DATE_SENT)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            val readIdx = it.getColumnIndex(Telephony.Sms.READ)
            val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)

            while (it.moveToNext()) {
                val threadId = it.getString(threadIdIdx) ?: "0"
                val address = it.getString(addrIdx) ?: "Unknown"
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)

                val detail = BackupItem.MessageDetail(
                    id = it.getString(idIdx),
                    body = body,
                    date = date,
                    dateSent = it.getLong(dateSentIdx),
                    type = it.getInt(typeIdx),
                    read = it.getInt(readIdx),
                    threadId = threadId.toLong(),
                    isMms = false
                )
                threadsMap.getOrPut(threadId) { mutableListOf() }.add(detail)

                if (!threadInfoMap.containsKey(threadId)) {
                    threadInfoMap[threadId] = SmsThread(threadId, address, address, body, date)
                }
            }
        }
    }

    private fun fetchMms(
        threadsMap: MutableMap<String, MutableList<BackupItem.MessageDetail>>,
        threadInfoMap: MutableMap<String, SmsThread>,
        mmsBaseDir: File
    ) {
        val cursor = context.contentResolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.SUBJECT),
            null, null, Telephony.Mms.DATE + " DESC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(Telephony.Mms._ID)
            val dateIdx = it.getColumnIndex(Telephony.Mms.DATE)
            val threadIdIdx = it.getColumnIndex(Telephony.Mms.THREAD_ID)
            val boxIdx = it.getColumnIndex(Telephony.Mms.MESSAGE_BOX)
            val subIdx = it.getColumnIndex(Telephony.Mms.SUBJECT)

            while (it.moveToNext()) {
                val mmsId = it.getString(idIdx)
                val threadId = it.getString(threadIdIdx) ?: "0"
                val date = it.getLong(dateIdx) * 1000
                val type = it.getInt(boxIdx)
                val subject = it.getString(subIdx)

                val attachments = getMmsAttachments(mmsId, threadId, mmsBaseDir)
                val addresses = getMmsAddresses(mmsId)
                
                var body = subject ?: ""
                attachments.find { it.contentType == "text/plain" }?.text?.let { body = it }

                val detail = BackupItem.MessageDetail(
                    id = mmsId,
                    body = body,
                    date = date,
                    dateSent = date,
                    type = type,
                    threadId = threadId.toLong(),
                    isMms = true,
                    subject = subject,
                    attachments = attachments,
                    addresses = addresses
                )
                threadsMap.getOrPut(threadId) { mutableListOf() }.add(detail)

                if (!threadInfoMap.containsKey(threadId)) {
                    val fromAddr = addresses.find { it.type == 137 }?.address ?: "Unknown"
                    threadInfoMap[threadId] = SmsThread(threadId, fromAddr, fromAddr, body, date)
                }
            }
        }
    }

    private fun getMmsAttachments(mmsId: String, threadId: String, mmsBaseDir: File): List<BackupItem.MmsAttachment> {
        val attachments = mutableListOf<BackupItem.MmsAttachment>()
        val threadMmsDir = File(mmsBaseDir, threadId).apply { mkdirs() }
        
        val uri = Uri.parse("content://mms/part")
        val cursor = context.contentResolver.query(uri, null, "mid=?", arrayOf(mmsId), null)
        
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
                
                var textContent: String? = null
                if (contentType == "text/plain") {
                    textContent = it.getString(textIdx) ?: readPartText(partId)
                } else {
                    savePartFile(partId, File(threadMmsDir, fileName))
                }

                attachments.add(BackupItem.MmsAttachment(partId, contentType, fileName, partId, textContent))
            }
        }
        return attachments
    }

    private fun readPartText(partId: String): String {
        return try {
            context.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))?.use {
                it.bufferedReader().use { r -> r.readText() }
            } ?: ""
        } catch (e: Exception) { "" }
    }

    private fun savePartFile(partId: String, destFile: File) {
        try {
            context.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) { }
    }

    private fun getMmsAddresses(mmsId: String): List<BackupItem.MmsAddress> {
        val addresses = mutableListOf<BackupItem.MmsAddress>()
        val cursor = context.contentResolver.query(Uri.parse("content://mms/$mmsId/addr"), null, null, null, null)
        cursor?.use {
            val addrIdx = it.getColumnIndex("address")
            val typeIdx = it.getColumnIndex("type")
            while (it.moveToNext()) {
                val addr = it.getString(addrIdx) ?: continue
                addresses.add(BackupItem.MmsAddress(addr, it.getInt(typeIdx)))
            }
        }
        return addresses
    }
}

data class ManifestEntry(
    val category: String,
    val itemName: String,
    val itemType: String,
    val identifier: String,
    val filePath: String,
    val size: Long = 0L,
    val date: Long = 0L
)
