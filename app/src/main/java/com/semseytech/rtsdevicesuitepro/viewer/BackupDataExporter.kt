package com.semseytech.rtsdevicesuitepro.viewer

import android.content.Context
import com.google.gson.Gson
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import java.io.File

/**
 * Helper class to export restored backup data into JSON files
 * compatible with the local viewer website.
 */
class BackupDataExporter(private val context: Context) {
    private val gson = Gson()
    private val viewerDir = File(context.filesDir, "viewer_data")

    init {
        if (!viewerDir.exists()) viewerDir.mkdirs()
    }

    fun clearData() {
        try {
            if (viewerDir.exists()) {
                viewerDir.deleteRecursively()
                viewerDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportSmsThreads(threads: List<BackupItem.SmsMessage>, tempDir: File? = null) {
        val conversations = threads.map { thread ->
            mapOf(
                "thread_id" to thread.id,
                "contact" to thread.sender,
                "phone" to thread.sender,
                "last_message" to (thread.messages.lastOrNull()?.body ?: ""),
                "timestamp" to (thread.messages.lastOrNull()?.date ?: 0L)
            )
        }
        
        writeFile("conversations.json", gson.toJson(conversations))

        threads.forEach { thread ->
            val messages = thread.messages.map { msg ->
                val mmsData = if (msg.isMms) {
                    val attachment = msg.attachments.firstOrNull { it.contentType.startsWith("image/") || it.contentType.startsWith("video/") }
                    if (attachment != null) {
                        tempDir?.let { dir ->
                            copyMmsAttachment(dir, attachment, msg.id)
                        }
                        mapOf(
                            "type" to if (attachment.contentType.startsWith("video/")) "video" else "image",
                            "path" to "mms/${msg.id}_${attachment.fileName}"
                        )
                    } else null
                } else null

                mapOf(
                    "type" to if (msg.type == 2) "outgoing" else "incoming",
                    "body" to msg.body,
                    "timestamp" to msg.date,
                    "mms" to mmsData
                )
            }
            writeFile("messages_${thread.id}.json", gson.toJson(messages))
        }
    }

    fun exportCallLogs(calls: List<BackupItem.CallLogEntry>) {
        val mappedCalls = calls.flatMap { entry ->
            entry.calls.map { call ->
                mapOf(
                    "number" to entry.number,
                    "name" to entry.displayName,
                    "type" to when (call.type) {
                        1 -> "incoming"
                        2 -> "outgoing"
                        3 -> "missed"
                        else -> "unknown"
                    },
                    "date" to call.date,
                    "duration" to call.duration
                )
            }
        }.sortedByDescending { it["date"] as Long }
        
        writeFile("calls.json", gson.toJson(mappedCalls))
    }

    fun exportContacts(contacts: List<BackupItem.Contact>) {
        val mappedContacts = contacts.map { contact ->
            mapOf(
                "id" to contact.id,
                "name" to contact.name,
                "phone" to (contact.phoneNumbers.firstOrNull() ?: ""),
                "email" to (contact.emails.firstOrNull() ?: "")
            )
        }
        writeFile("contacts.json", gson.toJson(mappedContacts))
    }

    fun exportUserFiles(files: List<BackupItem.UserFile>, tempDir: File) {
        val mappedFiles = files.map { file ->
            copyUserFile(tempDir, file)
            mapOf(
                "name" to file.fileName,
                "size" to file.size,
                "path" to "files/${file.fileName}",
                "mimeType" to file.mimeType
            )
        }
        writeFile("files.json", gson.toJson(mappedFiles))
    }

    private fun copyMmsAttachment(tempDir: File, attachment: BackupItem.MmsAttachment, messageId: String) {
        try {
            val sourceFile = File(tempDir, "mms/part_${attachment.partId}")
            if (sourceFile.exists()) {
                val mmsDir = File(viewerDir, "mms")
                if (!mmsDir.exists()) mmsDir.mkdirs()
                val destFile = File(mmsDir, "${messageId}_${attachment.fileName}")
                sourceFile.copyTo(destFile, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyUserFile(tempDir: File, userFile: BackupItem.UserFile) {
        try {
            // Need to match the logic in RestoreViewModel for finding the file in tempDir
            // For now, simplify and assume they are in tempDir/files/
            val sourceFile = File(tempDir, "files/${userFile.fileName}")
            if (sourceFile.exists()) {
                val filesDir = File(viewerDir, "files")
                if (!filesDir.exists()) filesDir.mkdirs()
                val destFile = File(filesDir, userFile.fileName)
                sourceFile.copyTo(destFile, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeFile(fileName: String, content: String) {
        try {
            val dataDir = File(viewerDir, "data")
            if (!dataDir.exists()) dataDir.mkdirs()
            File(dataDir, fileName).writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getViewerDataPath(): String {
        return viewerDir.absolutePath
    }
}