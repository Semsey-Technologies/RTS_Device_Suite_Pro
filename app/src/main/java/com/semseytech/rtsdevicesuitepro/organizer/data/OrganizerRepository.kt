package com.semseytech.rtsdevicesuitepro.organizer.data

import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerRule
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerOptions
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTrigger
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTriggerAdapter
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrganizerRepository(private val dao: OrganizerDao) {
    private val gson = GsonBuilder()
        .registerTypeAdapter(RuleTrigger::class.java, RuleTriggerAdapter())
        .create()

    val allRules: Flow<List<OrganizerRule>> = dao.getAllRules().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun insertRule(rule: OrganizerRule) {
        dao.insertRule(rule.toEntity())
    }

    suspend fun deleteRule(rule: OrganizerRule) {
        dao.deleteRule(rule.toEntity())
    }

    suspend fun updateRule(rule: OrganizerRule) {
        dao.updateRule(rule.toEntity())
    }

    private fun OrganizerRuleEntity.toDomain(): OrganizerRule {
        val paths = try {
            if (sourcePathsJson.startsWith("[")) {
                gson.fromJson(sourcePathsJson, Array<String>::class.java).toList()
            } else {
                listOf(sourcePathsJson)
            }
        } catch (e: Exception) {
            listOf(sourcePathsJson)
        }
        
        return OrganizerRule(
            id = id,
            name = name,
            sourcePaths = paths,
            targetPath = targetPath,
            fileTypes = gson.fromJson(fileTypesJson, Array<String>::class.java).toList(),
            trigger = gson.fromJson(triggerJson, RuleTrigger::class.java),
            options = gson.fromJson(optionsJson, OrganizerOptions::class.java),
            isEnabled = isEnabled
        )
    }

    private fun OrganizerRule.toEntity(): OrganizerRuleEntity {
        return OrganizerRuleEntity(
            id = id,
            name = name,
            sourcePathsJson = gson.toJson(sourcePaths),
            targetPath = targetPath,
            fileTypesJson = gson.toJson(fileTypes),
            triggerJson = gson.toJson(trigger),
            optionsJson = gson.toJson(options),
            isEnabled = isEnabled
        )
    }
}
