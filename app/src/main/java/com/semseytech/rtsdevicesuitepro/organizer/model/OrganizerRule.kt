package com.semseytech.rtsdevicesuitepro.organizer.model

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.LocalTime

data class OrganizerRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sourcePaths: List<String>,
    val targetPath: String,
    val fileTypes: List<String>, // e.g., ["mp3", "wav"] or ["audio"]
    val trigger: RuleTrigger,
    val options: OrganizerOptions = OrganizerOptions(),
    val isEnabled: Boolean = true
)

@JsonAdapter(RuleTriggerAdapter::class)
sealed class RuleTrigger {
    data class Interval(val minutes: Int) : RuleTrigger()
    data class Daily(val hour: Int, val minute: Int) : RuleTrigger()
    data class Weekly(val dayOfWeek: DayOfWeek, val hour: Int, val minute: Int) : RuleTrigger()
    object OnIdle : RuleTrigger()
    object OnPowerConnected : RuleTrigger()
    object OnPowerDisconnected : RuleTrigger()
    object OnFolderModified : RuleTrigger()
}

class RuleTriggerAdapter : JsonSerializer<RuleTrigger>, JsonDeserializer<RuleTrigger> {
    override fun serialize(src: RuleTrigger, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        val typeName = when (src) {
            is RuleTrigger.Interval -> "Interval"
            is RuleTrigger.Daily -> "Daily"
            is RuleTrigger.Weekly -> "Weekly"
            RuleTrigger.OnIdle -> "OnIdle"
            RuleTrigger.OnPowerConnected -> "OnPowerConnected"
            RuleTrigger.OnPowerDisconnected -> "OnPowerDisconnected"
            RuleTrigger.OnFolderModified -> "OnFolderModified"
        }
        jsonObject.addProperty("type", typeName)

        when (src) {
            is RuleTrigger.Interval -> jsonObject.addProperty("minutes", src.minutes)
            is RuleTrigger.Daily -> {
                jsonObject.addProperty("hour", src.hour)
                jsonObject.addProperty("minute", src.minute)
            }
            is RuleTrigger.Weekly -> {
                jsonObject.addProperty("dayOfWeek", src.dayOfWeek.name)
                jsonObject.addProperty("hour", src.hour)
                jsonObject.addProperty("minute", src.minute)
            }
            else -> {}
        }
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): RuleTrigger {
        val jsonObject = json.asJsonObject
        val typeElement = jsonObject.get("type")
        val type = if (typeElement != null && typeElement.isJsonPrimitive) typeElement.asString else null

        if (type == null) {
            // Fallback for legacy data without "type" field
            return when {
                jsonObject.has("minutes") -> RuleTrigger.Interval(jsonObject.get("minutes").asInt)
                jsonObject.has("dayOfWeek") -> {
                    val dayStr = jsonObject.get("dayOfWeek").asString
                    RuleTrigger.Weekly(
                        try { DayOfWeek.valueOf(dayStr) } catch (e: Exception) { DayOfWeek.MONDAY },
                        jsonObject.get("hour")?.asInt ?: 0,
                        jsonObject.get("minute")?.asInt ?: 0
                    )
                }
                jsonObject.has("hour") -> RuleTrigger.Daily(
                    jsonObject.get("hour")?.asInt ?: 0,
                    jsonObject.get("minute")?.asInt ?: 0
                )
                else -> RuleTrigger.OnIdle
            }
        }

        return when (type) {
            "Interval" -> RuleTrigger.Interval(jsonObject.get("minutes").asInt)
            "Daily" -> RuleTrigger.Daily(jsonObject.get("hour").asInt, jsonObject.get("minute").asInt)
            "Weekly" -> RuleTrigger.Weekly(
                DayOfWeek.valueOf(jsonObject.get("dayOfWeek").asString),
                jsonObject.get("hour").asInt,
                jsonObject.get("minute").asInt
            )
            "OnIdle" -> RuleTrigger.OnIdle
            "OnPowerConnected" -> RuleTrigger.OnPowerConnected
            "OnPowerDisconnected" -> RuleTrigger.OnPowerDisconnected
            "OnFolderModified" -> RuleTrigger.OnFolderModified
            else -> throw JsonParseException("Unknown RuleTrigger type: $type")
        }
    }
}

data class OrganizerOptions(
    val moveEntireFolderIfContainsMatch: Boolean = false,
    val ignoreSubfolders: Boolean = true,
    val archiveOptions: ArchiveOptions = ArchiveOptions()
)

data class ArchiveOptions(
    val autoExtract: Boolean = false,
    val deleteAfterExtraction: Boolean = false,
    val treatAsSingleUnit: Boolean = true // Treat extracted content as a folder to be moved
)
