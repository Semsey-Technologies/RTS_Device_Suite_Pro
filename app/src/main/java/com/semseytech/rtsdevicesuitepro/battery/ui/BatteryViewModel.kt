package com.semseytech.rtsdevicesuitepro.battery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.battery.data.BatteryDatabase
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleBatteryStatus
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleType
import com.semseytech.rtsdevicesuitepro.battery.data.OptimizationSuggestion
import com.semseytech.rtsdevicesuitepro.battery.estimation.BatteryEstimationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.semseytech.rtsdevicesuitepro.battery.export.PdfBatteryReportGenerator
import java.io.File

data class BatteryUiState(
    val moduleStatuses: List<ModuleBatteryStatus> = emptyList(),
    val suggestions: List<OptimizationSuggestion> = emptyList(),
    val isRefreshing: Boolean = false,
    val exportedFile: File? = null
)

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = BatteryDatabase.getDatabase(application).batteryDao()
    private val engine = BatteryEstimationEngine()
    private val pdfGenerator = PdfBatteryReportGenerator(application)

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000 // Last 24 hours
            val history = dao.getUsageSince(since)
            
            val statuses = ModuleType.values().map { type ->
                val moduleHistory = history.filter { it.moduleType == type }
                engine.estimateUsage(type, moduleHistory)
            }.sortedByDescending { it.estimatedMah }

            val suggestions = generateSuggestions(statuses)

            _uiState.value = _uiState.value.copy(
                moduleStatuses = statuses,
                suggestions = suggestions,
                isRefreshing = false
            )
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            val file = pdfGenerator.generateReport(_uiState.value.moduleStatuses)
            _uiState.value = _uiState.value.copy(exportedFile = file)
        }
    }

    fun clearExportedFile() {
        _uiState.value = _uiState.value.copy(exportedFile = null)
    }

    private fun generateSuggestions(statuses: List<ModuleBatteryStatus>): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()
        
        statuses.firstOrNull { it.batteryPercent > 2f }?.let { highDrainModule ->
            suggestions.add(
                OptimizationSuggestion(
                    title = "Optimize ${highDrainModule.name}",
                    description = "This module is using ${highDrainModule.batteryPercent.format(1)}% of your battery. Consider reducing its polling frequency.",
                    actionLabel = "Optimize",
                    moduleType = highDrainModule.type
                )
            )
        }

        suggestions.add(
            OptimizationSuggestion(
                title = "General Optimization",
                description = "Disable modules when screen is off to save up to 15% battery.",
                actionLabel = "Enable"
            )
        )

        return suggestions
    }

    fun toggleModule(type: ModuleType, enabled: Boolean) {
        // Logic to enable/disable module
        refreshUsage()
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)
}
