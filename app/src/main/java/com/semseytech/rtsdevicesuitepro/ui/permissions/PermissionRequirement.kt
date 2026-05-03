package com.semseytech.rtsdevicesuitepro.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class PermissionRequirement(
    val permission: String,
    val title: String,
    val shortDescription: String,
    val longDescription: String,
    val usage: String,
    val consequences: String,
    val icon: ImageVector
)

object PermissionRegistry {
    val permissions = listOf(
        PermissionRequirement(
            Manifest.permission.READ_SMS,
            "SMS Access",
            "Needed to backup and restore text messages.",
            "This app requires access to your SMS messages to provide backup and restore functionality.",
            "Used by the SMS module in the Backup Suite.",
            "You won't be able to backup or restore any text messages.",
            Icons.Default.Sms
        ),
        PermissionRequirement(
            Manifest.permission.READ_CONTACTS,
            "Contacts Access",
            "Needed to backup and organize contacts.",
            "Access to contacts is required to backup your address book and help you organize duplicates.",
            "Used by the Contact Manager and Backup Suite.",
            "Contact-related features and backups will be disabled.",
            Icons.Default.Contacts
        ),
        PermissionRequirement(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Location Access",
            "Used for network optimization and triggers.",
            "Location access helps the app identify nearby WiFi networks and signal strength for optimization.",
            "Used by Net Optimizer and Automation Engine.",
            "Location-based automation triggers and detailed network stats will not work.",
            Icons.Default.LocationOn
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "android.permission.MANAGE_EXTERNAL_STORAGE" else Manifest.permission.WRITE_EXTERNAL_STORAGE,
            "Storage Access",
            "Used to analyze and manage files.",
            "Deep file management requires access to your device's storage to scan for large files and junk.",
            "Core requirement for Storage Analyzer, File Explorer, and Backups.",
            "The app will be unable to see or manage most files on the device.",
            Icons.Default.Storage
        ),
        PermissionRequirement(
            Manifest.permission.READ_CALL_LOG,
            "Call Logs Access",
            "Used to backup and restore call history.",
            "Access to call logs is needed to create a history of your communications for backup.",
            "Required for Call Log module in Backup Suite.",
            "Call logs cannot be backed up or restored.",
            Icons.Default.Call
        )
    )

    fun getForPermission(permission: String): PermissionRequirement? {
        return permissions.find { it.permission == permission }
    }
}
