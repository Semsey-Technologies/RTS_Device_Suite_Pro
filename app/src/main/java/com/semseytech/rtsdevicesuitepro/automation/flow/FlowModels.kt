package com.semseytech.rtsdevicesuitepro.automation.flow

import androidx.compose.ui.geometry.Offset
import com.semseytech.rtsdevicesuitepro.automation.models.AutomationComponent
import java.util.UUID

data class FlowGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Flow",
    val nodes: List<FlowNode> = emptyList(),
    val connections: List<FlowConnection> = emptyList()
)

data class FlowNode(
    val id: String = UUID.randomUUID().toString(),
    val component: AutomationComponent,
    val position: Offset,
    val size: Offset = Offset(200f, 100f), // Default size
    val executionStatus: NodeStatus = NodeStatus.IDLE
)

enum class NodeStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILURE,
    STOPPED
}

data class FlowConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val connectionType: ConnectionType = ConnectionType.DEFAULT
)

enum class ConnectionType {
    DEFAULT,
    TRUE,
    FALSE
}
