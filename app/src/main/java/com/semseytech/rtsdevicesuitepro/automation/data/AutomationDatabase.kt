package com.semseytech.rtsdevicesuitepro.automation.data

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.semseytech.rtsdevicesuitepro.automation.models.*

@Dao
interface AutomationDao {
    @Query("SELECT * FROM rules")
    suspend fun getAllRules(): List<RuleEntity>

    @Query("SELECT * FROM rule_groups")
    suspend fun getAllGroups(): List<RuleGroup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: RuleGroup)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Delete
    suspend fun deleteGroup(group: RuleGroup)

    @Query("UPDATE rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setRuleEnabled(id: String, enabled: Boolean)

    @Query("UPDATE rules SET groupId = :groupId WHERE id = :ruleId")
    suspend fun updateRuleGroup(ruleId: String, groupId: String?)
}

@Database(entities = [RuleEntity::class, RuleGroup::class], version = 2, exportSchema = false)
@TypeConverters(AutomationConverters::class)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile
        private var INSTANCE: AutomationDatabase? = null

        fun getDatabase(context: Context): AutomationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDatabase::class.java,
                    "automation_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AutomationConverters {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Trigger::class.java, AutomationTypeAdapter<Trigger>())
        .registerTypeAdapter(Condition::class.java, AutomationTypeAdapter<Condition>())
        .registerTypeAdapter(Action::class.java, AutomationTypeAdapter<Action>())
        .create()

    @TypeConverter
    fun fromTrigger(trigger: Trigger): String = gson.toJson(trigger)

    @TypeConverter
    fun toTrigger(json: String): Trigger = gson.fromJson(json, Trigger::class.java)

    @TypeConverter
    fun fromConditions(conditions: List<Condition>): String = gson.toJson(conditions)

    @TypeConverter
    fun toConditions(json: String): List<Condition> {
        val type = object : TypeToken<List<Condition>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromActions(actions: List<Action>): String = gson.toJson(actions)

    @TypeConverter
    fun toActions(json: String): List<Action> {
        val type = object : TypeToken<List<Action>>() {}.type
        return gson.fromJson(json, type)
    }
}
