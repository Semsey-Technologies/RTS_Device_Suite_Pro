package com.semseytech.rtsdevicesuitepro.automation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.data.RuleEntity
import com.semseytech.rtsdevicesuitepro.automation.data.RuleGroup
import com.semseytech.rtsdevicesuitepro.automation.data.FlowGraphEntity
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationService
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

    private val _flows = MutableStateFlow<List<FlowGraphEntity>>(emptyList())
    val flows = _flows.asStateFlow()

    private val _runningRules = MutableStateFlow<Set<String>>(emptySet())
    val runningRules = _runningRules.asStateFlow()

    private var engine: com.semseytech.rtsdevicesuitepro.automation.engine.AutomationEngine? = null

    init {
        refreshData()
    }

    fun setEngine(engine: com.semseytech.rtsdevicesuitepro.automation.engine.AutomationEngine) {
        this.engine = engine
    }

    fun runRule(rule: RuleEntity) {
        viewModelScope.launch {
            engine?.runRule(rule)
            _runningRules.value = _runningRules.value + rule.id
            // In a real app, you might want to observe the engine for job completion
            // For now, we'll just keep it in "running" state until stopped or refreshed
        }
    }

    fun stopRule(ruleId: String) {
        viewModelScope.launch {
            engine?.stopRule(ruleId)
            _runningRules.value = _runningRules.value - ruleId
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _rules.value = dao.getAllRules()
            _groups.value = dao.getAllGroups()
            _flows.value = dao.getAllFlows()
        }
    }

    fun deleteFlow(flow: FlowGraphEntity) {
        viewModelScope.launch {
            dao.deleteFlow(flow)
            refreshData()
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
            AutomationService.requestRefresh()
        }
    }

    fun updateRule(rule: RuleEntity) {
        viewModelScope.launch {
            dao.insertRule(rule)
            refreshData()
            AutomationService.requestRefresh()
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
            AutomationService.requestRefresh()
        }
    }

    fun toggleRule(id: String, enabled: Boolean) {
        viewModelScope.launch {
            dao.setRuleEnabled(id, enabled)
            refreshData()
            AutomationService.requestRefresh()
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            dao.deleteRule(rule)
            refreshData()
            AutomationService.requestRefresh()
        }
    }
}
