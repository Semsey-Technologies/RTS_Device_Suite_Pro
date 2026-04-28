package com.semseytech.rtsdevicesuitepro.backup.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

sealed class BackupItem {
    abstract val id: String
    abstract val displayName: String
    abstract val isSelected: Boolean
    
    data class SmsMessage(
        override val id: String,
        val sender: String,
        val snippet: String,
        val date: Long,
        val messageCount: Int = 1,
        val messages: List<MessageDetail> = emptyList(),
        override val displayName: String = sender,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class MessageDetail(
        val id: String,
        val body: String,
        val date: Long,
        val dateSent: Long,
        val type: Int, // 1 = Inbox, 2 = Sent
        val read: Int = 1,
        val threadId: Long = 0L,
        val isMms: Boolean = false,
        val subject: String? = null,
        val subCs: Int = 106, // UTF-8
        val mType: Int = 132, // m-retrieve-conf
        val contentType: String? = "application/vnd.wap.multipart.related",
        val attachments: List<MmsAttachment> = emptyList(),
        val addresses: List<MmsAddress> = emptyList()
    )

    data class MmsAttachment(
        val id: String,
        val contentType: String,
        val fileName: String,
        val partId: String,
        val text: String? = null
    )

    data class MmsAddress(
        val address: String,
        val type: Int, // 137=From, 151=To, 130=Cc, 129=Bcc
        val charset: Int = 106 // UTF-8
    )

    data class CallLogEntry(
        override val id: String,
        val number: String,
        val latestType: String, // Incoming, Outgoing, Missed
        val latestDate: Long,
        val totalDuration: Long,
        val callCount: Int = 1,
        val calls: List<CallDetail> = emptyList(),
        override val displayName: String = number,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class CallDetail(
        val id: String,
        val type: Int,
        val date: Long,
        val duration: Long
    )

    data class Contact(
        override val id: String,
        val name: String,
        val phoneNumbers: List<String>,
        val emails: List<String>,
        override val displayName: String = name,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class Apk(
        override val id: String,
        val appName: String,
        val packageName: String,
        val version: String,
        val sourceDir: String? = null,
        val icon: Drawable? = null,
        override val displayName: String = appName,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class UserFile(
        override val id: String,
        val fileName: String,
        val size: Long,
        val path: String,
        val mimeType: String,
        override val displayName: String = fileName,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class LauncherConfig(
        override val id: String,
        val configName: String,
        override val displayName: String = configName,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class UserSetting(
        override val id: String,
        val settingName: String,
        val value: String,
        override val displayName: String = settingName,
        override val isSelected: Boolean = false
    ) : BackupItem()
}

data class BackupCategory(
    val id: String,
    val name: String,
    val items: List<BackupItem>,
    val isExpanded: Boolean = false,
    val isAllSelected: Boolean = false
)

data class BackupManifest(
    val timestamp: Long,
    val deviceName: String,
    val entries: List<ManifestEntry>
)

data class ManifestEntry(
    val category: String,
    val itemName: String,
    val itemType: String,
    val identifier: String,
    val filePath: String? = null
)
