package com.semseytech.rtsdevicesuitepro.automation.flow

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.semseytech.rtsdevicesuitepro.automation.models.*

class FlowEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val _nodes = mutableStateListOf<FlowNode>()
    val nodes: List<FlowNode> get() = _nodes

    private val _connections = mutableStateListOf<FlowConnection>()
    val connections: List<FlowConnection> get() = _connections

    var flowName = mutableStateOf("Untitled Flow")
    
    private val executionEngine = FlowExecutionEngine(application)
    var isRunning = mutableStateOf(false)

    var scale = mutableStateOf(1f)
    var offset = mutableStateOf(Offset.Zero)

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
