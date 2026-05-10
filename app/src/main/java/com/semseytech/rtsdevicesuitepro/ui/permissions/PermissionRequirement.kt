package com.semseytech.rtsdevicesuitepro.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
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
            "Needed to monitor, backup and restore text messages.",
            "This app requires access to your SMS messages to provide automation and backup functionality.",
            "Used by the SMS triggers in Automation and Backup Suite.",
            "You won't be able to use SMS-based automation or backups.",
            Icons.Default.Sms
        ),
        PermissionRequirement(
            Manifest.permission.RECEIVE_SMS,
            "Receive SMS",
            "Needed to detect incoming text messages.",
            "Required to trigger actions when an SMS is received.",
            "Used by Automation Engine.",
            "SMS triggers will not function.",
            Icons.Default.Sms
        ),
        PermissionRequirement(
            Manifest.permission.READ_CONTACTS,
            "Contacts Access",
            "Needed to identify contacts in messages and backups.",
            "Access to contacts is required to backup your address book and identify senders in automation.",
            "Used by the Contact Manager, Backup Suite, and Automation.",
            "Contact-related features and identification will be disabled.",
            Icons.Default.Contacts
        ),
        PermissionRequirement(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Location Access",
            "Used for network optimization, geofencing, and WiFi triggers.",
            "Location access helps the app identify nearby WiFi networks and signal strength, and enables geofence-based automation.",
            "Used by Net Optimizer and Automation Engine.",
            "Location-based automation triggers and detailed network stats will not work.",
            Icons.Default.LocationOn
        ),
        PermissionRequirement(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            "Approximate Location",
            "Used for basic network and location triggers.",
            "Allows the app to determine your general location for regional triggers.",
            "Used by Automation Engine.",
            "General location-based triggers will be unavailable.",
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
            Manifest.permission.READ_EXTERNAL_STORAGE,
            "Read Storage",
            "Needed to monitor file changes.",
            "Allows the app to detect when files are created or modified in specific folders.",
            "Used by Filesystem Triggers in Automation.",
            "File-monitoring automation will not work.",
            Icons.Default.Folder
        ),
        PermissionRequirement(
            Manifest.permission.READ_CALL_LOG,
            "Call Logs Access",
            "Used to backup and restore call history.",
            "Access to call logs is needed to create a history of your communications for backup.",
            "Required for Call Log module in Backup Suite.",
            "Call logs cannot be backed up or restored.",
            Icons.Default.Call
        ),
        PermissionRequirement(
            Manifest.permission.READ_PHONE_STATE,
            "Phone State",
            "Needed to detect incoming calls and network types.",
            "Allows the app to know when the phone is ringing or if you are on 5G/LTE.",
            "Used by Communication and Network triggers.",
            "Call triggers and detailed mobile data info will not work.",
            Icons.Default.Phone
        ),
        PermissionRequirement(
            Manifest.permission.RECORD_AUDIO,
            "Microphone Access",
            "Used for ambient noise detection.",
            "Allows the app to measure sound levels for noise-based automation.",
            "Used by the Ambient Noise trigger.",
            "Noise-level automation triggers will not function.",
            Icons.Default.Mic
        ),
        PermissionRequirement(
            Manifest.permission.BODY_SENSORS,
            "Body Sensors",
            "Used for heart rate monitoring.",
            "Access to body sensors (like heart rate) for health-related automation.",
            "Used by Sensor triggers.",
            "Heart rate automation will not work.",
            Icons.Default.Favorite
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else "android.permission.ACTIVITY_RECOGNITION",
            "Activity Recognition",
            "Used to detect walking, running, or driving.",
            "Allows the app to recognize your physical activity state for motion triggers.",
            "Used by Motion triggers in Automation.",
            "Activity-based automation (walking, driving, etc.) will not work.",
            Icons.AutoMirrored.Filled.DirectionsRun
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else "android.permission.BLUETOOTH_CONNECT",
            "Bluetooth Connect",
            "Used to detect connected Bluetooth devices.",
            "Allows the app to trigger actions when specific BT devices connect or disconnect.",
            "Used by Bluetooth triggers in Automation.",
            "Bluetooth device-specific automation will not work.",
            Icons.Default.Bluetooth
        ),
        PermissionRequirement(
            Manifest.permission.RECEIVE_MMS,
            "Receive MMS",
            "Needed to detect incoming multimedia messages.",
            "Required to trigger actions when an MMS is received.",
            "Used by Automation Engine.",
            "MMS triggers will not function.",
            Icons.Default.Mms
        ),
        PermissionRequirement(
            Manifest.permission.WRITE_CONTACTS,
            "Modify Contacts",
            "Needed to update or delete contact entries.",
            "Allows the app to clean up duplicates or update contact info.",
            "Used by Contact Manager.",
            "Contact modification features will be disabled.",
            Icons.Default.ContactPage
        ),
        PermissionRequirement(
            "android.permission.PACKAGE_USAGE_STATS",
            "Usage Stats",
            "Needed to detect which apps are running.",
            "Allows the app to see when other apps are opened or closed.",
            "Used by App triggers in Automation.",
            "App-based automation (App Opened/Closed) will not work.",
            Icons.Default.Apps
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Manifest.permission.QUERY_ALL_PACKAGES else "android.permission.QUERY_ALL_PACKAGES",
            "Query Packages",
            "Needed to see all installed apps.",
            "Allows the app to list all installed applications for selection.",
            "Used by App selectors in Automation.",
            "You won't be able to select specific apps for triggers.",
            Icons.Default.List
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else "android.permission.ACCESS_BACKGROUND_LOCATION",
            "Background Location",
            "Needed for automation while the app is closed.",
            "Allows the app to monitor geofences and location triggers even when not in the foreground.",
            "Used by Geofencing in Automation.",
            "Location triggers will only work while the app is open.",
            Icons.Default.LocationOn
        ),
        PermissionRequirement(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "android.permission.POST_NOTIFICATIONS",
            "Notifications",
            "Needed to show alerts and status.",
            "Allows the app to post notifications for automation actions and system status.",
            "Used by Notification actions and Foreground Services.",
            "You won't see status updates or automation alerts.",
            Icons.Default.Notifications
        )
    )

    fun getForPermission(permission: String): PermissionRequirement? {
        return permissions.find { it.permission == permission }
    }
}
