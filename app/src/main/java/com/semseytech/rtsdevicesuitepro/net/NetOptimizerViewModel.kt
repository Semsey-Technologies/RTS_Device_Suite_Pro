package com.semseytech.rtsdevicesuitepro.net

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetOptimizerViewModel(application: Application) : AndroidViewModel(application) {

    private val infoProvider = NetworkInfoProvider(application)
    private val optimizer = NetworkOptimizer(application)
    private val latencyTester = LatencyTester()
    private val dnsBenchmark = DnsBenchmark()

    private val _uiState = MutableStateFlow(NetOptimizerState())
    val uiState: StateFlow<NetOptimizerState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                updateNetworkInfo()
                delay(2000)
            }
        }
    }

    private suspend fun updateNetworkInfo() {
        val info = infoProvider.getNetworkInfo()
        val ipInfo = infoProvider.getExternalIpInfo()
        val quality = latencyTester.testQuality()
        
        _uiState.update { it.copy(
            networkInfo = info.copy(
                externalIp = ipInfo.first,
                location = ipInfo.second
            ),
            qualityMetrics = quality
        ) }
    }

    fun runOptimization() {
        viewModelScope.launch {
            _uiState.update { it.copy(isOptimizing = true, optimizationLog = emptyList()) }
            
            optimizer.optimize().collect { logEntry ->
                _uiState.update { state ->
                    state.copy(optimizationLog = state.optimizationLog + logEntry)
                }
            }
            
            // Re-benchmark DNS for the UI
            val dnsRes = dnsBenchmark.benchmark()
            _uiState.update { it.copy(isOptimizing = false, dnsResults = dnsRes) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
