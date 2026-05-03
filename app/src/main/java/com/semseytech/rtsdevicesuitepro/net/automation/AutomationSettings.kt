package com.semseytech.rtsdevicesuitepro.net.automation

import androidx.datastore.preferences.core.booleanPreferencesKey

data class AutomationSettings(
    val autoDnsBenchmark: Boolean = false,
    val autoDnsRefresh: Boolean = false,
    val autoSocketFlush: Boolean = false,
    val autoNetworkRebind: Boolean = false,
    val autoWifiQualityMonitor: Boolean = false,
    val autoLatencyMonitor: Boolean = false,
    val autoCaptivePortalDetection: Boolean = false,
    val autoWifiReset: Boolean = false,
    val rules: List<AutomationRule> = emptyList()
)

data class AutomationRule(
    val id: String,
    val name: String,
    val conditionType: ConditionType,
    val conditionValue: String,
    val actionType: ActionType,
    val isEnabled: Boolean = true,
    val audioChannel: AudioChannel = AudioChannel.MEDIA
)

enum class AudioChannel {
    MEDIA,
    NOTIFICATION,
    ALARM,
    RING,
    SYSTEM
}

enum class ConditionType {
    TIME_INTERVAL,
    LATENCY_ABOVE,
    WIFI_SIGNAL_BELOW,
    BATTERY_BELOW,
    CHARGING_STATUS
}

enum class ActionType {
    DNS_BENCHMARK,
    WIFI_RESET,
    SOCKET_FLUSH,
    NOTIFY_USER,
    OPTIMIZE_NETWORK
}

object AutomationPreferencesKeys {
    val RULES_JSON = androidx.datastore.preferences.core.stringPreferencesKey("automation_rules_json")
    val AUTO_DNS_BENCHMARK = booleanPreferencesKey("auto_dns_benchmark")
    val AUTO_DNS_REFRESH = booleanPreferencesKey("auto_dns_refresh")
    val AUTO_SOCKET_FLUSH = booleanPreferencesKey("auto_socket_flush")
    val AUTO_NETWORK_REBIND = booleanPreferencesKey("auto_network_rebind")
    val AUTO_WIFI_QUALITY_MONITOR = booleanPreferencesKey("auto_wifi_quality_monitor")
    val AUTO_LATENCY_MONITOR = booleanPreferencesKey("auto_latency_monitor")
    val AUTO_CAPTIVE_PORTAL_DETECTION = booleanPreferencesKey("auto_captive_portal_detection")
    val AUTO_WIFI_RESET = booleanPreferencesKey("auto_wifi_reset")
}
