package com.semseytech.rtsdevicesuitepro.battery.estimation

import com.semseytech.rtsdevicesuitepro.battery.data.BatteryUsageEntity
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleBatteryStatus
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleType
import java.util.Locale

class BatteryEstimationEngine {
    
    // Constants for estimation (approximate values for a typical modern smartphone)
    private val MAH_PER_CPU_HOUR = 250f
    private val MAH_PER_WAKELOCK_HOUR = 40f
    private val MAH_PER_MB_NETWORK = 0.4f
    private val MAH_PER_SENSOR_HOUR = 70f
    private val TOTAL_BATTERY_CAPACITY_MAH = 4500f // Common 4500mAh battery

    fun estimateUsage(moduleType: ModuleType, history: List<BatteryUsageEntity>): ModuleBatteryStatus {
        var totalCpuMs = 0L
        var totalWakeLockMs = 0L
        var totalNetworkBytes = 0L
        var totalSensorMs = 0L
        var totalForegroundMs = 0L
        var totalBackgroundMs = 0L

        history.forEach {
            totalCpuMs += it.cpuTimeMs
            totalWakeLockMs += it.wakeLockMs
            totalNetworkBytes += it.networkBytes
            totalSensorMs += it.sensorMs
            totalForegroundMs += it.foregroundMs
            totalBackgroundMs += it.backgroundMs
        }

        val cpuMah = (totalCpuMs / 3600000f) * MAH_PER_CPU_HOUR
        val wakeLockMah = (totalWakeLockMs / 3600000f) * MAH_PER_WAKELOCK_HOUR
        val networkMah = (totalNetworkBytes / (1024f * 1024f)) * MAH_PER_MB_NETWORK
        val sensorMah = (totalSensorMs / 3600000f) * MAH_PER_SENSOR_HOUR
        
        val totalMah = cpuMah + wakeLockMah + networkMah + sensorMah
        val batteryPercent = (totalMah / TOTAL_BATTERY_CAPACITY_MAH) * 100f
        
        // Estimating time impact: if usage continues at this rate, how much sooner will battery die?
        // Let's assume a 24-hour period for the provided history.
        val timeImpactHours = (totalMah / TOTAL_BATTERY_CAPACITY_MAH) * 24f

        return ModuleBatteryStatus(
            type = moduleType,
            name = formatModuleName(moduleType),
            isEnabled = true, // This should be synced with actual module state
            estimatedMah = totalMah,
            batteryPercent = batteryPercent,
            timeImpactHours = timeImpactHours,
            explanation = generateExplanation(moduleType, totalCpuMs, totalWakeLockMs, totalNetworkBytes, totalSensorMs),
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun formatModuleName(type: ModuleType): String = type.name.replace("_", " ").lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun generateExplanation(
        type: ModuleType,
        cpu: Long,
        wakeLock: Long,
        network: Long,
        sensor: Long
    ): String {
        val reasons = mutableListOf<String>()
        if (cpu > 60000) reasons.add("processing tasks")
        if (wakeLock > 60000) reasons.add("keeping device awake")
        if (network > 1024 * 1024) reasons.add("network data transfer")
        if (sensor > 60000) reasons.add("using hardware sensors")

        val reasonText = if (reasons.isEmpty()) "minimal background activity" else reasons.joinToString(", ")
        
        return when (type) {
            ModuleType.AUTOMATION -> "Runs background rules and triggers. Primary drain: $reasonText."
            ModuleType.ADB -> "Manages remote connections. Primary drain: $reasonText."
            ModuleType.SENSORS -> "Monitors environmental data. Primary drain: $reasonText."
            else -> "Standard module operations. Primary drain: $reasonText."
        }
    }
}
