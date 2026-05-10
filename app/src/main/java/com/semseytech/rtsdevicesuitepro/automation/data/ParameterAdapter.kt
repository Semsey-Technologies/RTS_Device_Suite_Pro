package com.semseytech.rtsdevicesuitepro.automation.data

import com.google.gson.*
import com.semseytech.rtsdevicesuitepro.automation.models.*
import java.lang.reflect.Type

class ParameterAdapter : JsonSerializer<Parameter<*>>, JsonDeserializer<Parameter<*>> {
    private val gson = Gson()

    override fun serialize(src: Parameter<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonElement = gson.toJsonTree(src)
        if (jsonElement.isJsonObject) {
            val jsonObject = jsonElement.asJsonObject
            val typeName = when (src) {
                is Parameter.TextParameter -> "TEXT"
                is Parameter.NumberParameter -> "NUMBER"
                is Parameter.SelectionParameter -> "SELECTION"
                is Parameter.TimeParameter -> "TIME"
                is Parameter.AppParameter -> "APP"
                is Parameter.BluetoothParameter -> "BLUETOOTH"
            }
            jsonObject.addProperty("parameter_type", typeName)
            return jsonObject
        }
        return jsonElement
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Parameter<*> {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("parameter_type")?.asString

        if (type == null) {
            // Attempt to infer type from fields to handle legacy data
            return when {
                jsonObject.has("options") -> gson.fromJson(json, Parameter.SelectionParameter::class.java)
                jsonObject.has("min") || jsonObject.has("max") -> gson.fromJson(json, Parameter.NumberParameter::class.java)
                jsonObject.has("packageName") -> gson.fromJson(json, Parameter.AppParameter::class.java)
                jsonObject.has("address") -> gson.fromJson(json, Parameter.BluetoothParameter::class.java)
                else -> gson.fromJson(json, Parameter.TextParameter::class.java)
            }
        }

        return when (type) {
            "TEXT" -> gson.fromJson(json, Parameter.TextParameter::class.java)
            "NUMBER" -> gson.fromJson(json, Parameter.NumberParameter::class.java)
            "SELECTION" -> gson.fromJson(json, Parameter.SelectionParameter::class.java)
            "TIME" -> gson.fromJson(json, Parameter.TimeParameter::class.java)
            "APP" -> gson.fromJson(json, Parameter.AppParameter::class.java)
            "BLUETOOTH" -> gson.fromJson(json, Parameter.BluetoothParameter::class.java)
            else -> throw JsonParseException("Unknown parameter_type: $type")
        }
    }
}
