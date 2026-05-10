package com.semseytech.rtsdevicesuitepro.wipe.model

import androidx.compose.ui.graphics.vector.ImageVector

enum class WipeControlMode {
    AUTOMATED, GUIDED, MANUAL
}

enum class WipeStatus {
    IDLE, ACTIVE, NEEDS_PERMISSION, ERROR, COMPLETED, SIMULATED
}

enum class WipeSeverity {
    INFO, WARNING, CRITICAL
}

data class WipeCategory(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    var controlMode: WipeControlMode = WipeControlMode.MANUAL,
    var isExpanded: Boolean = false,
    val items: List<WipeItem> = emptyList(),
    var status: WipeStatus = WipeStatus.IDLE
)

data class WipeItem(
    val id: String,
    val name: String,
    val description: String,
    val infoText: String,
    var controlMode: WipeControlMode = WipeControlMode.MANUAL,
    var isSelected: Boolean = false,
    var status: WipeStatus = WipeStatus.IDLE,
    val estimatedTimeSeconds: Long = 0,
    val requiredPermissions: List<String> = emptyList(),
    val targetPath: String? = null
)

data class WipeReport(
    val startTime: Long,
    val endTime: Long,
    val totalItems: Int,
    val wipedItems: Int,
    val errors: Int,
    val isSimulation: Boolean,
    val logEntries: List<WipeLogEntry>
)

data class WipeProgress(
    val currentItem: String = "",
    val progress: Float = 0f,
    val detail: String = "",
    val passes: Int = 0,
    val totalPasses: Int = 1
)

data class WipeLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val itemId: String,
    val itemName: String,
    val action: String,
    val passes: Int,
    val status: WipeStatus,
    val details: String
)

data class WipeReadiness(
    val score: Int, // 0-100
    val batteryLevel: Int,
    val isCharging: Boolean,
    val storageHealth: String,
    val backupStatus: String,
    val permissionsGranted: Boolean,
    val recommendations: List<String>
)

data class WipeProfile(
    val id: String,
    val name: String,
    val description: String,
    val selectedItemIds: List<String>,
    val preferredControlMode: WipeControlMode
)
