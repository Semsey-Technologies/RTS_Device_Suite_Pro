package com.semseytech.rtsdevicesuitepro.organizer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerRepository
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerRule
import com.semseytech.rtsdevicesuitepro.organizer.worker.FileOrganizerWorker
import com.semseytech.rtsdevicesuitepro.organizer.worker.FolderMonitorService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OrganizerViewModel(
    application: Application,
    private val repository: OrganizerRepository
) : AndroidViewModel(application) {

    val rules: StateFlow<List<OrganizerRule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRule(rule: OrganizerRule) {
        viewModelScope.launch {
            repository.insertRule(rule)
            scheduleWork()
            refreshMonitorService()
        }
    }

    fun runRulesNow() {
        scheduleWork()
    }

    private fun refreshMonitorService() {
        val intent = Intent(getApplication(), FolderMonitorService::class.java).apply {
            action = "REFRESH"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    private fun scheduleWork() {
        val workRequest = OneTimeWorkRequestBuilder<FileOrganizerWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "FileOrganizerWork",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun deleteRule(rule: OrganizerRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            refreshMonitorService()
        }
    }

    fun toggleRule(rule: OrganizerRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
            refreshMonitorService()
        }
    }
}
