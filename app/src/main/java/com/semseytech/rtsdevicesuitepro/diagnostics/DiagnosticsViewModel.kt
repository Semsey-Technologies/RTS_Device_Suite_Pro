package com.semseytech.rtsdevicesuitepro.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.diagnostics.engine.AutoFixEngine
import com.semseytech.rtsdevicesuitepro.diagnostics.engine.RecommendationEngine
import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.scanners.DefensiveScanner
import com.semseytech.rtsdevicesuitepro.diagnostics.scanners.OffensiveScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val issues: List<DiagnosticsIssue> = emptyList(),
    val isScanning: Boolean = false,
    val lastScanTime: Long = 0,
    val fixingIssueId: String? = null,
    val logMessages: List<String> = emptyList()
)

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {
    private val defensiveScanner = DefensiveScanner(application)
    private val offensiveScanner = OffensiveScanner(application)
    private val autoFixEngine = AutoFixEngine(application)
    private val recommendationEngine = RecommendationEngine()

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    fun runScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, logMessages = listOf("Starting full system diagnostic...")) }
            
            val defensiveIssues = defensiveScanner.scan()
            addLog("Defensive scan complete: ${defensiveIssues.size} issues found.")
            
            val offensiveIssues = offensiveScanner.scan()
            addLog("Offensive scan complete: ${offensiveIssues.size} issues found.")
            
            _uiState.update { 
                it.copy(
                    issues = defensiveIssues + offensiveIssues,
                    isScanning = false,
                    lastScanTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun applyFix(issue: DiagnosticsIssue) {
        viewModelScope.launch {
            _uiState.update { it.copy(fixingIssueId = issue.id) }
            addLog("Applying auto-fix for: ${issue.id}...")
            
            val success = autoFixEngine.executeFix(issue)
            
            if (success) {
                addLog("Fix applied successfully. Re-scanning...")
                runScan()
            } else {
                addLog("Fix failed. Manual intervention may be required.")
            }
            
            _uiState.update { it.copy(fixingIssueId = null) }
        }
    }

    private fun addLog(message: String) {
        _uiState.update { it.copy(logMessages = it.logMessages + message) }
    }

    fun getExplanation(issue: DiagnosticsIssue) = recommendationEngine.getExplanation(issue)
    fun getRisk(issue: DiagnosticsIssue) = recommendationEngine.getRiskAssessment(issue)
    fun getFixImpact(issue: DiagnosticsIssue) = recommendationEngine.getAutoFixImpact(issue)
}
