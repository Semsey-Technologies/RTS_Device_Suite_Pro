package com.semseytech.rtsdevicesuitepro.net.automation

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AutomationRepository(application)

    val settings: StateFlow<AutomationSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutomationSettings()
    )

    fun toggleSetting(key: Preferences.Key<Boolean>, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSetting(key, enabled)
        }
    }

    fun addRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.addRule(rule)
        }
    }

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.saveRule(rule)
        }
    }

    fun removeRule(ruleId: String) {
        viewModelScope.launch {
            repository.removeRule(ruleId)
        }
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRule(ruleId, isEnabled)
        }
    }

    fun stopAllRules() {
        viewModelScope.launch {
            repository.stopAllRules()
        }
    }
}
