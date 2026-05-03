package com.semseytech.rtsdevicesuitepro.automation.data

import com.google.gson.*
import com.semseytech.rtsdevicesuitepro.automation.models.*
import java.lang.reflect.Type

class AutomationTypeAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T> {
    private val innerGson = GsonBuilder()
        .registerTypeAdapter(Trigger::class.java, this)
        .registerTypeAdapter(Condition::class.java, this)
        .registerTypeAdapter(Action::class.java, this)
        .create()

    private val baseGson = Gson()

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
            ?: return context.deserialize(json, typeOfT) // Fallback to default if type missing
        
        return try {
            when {
                isTrigger(type) -> deserializeTrigger(type, json, context) as T
                isCondition(type) -> deserializeCondition(type, json, context) as T
                isAction(type) -> deserializeAction(type, json, context) as T
                else -> throw JsonParseException("Unknown component type: $type")
            }
        } catch (e: Exception) {
            if (e is JsonParseException) throw e
            throw JsonParseException("Failed to deserialize $type", e)
        }
    }

    private fun isTrigger(type: String) = type in listOf(
        "WIFI_CONNECTED", "WIFI_DISCONNECTED", "SPECIFIC_WIFI_CONNECTED", "WIFI_SIGNAL_LOW", "MOBILE_DATA_ACTIVE",
        "POWER_CONNECTED", "POWER_DISCONNECTED", "BATTERY_LEVEL_ABOVE", "BATTERY_LEVEL_BELOW",
        "SCREEN_ON", "SCREEN_OFF", "SCREEN_UNLOCKED", "BOOT_COMPLETED",
        "BLUETOOTH_ON", "BT_DEVICE_CONNECTED", "TIME_OF_DAY", "DEVICE_IDLE", "DAYS_OF_WEEK", "RECURRING", "INTERVAL", "STORAGE_SIZE", "APP_OPENED"
    )

    private fun isCondition(type: String) = type in listOf(
        "IS_WIFI_CONNECTED", "WIFI_SSID_IS", "BATTERY_BETWEEN", "IS_CHARGING",
        "SCREEN_IS_ON", "DEVICE_IS_LOCKED", "STORAGE_FREE_ABOVE"
    )

    private fun isAction(type: String) = type in listOf(
        "RUN_DNS_BENCHMARK", "REFRESH_DNS", "TOGGLE_WIFI", "SET_VOLUME", "SPEAK_TTS", "VIBRATE",
        "LAUNCH_APP", "SHOW_NOTIFICATION", "SHOW_TOAST", "AUTO_CLEAN", "TOGGLE_FLASHLIGHT", "SET_BRIGHTNESS", "DELAY"
    )

    private fun deserializeTrigger(type: String, json: JsonElement, context: JsonDeserializationContext): Trigger {
        return when (type) {
            "WIFI_CONNECTED" -> Trigger.WiFiConnected
            "WIFI_DISCONNECTED" -> Trigger.WiFiDisconnected
            "SPECIFIC_WIFI_CONNECTED" -> context.deserialize(json, Trigger.SpecificWiFiConnected::class.java)
            "WIFI_SIGNAL_LOW" -> context.deserialize(json, Trigger.WiFiSignalLow::class.java)
            "MOBILE_DATA_ACTIVE" -> Trigger.MobileDataActive
            "POWER_CONNECTED" -> Trigger.PowerConnected
            "POWER_DISCONNECTED" -> Trigger.PowerDisconnected
            "BATTERY_LEVEL_ABOVE" -> context.deserialize(json, Trigger.BatteryLevelAbove::class.java)
            "BATTERY_LEVEL_BELOW" -> context.deserialize(json, Trigger.BatteryLevelBelow::class.java)
            "SCREEN_ON" -> Trigger.ScreenOn
            "SCREEN_OFF" -> Trigger.ScreenOff
            "SCREEN_UNLOCKED" -> Trigger.ScreenUnlocked
            "BOOT_COMPLETED" -> Trigger.DeviceBootCompleted
            "BLUETOOTH_ON" -> Trigger.BluetoothOn
            "BT_DEVICE_CONNECTED" -> context.deserialize(json, Trigger.BluetoothDeviceConnected::class.java)
            "TIME_OF_DAY" -> context.deserialize(json, Trigger.TimeOfDay::class.java)
            "DEVICE_IDLE" -> Trigger.DeviceIdle
            "DAYS_OF_WEEK" -> context.deserialize(json, Trigger.DaysOfWeek::class.java)
            "RECURRING" -> context.deserialize(json, Trigger.RecurringTrigger::class.java)
            "INTERVAL" -> context.deserialize(json, Trigger.IntervalTrigger::class.java)
            "STORAGE_SIZE" -> context.deserialize(json, Trigger.StorageSizeTrigger::class.java)
            "APP_OPENED" -> context.deserialize(json, Trigger.AppOpened::class.java)
            else -> throw JsonParseException("Unknown trigger: $type")
        }
    }

    private fun deserializeCondition(type: String, json: JsonElement, context: JsonDeserializationContext): Condition {
        return when (type) {
            "IS_WIFI_CONNECTED" -> Condition.IsConnectedToWiFi
            "WIFI_SSID_IS" -> context.deserialize(json, Condition.WiFiSSIDIs::class.java)
            "BATTERY_BETWEEN" -> context.deserialize(json, Condition.BatteryLevelBetween::class.java)
            "IS_CHARGING" -> Condition.IsCharging
            "SCREEN_IS_ON" -> Condition.ScreenIsOn
            "DEVICE_IS_LOCKED" -> Condition.DeviceIsLocked
            "STORAGE_FREE_ABOVE" -> context.deserialize(json, Condition.StorageFreeAbove::class.java)
            else -> throw JsonParseException("Unknown condition: $type")
        }
    }

    private fun deserializeAction(type: String, json: JsonElement, context: JsonDeserializationContext): Action {
        return when (type) {
            "RUN_DNS_BENCHMARK" -> Action.RunDNSBenchmark
            "REFRESH_DNS" -> Action.RefreshDNS
            "TOGGLE_WIFI" -> Action.ToggleWiFi
            "SET_VOLUME" -> context.deserialize(json, Action.SetVolume::class.java)
            "SPEAK_TTS" -> context.deserialize(json, Action.Speak::class.java)
            "VIBRATE" -> context.deserialize(json, Action.Vibrate::class.java)
            "LAUNCH_APP" -> context.deserialize(json, Action.LaunchApp::class.java)
            "SHOW_NOTIFICATION" -> context.deserialize(json, Action.ShowNotification::class.java)
            "SHOW_TOAST" -> context.deserialize(json, Action.ShowToast::class.java)
            "AUTO_CLEAN" -> context.deserialize(json, Action.AutoClean::class.java)
            "TOGGLE_FLASHLIGHT" -> Action.ToggleFlashlight
            "SET_BRIGHTNESS" -> context.deserialize(json, Action.SetBrightness::class.java)
            "DELAY" -> context.deserialize(json, Action.Delay::class.java)
            else -> throw JsonParseException("Unknown action: $type")
        }
    }
}
