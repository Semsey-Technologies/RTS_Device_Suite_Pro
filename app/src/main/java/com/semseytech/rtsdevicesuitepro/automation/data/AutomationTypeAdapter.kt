package com.semseytech.rtsdevicesuitepro.automation.data

import com.google.gson.*
import com.semseytech.rtsdevicesuitepro.automation.models.*
import java.lang.reflect.Type

class AutomationTypeAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T> {
    private val baseGson = GsonBuilder()
        .registerTypeAdapter(Parameter::class.java, ParameterAdapter())
        .create()

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonElement = baseGson.toJsonTree(src)
        if (jsonElement.isJsonObject) {
            val jsonObject = jsonElement.asJsonObject
            if (src is AutomationComponent) {
                jsonObject.addProperty("type", src.type)
            }
            return jsonObject
        }
        return jsonElement
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString
        
        // If type is missing, we can't determine the subclass if typeOfT is a base class.
        // If typeOfT is a concrete class, we should use baseGson to deserialize it.
        if (type == null) {
            val typeName = when (typeOfT) {
                Trigger::class.java -> "Trigger"
                Condition::class.java -> "Condition"
                Action::class.java -> "Action"
                else -> typeOfT.toString()
            }
            
            // If it's a base class, try to return an Unknown variant instead of throwing.
            if (typeOfT == Trigger::class.java) return Trigger.Unknown as T
            if (typeOfT == Condition::class.java) return Condition.Unknown as T
            if (typeOfT == Action::class.java) return Action.Unknown as T
            
            // If it's a concrete class, baseGson might be able to handle it.
            return baseGson.fromJson(json, typeOfT)
        }
        
        return try {
            when {
                isTrigger(type) -> deserializeTrigger(type, json) as T
                isCondition(type) -> deserializeCondition(type, json) as T
                isAction(type) -> deserializeAction(type, json) as T
                else -> {
                    // Fallback for unknown types
                    when (typeOfT) {
                        Trigger::class.java -> Trigger.Unknown as T
                        Condition::class.java -> Condition.Unknown as T
                        Action::class.java -> Action.Unknown as T
                        else -> throw JsonParseException("Unknown component type: $type for $typeOfT")
                    }
                }
            }
        } catch (e: Exception) {
            if (e is JsonParseException) throw e
            throw JsonParseException("Failed to deserialize $type", e)
        }
    }

    private fun isTrigger(type: String) = type in listOf(
        "GSHEET_EDITED", "GSHEET_MENTIONED", "GSHEET_COMMENT_ADDED", "GSHEET_COMMENT_RESOLVED", "GSHEET_ACCESS_REQUEST", "GSHEET_SHARED",
        "FILE_GSHEET_CREATED", "FILE_CSV_EXPORTED", "FILE_XLSX_EXPORTED", "FILE_CREATED", "FILE_DELETED", "FILE_MODIFIED", "FOLDER_CHANGED",
        "STORAGE_MOUNTED", "STORAGE_UNMOUNTED", "DOWNLOAD_COMPLETED",
        "WIFI_CONNECTED", "WIFI_DISCONNECTED", "SPECIFIC_WIFI_CONNECTED", "WIFI_SIGNAL_THRESHOLD",
        "MOBILE_DATA_ON", "MOBILE_DATA_OFF", "MOBILE_DATA_TYPE_CHANGED",
        "POWER_CONNECTED", "POWER_DISCONNECTED", "BATTERY_LEVEL_ABOVE", "BATTERY_LEVEL_BELOW",
        "BATTERY_TEMP_ABOVE", "BATTERY_TEMP_BELOW", "BATTERY_FAST_CHARGING", "BATTERY_WIRELESS_CHARGING",
        "POWER_SAVE_ON", "POWER_SAVE_OFF", "HOTSPOT_ON", "HOTSPOT_OFF", "VPN_CONNECTED", "VPN_DISCONNECTED",
        "IP_ADDRESS_CHANGED", "NETWORK_AVAILABLE", "NETWORK_LOST", "BT_DEVICE_CONNECTED", "BT_DEVICE_DISCONNECTED",
        "PING_STATUS", "DOMAIN_REACHABILITY", "NETWORK_SPEED_THRESHOLD",
        "SCREEN_ON", "SCREEN_OFF", "SCREEN_UNLOCKED", "SCREEN_LOCKED", "BOOT_COMPLETED", "SHUTDOWN_INITIATED", "REBOOT_REQUESTED",
        "DEVICE_IDLE_ENTERED", "DEVICE_IDLE", "DOZE_MODE_ENTERED", "DOZE_MODE_EXITED",
        "AIRPLANE_MODE_ON", "AIRPLANE_MODE_OFF", "NFC_SCANNED", "USB_CONNECTED", "USB_DISCONNECTED",
        "HEADPHONES_PLUGGED", "HEADPHONES_UNPLUGGED", "ORIENTATION_CHANGED", "ROTATION_LOCKED", "ROTATION_UNLOCKED",
        "CLIPBOARD_CHANGED", "SCREENSHOT_TAKEN", "VOLUME_BUTTON_PRESSED", "HARDWARE_BUTTON_LONG_PRESS",
        "FINGERPRINT_AUTH", "BIOMETRIC_FAILED", "BLUETOOTH_ON", "BLUETOOTH_OFF", "TIME_OF_DAY", "TIME_RANGE", "DAYS_OF_WEEK",
        "DAY_OF_MONTH", "MONTH_OF_YEAR", "RECURRING", "INTERVAL", "SUNRISE", "SUNSET", "GOLDEN_HOUR",
        "TIMER_FINISHED", "STOPWATCH_THRESHOLD", "COUNTDOWN_REACHED", "CRON_SCHEDULE",
        "SENSOR_LIGHT", "STORAGE_SIZE", "APP_OPENED", "APP_CLOSED",
        "APP_INSTALLED", "APP_UNINSTALLED", "APP_UPDATED", "APP_CRASHED", "FOREGROUND_APP_CHANGED",
        "NOTIFICATION_POSTED", "NOTIFICATION_REMOVED", "NOTIFICATION_MATCHES", "SYSTEM_DIALOG_OPENED",
        "KEYBOARD_STATE_CHANGED", "ACCESSIBILITY_EVENT", "TOAST_DETECTED", "OVERLAY_PERMISSION_CHANGED",
        "WEB_CHANGED",
        "MUSIC_STATE_CHANGED", "APP_PLAYING_AUDIO", "VOLUME_CHANGED", "RINGER_MODE_CHANGED", "AUDIO_DEVICE_CONNECTED",
        "MICROPHONE_ACTIVATED", "MEDIA_METADATA_CHANGED",
        "GEOFENCE_ENTER", "GEOFENCE_EXIT", "ARRIVE_AT_PLACE", "LEAVE_PLACE", "GPS_STATE_CHANGED", "SPEED_THRESHOLD",
        "ACTIVITY_DETECTED", "SIGNIFICANT_LOCATION_CHANGE", "COMPASS_HEADING", "ALTITUDE_THRESHOLD",
        "STEP_COUNT_THRESHOLD", "PROXIMITY_TRIGGERED", "ACCELEROMETER_PATTERN", "GYROSCOPE_PATTERN",
        "SENSOR_MAGNETIC", "SENSOR_PRESSURE", "SENSOR_TEMP", "SENSOR_HUMIDITY", "SENSOR_HEART_RATE",
        "SENSOR_NOISE", "TOUCH_GESTURE",
        "ACTIVITY_WALKING", "ACTIVITY_DRIVING", "SMS_RECEIVED", "SMS_FROM_CONTACT", "MMS_RECEIVED",
        "CALL_INCOMING", "CALL_ANSWERED", "CALL_ENDED", "CALL_MISSED", "VOICEMAIL_RECEIVED",
        "EMAIL_RECEIVED", "MESSAGING_APP_NOTIFICATION", "CONTACT_STATUS_CHANGED", "NOTIFICATION_KEYWORD"
    )

    private fun isCondition(type: String) = type in listOf(
        "IS_WIFI_CONNECTED", "WIFI_SSID_IS", "BATTERY_BETWEEN", "IS_CHARGING",
        "SCREEN_IS_ON", "DEVICE_IS_LOCKED", "STORAGE_FREE_ABOVE"
    )

    private fun isAction(type: String) = type in listOf(
        "RUN_DNS_BENCHMARK", "REFRESH_DNS", "TOGGLE_WIFI", "SET_VOLUME", "SPEAK_TTS", "VIBRATE",
        "LAUNCH_APP", "SHOW_NOTIFICATION", "SHOW_TOAST", "AUTO_CLEAN", "RUN_BACKUP", "RUN_RESTORE",
        "TOGGLE_FLASHLIGHT", "SET_BRIGHTNESS", "DELAY", "RUN_ADB_COMMAND"
    )

    private fun deserializeTrigger(type: String, json: JsonElement): Trigger {
        return when (type) {
            "GSHEET_EDITED" -> Trigger.SheetEdited
            "GSHEET_MENTIONED" -> Trigger.MentionedInComment
            "GSHEET_COMMENT_ADDED" -> Trigger.CommentAdded
            "GSHEET_COMMENT_RESOLVED" -> Trigger.CommentResolved
            "GSHEET_ACCESS_REQUEST" -> Trigger.AccessRequestReceived
            "GSHEET_SHARED" -> Trigger.SpreadsheetShared
            "FILE_GSHEET_CREATED" -> Trigger.GSheetFileCreated
            "FILE_CSV_EXPORTED" -> Trigger.CsvExported
            "FILE_XLSX_EXPORTED" -> Trigger.XlsxExported
            "FILE_CREATED" -> baseGson.fromJson(json, Trigger.FileCreated::class.java)
            "FILE_DELETED" -> baseGson.fromJson(json, Trigger.FileDeleted::class.java)
            "FILE_MODIFIED" -> baseGson.fromJson(json, Trigger.FileModified::class.java)
            "FOLDER_CHANGED" -> baseGson.fromJson(json, Trigger.FolderChanged::class.java)
            "STORAGE_MOUNTED" -> Trigger.ExternalStorageMounted
            "STORAGE_UNMOUNTED" -> Trigger.ExternalStorageUnmounted
            "DOWNLOAD_COMPLETED" -> Trigger.DownloadCompleted
            "WIFI_CONNECTED" -> Trigger.WiFiConnected
            "WIFI_DISCONNECTED" -> Trigger.WiFiDisconnected
            "SPECIFIC_WIFI_CONNECTED" -> baseGson.fromJson(json, Trigger.SpecificWiFiConnected::class.java)
            "WIFI_SIGNAL_THRESHOLD" -> baseGson.fromJson(json, Trigger.WiFiSignalStrength::class.java)
            "MOBILE_DATA_ON" -> Trigger.MobileDataOn
            "MOBILE_DATA_OFF" -> Trigger.MobileDataOff
            "MOBILE_DATA_TYPE_CHANGED" -> baseGson.fromJson(json, Trigger.MobileDataTypeChanged::class.java)
            "POWER_CONNECTED" -> Trigger.PowerConnected
            "POWER_DISCONNECTED" -> Trigger.PowerDisconnected
            "BATTERY_LEVEL_ABOVE" -> baseGson.fromJson(json, Trigger.BatteryLevelAbove::class.java)
            "BATTERY_LEVEL_BELOW" -> baseGson.fromJson(json, Trigger.BatteryLevelBelow::class.java)
            "BATTERY_TEMP_ABOVE" -> baseGson.fromJson(json, Trigger.BatteryTempAbove::class.java)
            "BATTERY_TEMP_BELOW" -> baseGson.fromJson(json, Trigger.BatteryTempBelow::class.java)
            "BATTERY_FAST_CHARGING" -> Trigger.BatteryFastCharging
            "BATTERY_WIRELESS_CHARGING" -> Trigger.BatteryWirelessCharging
            "POWER_SAVE_ON" -> Trigger.PowerSaveModeOn
            "POWER_SAVE_OFF" -> Trigger.PowerSaveModeOff
            "HOTSPOT_ON" -> Trigger.HotspotOn
            "HOTSPOT_OFF" -> Trigger.HotspotOff
            "VPN_CONNECTED" -> Trigger.VpnConnected
            "VPN_DISCONNECTED" -> Trigger.VpnDisconnected
            "IP_ADDRESS_CHANGED" -> Trigger.IpAddressChanged
            "NETWORK_AVAILABLE" -> Trigger.NetworkAvailable
            "NETWORK_LOST" -> Trigger.NetworkLost
            "BT_DEVICE_CONNECTED" -> baseGson.fromJson(json, Trigger.BluetoothDeviceConnected::class.java)
            "BT_DEVICE_DISCONNECTED" -> baseGson.fromJson(json, Trigger.BluetoothDeviceDisconnected::class.java)
            "PING_STATUS" -> baseGson.fromJson(json, Trigger.PingStatus::class.java)
            "DOMAIN_REACHABILITY" -> baseGson.fromJson(json, Trigger.DomainReachability::class.java)
            "NETWORK_SPEED_THRESHOLD" -> baseGson.fromJson(json, Trigger.NetworkSpeedThreshold::class.java)
            "SCREEN_ON" -> Trigger.ScreenOn
            "SCREEN_OFF" -> Trigger.ScreenOff
            "SCREEN_UNLOCKED" -> Trigger.ScreenUnlocked
            "SCREEN_LOCKED" -> Trigger.ScreenLocked
            "BOOT_COMPLETED" -> Trigger.BootCompleted
            "SHUTDOWN_INITIATED" -> Trigger.ShutdownInitiated
            "REBOOT_REQUESTED" -> Trigger.RebootRequested
            "DEVICE_IDLE_ENTERED" -> Trigger.DeviceIdleEntered
            "DEVICE_IDLE" -> Trigger.DeviceIdle
            "DOZE_MODE_ENTERED" -> Trigger.DozeModeEntered
            "DOZE_MODE_EXITED" -> Trigger.DozeModeExited
            "AIRPLANE_MODE_ON" -> Trigger.AirplaneModeOn
            "AIRPLANE_MODE_OFF" -> Trigger.AirplaneModeOff
            "NFC_SCANNED" -> Trigger.NfcTagScanned
            "USB_CONNECTED" -> Trigger.UsbConnected
            "USB_DISCONNECTED" -> Trigger.UsbDisconnected
            "HEADPHONES_PLUGGED" -> Trigger.HeadphonesPlugged
            "HEADPHONES_UNPLUGGED" -> Trigger.HeadphonesUnplugged
            "ORIENTATION_CHANGED" -> Trigger.OrientationChanged
            "ROTATION_LOCKED" -> Trigger.RotationLocked
            "ROTATION_UNLOCKED" -> Trigger.RotationUnlocked
            "CLIPBOARD_CHANGED" -> Trigger.ClipboardChanged
            "SCREENSHOT_TAKEN" -> Trigger.ScreenshotTaken
            "VOLUME_BUTTON_PRESSED" -> Trigger.VolumeButtonPressed
            "HARDWARE_BUTTON_LONG_PRESS" -> Trigger.HardwareButtonLongPress
            "FINGERPRINT_AUTH" -> Trigger.FingerprintAuthenticated
            "BIOMETRIC_FAILED" -> Trigger.BiometricAuthFailed
            "BLUETOOTH_ON" -> Trigger.BluetoothOn
            "BLUETOOTH_OFF" -> Trigger.BluetoothOff
            "TIME_OF_DAY" -> baseGson.fromJson(json, Trigger.TimeOfDay::class.java)
            "TIME_RANGE" -> baseGson.fromJson(json, Trigger.TimeRange::class.java)
            "DAYS_OF_WEEK" -> baseGson.fromJson(json, Trigger.DaysOfWeek::class.java)
            "DAY_OF_MONTH" -> baseGson.fromJson(json, Trigger.DayOfMonth::class.java)
            "MONTH_OF_YEAR" -> baseGson.fromJson(json, Trigger.MonthOfYear::class.java)
            "RECURRING" -> baseGson.fromJson(json, Trigger.RecurringTrigger::class.java)
            "INTERVAL" -> baseGson.fromJson(json, Trigger.IntervalTrigger::class.java)
            "SUNRISE" -> Trigger.Sunrise
            "SUNSET" -> Trigger.Sunset
            "GOLDEN_HOUR" -> baseGson.fromJson(json, Trigger.GoldenHour::class.java)
            "TIMER_FINISHED" -> baseGson.fromJson(json, Trigger.TimerFinished::class.java)
            "STOPWATCH_THRESHOLD" -> baseGson.fromJson(json, Trigger.StopwatchThreshold::class.java)
            "COUNTDOWN_REACHED" -> baseGson.fromJson(json, Trigger.CountdownReached::class.java)
            "CRON_SCHEDULE" -> baseGson.fromJson(json, Trigger.CronSchedule::class.java)
            "SENSOR_LIGHT" -> baseGson.fromJson(json, Trigger.LightSensorThreshold::class.java)
            "STORAGE_SIZE" -> baseGson.fromJson(json, Trigger.StorageSizeTrigger::class.java)
            "APP_OPENED" -> baseGson.fromJson(json, Trigger.AppOpened::class.java)
            "APP_CLOSED" -> baseGson.fromJson(json, Trigger.AppClosed::class.java)
            "APP_INSTALLED" -> baseGson.fromJson(json, Trigger.AppInstalled::class.java)
            "APP_UNINSTALLED" -> baseGson.fromJson(json, Trigger.AppUninstalled::class.java)
            "APP_UPDATED" -> baseGson.fromJson(json, Trigger.AppUpdated::class.java)
            "APP_CRASHED" -> baseGson.fromJson(json, Trigger.AppCrashed::class.java)
            "FOREGROUND_APP_CHANGED" -> Trigger.ForegroundAppChanged
            "NOTIFICATION_POSTED" -> baseGson.fromJson(json, Trigger.NotificationPosted::class.java)
            "NOTIFICATION_REMOVED" -> baseGson.fromJson(json, Trigger.NotificationRemoved::class.java)
            "NOTIFICATION_MATCHES" -> baseGson.fromJson(json, Trigger.NotificationMatches::class.java)
            "SYSTEM_DIALOG_OPENED" -> Trigger.SystemDialogOpened
            "KEYBOARD_STATE_CHANGED" -> baseGson.fromJson(json, Trigger.KeyboardStateChanged::class.java)
            "ACCESSIBILITY_EVENT" -> baseGson.fromJson(json, Trigger.AccessibilityEventDetected::class.java)
            "TOAST_DETECTED" -> baseGson.fromJson(json, Trigger.ToastDetected::class.java)
            "OVERLAY_PERMISSION_CHANGED" -> baseGson.fromJson(json, Trigger.OverlayPermissionChanged::class.java)
            "WEB_CHANGED" -> baseGson.fromJson(json, Trigger.WebsiteContentChanged::class.java)
            "MUSIC_STATE_CHANGED" -> baseGson.fromJson(json, Trigger.MusicStateChanged::class.java)
            "APP_PLAYING_AUDIO" -> baseGson.fromJson(json, Trigger.AppPlayingAudio::class.java)
            "VOLUME_CHANGED" -> baseGson.fromJson(json, Trigger.VolumeChanged::class.java)
            "RINGER_MODE_CHANGED" -> baseGson.fromJson(json, Trigger.RingerModeChanged::class.java)
            "AUDIO_DEVICE_CONNECTED" -> baseGson.fromJson(json, Trigger.AudioDeviceConnected::class.java)
            "MICROPHONE_ACTIVATED" -> Trigger.MicrophoneActivated
            "MEDIA_METADATA_CHANGED" -> Trigger.MediaMetadataChanged
            "GEOFENCE_ENTER" -> baseGson.fromJson(json, Trigger.GeofenceEnter::class.java)
            "GEOFENCE_EXIT" -> baseGson.fromJson(json, Trigger.GeofenceExit::class.java)
            "ARRIVE_AT_PLACE" -> baseGson.fromJson(json, Trigger.ArriveAtPlace::class.java)
            "LEAVE_PLACE" -> baseGson.fromJson(json, Trigger.LeavePlace::class.java)
            "GPS_STATE_CHANGED" -> baseGson.fromJson(json, Trigger.GpsStateChanged::class.java)
            "SPEED_THRESHOLD" -> baseGson.fromJson(json, Trigger.SpeedThreshold::class.java)
            "ACTIVITY_DETECTED" -> baseGson.fromJson(json, Trigger.ActivityDetected::class.java)
            "SIGNIFICANT_LOCATION_CHANGE" -> Trigger.SignificantLocationChange
            "COMPASS_HEADING" -> baseGson.fromJson(json, Trigger.CompassHeading::class.java)
            "ALTITUDE_THRESHOLD" -> baseGson.fromJson(json, Trigger.AltitudeThreshold::class.java)
            "STEP_COUNT_THRESHOLD" -> baseGson.fromJson(json, Trigger.StepCountThreshold::class.java)
            "PROXIMITY_TRIGGERED" -> baseGson.fromJson(json, Trigger.ProximityTriggered::class.java)
            "ACCELEROMETER_PATTERN" -> baseGson.fromJson(json, Trigger.AccelerometerPattern::class.java)
            "GYROSCOPE_PATTERN" -> baseGson.fromJson(json, Trigger.GyroscopePattern::class.java)
            "SENSOR_MAGNETIC" -> baseGson.fromJson(json, Trigger.MagnetometerThreshold::class.java)
            "SENSOR_PRESSURE" -> baseGson.fromJson(json, Trigger.BarometerThreshold::class.java)
            "SENSOR_TEMP" -> baseGson.fromJson(json, Trigger.TemperatureThreshold::class.java)
            "SENSOR_HUMIDITY" -> baseGson.fromJson(json, Trigger.HumidityThreshold::class.java)
            "SENSOR_HEART_RATE" -> baseGson.fromJson(json, Trigger.HeartRateThreshold::class.java)
            "SENSOR_NOISE" -> baseGson.fromJson(json, Trigger.AmbientNoiseThreshold::class.java)
            "TOUCH_GESTURE" -> baseGson.fromJson(json, Trigger.TouchGesturePattern::class.java)
            "ACTIVITY_WALKING" -> Trigger.ActivityWalking
            "ACTIVITY_DRIVING" -> Trigger.ActivityDriving
            "SMS_RECEIVED" -> Trigger.SmsReceived
            "SMS_FROM_CONTACT" -> baseGson.fromJson(json, Trigger.SmsFromContact::class.java)
            "MMS_RECEIVED" -> Trigger.MmsReceived
            "CALL_INCOMING" -> Trigger.IncomingCall
            "CALL_ANSWERED" -> Trigger.CallAnswered
            "CALL_ENDED" -> Trigger.CallEnded
            "CALL_MISSED" -> Trigger.MissedCall
            "VOICEMAIL_RECEIVED" -> Trigger.VoicemailReceived
            "EMAIL_RECEIVED" -> baseGson.fromJson(json, Trigger.EmailReceived::class.java)
            "MESSAGING_APP_NOTIFICATION" -> baseGson.fromJson(json, Trigger.MessagingAppNotification::class.java)
            "CONTACT_STATUS_CHANGED" -> baseGson.fromJson(json, Trigger.ContactStatusChanged::class.java)
            "NOTIFICATION_KEYWORD" -> baseGson.fromJson(json, Trigger.NotificationKeyword::class.java)
            else -> throw JsonParseException("Unknown trigger: $type")
        }
    }

    private fun deserializeCondition(type: String, json: JsonElement): Condition {
        return when (type) {
            "IS_WIFI_CONNECTED" -> Condition.IsConnectedToWiFi
            "WIFI_SSID_IS" -> baseGson.fromJson(json, Condition.WiFiSSIDIs::class.java)
            "BATTERY_BETWEEN" -> baseGson.fromJson(json, Condition.BatteryLevelBetween::class.java)
            "IS_CHARGING" -> Condition.IsCharging
            "SCREEN_IS_ON" -> Condition.ScreenIsOn
            "DEVICE_IS_LOCKED" -> Condition.DeviceIsLocked
            "STORAGE_FREE_ABOVE" -> baseGson.fromJson(json, Condition.StorageFreeAbove::class.java)
            else -> throw JsonParseException("Unknown condition: $type")
        }
    }

    private fun deserializeAction(type: String, json: JsonElement): Action {
        return when (type) {
            "RUN_DNS_BENCHMARK" -> Action.RunDNSBenchmark
            "REFRESH_DNS" -> Action.RefreshDNS
            "TOGGLE_WIFI" -> Action.ToggleWiFi
            "SET_VOLUME" -> baseGson.fromJson(json, Action.SetVolume::class.java)
            "SPEAK_TTS" -> baseGson.fromJson(json, Action.Speak::class.java)
            "VIBRATE" -> baseGson.fromJson(json, Action.Vibrate::class.java)
            "LAUNCH_APP" -> baseGson.fromJson(json, Action.LaunchApp::class.java)
            "SHOW_NOTIFICATION" -> baseGson.fromJson(json, Action.ShowNotification::class.java)
            "SHOW_TOAST" -> baseGson.fromJson(json, Action.ShowToast::class.java)
            "AUTO_CLEAN" -> baseGson.fromJson(json, Action.AutoClean::class.java)
            "RUN_BACKUP" -> baseGson.fromJson(json, Action.RunBackup::class.java)
            "RUN_RESTORE" -> baseGson.fromJson(json, Action.RunRestore::class.java)
            "TOGGLE_FLASHLIGHT" -> Action.ToggleFlashlight
            "SET_BRIGHTNESS" -> baseGson.fromJson(json, Action.SetBrightness::class.java)
            "DELAY" -> baseGson.fromJson(json, Action.Delay::class.java)
            "RUN_ADB_COMMAND" -> baseGson.fromJson(json, Action.RunAdbCommand::class.java)
            else -> throw JsonParseException("Unknown action: $type")
        }
    }
}
