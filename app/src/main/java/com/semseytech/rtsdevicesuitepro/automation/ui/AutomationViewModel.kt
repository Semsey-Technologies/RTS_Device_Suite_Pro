package com.semseytech.rtsdevicesuitepro.automation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.data.RuleEntity
import com.semseytech.rtsdevicesuitepro.automation.data.RuleGroup
import com.semseytech.rtsdevicesuitepro.automation.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AutomationDatabase.getDatabase(application)
    private val dao = db.automationDao()

    private val _rules = MutableStateFlow<List<RuleEntity>>(emptyList())
    val rules = _rules.asStateFlow()

    private val _groups = MutableStateFlow<List<RuleGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    init {
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch {
            _rules.value = dao.getAllRules()
            _groups.value = dao.getAllGroups()
        }
    }

    fun addRule(name: String, trigger: Trigger, conditions: List<Condition>, actions: List<Action>, groupId: String? = null) {
        viewModelScope.launch {
            val rule = RuleEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                trigger = trigger,
                conditions = conditions,
                actions = actions,
                isEnabled = true,
                groupId = groupId
            )
            dao.insertRule(rule)
            refreshData()
        }
    }

    fun addGroup(name: String) {
        viewModelScope.launch {
            val group = RuleGroup(id = UUID.randomUUID().toString(), name = name)
            dao.insertGroup(group)
            refreshData()
        }
    }

    fun deleteGroup(group: RuleGroup) {
        viewModelScope.launch {
            dao.deleteGroup(group)
            refreshData()
        }
    }

    fun moveRuleToGroup(ruleId: String, groupId: String?) {
        viewModelScope.launch {
            dao.updateRuleGroup(ruleId, groupId)
            refreshData()
        }
    }

    fun toggleRule(id: String, enabled: Boolean) {
        viewModelScope.launch {
            dao.setRuleEnabled(id, enabled)
            refreshData()
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            dao.deleteRule(rule)
            refreshData()
        }
    }
}
