package com.semseytech.rtsdevicesuitepro.battery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert
    suspend fun insertUsage(usage: BatteryUsageEntity)

    @Query("SELECT * FROM battery_usage_history WHERE moduleType = :moduleType ORDER BY timestamp DESC")
    fun getUsageHistory(moduleType: ModuleType): Flow<List<BatteryUsageEntity>>

    @Query("SELECT * FROM battery_usage_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getUsageSince(since: Long): List<BatteryUsageEntity>

    @Query("DELETE FROM battery_usage_history WHERE timestamp < :threshold")
    suspend fun clearOldHistory(threshold: Long)
}
