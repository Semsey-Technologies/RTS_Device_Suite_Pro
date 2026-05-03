package com.semseytech.rtsdevicesuitepro.net.automation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NetworkAutomationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetworkAutomationRepository(application)

    val settings: StateFlow<NetworkAutomationSettings> = repository.automationSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NetworkAutomationSettings(
                autoDnsBenchmark = false,
                autoDnsRefresh = false,
                autoSocketFlush = false,
                autoNetworkRebind = false,
                autoWifiQualityMonitor = false,
                autoLatencyMonitor = false,
                autoCaptivePortalDetection = false,
                autoWifiReset = false
            )
        )

    fun toggleSetting(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            repository.updateSetting(key, value)
            // If any toggle is ON, ensure WorkManager is scheduled
            // In a real app, we'd check if ALL are OFF to cancel, but keeping it simple for now
            NetworkAutomationWorker.schedule(getApplication())
        }
    }
}
