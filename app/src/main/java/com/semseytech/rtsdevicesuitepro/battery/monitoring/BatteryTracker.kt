package com.semseytech.rtsdevicesuitepro.battery.monitoring

import android.content.Context
import com.semseytech.rtsdevicesuitepro.battery.data.BatteryDatabase
import com.semseytech.rtsdevicesuitepro.battery.data.BatteryUsageEntity
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class BatteryTracker private constructor(context: Context) {
    private val dao = BatteryDatabase.getDatabase(context).batteryDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingUsage = ConcurrentHashMap<ModuleType, BatteryUsageEntity>()

    companion object {
        @Volatile
        private var INSTANCE: BatteryTracker? = null

        fun getInstance(context: Context): BatteryTracker {
            return INSTANCE ?: synchronized(this) {
                val instance = BatteryTracker(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Records activity for a specific module.
     * In a real app, this would be called by the modules themselves.
     */
    fun recordActivity(
        moduleType: ModuleType,
        cpuTimeMs: Long = 0,
        wakeups: Int = 0,
        wakeLockMs: Long = 0,
        networkBytes: Long = 0,
        sensorMs: Long = 0,
        foregroundMs: Long = 0,
        backgroundMs: Long = 0
    ) {
        scope.launch {
            val entity = BatteryUsageEntity(
                moduleType = moduleType,
                cpuTimeMs = cpuTimeMs,
                wakeups = wakeups,
                wakeLockMs = wakeLockMs,
                networkBytes = networkBytes,
                sensorMs = sensorMs,
                foregroundMs = foregroundMs,
                backgroundMs = backgroundMs
            )
            dao.insertUsage(entity)
        }
    }

    // Helper methods for modules to track duration
    private val activeSessions = ConcurrentHashMap<String, Long>()

    fun startSession(sessionId: String) {
        activeSessions[sessionId] = System.currentTimeMillis()
    }

    fun endSession(sessionId: String, moduleType: ModuleType, isForeground: Boolean) {
        val start = activeSessions.remove(sessionId) ?: return
        val duration = System.currentTimeMillis() - start
        if (isForeground) {
            recordActivity(moduleType, foregroundMs = duration)
        } else {
            recordActivity(moduleType, backgroundMs = duration)
        }
    }
}
