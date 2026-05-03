package com.semseytech.rtsdevicesuitepro.adb.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.adb.core.AdbDiscovery
import com.semseytech.rtsdevicesuitepro.adb.core.AdbManager
import com.semseytech.rtsdevicesuitepro.adb.core.AdbService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AdbUiState(
    val isEnabled: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val lastCommandOutput: String = "",
    val isLoading: Boolean = false,
    val adbPort: Int = 0,
    val isPaired: Boolean = false,
    val discoveredServices: List<AdbService> = emptyList()
)

class AdbViewModel(application: Application) : AndroidViewModel(application) {
    private val adbManager = AdbManager(application)
    private val adbDiscovery = AdbDiscovery(application)
    private var discoveryJob: kotlinx.coroutines.Job? = null
    
    private val _uiState = MutableStateFlow(AdbUiState())
    val uiState: StateFlow<AdbUiState> = _uiState.asStateFlow()

    init {
        startDiscovery()
        viewModelScope.launch {
            adbManager.isEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(isEnabled = enabled)
            }
        }
    }

    private fun startDiscovery() {
        stopDiscovery()
        discoveryJob = viewModelScope.launch {
            adbDiscovery.discoverServices().collectLatest { services ->
                _uiState.value = _uiState.value.copy(discoveredServices = services)
            }
        }
    }

    private fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _uiState.value = _uiState.value.copy(discoveredServices = emptyList())
    }

    fun toggleAdb(enabled: Boolean) {
        viewModelScope.launch {
            adbManager.setEnabled(enabled)
        }
    }

    fun pair(port: String, code: String, host: String? = null) {
        val portInt = port.toIntOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, connectionStatus = "Pairing with ${host ?: "local device"}...")
            val success = adbManager.pair(portInt, code, host)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                connectionStatus = if (success) "Pairing successful! Now enter the ADB Port and Connect." else "Pairing failed. Check code/port.",
                isPaired = success
            )
        }
    }

    fun connect(port: String, host: String? = null) {
        val portInt = port.toIntOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, connectionStatus = "Connecting...")
            val success = adbManager.connect(host = host, port = portInt)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                connectionStatus = if (success) "Connected" else "Connection Failed (Check Wireless Debugging is ON)",
                adbPort = portInt
            )
        }
    }

    fun runCommand(command: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val output = adbManager.executeSafeCommand(command)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lastCommandOutput = output
            )
        }
    }
    
    fun disconnect() {
        adbManager.disconnect()
        _uiState.value = _uiState.value.copy(connectionStatus = "Disconnected")
    }
}
