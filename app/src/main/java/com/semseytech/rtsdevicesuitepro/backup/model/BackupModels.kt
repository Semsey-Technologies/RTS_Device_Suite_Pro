package com.semseytech.rtsdevicesuitepro.backup.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel

@Immutable
sealed class BackupItem {
    abstract val id: String
    abstract val displayName: String
    abstract val isSelected: Boolean
    abstract val date: Long
    abstract val size: Long
    abstract val type: String
    
    data class SmsMessage(
        override val id: String,
        val sender: String,
        val snippet: String,
        override val date: Long,
        val messageCount: Int = 1,
        val messages: List<MessageDetail> = emptyList(),
        override val size: Long = 0L,
        override val type: String = "SMS",
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
        val type: Int // 137=From, 151=To, 130=Cc, 129=Bcc
    )

    data class CallLogEntry(
        override val id: String,
        val number: String,
        val latestType: String,
        override val date: Long,
        val totalDuration: Long,
        val callCount: Int = 1,
        val calls: List<CallDetail> = emptyList(),
        override val size: Long = 0L,
        override val type: String = "Call Log",
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
        val photoUri: String? = null,
        override val date: Long = 0L,
        override val size: Long = 0L,
        override val type: String = "Contact",
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
        override val date: Long = 0L,
        override val size: Long = 0L,
        override val type: String = "APK",
        override val displayName: String = appName,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class UserFile(
        override val id: String,
        val fileName: String,
        override val size: Long,
        val path: String,
        val mimeType: String,
        override val date: Long,
        override val type: String,
        override val displayName: String = fileName,
        override val isSelected: Boolean = false
    ) : BackupItem()

    data class SystemSetting(
        override val id: String,
        val settingName: String,
        val value: String,
        val category: String, // WiFi, Bluetooth, Wallpaper, etc.
        override val date: Long = 0L,
        override val size: Long = 0L,
        override val type: String = "Setting",
        override val displayName: String = settingName,
        override val isSelected: Boolean = false
    ) : BackupItem()
}

data class BackupCategory(
    val id: String,
    val name: String,
    val items: List<BackupItem>,
    val isExpanded: Boolean = false,
    val isAllSelected: Boolean = false,
    val parentCategory: String? = null // For grouping like "User Files"
)

enum class BackupDestinationType {
    INTERNAL, SD_CARD, USB_OTG, GOOGLE_DRIVE, ONEDRIVE, DROPBOX, MEGA, WEBDAV, SAF
}

data class BackupDestination(
    val type: BackupDestinationType,
    val displayName: String,
    val path: String? = null,
    val uri: String? = null
)

enum class ViewMode {
    SMALL_THUMBNAIL, MEDIUM_THUMBNAIL, LARGE_THUMBNAIL, LIST, DETAILS
}

enum class SortType {
    NAME, DATE, SIZE, TYPE
}

enum class GroupType {
    NONE, DATE, FOLDER, TYPE, SIZE
}

data class BackupManifest(
    val timestamp: Long,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val archiveFormat: String,
    val categories: List<String>,
    val entries: List<ManifestEntry>,
    val smsIndexAvailable: Boolean = false,
    val viewerVersion: String = "1.0"
)

data class ManifestEntry(
    val category: String,
    val itemName: String,
    val itemType: String,
    val identifier: String,
    val filePath: String? = null,
    val originalPath: String? = null,
    val size: Long = 0L,
    val date: Long = 0L
)

data class RestoreReport(
    val restoredCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val details: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
