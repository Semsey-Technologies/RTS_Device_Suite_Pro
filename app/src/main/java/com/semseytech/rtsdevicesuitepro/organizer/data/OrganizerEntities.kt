package com.semseytech.rtsdevicesuitepro.organizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerOptions
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTrigger
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTriggerAdapter
import com.google.gson.GsonBuilder

@Entity(tableName = "organizer_rules")
data class OrganizerRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourcePath: String,
    val targetPath: String,
    val fileTypesJson: String, // Store as JSON string
    val triggerJson: String,   // Store as JSON string
    val optionsJson: String,   // Store as JSON string
    val isEnabled: Boolean
)

class Converters {
    private val gson = GsonBuilder()
        .registerTypeAdapter(RuleTrigger::class.java, RuleTriggerAdapter())
        .create()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = gson.fromJson(value, Array<String>::class.java).toList()

    @TypeConverter
    fun fromTrigger(value: RuleTrigger): String = gson.toJson(value)

    @TypeConverter
    fun toTrigger(value: String): RuleTrigger = gson.fromJson(value, RuleTrigger::class.java)

    @TypeConverter
    fun fromOptions(value: OrganizerOptions): String = gson.toJson(value)

    @TypeConverter
    fun toOptions(value: String): OrganizerOptions = gson.fromJson(value, OrganizerOptions::class.java)
}
