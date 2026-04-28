package com.semseytech.rtsdevicesuitepro.organizer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizerDao {
    @Query("SELECT * FROM organizer_rules")
    fun getAllRules(): Flow<List<OrganizerRuleEntity>>

    @Query("SELECT * FROM organizer_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<OrganizerRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: OrganizerRuleEntity)

    @Delete
    suspend fun deleteRule(rule: OrganizerRuleEntity)

    @Update
    suspend fun updateRule(rule: OrganizerRuleEntity)
}
