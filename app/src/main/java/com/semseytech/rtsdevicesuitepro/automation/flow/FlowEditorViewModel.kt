package com.semseytech.rtsdevicesuitepro.automation.flow

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationGson
import com.semseytech.rtsdevicesuitepro.automation.data.FlowGraphEntity
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class FlowEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AutomationDatabase.getDatabase(application)
    private val dao = db.automationDao()
    private val gson = AutomationGson.instance

    private val _nodes = mutableStateListOf<FlowNode>()
    val nodes: List<FlowNode> get() = _nodes

    private val _connections = mutableStateListOf<FlowConnection>()
    val connections: List<FlowConnection> get() = _connections

    var flowName = mutableStateOf("Untitled Flow")
    var flowId = mutableStateOf(java.util.UUID.randomUUID().toString())
    
    private val executionEngine = FlowExecutionEngine(application)
    var isRunning = mutableStateOf(false)

    var scale = mutableStateOf(1f)
    var offset = mutableStateOf(Offset.Zero)

    fun loadFlow(id: String) {
        viewModelScope.launch {
            val flows = dao.getAllFlows()
            val entity = flows.find { it.id == id }
            if (entity != null) {
                flowId.value = entity.id
                flowName.value = entity.name
                
                try {
                    val nodeType = object : TypeToken<List<FlowNode>>() {}.type
                    val loadedNodes: List<FlowNode> = gson.fromJson(entity.nodesJson, nodeType)
                    _nodes.clear()
                    _nodes.addAll(loadedNodes)
                    
                    val connType = object : TypeToken<List<FlowConnection>>() {}.type
                    val loadedConns: List<FlowConnection> = gson.fromJson(entity.connectionsJson, connType)
                    _connections.clear()
                    _connections.addAll(loadedConns)
                    
                    // Center the view on nodes
                    if (loadedNodes.isNotEmpty()) {
                        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
                        loadedNodes.forEach {
                            minX = minOf(minX, it.position.x)
                            minY = minOf(minY, it.position.y)
                        }
                        offset.value = Offset(-minX + 100f, -minY + 100f)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FlowEditorVM", "Failed to load flow: ${e.message}")
                }
            }
        }
    }

    fun saveFlow(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val entity = FlowGraphEntity(
                id = flowId.value,
                name = flowName.value,
                nodesJson = gson.toJson(_nodes.toList()),
                connectionsJson = gson.toJson(_connections.toList())
            )
            dao.insertFlow(entity)
            onComplete()
        }
    }

    fun addNode(component: AutomationComponent, position: Offset) {
        _nodes.add(FlowNode(component = component, position = position))
    }

    fun updateNodePosition(nodeId: String, delta: Offset) {
        val index = _nodes.indexOfFirst { it.id == nodeId }
        if (index != -1) {
            val node = _nodes[index]
            _nodes[index] = node.copy(position = node.position + delta)
        }
    }

    fun addConnection(fromId: String, toId: String, type: ConnectionType = ConnectionType.DEFAULT) {
        // Prevent duplicate connections
        if (_connections.none { it.fromNodeId == fromId && it.toNodeId == toId }) {
            _connections.add(FlowConnection(fromNodeId = fromId, toNodeId = toId, connectionType = type))
        }
    }

    fun removeNode(nodeId: String) {
        _nodes.removeIf { it.id == nodeId }
        _connections.removeIf { it.fromNodeId == nodeId || it.toNodeId == nodeId }
    }

    fun removeConnection(connectionId: String) {
        _connections.removeIf { it.id == connectionId }
    }

    fun updateNodeComponent(nodeId: String, updatedComponent: AutomationComponent) {
        val index = _nodes.indexOfFirst { it.id == nodeId }
        if (index != -1) {
            _nodes[index] = _nodes[index].copy(component = updatedComponent)
        }
    }

    fun runManual() {
        val startNode = _nodes.find { it.component is Trigger } ?: _nodes.firstOrNull() ?: return
        isRunning.value = true
        _nodes.indices.forEach { i -> _nodes[i] = _nodes[i].copy(executionStatus = NodeStatus.IDLE) }

        executionEngine.runFlow(
            FlowGraph(name = flowName.value, nodes = _nodes.toList(), connections = _connections.toList()),
            startNode,
            object : FlowExecutionEngine.FlowExecutionListener {
                override fun onNodeStarted(nodeId: String) {
                    updateNodeStatus(nodeId, NodeStatus.RUNNING)
                }

                override fun onNodeFinished(nodeId: String, success: Boolean) {
                    updateNodeStatus(nodeId, if (success) NodeStatus.SUCCESS else NodeStatus.FAILURE)
                }

                override fun onFlowFinished() {
                    isRunning.value = false
                }
            }
        )
    }

    fun stopManual() {
        executionEngine.stopFlow()
        isRunning.value = false
        _nodes.indices.forEach { i ->
            if (_nodes[i].executionStatus == NodeStatus.RUNNING) {
                _nodes[i] = _nodes[i].copy(executionStatus = NodeStatus.STOPPED)
            }
        }
    }

    private fun updateNodeStatus(nodeId: String, status: NodeStatus) {
        val index = _nodes.indexOfFirst { it.id == nodeId }
        if (index != -1) {
            _nodes[index] = _nodes[index].copy(executionStatus = status)
        }
    }
}
