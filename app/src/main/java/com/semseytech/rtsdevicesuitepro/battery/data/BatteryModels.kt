package com.semseytech.rtsdevicesuitepro.battery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ModuleType {
    AUTOMATION,
    FLOWCHART,
    ADB,
    TERMINAL,
    BACKUP,
    RESTORE,
    SMS_VIEWER,
    NETWORK_OPTIMIZER,
    SENSORS,
    SCHEDULERS,
    BACKGROUND_WORKERS
}

@Entity(tableName = "battery_usage_history")
data class BatteryUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleType: ModuleType,
    val timestamp: Long = System.currentTimeMillis(),
    val cpuTimeMs: Long = 0,
    val wakeups: Int = 0,
    val wakeLockMs: Long = 0,
    val networkBytes: Long = 0,
    val sensorMs: Long = 0,
    val foregroundMs: Long = 0,
    val backgroundMs: Long = 0
)

data class ModuleBatteryStatus(
    val type: ModuleType,
    val name: String,
    val isEnabled: Boolean,
    val estimatedMah: Float,
    val batteryPercent: Float,
    val timeImpactHours: Float,
    val explanation: String,
    val lastUpdated: Long
)

data class OptimizationSuggestion(
    val title: String,
    val description: String,
    val actionLabel: String,
    val moduleType: ModuleType? = null
)
