package com.semseytech.rtsdevicesuitepro.net.automation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "automation_settings")

class AutomationRepository(private val context: Context) {
    private val gson = Gson()

    val settingsFlow: Flow<AutomationSettings> = context.dataStore.data.map { preferences ->
        val rulesJson = preferences[AutomationPreferencesKeys.RULES_JSON] ?: "[]"
        val rulesType = object : TypeToken<List<AutomationRule>>() {}.type
        val rules: List<AutomationRule> = gson.fromJson(rulesJson, rulesType)

        AutomationSettings(
            autoDnsBenchmark = preferences[AutomationPreferencesKeys.AUTO_DNS_BENCHMARK] ?: false,
            autoDnsRefresh = preferences[AutomationPreferencesKeys.AUTO_DNS_REFRESH] ?: false,
            autoSocketFlush = preferences[AutomationPreferencesKeys.AUTO_SOCKET_FLUSH] ?: false,
            autoNetworkRebind = preferences[AutomationPreferencesKeys.AUTO_NETWORK_REBIND] ?: false,
            autoWifiQualityMonitor = preferences[AutomationPreferencesKeys.AUTO_WIFI_QUALITY_MONITOR] ?: false,
            autoLatencyMonitor = preferences[AutomationPreferencesKeys.AUTO_LATENCY_MONITOR] ?: false,
            autoCaptivePortalDetection = preferences[AutomationPreferencesKeys.AUTO_CAPTIVE_PORTAL_DETECTION] ?: false,
            autoWifiReset = preferences[AutomationPreferencesKeys.AUTO_WIFI_RESET] ?: false,
            rules = rules
        )
    }

    suspend fun addRule(rule: AutomationRule) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[AutomationPreferencesKeys.RULES_JSON] ?: "[]"
            val type = object : TypeToken<List<AutomationRule>>() {}.type
            val currentRules: MutableList<AutomationRule> = gson.fromJson<List<AutomationRule>>(currentJson, type).toMutableList()
            currentRules.add(rule)
            preferences[AutomationPreferencesKeys.RULES_JSON] = gson.toJson(currentRules)
        }
    }

    suspend fun removeRule(ruleId: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[AutomationPreferencesKeys.RULES_JSON] ?: "[]"
            val type = object : TypeToken<List<AutomationRule>>() {}.type
            val currentRules: MutableList<AutomationRule> = gson.fromJson<List<AutomationRule>>(currentJson, type).toMutableList()
            currentRules.removeAll { it.id == ruleId }
            preferences[AutomationPreferencesKeys.RULES_JSON] = gson.toJson(currentRules)
        }
    }

    suspend fun updateRule(ruleId: String, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[AutomationPreferencesKeys.RULES_JSON] ?: "[]"
            val type = object : TypeToken<List<AutomationRule>>() {}.type
            val currentRules: MutableList<AutomationRule> = gson.fromJson<List<AutomationRule>>(currentJson, type).toMutableList()
            val index = currentRules.indexOfFirst { it.id == ruleId }
            if (index != -1) {
                currentRules[index] = currentRules[index].copy(isEnabled = isEnabled)
                preferences[AutomationPreferencesKeys.RULES_JSON] = gson.toJson(currentRules)
            }
        }
    }

    suspend fun stopAllRules() {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[AutomationPreferencesKeys.RULES_JSON] ?: "[]"
            val type = object : TypeToken<List<AutomationRule>>() {}.type
            val currentRules: List<AutomationRule> = gson.fromJson(currentJson, type)
            val updatedRules = currentRules.map { it.copy(isEnabled = false) }
            preferences[AutomationPreferencesKeys.RULES_JSON] = gson.toJson(updatedRules)
        }
    }

    suspend fun updateSetting(key: Preferences.Key<Boolean>, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = enabled
        }
        
        if (enabled) {
            scheduleWorker(key)
        } else {
            cancelWorker(key)
        }
    }

    private fun scheduleWorker(key: Preferences.Key<Boolean>) {
        val workManager = WorkManager.getInstance(context)
        val workerClass = getWorkerClassForKey(key) ?: return
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ListenableWorker>(
            1, TimeUnit.HOURS, // Default interval
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(key.name)
            .build()

        // Use reflection to create the request because PeriodicWorkRequestBuilder needs a specific class
        // Better: have a factory or just handle each case
        
        val specificRequest = when (key) {
            AutomationPreferencesKeys.AUTO_DNS_BENCHMARK -> createPeriodicRequest<DnsBenchmarkWorker>(1, TimeUnit.HOURS, key.name)
            AutomationPreferencesKeys.AUTO_DNS_REFRESH -> createPeriodicRequest<DnsRefreshWorker>(30, TimeUnit.MINUTES, key.name)
            AutomationPreferencesKeys.AUTO_SOCKET_FLUSH -> createPeriodicRequest<SocketFlushWorker>(2, TimeUnit.HOURS, key.name)
            AutomationPreferencesKeys.AUTO_NETWORK_REBIND -> createPeriodicRequest<NetworkRebindWorker>(4, TimeUnit.HOURS, key.name)
            AutomationPreferencesKeys.AUTO_WIFI_QUALITY_MONITOR -> createPeriodicRequest<WifiQualityWorker>(15, TimeUnit.MINUTES, key.name)
            AutomationPreferencesKeys.AUTO_LATENCY_MONITOR -> createPeriodicRequest<LatencyMonitorWorker>(20, TimeUnit.MINUTES, key.name)
            AutomationPreferencesKeys.AUTO_CAPTIVE_PORTAL_DETECTION -> createPeriodicRequest<CaptivePortalWorker>(15, TimeUnit.MINUTES, key.name)
            AutomationPreferencesKeys.AUTO_WIFI_RESET -> createPeriodicRequest<WifiResetWorker>(6, TimeUnit.HOURS, key.name)
            else -> null
        }

        specificRequest?.let {
            workManager.enqueueUniquePeriodicWork(
                key.name,
                ExistingPeriodicWorkPolicy.UPDATE,
                it
            )
        }
    }

    private inline fun <reified T : ListenableWorker> createPeriodicRequest(
        interval: Long,
        unit: TimeUnit,
        tag: String
    ): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<T>(interval, unit)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(tag)
            .build()
    }

    private fun cancelWorker(key: Preferences.Key<Boolean>) {
        WorkManager.getInstance(context).cancelUniqueWork(key.name)
    }

    private fun getWorkerClassForKey(key: Preferences.Key<Boolean>) = when (key) {
        AutomationPreferencesKeys.AUTO_DNS_BENCHMARK -> DnsBenchmarkWorker::class.java
        AutomationPreferencesKeys.AUTO_DNS_REFRESH -> DnsRefreshWorker::class.java
        AutomationPreferencesKeys.AUTO_SOCKET_FLUSH -> SocketFlushWorker::class.java
        AutomationPreferencesKeys.AUTO_NETWORK_REBIND -> NetworkRebindWorker::class.java
        AutomationPreferencesKeys.AUTO_WIFI_QUALITY_MONITOR -> WifiQualityWorker::class.java
        AutomationPreferencesKeys.AUTO_LATENCY_MONITOR -> LatencyMonitorWorker::class.java
        AutomationPreferencesKeys.AUTO_CAPTIVE_PORTAL_DETECTION -> CaptivePortalWorker::class.java
        AutomationPreferencesKeys.AUTO_WIFI_RESET -> WifiResetWorker::class.java
        else -> null
    }
}
