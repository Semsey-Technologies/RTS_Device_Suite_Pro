package com.semseytech.rtsdevicesuitepro.automation.models

import com.google.gson.annotations.JsonAdapter
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationTypeAdapter
import com.semseytech.rtsdevicesuitepro.automation.data.ParameterAdapter
import java.util.UUID

data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val actions: List<Action> = emptyList(),
    val isEnabled: Boolean = true
)

interface AutomationComponent {
    val type: String
    val displayName: String
    val description: String
    val category: String
    val icon: String
    val parameters: List<Parameter<*>>
    val requiredPermissions: List<String> get() = emptyList()
}

@JsonAdapter(AutomationTypeAdapter::class)
sealed class Trigger : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val category: String
    abstract override val icon: String
    abstract override val parameters: List<Parameter<*>>

    // --- 1. Google Sheets Notifications ---
    object SheetEdited : Trigger() {
        override val type = "GSHEET_EDITED"; override val displayName = "Sheet Edited"; override val description = "Someone edited your spreadsheet"; override val category = "Google Sheets"; override val icon = "table_chart"; override val parameters = emptyList<Parameter<*>>()
    }
    object MentionedInComment : Trigger() {
        override val type = "GSHEET_MENTIONED"; override val displayName = "Mentioned in Comment"; override val description = "You were mentioned in a comment"; override val category = "Google Sheets"; override val icon = "add_comment"; override val parameters = emptyList<Parameter<*>>()
    }
    object CommentAdded : Trigger() {
        override val type = "GSHEET_COMMENT_ADDED"; override val displayName = "Comment Added"; override val description = "A comment was added"; override val category = "Google Sheets"; override val icon = "comment"; override val parameters = emptyList<Parameter<*>>()
    }
    object CommentResolved : Trigger() {
        override val type = "GSHEET_COMMENT_RESOLVED"; override val displayName = "Comment Resolved"; override val description = "A comment was resolved"; override val category = "Google Sheets"; override val icon = "check_circle"; override val parameters = emptyList<Parameter<*>>()
    }
    object AccessRequestReceived : Trigger() {
        override val type = "GSHEET_ACCESS_REQUEST"; override val displayName = "Access Request"; override val description = "Access request received"; override val category = "Google Sheets"; override val icon = "person_add"; override val parameters = emptyList<Parameter<*>>()
    }
    object SpreadsheetShared : Trigger() {
        override val type = "GSHEET_SHARED"; override val displayName = "Spreadsheet Shared"; override val description = "Spreadsheet shared with you"; override val category = "Google Sheets"; override val icon = "share"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 2. Filesystem Triggers ---
    object GSheetFileCreated : Trigger() {
        override val type = "FILE_GSHEET_CREATED"; override val displayName = ".gsheet Created"; override val description = ".gsheet link file created"; override val category = "Filesystem"; override val icon = "insert_drive_file"; override val parameters = emptyList<Parameter<*>>()
    }
    object CsvExported : Trigger() {
        override val type = "FILE_CSV_EXPORTED"; override val displayName = "CSV Exported"; override val description = "CSV file exported"; override val category = "Filesystem"; override val icon = "file_present"; override val parameters = emptyList<Parameter<*>>()
    }
    object XlsxExported : Trigger() {
        override val type = "FILE_XLSX_EXPORTED"; override val displayName = "XLSX Exported"; override val description = "XLSX file exported"; override val category = "Filesystem"; override val icon = "grid_on"; override val parameters = emptyList<Parameter<*>>()
    }
    data class FileCreated(val path: String = "") : Trigger() {
        override val type = "FILE_CREATED"; override val displayName = "File Created"; override val description = "A file was created in a folder"; override val category = "Filesystem"; override val icon = "create_new_folder"; override val parameters = listOf(Parameter.TextParameter("path", "Folder Path", path))
        override val requiredPermissions = listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    data class FileDeleted(val path: String = "") : Trigger() {
        override val type = "FILE_DELETED"; override val displayName = "File Deleted"; override val description = "A file was deleted from a folder"; override val category = "Filesystem"; override val icon = "delete_forever"; override val parameters = listOf(Parameter.TextParameter("path", "Folder Path", path))
        override val requiredPermissions = listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    data class FileModified(val path: String = "") : Trigger() {
        override val type = "FILE_MODIFIED"; override val displayName = "File Modified"; override val description = "A file was modified in a folder"; override val category = "Filesystem"; override val icon = "edit_note"; override val parameters = listOf(Parameter.TextParameter("path", "Folder Path", path))
        override val requiredPermissions = listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    data class FolderChanged(val path: String = "") : Trigger() {
        override val type = "FOLDER_CHANGED"; override val displayName = "Folder Changed"; override val description = "Any change in folder contents"; override val category = "Filesystem"; override val icon = "folder_zip"; override val parameters = listOf(Parameter.TextParameter("path", "Folder Path", path))
        override val requiredPermissions = listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    object ExternalStorageMounted : Trigger() {
        override val type = "STORAGE_MOUNTED"; override val displayName = "Storage Mounted"; override val description = "External SD or USB mounted"; override val category = "Resources"; override val icon = "sd_card"; override val parameters = emptyList<Parameter<*>>()
    }
    object ExternalStorageUnmounted : Trigger() {
        override val type = "STORAGE_UNMOUNTED"; override val displayName = "Storage Unmounted"; override val description = "External storage removed"; override val category = "Resources"; override val icon = "sd_card_alert"; override val parameters = emptyList<Parameter<*>>()
    }
    object DownloadCompleted : Trigger() {
        override val type = "DOWNLOAD_COMPLETED"; override val displayName = "Download Done"; override val description = "A file finished downloading"; override val category = "Filesystem"; override val icon = "download_done"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 3. Display & System ---
    object ScreenOn : Trigger() {
        override val type = "SCREEN_ON"; override val displayName = "Screen On"; override val description = "Screen turned on"; override val category = "Display"; override val icon = "smartphone"; override val parameters = emptyList<Parameter<*>>()
    }
    object ScreenOff : Trigger() {
        override val type = "SCREEN_OFF"; override val displayName = "Screen Off"; override val description = "Screen turned off"; override val category = "Display"; override val icon = "screen_lock_portrait"; override val parameters = emptyList<Parameter<*>>()
    }
    object ScreenUnlocked : Trigger() {
        override val type = "SCREEN_UNLOCKED"; override val displayName = "Screen Unlocked"; override val description = "User unlocked device"; override val category = "Display"; override val icon = "lock_open"; override val parameters = emptyList<Parameter<*>>()
    }
    object ScreenLocked : Trigger() {
        override val type = "SCREEN_LOCKED"; override val displayName = "Screen Locked"; override val description = "Screen was locked"; override val category = "Display"; override val icon = "lock"; override val parameters = emptyList<Parameter<*>>()
    }
    object OrientationChanged : Trigger() {
        override val type = "ORIENTATION_CHANGED"; override val displayName = "Orientation Changed"; override val description = "Device rotated"; override val category = "Display"; override val icon = "screen_rotation"; override val parameters = emptyList<Parameter<*>>()
    }
    object RotationLocked : Trigger() {
        override val type = "ROTATION_LOCKED"; override val displayName = "Rotation Locked"; override val description = "Auto-rotation disabled"; override val category = "Display"; override val icon = "screen_lock_rotation"; override val parameters = emptyList<Parameter<*>>()
    }
    object RotationUnlocked : Trigger() {
        override val type = "ROTATION_UNLOCKED"; override val displayName = "Rotation Unlocked"; override val description = "Auto-rotation enabled"; override val category = "Display"; override val icon = "screen_rotation"; override val parameters = emptyList<Parameter<*>>()
    }
    object BootCompleted : Trigger() {
        override val type = "BOOT_COMPLETED"; override val displayName = "Device Booted"; override val description = "Device finished booting"; override val category = "System"; override val icon = "restart_alt"; override val parameters = emptyList<Parameter<*>>()
    }
    object ShutdownInitiated : Trigger() {
        override val type = "SHUTDOWN_INITIATED"; override val displayName = "Shutdown Started"; override val description = "Device is shutting down"; override val category = "System"; override val icon = "power_settings_new"; override val parameters = emptyList<Parameter<*>>()
    }
    object RebootRequested : Trigger() {
        override val type = "REBOOT_REQUESTED"; override val displayName = "Reboot Initiated"; override val description = "Device is rebooting"; override val category = "System"; override val icon = "cached"; override val parameters = emptyList<Parameter<*>>()
    }
    object DeviceIdleEntered : Trigger() {
        override val type = "DEVICE_IDLE_ENTERED"; override val displayName = "Idle Mode Entered"; override val description = "Device entered idle state"; override val category = "System"; override val icon = "bedtime"; override val parameters = emptyList<Parameter<*>>()
    }
    object DeviceIdle : Trigger() {
        override val type = "DEVICE_IDLE"; override val displayName = "Device Idle"; override val description = "Device is not being used"; override val category = "System"; override val icon = "bedtime"; override val parameters = emptyList<Parameter<*>>()
    }
    object DozeModeEntered : Trigger() {
        override val type = "DOZE_MODE_ENTERED"; override val displayName = "Doze Mode Entered"; override val description = "Device entered doze mode"; override val category = "System"; override val icon = "snooze"; override val parameters = emptyList<Parameter<*>>()
    }
    object DozeModeExited : Trigger() {
        override val type = "DOZE_MODE_EXITED"; override val displayName = "Doze Mode Exited"; override val description = "Device exited doze mode"; override val category = "System"; override val icon = "wb_sunny"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 4. Power & Battery ---
    object PowerConnected : Trigger() {
        override val type = "POWER_CONNECTED"; override val displayName = "Power Connected"; override val description = "Charger plugged in"; override val category = "Power"; override val icon = "power"; override val parameters = emptyList<Parameter<*>>()
    }
    object PowerDisconnected : Trigger() {
        override val type = "POWER_DISCONNECTED"; override val displayName = "Power Disconnected"; override val description = "Charger removed"; override val category = "Power"; override val icon = "power_off"; override val parameters = emptyList<Parameter<*>>()
    }
    data class BatteryLevelAbove(val level: Int = 80) : Trigger() {
        override val type = "BATTERY_LEVEL_ABOVE"; override val displayName = "Battery Above"; override val description = "Battery level above X%"; override val category = "Power"; override val icon = "battery_full"; override val parameters = listOf(Parameter.NumberParameter("level", "Level %", level, 0, 100))
    }
    data class BatteryLevelBelow(val level: Int = 20) : Trigger() {
        override val type = "BATTERY_LEVEL_BELOW"; override val displayName = "Battery Below"; override val description = "Battery level below X%"; override val category = "Power"; override val icon = "battery_alert"; override val parameters = listOf(Parameter.NumberParameter("level", "Level %", level, 0, 100))
    }
    data class BatteryTempAbove(val temp: Int = 40) : Trigger() {
        override val type = "BATTERY_TEMP_ABOVE"; override val displayName = "Battery Temp Above"; override val description = "Temperature above X°C"; override val category = "Power"; override val icon = "thermostat"; override val parameters = listOf(Parameter.NumberParameter("temp", "Temp °C", temp, 0, 100))
    }
    data class BatteryTempBelow(val temp: Int = 20) : Trigger() {
        override val type = "BATTERY_TEMP_BELOW"; override val displayName = "Battery Temp Below"; override val description = "Temperature below X°C"; override val category = "Power"; override val icon = "ac_unit"; override val parameters = listOf(Parameter.NumberParameter("temp", "Temp °C", temp, 0, 100))
    }
    object BatteryFastCharging : Trigger() {
        override val type = "BATTERY_FAST_CHARGING"; override val displayName = "Fast Charging"; override val description = "Fast charging detected"; override val category = "Power"; override val icon = "bolt"; override val parameters = emptyList<Parameter<*>>()
    }
    object BatteryWirelessCharging : Trigger() {
        override val type = "BATTERY_WIRELESS_CHARGING"; override val displayName = "Wireless Charging"; override val description = "Wireless charging started"; override val category = "Power"; override val icon = "settings_input_antenna"; override val parameters = emptyList<Parameter<*>>()
    }
    object PowerSaveModeOn : Trigger() {
        override val type = "POWER_SAVE_ON"; override val displayName = "Power Save On"; override val description = "Battery saver enabled"; override val category = "Power"; override val icon = "battery_saver"; override val parameters = emptyList<Parameter<*>>()
    }
    object PowerSaveModeOff : Trigger() {
        override val type = "POWER_SAVE_OFF"; override val displayName = "Power Save Off"; override val description = "Battery saver disabled"; override val category = "Power"; override val icon = "battery_full"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 5. Network & Connectivity ---
    object WiFiConnected : Trigger() {
        override val type = "WIFI_CONNECTED"; override val displayName = "WiFi Connected"; override val description = "Connected to any WiFi"; override val category = "Network"; override val icon = "wifi"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    object WiFiDisconnected : Trigger() {
        override val type = "WIFI_DISCONNECTED"; override val displayName = "WiFi Disconnected"; override val description = "Lost WiFi connection"; override val category = "Network"; override val icon = "wifi_off"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class SpecificWiFiConnected(val ssid: String = "") : Trigger() {
        override val type = "SPECIFIC_WIFI_CONNECTED"; override val displayName = "WiFi SSID Connected"; override val description = "Connected to specific SSID"; override val category = "Network"; override val icon = "network_wifi"; override val parameters = listOf(Parameter.TextParameter("ssid", "SSID", ssid))
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class WiFiSignalStrength(val threshold: Int = -70, val comparison: String = "LESS_THAN") : Trigger() {
        override val type = "WIFI_SIGNAL_THRESHOLD"; override val displayName = "WiFi Signal Strength"; override val description = "WiFi signal threshold crossed"; override val category = "Network"; override val icon = "signal_wifi_4_bar"; override val parameters = listOf(Parameter.NumberParameter("threshold", "Threshold (dBm)", threshold, -100, -30), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("LESS_THAN", "GREATER_THAN")))
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    object MobileDataOn : Trigger() {
        override val type = "MOBILE_DATA_ON"; override val displayName = "Mobile Data On"; override val description = "Mobile data enabled"; override val category = "Network"; override val icon = "signal_cellular_4_bar"; override val parameters = emptyList<Parameter<*>>()
    }
    object MobileDataOff : Trigger() {
        override val type = "MOBILE_DATA_OFF"; override val displayName = "Mobile Data Off"; override val description = "Mobile data disabled"; override val category = "Network"; override val icon = "signal_cellular_connected_no_internet_4_bar"; override val parameters = emptyList<Parameter<*>>()
    }
    data class MobileDataTypeChanged(val targetType: String = "ANY") : Trigger() {
        override val type = "MOBILE_DATA_TYPE_CHANGED"; override val displayName = "Mobile Data Type"; override val description = "3G/4G/5G changed"; override val category = "Network"; override val icon = "network_check"; override val parameters = listOf(Parameter.SelectionParameter("targetType", "Target Type", targetType, listOf("ANY", "2G", "3G", "4G", "5G")))
    }
    object HotspotOn : Trigger() {
        override val type = "HOTSPOT_ON"; override val displayName = "Hotspot On"; override val description = "WiFi Hotspot enabled"; override val category = "Network"; override val icon = "wifi_tethering"; override val parameters = emptyList<Parameter<*>>()
    }
    object HotspotOff : Trigger() {
        override val type = "HOTSPOT_OFF"; override val displayName = "Hotspot Off"; override val description = "WiFi Hotspot disabled"; override val category = "Network"; override val icon = "wifi_tethering_off"; override val parameters = emptyList<Parameter<*>>()
    }
    object VpnConnected : Trigger() {
        override val type = "VPN_CONNECTED"; override val displayName = "VPN Connected"; override val description = "VPN connection established"; override val category = "Network"; override val icon = "vpn_lock"; override val parameters = emptyList<Parameter<*>>()
    }
    object VpnDisconnected : Trigger() {
        override val type = "VPN_DISCONNECTED"; override val displayName = "VPN Disconnected"; override val description = "VPN connection lost"; override val category = "Network"; override val icon = "vpn_lock"; override val parameters = emptyList<Parameter<*>>()
    }
    object IpAddressChanged : Trigger() {
        override val type = "IP_ADDRESS_CHANGED"; override val displayName = "IP Address Changed"; override val description = "Local IP address changed"; override val category = "Network"; override val icon = "lan"; override val parameters = emptyList<Parameter<*>>()
    }
    object NetworkAvailable : Trigger() {
        override val type = "NETWORK_AVAILABLE"; override val displayName = "Network Available"; override val description = "Internet access restored"; override val category = "Network"; override val icon = "public"; override val parameters = emptyList<Parameter<*>>()
    }
    object NetworkLost : Trigger() {
        override val type = "NETWORK_LOST"; override val displayName = "Network Lost"; override val description = "Internet access lost"; override val category = "Network"; override val icon = "public_off"; override val parameters = emptyList<Parameter<*>>()
    }
    data class BluetoothDeviceDisconnected(val address: String = "") : Trigger() {
        override val type = "BT_DEVICE_DISCONNECTED"; override val displayName = "BT Device Disconnected"; override val description = "Specific device disconnected"; override val category = "Network"; override val icon = "bluetooth_disabled"; override val parameters = listOf(Parameter.BluetoothParameter("address", "Select Device", address))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) listOf(android.Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
    }
    data class PingStatus(val host: String = "8.8.8.8", val status: String = "SUCCESS") : Trigger() {
        override val type = "PING_STATUS"; override val displayName = "Ping Status"; override val description = "Ping success or failure"; override val category = "Network"; override val icon = "settings_ethernet"; override val parameters = listOf(Parameter.TextParameter("host", "Host", host), Parameter.SelectionParameter("status", "Status", status, listOf("SUCCESS", "FAILURE")))
    }
    data class DomainReachability(val domain: String = "google.com", val status: String = "REACHABLE") : Trigger() {
        override val type = "DOMAIN_REACHABILITY"; override val displayName = "Domain Reachability"; override val description = "Domain becomes reachable/unreachable"; override val category = "Network"; override val icon = "language"; override val parameters = listOf(Parameter.TextParameter("domain", "Domain", domain), Parameter.SelectionParameter("status", "Status", status, listOf("REACHABLE", "UNREACHABLE")))
    }
    data class NetworkSpeedThreshold(val speedMbps: Int = 10, val comparison: String = "LESS_THAN") : Trigger() {
        override val type = "NETWORK_SPEED_THRESHOLD"; override val displayName = "Network Speed"; override val description = "Speed threshold crossed"; override val category = "Network"; override val icon = "speed"; override val parameters = listOf(Parameter.NumberParameter("speedMbps", "Speed (Mbps)", speedMbps, 1, 1000), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("LESS_THAN", "GREATER_THAN")))
    }
    object BluetoothOn : Trigger() {
        override val type = "BLUETOOTH_ON"; override val displayName = "Bluetooth On"; override val description = "Bluetooth enabled"; override val category = "Network"; override val icon = "bluetooth"; override val parameters = emptyList<Parameter<*>>()
    }
    object BluetoothOff : Trigger() {
        override val type = "BLUETOOTH_OFF"; override val displayName = "Bluetooth Off"; override val description = "Bluetooth disabled"; override val category = "Network"; override val icon = "bluetooth_disabled"; override val parameters = emptyList<Parameter<*>>()
    }
    data class BluetoothDeviceConnected(val address: String = "") : Trigger() {
        override val type = "BT_DEVICE_CONNECTED"; override val displayName = "BT Device Connected"; override val description = "Specific device connected"; override val category = "Network"; override val icon = "bluetooth_connected"; override val parameters = listOf(Parameter.BluetoothParameter("address", "Select Device", address))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) listOf(android.Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
    }
    object AirplaneModeOn : Trigger() {
        override val type = "AIRPLANE_MODE_ON"; override val displayName = "Airplane Mode On"; override val description = "Airplane mode enabled"; override val category = "Hardware"; override val icon = "airplane_ticket"; override val parameters = emptyList<Parameter<*>>()
    }
    object AirplaneModeOff : Trigger() {
        override val type = "AIRPLANE_MODE_OFF"; override val displayName = "Airplane Mode Off"; override val description = "Airplane mode disabled"; override val category = "Hardware"; override val icon = "airplanemode_inactive"; override val parameters = emptyList<Parameter<*>>()
    }
    object NfcTagScanned : Trigger() {
        override val type = "NFC_SCANNED"; override val displayName = "NFC Tag Scanned"; override val description = "An NFC tag was detected"; override val category = "Hardware"; override val icon = "nfc"; override val parameters = emptyList<Parameter<*>>()
    }
    object UsbConnected : Trigger() {
        override val type = "USB_CONNECTED"; override val displayName = "USB Connected"; override val description = "USB cable plugged in"; override val category = "Hardware"; override val icon = "usb"; override val parameters = emptyList<Parameter<*>>()
    }
    object UsbDisconnected : Trigger() {
        override val type = "USB_DISCONNECTED"; override val displayName = "USB Disconnected"; override val description = "USB cable removed"; override val category = "Hardware"; override val icon = "usb_off"; override val parameters = emptyList<Parameter<*>>()
    }
    object HeadphonesPlugged : Trigger() {
        override val type = "HEADPHONES_PLUGGED"; override val displayName = "Headphones Plugged"; override val description = "Headphones connected"; override val category = "Hardware"; override val icon = "headset"; override val parameters = emptyList<Parameter<*>>()
    }
    object HeadphonesUnplugged : Trigger() {
        override val type = "HEADPHONES_UNPLUGGED"; override val displayName = "Headphones Unplugged"; override val description = "Headphones removed"; override val category = "Hardware"; override val icon = "headset_off"; override val parameters = emptyList<Parameter<*>>()
    }

    data class GeofenceEnter(val name: String = "Home", val latitude: Double = 0.0, val longitude: Double = 0.0, val radius: Float = 100f) : Trigger() {
        override val type = "GEOFENCE_ENTER"; override val displayName = "Enter Geofence"; override val description = "Entered specific area"; override val category = "Location"; override val icon = "location_on"; override val parameters = listOf(Parameter.TextParameter("name", "Location Name", name), Parameter.NumberParameter("radius", "Radius (m)", radius.toInt(), 50, 5000))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    data class GeofenceExit(val name: String = "Home", val latitude: Double = 0.0, val longitude: Double = 0.0, val radius: Float = 100f) : Trigger() {
        override val type = "GEOFENCE_EXIT"; override val displayName = "Exit Geofence"; override val description = "Left specific area"; override val category = "Location"; override val icon = "location_off"; override val parameters = listOf(Parameter.TextParameter("name", "Location Name", name), Parameter.NumberParameter("radius", "Radius (m)", radius.toInt(), 50, 5000))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    data class ArriveAtPlace(val place: String = "Home") : Trigger() {
        override val type = "ARRIVE_AT_PLACE"; override val displayName = "Arrive at Place"; override val description = "Arrived at a saved place"; override val category = "Location"; override val icon = "home"; override val parameters = listOf(Parameter.SelectionParameter("place", "Saved Place", place, listOf("Home", "Work", "Gym", "School")))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    data class LeavePlace(val place: String = "Home") : Trigger() {
        override val type = "LEAVE_PLACE"; override val displayName = "Leave Place"; override val description = "Left a saved place"; override val category = "Location"; override val icon = "exit_to_app"; override val parameters = listOf(Parameter.SelectionParameter("place", "Saved Place", place, listOf("Home", "Work", "Gym", "School")))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    data class GpsStateChanged(val enabled: Boolean = true) : Trigger() {
        override val type = "GPS_STATE_CHANGED"; override val displayName = "GPS On/Off"; override val description = "GPS was enabled or disabled"; override val category = "Location"; override val icon = "gps_fixed"; override val parameters = listOf(Parameter.SelectionParameter("enabled", "Target State", if (enabled) "ON" else "OFF", listOf("ON", "OFF")))
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class SpeedThreshold(val speedKmh: Int = 50, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SPEED_THRESHOLD"; override val displayName = "Speed Threshold"; override val description = "Moving faster/slower than X km/h"; override val category = "Motion"; override val icon = "speed"; override val parameters = listOf(Parameter.NumberParameter("speed", "Speed (km/h)", speedKmh, 0, 300), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class ActivityDetected(val activity: String = "WALKING") : Trigger() {
        override val type = "ACTIVITY_DETECTED"; override val displayName = "Activity Detected"; override val description = "Walking, running, driving, etc."; override val category = "Motion"; override val icon = "directions_run"; override val parameters = listOf(Parameter.SelectionParameter("activity", "Activity", activity, listOf("WALKING", "RUNNING", "DRIVING", "BIKING", "STILL")))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) listOf(android.Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
    }
    object ActivityWalking : Trigger() {
        override val type = "ACTIVITY_WALKING"; override val displayName = "Walking Detected"; override val description = "Device detects walking"; override val category = "Motion"; override val icon = "directions_walk"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) listOf(android.Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
    }
    object ActivityDriving : Trigger() {
        override val type = "ACTIVITY_DRIVING"; override val displayName = "Driving Detected"; override val description = "Device detects driving"; override val category = "Motion"; override val icon = "directions_car"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) listOf(android.Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
    }
    object SignificantLocationChange : Trigger() {
        override val type = "SIGNIFICANT_LOCATION_CHANGE"; override val displayName = "Significant Location Change"; override val description = "Moved a significant distance"; override val category = "Location"; override val icon = "my_location"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class CompassHeading(val degree: Int = 0, val tolerance: Int = 10) : Trigger() {
        override val type = "COMPASS_HEADING"; override val displayName = "Compass Direction"; override val description = "Facing a specific direction"; override val category = "Sensors"; override val icon = "explore"; override val parameters = listOf(Parameter.NumberParameter("degree", "Heading (°)", degree, 0, 359), Parameter.NumberParameter("tolerance", "Tolerance (°)", tolerance, 1, 45))
    }
    data class AltitudeThreshold(val altitudeMeters: Int = 500, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "ALTITUDE_THRESHOLD"; override val displayName = "Altitude Threshold"; override val description = "Above/below specific altitude"; override val category = "Sensors"; override val icon = "landscape"; override val parameters = listOf(Parameter.NumberParameter("altitude", "Altitude (m)", altitudeMeters, -500, 10000), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
    }
    data class StepCountThreshold(val steps: Int = 10000) : Trigger() {
        override val type = "STEP_COUNT_THRESHOLD"; override val displayName = "Step Counter"; override val description = "Daily steps reached threshold"; override val category = "Motion"; override val icon = "directions_walk"; override val parameters = listOf(Parameter.NumberParameter("steps", "Target Steps", steps, 1, 100000))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) listOf(android.Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
    }
    data class ProximityTriggered(val near: Boolean = true) : Trigger() {
        override val type = "PROXIMITY_TRIGGERED"; override val displayName = "Proximity Sensor"; override val description = "Object detected near sensor"; override val category = "Sensors"; override val icon = "visibility"; override val parameters = listOf(Parameter.SelectionParameter("near", "State", if (near) "NEAR" else "FAR", listOf("NEAR", "FAR")))
    }
    data class AccelerometerPattern(val pattern: String = "SHAKE") : Trigger() {
        override val type = "ACCELEROMETER_PATTERN"; override val displayName = "Movement Pattern"; override val description = "Shake, Tilt, or Jolt detected"; override val category = "Sensors"; override val icon = "vibration"; override val parameters = listOf(Parameter.SelectionParameter("pattern", "Pattern", pattern, listOf("SHAKE", "TILT_LEFT", "TILT_RIGHT", "JOLT")))
    }
    data class GyroscopePattern(val pattern: String = "ROTATION") : Trigger() {
        override val type = "GYROSCOPE_PATTERN"; override val displayName = "Rotation Pattern"; override val description = "Specific rotation detected"; override val category = "Sensors"; override val icon = "sync"; override val parameters = listOf(Parameter.SelectionParameter("pattern", "Pattern", pattern, listOf("ROTATION", "FLIP")))
    }
    data class MagnetometerThreshold(val strength: Int = 50, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_MAGNETIC"; override val displayName = "Magnetic Field"; override val description = "Magnetic field strength threshold"; override val category = "Sensors"; override val icon = "explore"; override val parameters = listOf(Parameter.NumberParameter("strength", "Strength (µT)", strength, 0, 1000), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
    }
    data class BarometerThreshold(val pressure: Int = 1013, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_PRESSURE"; override val displayName = "Barometer"; override val description = "Atmospheric pressure threshold"; override val category = "Sensors"; override val icon = "compress"; override val parameters = listOf(Parameter.NumberParameter("pressure", "Pressure (hPa)", pressure, 800, 1200), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
    }
    data class TemperatureThreshold(val temp: Int = 25, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_TEMP"; override val displayName = "Ambient Temperature"; override val description = "Air temperature threshold"; override val category = "Sensors"; override val icon = "thermostat"; override val parameters = listOf(Parameter.NumberParameter("temp", "Temp (°C)", temp, -20, 60), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
    }
    data class HumidityThreshold(val humidity: Int = 50, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_HUMIDITY"; override val displayName = "Ambient Humidity"; override val description = "Relative humidity threshold"; override val category = "Sensors"; override val icon = "water_drop"; override val parameters = listOf(Parameter.NumberParameter("humidity", "Humidity (%)", humidity, 0, 100), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
    }
    data class HeartRateThreshold(val bpm: Int = 80, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_HEART_RATE"; override val displayName = "Heart Rate"; override val description = "Heart rate (BPM) threshold"; override val category = "Sensors"; override val icon = "favorite"; override val parameters = listOf(Parameter.NumberParameter("bpm", "BPM", bpm, 30, 220), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
        override val requiredPermissions = listOf(android.Manifest.permission.BODY_SENSORS)
    }
    data class AmbientNoiseThreshold(val decibels: Int = 60, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "SENSOR_NOISE"; override val displayName = "Ambient Noise"; override val description = "Sound level (dB) threshold"; override val category = "Sensors"; override val icon = "graphic_eq"; override val parameters = listOf(Parameter.NumberParameter("decibels", "Level (dB)", decibels, 0, 120), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN")))
        override val requiredPermissions = listOf(android.Manifest.permission.RECORD_AUDIO)
    }
    data class TouchGesturePattern(val pattern: String = "DOUBLE_TAP") : Trigger() {
        override val type = "TOUCH_GESTURE"; override val displayName = "Touch Gesture"; override val description = "Specific screen gesture detected"; override val category = "Interaction"; override val icon = "touch_app"; override val parameters = listOf(Parameter.SelectionParameter("pattern", "Gesture", pattern, listOf("DOUBLE_TAP", "TRIPLE_TAP", "LONG_PRESS_TWO_FINGERS", "SWIPE_UP_THREE_FINGERS")))
    }

    // --- 7. Communication ---
    object SmsReceived : Trigger() {
        override val type = "SMS_RECEIVED"; override val displayName = "SMS Received"; override val description = "New SMS arrived"; override val category = "Communication"; override val icon = "sms"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_SMS)
    }
    data class SmsFromContact(val contact: String = "") : Trigger() {
        override val type = "SMS_FROM_CONTACT"; override val displayName = "SMS From Contact"; override val description = "SMS from specific person"; override val category = "Communication"; override val icon = "person"; override val parameters = listOf(Parameter.TextParameter("contact", "Contact/Number", contact))
        override val requiredPermissions = listOf(android.Manifest.permission.RECEIVE_SMS, android.Manifest.permission.READ_SMS)
    }
    object MmsReceived : Trigger() {
        override val type = "MMS_RECEIVED"; override val displayName = "MMS Received"; override val description = "New MMS arrived"; override val category = "Communication"; override val icon = "mms"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.RECEIVE_MMS)
    }
    object IncomingCall : Trigger() {
        override val type = "CALL_INCOMING"; override val displayName = "Incoming Call"; override val description = "Phone is ringing"; override val category = "Communication"; override val icon = "call"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE)
    }
    object CallAnswered : Trigger() {
        override val type = "CALL_ANSWERED"; override val displayName = "Call Answered"; override val description = "Call was picked up"; override val category = "Communication"; override val icon = "call_received"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE)
    }
    object CallEnded : Trigger() {
        override val type = "CALL_ENDED"; override val displayName = "Call Ended"; override val description = "Phone call finished"; override val category = "Communication"; override val icon = "call_end"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE)
    }
    object MissedCall : Trigger() {
        override val type = "CALL_MISSED"; override val displayName = "Missed Call"; override val description = "A call was missed"; override val category = "Communication"; override val icon = "call_missed"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE)
    }
    object VoicemailReceived : Trigger() {
        override val type = "VOICEMAIL_RECEIVED"; override val displayName = "Voicemail Received"; override val description = "New voicemail arrived"; override val category = "Communication"; override val icon = "voicemail"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE)
    }
    data class EmailReceived(val account: String = "") : Trigger() {
        override val type = "EMAIL_RECEIVED"; override val displayName = "Email Received"; override val description = "New email in account"; override val category = "Communication"; override val icon = "email"; override val parameters = listOf(Parameter.TextParameter("account", "Account", account))
    }
    data class MessagingAppNotification(val packageName: String = "") : Trigger() {
        override val type = "MESSAGING_APP_NOTIFICATION"; override val displayName = "Messenger Notification"; override val description = "Message from chat app"; override val category = "Communication"; override val icon = "chat"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class ContactStatusChanged(val contact: String = "", val status: String = "ONLINE") : Trigger() {
        override val type = "CONTACT_STATUS_CHANGED"; override val displayName = "Contact Status"; override val description = "Contact online/offline"; override val category = "Communication"; override val icon = "person_search"; override val parameters = listOf(Parameter.TextParameter("contact", "Contact Name", contact), Parameter.SelectionParameter("status", "Status", status, listOf("ONLINE", "OFFLINE")))
    }
    data class NotificationKeyword(val keyword: String = "", val packageName: String = "") : Trigger() {
        override val type = "NOTIFICATION_KEYWORD"; override val displayName = "Keyword in Message"; override val description = "Keyword found in notification"; override val category = "Communication"; override val icon = "find_in_page"; override val parameters = listOf(Parameter.TextParameter("keyword", "Keyword", keyword), Parameter.AppParameter("packageName", "App (Optional)", packageName))
    }

    // --- 8. Apps & Interaction ---
    data class AppOpened(val packageName: String = "") : Trigger() {
        override val type = "APP_OPENED"; override val displayName = "App Opened"; override val description = "Specific app launched"; override val category = "Apps"; override val icon = "apps"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
        override val requiredPermissions = listOf("android.permission.PACKAGE_USAGE_STATS")
    }
    data class AppClosed(val packageName: String = "") : Trigger() {
        override val type = "APP_CLOSED"; override val displayName = "App Closed"; override val description = "Specific app closed"; override val category = "Apps"; override val icon = "close"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
        override val requiredPermissions = listOf("android.permission.PACKAGE_USAGE_STATS")
    }
    data class AppInstalled(val packageName: String = "") : Trigger() {
        override val type = "APP_INSTALLED"; override val displayName = "App Installed"; override val description = "New app installed"; override val category = "Apps"; override val icon = "install_mobile"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) listOf(android.Manifest.permission.QUERY_ALL_PACKAGES) else emptyList()
    }
    data class AppUninstalled(val packageName: String = "") : Trigger() {
        override val type = "APP_UNINSTALLED"; override val displayName = "App Uninstalled"; override val description = "App uninstalled"; override val category = "Apps"; override val icon = "delete_sweep"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class AppUpdated(val packageName: String = "") : Trigger() {
        override val type = "APP_UPDATED"; override val displayName = "App Updated"; override val description = "App updated to new version"; override val category = "Apps"; override val icon = "update"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class AppCrashed(val packageName: String = "") : Trigger() {
        override val type = "APP_CRASHED"; override val displayName = "App Crashed"; override val description = "App stopped unexpectedly"; override val category = "Apps"; override val icon = "error"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    object ForegroundAppChanged : Trigger() {
        override val type = "FOREGROUND_APP_CHANGED"; override val displayName = "Foreground App Changed"; override val description = "User switched apps"; override val category = "Apps"; override val icon = "api"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf("android.permission.PACKAGE_USAGE_STATS")
    }
    data class NotificationPosted(val packageName: String = "") : Trigger() {
        override val type = "NOTIFICATION_POSTED"; override val displayName = "Notification Posted"; override val description = "App posted a notification"; override val category = "System UI"; override val icon = "notifications_active"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class NotificationRemoved(val packageName: String = "") : Trigger() {
        override val type = "NOTIFICATION_REMOVED"; override val displayName = "Notification Removed"; override val description = "Notification was dismissed"; override val category = "System UI"; override val icon = "notifications_off"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class NotificationMatches(val pattern: String = "") : Trigger() {
        override val type = "NOTIFICATION_MATCHES"; override val displayName = "Notification Content"; override val description = "Notification text matches pattern"; override val category = "System UI"; override val icon = "notification_important"; override val parameters = listOf(Parameter.TextParameter("pattern", "Text Pattern", pattern))
    }
    object SystemDialogOpened : Trigger() {
        override val type = "SYSTEM_DIALOG_OPENED"; override val displayName = "System Dialog Opened"; override val description = "A system dialog appeared"; override val category = "System UI"; override val icon = "picture_in_picture"; override val parameters = emptyList<Parameter<*>>()
    }
    data class KeyboardStateChanged(val opened: Boolean = true) : Trigger() {
        override val type = "KEYBOARD_STATE_CHANGED"; override val displayName = "Keyboard State"; override val description = "Keyboard opened or closed"; override val category = "Interaction"; override val icon = "keyboard"; override val parameters = listOf(Parameter.SelectionParameter("opened", "State", if (opened) "OPENED" else "CLOSED", listOf("OPENED", "CLOSED")))
    }
    data class AccessibilityEventDetected(val eventType: String = "VIEW_CLICKED") : Trigger() {
        override val type = "ACCESSIBILITY_EVENT"; override val displayName = "Accessibility Event"; override val description = "Detected UI interaction"; override val category = "Interaction"; override val icon = "accessibility"; override val parameters = listOf(Parameter.SelectionParameter("eventType", "Event Type", eventType, listOf("VIEW_CLICKED", "VIEW_FOCUSED", "WINDOW_STATE_CHANGED", "NOTIFICATION_STATE_CHANGED")))
    }
    data class ToastDetected(val message: String = "") : Trigger() {
        override val type = "TOAST_DETECTED"; override val displayName = "Toast Message"; override val description = "A toast popup was shown"; override val category = "System UI"; override val icon = "announcement"; override val parameters = listOf(Parameter.TextParameter("message", "Message Pattern", message))
    }
    data class OverlayPermissionChanged(val granted: Boolean = true) : Trigger() {
        override val type = "OVERLAY_PERMISSION_CHANGED"; override val displayName = "Overlay Permission"; override val description = "Overlay permission changed"; override val category = "System"; override val icon = "layers"; override val parameters = listOf(Parameter.SelectionParameter("granted", "State", if (granted) "GRANTED" else "REMOVED", listOf("GRANTED", "REMOVED")))
    }
    object ClipboardChanged : Trigger() {
        override val type = "CLIPBOARD_CHANGED"; override val displayName = "Clipboard Changed"; override val description = "Copied text changed"; override val category = "Interaction"; override val icon = "content_paste"; override val parameters = emptyList<Parameter<*>>()
    }
    object ScreenshotTaken : Trigger() {
        override val type = "SCREENSHOT_TAKEN"; override val displayName = "Screenshot Taken"; override val description = "A screenshot was captured"; override val category = "Interaction"; override val icon = "screenshot"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    object VolumeButtonPressed : Trigger() {
        override val type = "VOLUME_BUTTON_PRESSED"; override val displayName = "Volume Button"; override val description = "Volume key pressed"; override val category = "Interaction"; override val icon = "volume_up"; override val parameters = emptyList<Parameter<*>>()
    }
    object HardwareButtonLongPress : Trigger() {
        override val type = "HARDWARE_BUTTON_LONG_PRESS"; override val displayName = "Button Long Press"; override val description = "Button held down"; override val category = "Interaction"; override val icon = "touch_app"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 9. Time & Schedule ---
    data class TimeOfDay(val hour: Int = 12, val minute: Int = 0) : Trigger() {
        override val type = "TIME_OF_DAY"; override val displayName = "Time of Day"; override val description = "Runs at set time daily"; override val category = "Time"; override val icon = "schedule"; override val parameters = listOf(Parameter.TimeParameter("time", "Time", String.format("%02d:%02d", hour, minute)))
    }
    data class TimeRange(val startHour: Int = 9, val startMin: Int = 0, val endHour: Int = 17, val endMin: Int = 0) : Trigger() {
        override val type = "TIME_RANGE"; override val displayName = "Time Range"; override val description = "Triggered within time window"; override val category = "Time"; override val icon = "date_range"; override val parameters = listOf(Parameter.TimeParameter("start", "Start Time", String.format("%02d:%02d", startHour, startMin)), Parameter.TimeParameter("end", "End Time", String.format("%02d:%02d", endHour, endMin)))
    }
    data class DaysOfWeek(val days: List<Int> = emptyList()) : Trigger() {
        override val type = "DAYS_OF_WEEK"; override val displayName = "Days of Week"; override val description = "Runs on selected days"; override val category = "Schedule"; override val icon = "calendar_month"; override val parameters = emptyList<Parameter<*>>()
    }
    data class DayOfMonth(val day: Int = 1) : Trigger() {
        override val type = "DAY_OF_MONTH"; override val displayName = "Day of Month"; override val description = "Runs on a specific day each month"; override val category = "Schedule"; override val icon = "calendar_today"; override val parameters = listOf(Parameter.NumberParameter("day", "Day", day, 1, 31))
    }
    data class MonthOfYear(val month: Int = 1) : Trigger() {
        override val type = "MONTH_OF_YEAR"; override val displayName = "Month of Year"; override val description = "Runs on first day of selected month"; override val category = "Schedule"; override val icon = "calendar_view_month"; override val parameters = listOf(Parameter.NumberParameter("month", "Month (1-12)", month, 1, 12))
    }
    data class RecurringTrigger(val type_rec: String = "WEEKLY") : Trigger() {
        override val type = "RECURRING"; override val displayName = "Recurring"; override val description = "Weekly/Monthly schedule"; override val category = "Schedule"; override val icon = "event_repeat"; override val parameters = listOf(Parameter.SelectionParameter("type_rec", "Frequency", type_rec, listOf("WEEKLY", "BIWEEKLY", "MONTHLY")))
    }
    data class IntervalTrigger(val days: Int = 1) : Trigger() {
        override val type = "INTERVAL"; override val displayName = "Interval"; override val description = "Every X days"; override val category = "Schedule"; override val icon = "update"; override val parameters = listOf(Parameter.NumberParameter("days", "Days", days, 1, 365))
    }
    object Sunrise : Trigger() {
        override val type = "SUNRISE"; override val displayName = "Sunrise"; override val description = "At sunrise"; override val category = "Time"; override val icon = "wb_sunny"; override val parameters = emptyList<Parameter<*>>()
    }
    object Sunset : Trigger() {
        override val type = "SUNSET"; override val displayName = "Sunset"; override val description = "At sunset"; override val category = "Time"; override val icon = "nights_stay"; override val parameters = emptyList<Parameter<*>>()
    }
    data class GoldenHour(val isMorning: Boolean = true) : Trigger() {
        override val type = "GOLDEN_HOUR"; override val displayName = "Golden Hour"; override val description = "Morning or evening golden hour"; override val category = "Time"; override val icon = "wb_twilight"; override val parameters = listOf(Parameter.SelectionParameter("isMorning", "Time", if(isMorning) "MORNING" else "EVENING", listOf("MORNING", "EVENING")))
    }
    data class TimerFinished(val timerName: String = "Default") : Trigger() {
        override val type = "TIMER_FINISHED"; override val displayName = "Timer Finished"; override val description = "Specific timer reached zero"; override val category = "Time"; override val icon = "timer"; override val parameters = listOf(Parameter.TextParameter("timerName", "Timer Name", timerName))
    }
    data class StopwatchThreshold(val stopwatchName: String = "Default", val thresholdSeconds: Int = 60) : Trigger() {
        override val type = "STOPWATCH_THRESHOLD"; override val displayName = "Stopwatch Threshold"; override val description = "Stopwatch exceeded X seconds"; override val category = "Time"; override val icon = "timer_10"; override val parameters = listOf(Parameter.TextParameter("stopwatchName", "Stopwatch Name", stopwatchName), Parameter.NumberParameter("threshold", "Seconds", thresholdSeconds, 1, 36000))
    }
    data class CountdownReached(val countdownName: String = "Default") : Trigger() {
        override val type = "COUNTDOWN_REACHED"; override val displayName = "Countdown Reached"; override val description = "Countdown timer finished"; override val category = "Time"; override val icon = "hourglass_bottom"; override val parameters = listOf(Parameter.TextParameter("countdownName", "Countdown Name", countdownName))
    }
    data class CronSchedule(val expression: String = "0 0 * * *") : Trigger() {
        override val type = "CRON_SCHEDULE"; override val displayName = "Cron Schedule"; override val description = "Advanced cron-style scheduling"; override val category = "Schedule"; override val icon = "code"; override val parameters = listOf(Parameter.TextParameter("expression", "Cron Expression", expression))
    }

    // --- 10. Sensors & Resources ---
    data class LightSensorThreshold(val lux: Int = 100) : Trigger() {
        override val type = "SENSOR_LIGHT"; override val displayName = "Light Level"; override val description = "Ambient light threshold"; override val category = "Sensors"; override val icon = "brightness_medium"; override val parameters = listOf(Parameter.NumberParameter("lux", "Lux", lux, 0, 50000))
    }
    data class StorageSizeTrigger(val sizeGB: Int = 10, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "STORAGE_SIZE"; override val displayName = "Storage Usage"; override val description = "Storage exceeds X GB"; override val category = "Resources"; override val icon = "storage"; override val parameters = listOf(Parameter.NumberParameter("sizeGB", "Size (GB)", sizeGB, 1, 1024), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("MORE_THAN", "LESS_THAN", "EQUALS")))
    }

    // --- 11. Security ---
    object FingerprintAuthenticated : Trigger() {
        override val type = "FINGERPRINT_AUTH"; override val displayName = "Fingerprint OK"; override val description = "Fingerprint recognized"; override val category = "Security"; override val icon = "fingerprint"; override val parameters = emptyList<Parameter<*>>()
    }
    object BiometricAuthFailed : Trigger() {
        override val type = "BIOMETRIC_FAILED"; override val displayName = "Biometric Failed"; override val description = "Auth attempt failed"; override val category = "Security"; override val icon = "error_outline"; override val parameters = emptyList<Parameter<*>>()
    }

    // --- 12. Web & Cloud ---
    data class WebsiteContentChanged(val url: String = "") : Trigger() {
        override val type = "WEB_CHANGED"; override val displayName = "Website Changed"; override val description = "Monitor URL for changes"; override val category = "Web"; override val icon = "public"; override val parameters = listOf(Parameter.TextParameter("url", "URL", url))
    }

    // --- 13. Audio ---
    data class MusicStateChanged(val state: String = "PLAYING") : Trigger() {
        override val type = "MUSIC_STATE_CHANGED"; override val displayName = "Music State"; override val description = "Music playing, paused, or stopped"; override val category = "Audio"; override val icon = "music_note"; override val parameters = listOf(Parameter.SelectionParameter("state", "State", state, listOf("PLAYING", "PAUSED", "STOPPED")))
    }
    data class AppPlayingAudio(val packageName: String = "") : Trigger() {
        override val type = "APP_PLAYING_AUDIO"; override val displayName = "App Playing Audio"; override val description = "Specific app is playing audio"; override val category = "Audio"; override val icon = "audiotrack"; override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
    data class VolumeChanged(val streamType: String = "MUSIC", val threshold: Int = 50, val comparison: String = "GREATER_THAN") : Trigger() {
        override val type = "VOLUME_CHANGED"; override val displayName = "Volume Changed"; override val description = "Volume level crossed threshold"; override val category = "Audio"; override val icon = "volume_up"; override val parameters = listOf(Parameter.SelectionParameter("streamType", "Stream", streamType, listOf("MUSIC", "RINGER", "ALARM", "NOTIFICATION")), Parameter.NumberParameter("threshold", "Threshold %", threshold, 0, 100), Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("LESS_THAN", "GREATER_THAN", "EQUALS")))
    }
    data class RingerModeChanged(val mode: String = "NORMAL") : Trigger() {
        override val type = "RINGER_MODE_CHANGED"; override val displayName = "Ringer Mode"; override val description = "Ringer mode changed"; override val category = "Audio"; override val icon = "notifications_active"; override val parameters = listOf(Parameter.SelectionParameter("mode", "Mode", mode, listOf("NORMAL", "VIBRATE", "SILENT")))
    }
    data class AudioDeviceConnected(val deviceType: String = "ANY") : Trigger() {
        override val type = "AUDIO_DEVICE_CONNECTED"; override val displayName = "Audio Device Connected"; override val description = "BT, wired, or speaker connected"; override val category = "Audio"; override val icon = "headset"; override val parameters = listOf(Parameter.SelectionParameter("deviceType", "Device Type", deviceType, listOf("ANY", "BLUETOOTH", "WIRED", "SPEAKER")))
    }
    object MicrophoneActivated : Trigger() {
        override val type = "MICROPHONE_ACTIVATED"; override val displayName = "Microphone Activated"; override val description = "An app started using the microphone"; override val category = "Audio"; override val icon = "mic"; override val parameters = emptyList<Parameter<*>>()
    }
    object MediaMetadataChanged : Trigger() {
        override val type = "MEDIA_METADATA_CHANGED"; override val displayName = "Song Changed"; override val description = "Media title or artist changed"; override val category = "Audio"; override val icon = "queue_music"; override val parameters = emptyList<Parameter<*>>()
    }

    object Unknown : Trigger() {
        override val type = "UNKNOWN"; override val displayName = "Unknown Trigger"; override val description = "Unrecognized trigger type"; override val category = "General"; override val icon = "help"; override val parameters = emptyList<Parameter<*>>()
    }
}

@JsonAdapter(AutomationTypeAdapter::class)
sealed class Condition : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val category: String
    abstract override val icon: String
    abstract override val parameters: List<Parameter<*>>

    object IsConnectedToWiFi : Condition() {
        override val type = "IS_WIFI_CONNECTED"; override val displayName = "WiFi Connected"; override val description = "Only if WiFi is active"; override val category = "Network"; override val icon = "wifi"; override val parameters = emptyList<Parameter<*>>()
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    object IsCharging : Condition() {
        override val type = "IS_CHARGING"; override val displayName = "Is Charging"; override val description = "Only if charging"; override val category = "Power"; override val icon = "battery_charging_full"; override val parameters = emptyList<Parameter<*>>()
    }
    object ScreenIsOn : Condition() {
        override val type = "SCREEN_IS_ON"; override val displayName = "Screen is On"; override val description = "Only if screen active"; override val category = "Display"; override val icon = "smartphone"; override val parameters = emptyList<Parameter<*>>()
    }
    object DeviceIsLocked : Condition() {
        override val type = "DEVICE_IS_LOCKED"; override val displayName = "Device is Locked"; override val description = "Only if locked"; override val category = "Security"; override val icon = "lock"; override val parameters = emptyList<Parameter<*>>()
    }
    data class WiFiSSIDIs(val ssid: String = "") : Condition() {
        override val type = "WIFI_SSID_IS"; override val displayName = "WiFi SSID is"; override val description = "Only if connected to SSID"; override val category = "Network"; override val icon = "wifi"; override val parameters = listOf(Parameter.TextParameter("ssid", "SSID", ssid))
        override val requiredPermissions = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    data class BatteryLevelBetween(val min: Int = 20, val max: Int = 80) : Condition() {
        override val type = "BATTERY_BETWEEN"; override val displayName = "Battery Level Between"; override val description = "Only if battery in range"; override val category = "Power"; override val icon = "battery_3_bar"; override val parameters = listOf(Parameter.NumberParameter("min", "Min %", min, 0, 100), Parameter.NumberParameter("max", "Max %", max, 0, 100))
    }
    data class StorageFreeAbove(val gb: Int = 5) : Condition() {
        override val type = "STORAGE_FREE_ABOVE"; override val displayName = "Free Storage Above"; override val description = "Only if free space > X GB"; override val category = "Resources"; override val icon = "cloud_done"; override val parameters = listOf(Parameter.NumberParameter("gb", "Free GB", gb, 1, 1024))
    }
    object Unknown : Condition() {
        override val type = "UNKNOWN"; override val displayName = "Unknown Condition"; override val description = "Unrecognized"; override val category = "General"; override val icon = "help"; override val parameters = emptyList<Parameter<*>>()
    }
}

@JsonAdapter(AutomationTypeAdapter::class)
sealed class Action : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val category: String
    abstract override val icon: String
    abstract override val parameters: List<Parameter<*>>

    data class Speak(val text: String = "Triggered") : Action() {
        override val type = "SPEAK_TTS"; override val displayName = "Speak (TTS)"; override val description = "Reads text aloud"; override val category = "Audio"; override val icon = "record_voice_over"; override val parameters = listOf(Parameter.TextParameter("text", "Text", text))
    }
    data class ShowNotification(val title: String = "RTS", val message: String = "") : Action() {
        override val type = "SHOW_NOTIFICATION"; override val displayName = "Notification"; override val description = "Shows system notification"; override val category = "General"; override val icon = "notifications"; override val parameters = listOf(Parameter.TextParameter("title", "Title", title), Parameter.TextParameter("message", "Message", message))
        override val requiredPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) listOf(android.Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    }
    data class SetVolume(val volume: Int = 50) : Action() {
        override val type = "SET_VOLUME"; override val displayName = "Set Volume"; override val description = "Sets media volume"; override val category = "Audio"; override val icon = "volume_up"; override val parameters = listOf(Parameter.NumberParameter("volume", "Volume %", volume, 0, 100))
    }
    object ToggleWiFi : Action() {
        override val type = "TOGGLE_WIFI"; override val displayName = "Toggle WiFi"; override val description = "Turns WiFi on/off"; override val category = "Network"; override val icon = "wifi"; override val parameters = emptyList<Parameter<*>>()
    }
    object RunDNSBenchmark : Action() {
        override val type = "RUN_DNS_BENCHMARK"; override val displayName = "DNS Benchmark"; override val description = "Find fastest DNS"; override val category = "Network"; override val icon = "speed"; override val parameters = emptyList<Parameter<*>>()
    }
    object RefreshDNS : Action() {
        override val type = "REFRESH_DNS"; override val displayName = "Refresh DNS"; override val description = "Clear DNS cache"; override val category = "Network"; override val icon = "refresh"; override val parameters = emptyList<Parameter<*>>()
    }
    data class Vibrate(val durationMs: Int = 500) : Action() {
        override val type = "VIBRATE"; override val displayName = "Vibrate"; override val description = "Vibrate device"; override val category = "General"; override val icon = "vibration"; override val parameters = listOf(Parameter.NumberParameter("duration", "Duration (ms)", durationMs, 100, 5000))
    }
    data class LaunchApp(val packageName: String = "") : Action() {
        override val type = "LAUNCH_APP"; override val displayName = "Launch App"; override val description = "Open specific app"; override val category = "Apps"; override val icon = "launch"; override val parameters = listOf(Parameter.AppParameter("packageName", "App", packageName))
    }
    data class ShowToast(val message: String = "") : Action() {
        override val type = "SHOW_TOAST"; override val displayName = "Toast Message"; override val description = "Short popup message"; override val category = "General"; override val icon = "message"; override val parameters = listOf(Parameter.TextParameter("message", "Message", message))
    }
    data class AutoClean(val categories: String = "cache,temp") : Action() {
        override val type = "AUTO_CLEAN"; override val displayName = "Auto Clean"; override val description = "Background cleanup"; override val category = "Resources"; override val icon = "cleaning_services"; override val parameters = listOf(Parameter.TextParameter("categories", "Categories (comma-sep)", categories))
    }
    data class RunBackup(val categories: String = "sms,calls,contacts,settings") : Action() {
        override val type = "RUN_BACKUP"; override val displayName = "Run Backup"; override val description = "Automated backup"; override val category = "Resources"; override val icon = "cloud_upload"; override val parameters = listOf(Parameter.TextParameter("categories", "Categories (comma-sep)", categories))
    }
    data class RunRestore(val archivePath: String = "", val categories: String = "all") : Action() {
        override val type = "RUN_RESTORE"; override val displayName = "Run Restore"; override val description = "Automated restore"; override val category = "Resources"; override val icon = "settings_backup_restore"; override val parameters = listOf(Parameter.TextParameter("archivePath", "Archive Path", archivePath), Parameter.TextParameter("categories", "Categories (comma-sep)", categories))
    }
    object ToggleFlashlight : Action() {
        override val type = "TOGGLE_FLASHLIGHT"; override val displayName = "Toggle Flashlight"; override val description = "Flash on/off"; override val category = "General"; override val icon = "flashlight_on"; override val parameters = emptyList<Parameter<*>>()
    }
    data class SetBrightness(val level: Int = 50) : Action() {
        override val type = "SET_BRIGHTNESS"; override val displayName = "Set Brightness"; override val description = "Adjust screen brightness"; override val category = "General"; override val icon = "brightness_6"; override val parameters = listOf(Parameter.NumberParameter("level", "Brightness %", level, 0, 100))
    }
    data class Delay(val seconds: Int = 5) : Action() {
        override val type = "DELAY"; override val displayName = "Delay"; override val description = "Wait before next action"; override val category = "General"; override val icon = "timer"; override val parameters = listOf(Parameter.NumberParameter("seconds", "Seconds", seconds, 1, 3600))
    }
    data class RunAdbCommand(val command: String = "") : Action() {
        override val type = "RUN_ADB_COMMAND"; override val displayName = "ADB Command"; override val description = "Run safe shell command"; override val category = "Advanced"; override val icon = "terminal"; override val parameters = listOf(Parameter.TextParameter("command", "Command", command))
    }
    object Unknown : Action() {
        override val type = "UNKNOWN"; override val displayName = "Unknown Action"; override val description = "Unrecognized"; override val category = "General"; override val icon = "help"; override val parameters = emptyList<Parameter<*>>()
    }
}

@JsonAdapter(ParameterAdapter::class)
sealed class Parameter<T>(
    val key: String,
    val label: String,
    val value: T,
    val hint: String? = null
) {
    class TextParameter(key: String, label: String, value: String?, hint: String? = null) : Parameter<String?>(key, label, value, hint)
    class NumberParameter(key: String, label: String, value: Int, val min: Int, val max: Int) : Parameter<Int>(key, label, value)
    class SelectionParameter(key: String, label: String, value: String, val options: List<String>) : Parameter<String>(key, label, value)
    class TimeParameter(key: String, label: String, value: String) : Parameter<String>(key, label, value)
    class AppParameter(key: String, label: String, value: String?) : Parameter<String?>(key, label, value)
    class BluetoothParameter(key: String, label: String, value: String?) : Parameter<String?>(key, label, value)
}
