package com.semseytech.rtsdevicesuitepro.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardUiState(
    val lastBackupDate: String = "Never",
    val storageUsedPercent: Float = 0.45f,
    val isAutoCleanEnabled: Boolean = true
)

class DashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Add logic for dashboard data fetching or actions here
}
