package com.semseytech.rtsdevicesuitepro.net.automation

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "network_automation_settings")

class NetworkAutomationRepository(private val context: Context) {

    object Keys {
        val AUTO_DNS_BENCHMARK = booleanPreferencesKey("auto_dns_benchmark")
        val AUTO_DNS_REFRESH = booleanPreferencesKey("auto_dns_refresh")
        val AUTO_SOCKET_FLUSH = booleanPreferencesKey("auto_socket_flush")
        val AUTO_NETWORK_REBIND = booleanPreferencesKey("auto_network_rebind")
        val AUTO_WIFI_QUALITY_MONITOR = booleanPreferencesKey("auto_wifi_quality_monitor")
        val AUTO_LATENCY_MONITOR = booleanPreferencesKey("auto_latency_monitor")
        val AUTO_CAPTIVE_PORTAL_DETECTION = booleanPreferencesKey("auto_captive_portal_detection")
        val AUTO_WIFI_RESET = booleanPreferencesKey("auto_wifi_reset")
    }

    val automationSettings: Flow<NetworkAutomationSettings> = context.dataStore.data.map { prefs ->
        NetworkAutomationSettings(
            autoDnsBenchmark = prefs[Keys.AUTO_DNS_BENCHMARK] ?: false,
            autoDnsRefresh = prefs[Keys.AUTO_DNS_REFRESH] ?: false,
            autoSocketFlush = prefs[Keys.AUTO_SOCKET_FLUSH] ?: false,
            autoNetworkRebind = prefs[Keys.AUTO_NETWORK_REBIND] ?: false,
            autoWifiQualityMonitor = prefs[Keys.AUTO_WIFI_QUALITY_MONITOR] ?: false,
            autoLatencyMonitor = prefs[Keys.AUTO_LATENCY_MONITOR] ?: false,
            autoCaptivePortalDetection = prefs[Keys.AUTO_CAPTIVE_PORTAL_DETECTION] ?: false,
            autoWifiReset = prefs[Keys.AUTO_WIFI_RESET] ?: false
        )
    }

    suspend fun updateSetting(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}

data class NetworkAutomationSettings(
    val autoDnsBenchmark: Boolean,
    val autoDnsRefresh: Boolean,
    val autoSocketFlush: Boolean,
    val autoNetworkRebind: Boolean,
    val autoWifiQualityMonitor: Boolean,
    val autoLatencyMonitor: Boolean,
    val autoCaptivePortalDetection: Boolean,
    val autoWifiReset: Boolean
)
