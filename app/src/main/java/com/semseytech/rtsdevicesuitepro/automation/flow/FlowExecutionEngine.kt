package com.semseytech.rtsdevicesuitepro.automation.flow

import android.content.Context
import android.util.Log
import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationEngine
import com.semseytech.rtsdevicesuitepro.automation.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.isActive

class FlowExecutionEngine(private val context: Context) {
    private val engine = AutomationEngine(context)
    private var executionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    interface FlowExecutionListener {
        fun onNodeStarted(nodeId: String)
        fun onNodeFinished(nodeId: String, success: Boolean)
        fun onFlowFinished()
    }

    fun runFlow(graph: FlowGraph, startNode: FlowNode, listener: FlowExecutionListener? = null) {
        executionJob?.cancel()
        executionJob = scope.launch {
            executeNode(graph, startNode, listener)
            listener?.onFlowFinished()
        }
    }

    fun stopFlow() {
        executionJob?.cancel()
        executionJob = null
    }

    private suspend fun executeNode(graph: FlowGraph, node: FlowNode, listener: FlowExecutionListener?) {
        if (!currentCoroutineContext().isActive) return
        listener?.onNodeStarted(node.id)
// ... rest of method ...
        Log.d("FlowExecution", "Executing node: ${node.component.displayName}")

        var success = true
        var resultType: ConnectionType = ConnectionType.DEFAULT

        when (val component = node.component) {
            is Action -> {
                executeAction(component)
                resultType = ConnectionType.DEFAULT
            }
            is Condition -> {
                success = evaluateCondition(component)
                resultType = if (success) ConnectionType.TRUE else ConnectionType.FALSE
            }
            is Trigger -> {
                // For manual run, triggers are always "successful" starters
                success = true
                resultType = ConnectionType.DEFAULT
            }
        }

        listener?.onNodeFinished(node.id, success)

        // Find next nodes based on connection type
        val nextConnections = graph.connections.filter { 
            it.fromNodeId == node.id && (it.connectionType == resultType || it.connectionType == ConnectionType.DEFAULT)
        }

        nextConnections.forEach { conn ->
            val nextNode = graph.nodes.find { it.id == conn.toNodeId }
            if (nextNode != null) {
                executeNode(graph, nextNode, listener)
            }
        }
    }

    private suspend fun executeAction(action: Action) {
        // We reuse the logic from AutomationEngine by exposing it or duplicating it for now
        // Ideally, AutomationEngine should have a single action executor method.
        // For this demo, I'll use a simplified version.
        Log.d("FlowExecution", "Executing Action: ${action.displayName}")
        if (action is Action.Delay) {
            delay(action.seconds * 1000L)
        }
        // In a real app, I'd refactor AutomationEngine to make this cleaner.
    }

    private fun evaluateCondition(condition: Condition): Boolean {
        Log.d("FlowExecution", "Evaluating Condition: ${condition.displayName}")
        // Simplified evaluation
        return true 
    }
}
