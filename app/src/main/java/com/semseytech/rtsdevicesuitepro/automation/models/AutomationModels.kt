package com.semseytech.rtsdevicesuitepro.automation.models

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
    val parameters: List<Parameter<*>>
}

sealed class Trigger : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val parameters: List<Parameter<*>>

    // --- Network Triggers ---
    object WiFiConnected : Trigger() {
        override val type = "WIFI_CONNECTED"
        override val displayName = "WiFi Connected"
        override val description = "Runs when connected to any WiFi"
        override val parameters = emptyList<Parameter<*>>()
    }

    object WiFiDisconnected : Trigger() {
        override val type = "WIFI_DISCONNECTED"
        override val displayName = "WiFi Disconnected"
        override val description = "Runs when WiFi is lost"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class SpecificWiFiConnected(val ssid: String = "") : Trigger() {
        override val type = "SPECIFIC_WIFI_CONNECTED"
        override val displayName = "Connected to Specific SSID"
        override val description = "Runs when connecting to a specific WiFi name"
        override val parameters = listOf(Parameter.TextParameter("ssid", "SSID", ssid))
    }

    data class WiFiSignalLow(val threshold: Int = -70) : Trigger() {
        override val type = "WIFI_SIGNAL_LOW"
        override val displayName = "WiFi Signal Low"
        override val description = "Runs when signal drops below X dBm"
        override val parameters = listOf(Parameter.NumberParameter("threshold", "Threshold (dBm)", threshold, -100, -30))
    }

    object MobileDataActive : Trigger() {
        override val type = "MOBILE_DATA_ACTIVE"
        override val displayName = "Mobile Data Active"
        override val description = "Runs when mobile data becomes the active network"
        override val parameters = emptyList<Parameter<*>>()
    }

    // --- Power & Battery Triggers ---
    object PowerConnected : Trigger() {
        override val type = "POWER_CONNECTED"
        override val displayName = "Power Connected"
        override val description = "Runs when charger is plugged in"
        override val parameters = emptyList<Parameter<*>>()
    }

    object PowerDisconnected : Trigger() {
        override val type = "POWER_DISCONNECTED"
        override val displayName = "Power Disconnected"
        override val description = "Runs when charger is removed"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class BatteryLevelAbove(val level: Int = 80) : Trigger() {
        override val type = "BATTERY_LEVEL_ABOVE"
        override val displayName = "Battery Level Above"
        override val description = "Runs when battery rises above X%"
        override val parameters = listOf(Parameter.NumberParameter("level", "Level (%)", level, 0, 100))
    }

    data class BatteryLevelBelow(val level: Int = 20) : Trigger() {
        override val type = "BATTERY_LEVEL_BELOW"
        override val displayName = "Battery Level Below"
        override val description = "Runs when battery drops below X%"
        override val parameters = listOf(Parameter.NumberParameter("level", "Level (%)", level, 0, 100))
    }

    // --- Device State Triggers ---
    object ScreenOn : Trigger() {
        override val type = "SCREEN_ON"
        override val displayName = "Screen On"
        override val description = "Runs when screen turns on"
        override val parameters = emptyList<Parameter<*>>()
    }

    object ScreenOff : Trigger() {
        override val type = "SCREEN_OFF"
        override val displayName = "Screen Off"
        override val description = "Runs when screen turns off"
        override val parameters = emptyList<Parameter<*>>()
    }

    object ScreenUnlocked : Trigger() {
        override val type = "SCREEN_UNLOCKED"
        override val displayName = "Screen Unlocked"
        override val description = "Runs when user unlocks the device"
        override val parameters = emptyList<Parameter<*>>()
    }

    object DeviceBootCompleted : Trigger() {
        override val type = "BOOT_COMPLETED"
        override val displayName = "Device Boot Completed"
        override val description = "Runs when the phone finishes starting up"
        override val parameters = emptyList<Parameter<*>>()
    }

    // --- Bluetooth Triggers ---
    object BluetoothOn : Trigger() {
        override val type = "BLUETOOTH_ON"
        override val displayName = "Bluetooth On"
        override val description = "Runs when Bluetooth is enabled"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class BluetoothDeviceConnected(val address: String = "") : Trigger() {
        override val type = "BT_DEVICE_CONNECTED"
        override val displayName = "BT Device Connected"
        override val description = "Runs when a specific device connects"
        override val parameters = listOf(Parameter.BluetoothParameter("address", "Select Device", address))
    }

    // --- Time Triggers ---
    data class TimeOfDay(val hour: Int = 12, val minute: Int = 0) : Trigger() {
        override val type = "TIME_OF_DAY"
        override val displayName = "Time of Day"
        override val description = "Runs at a specific time daily"
        override val parameters = listOf(Parameter.TimeParameter("time", "Time", String.format("%02d:%02d", hour, minute)))
    }

    object DeviceIdle : Trigger() {
        override val type = "DEVICE_IDLE"
        override val displayName = "Device Idle"
        override val description = "Runs when the device is not in use"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class DaysOfWeek(val days: List<Int> = emptyList()) : Trigger() {
        override val type = "DAYS_OF_WEEK"
        override val displayName = "Days of Week"
        override val description = "Runs on selected days"
        override val parameters = listOf(Parameter.SelectionParameter("days", "Select Days", days.joinToString(","), listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")))
    }

    data class RecurringTrigger(val frequency: String = "WEEKLY") : Trigger() {
        override val type = "RECURRING"
        override val displayName = "Recurring"
        override val description = "Runs at a specific frequency"
        override val parameters = listOf(Parameter.SelectionParameter("frequency", "Frequency", frequency, listOf("WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "YEARLY")))
    }

    data class IntervalTrigger(val everyXDays: Int = 1) : Trigger() {
        override val type = "INTERVAL"
        override val displayName = "Every X Days"
        override val description = "Runs every X days"
        override val parameters = listOf(Parameter.NumberParameter("interval", "Days", everyXDays, 1, 365))
    }

    data class StorageSizeTrigger(val sizeGB: Int = 10, val comparison: String = "MORE_THAN") : Trigger() {
        override val type = "STORAGE_SIZE"
        override val displayName = "Storage Size"
        override val description = "Runs based on storage usage"
        override val parameters = listOf(
            Parameter.NumberParameter("size", "Size (GB)", sizeGB, 1, 1024),
            Parameter.SelectionParameter("comparison", "Comparison", comparison, listOf("EQUALS", "MORE_THAN", "LESS_THAN"))
        )
    }

    // --- App Triggers ---
    data class AppOpened(val packageName: String = "") : Trigger() {
        override val type = "APP_OPENED"
        override val displayName = "App Opened"
        override val description = "Runs when a specific app is launched"
        override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }
}

sealed class Condition : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val parameters: List<Parameter<*>>

    // --- Network Conditions ---
    object IsConnectedToWiFi : Condition() {
        override val type = "IS_WIFI_CONNECTED"
        override val displayName = "Is Connected to WiFi"
        override val description = "Only if WiFi is currently connected"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class WiFiSSIDIs(val ssid: String = "") : Condition() {
        override val type = "WIFI_SSID_IS"
        override val displayName = "WiFi SSID is"
        override val description = "Only if connected to a specific WiFi name"
        override val parameters = listOf(Parameter.TextParameter("ssid", "SSID", ssid))
    }

    // --- Battery Conditions ---
    data class BatteryLevelBetween(val min: Int = 20, val max: Int = 80) : Condition() {
        override val type = "BATTERY_BETWEEN"
        override val displayName = "Battery Level Between"
        override val description = "Only if battery is within X and Y range"
        override val parameters = listOf(
            Parameter.NumberParameter("min", "Min (%)", min, 0, 100),
            Parameter.NumberParameter("max", "Max (%)", max, 0, 100)
        )
    }

    object IsCharging : Condition() {
        override val type = "IS_CHARGING"
        override val displayName = "Is Charging"
        override val description = "Only if device is plugged in"
        override val parameters = emptyList<Parameter<*>>()
    }

    // --- Device Conditions ---
    object ScreenIsOn : Condition() {
        override val type = "SCREEN_IS_ON"
        override val displayName = "Screen is On"
        override val description = "Only if screen is currently active"
        override val parameters = emptyList<Parameter<*>>()
    }

    object DeviceIsLocked : Condition() {
        override val type = "DEVICE_IS_LOCKED"
        override val displayName = "Device is Locked"
        override val description = "Only if the device is currently locked"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class StorageFreeAbove(val gb: Int = 5) : Condition() {
        override val type = "STORAGE_FREE_ABOVE"
        override val displayName = "Storage Free Above"
        override val description = "Only if free storage is more than X GB"
        override val parameters = listOf(Parameter.NumberParameter("gb", "Threshold (GB)", gb, 1, 512))
    }
}

sealed class Action : AutomationComponent {
    abstract override val type: String
    abstract override val displayName: String
    abstract override val description: String
    abstract override val parameters: List<Parameter<*>>

    // --- Network Actions ---
    object RunDNSBenchmark : Action() {
        override val type = "RUN_DNS_BENCHMARK"
        override val displayName = "Run DNS Benchmark"
        override val description = "Finds the fastest DNS for your network"
        override val parameters = emptyList<Parameter<*>>()
    }

    object RefreshDNS : Action() {
        override val type = "REFRESH_DNS"
        override val displayName = "Refresh DNS"
        override val description = "Updates system DNS settings"
        override val parameters = emptyList<Parameter<*>>()
    }

    object ToggleWiFi : Action() {
        override val type = "TOGGLE_WIFI"
        override val displayName = "Toggle WiFi"
        override val description = "Turns WiFi on if off, or off if on"
        override val parameters = emptyList<Parameter<*>>()
    }

    // --- Audio Actions ---
    data class SetVolume(val volume: Int = 50) : Action() {
        override val type = "SET_VOLUME"
        override val displayName = "Set Volume"
        override val description = "Sets media volume level"
        override val parameters = listOf(Parameter.NumberParameter("volume", "Volume (%)", volume, 0, 100))
    }

    data class Speak(val text: String = "Automation Triggered") : Action() {
        override val type = "SPEAK_TTS"
        override val displayName = "Speak (TTS)"
        override val description = "Reads text aloud using Text-to-Speech"
        override val parameters = listOf(Parameter.TextParameter("text", "Text to speak", text))
    }

    data class Vibrate(val durationMs: Int = 500) : Action() {
        override val type = "VIBRATE"
        override val displayName = "Vibrate"
        override val description = "Vibrates the device for X ms"
        override val parameters = listOf(Parameter.NumberParameter("duration", "Duration (ms)", durationMs, 100, 5000))
    }

    // --- App Actions ---
    data class LaunchApp(val packageName: String = "") : Action() {
        override val type = "LAUNCH_APP"
        override val displayName = "Launch App"
        override val description = "Opens a specific application"
        override val parameters = listOf(Parameter.AppParameter("packageName", "Select App", packageName))
    }

    data class ShowNotification(val title: String = "RTS Automation", val message: String = "") : Action() {
        override val type = "SHOW_NOTIFICATION"
        override val displayName = "Show Notification"
        override val description = "Displays a system notification"
        override val parameters = listOf(
            Parameter.TextParameter("title", "Title", title),
            Parameter.TextParameter("message", "Message", message)
        )
    }

    data class ShowToast(val message: String = "Hello!") : Action() {
        override val type = "SHOW_TOAST"
        override val displayName = "Show Toast"
        override val description = "Displays a brief popup message"
        override val parameters = listOf(Parameter.TextParameter("message", "Message", message))
    }

    // --- Device Actions ---
    data class AutoClean(val categories: List<String> = listOf("temp", "dupes", "empty_folders")) : Action() {
        override val type = "AUTO_CLEAN"
        override val displayName = "Auto Clean"
        override val description = "Performs automatic storage cleaning"
        override val parameters = listOf(Parameter.SelectionParameter("categories", "Categories", categories.joinToString(","), listOf("temp", "dupes", "empty_folders", "residual", "logs", "sms", "contact_dupes")))
    }

    object ToggleFlashlight : Action() {
        override val type = "TOGGLE_FLASHLIGHT"
        override val displayName = "Toggle Flashlight"
        override val description = "Turns the camera LED on or off"
        override val parameters = emptyList<Parameter<*>>()
    }

    data class SetBrightness(val level: Int = 50) : Action() {
        override val type = "SET_BRIGHTNESS"
        override val displayName = "Set Brightness"
        override val description = "Sets screen brightness level"
        override val parameters = listOf(Parameter.NumberParameter("level", "Brightness (%)", level, 0, 100))
    }

    // --- Flow Actions ---
    data class Delay(val seconds: Int = 5) : Action() {
        override val type = "DELAY"
        override val displayName = "Delay"
        override val description = "Waits for X seconds before next action"
        override val parameters = listOf(Parameter.NumberParameter("seconds", "Seconds", seconds, 1, 3600))
    }

    // --- ADB Actions ---
    data class RunAdbCommand(val command: String = "") : Action() {
        override val type = "RUN_ADB_COMMAND"
        override val displayName = "Run ADB Command"
        override val description = "Executes a safe ADB command via Local ADB"
        override val parameters = listOf(Parameter.TextParameter("command", "Command", command, "e.g. pm clear ..."))
    }
}

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
