package com.semseytech.rtsdevicesuitepro.automation.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.semseytech.rtsdevicesuitepro.automation.models.*

object AutomationGson {
    val instance: Gson = GsonBuilder()
        .registerTypeHierarchyAdapter(Trigger::class.java, AutomationTypeAdapter<Trigger>())
        .registerTypeHierarchyAdapter(Condition::class.java, AutomationTypeAdapter<Condition>())
        .registerTypeHierarchyAdapter(Action::class.java, AutomationTypeAdapter<Action>())
        .registerTypeAdapter(Parameter::class.java, ParameterAdapter())
        .create()
}
