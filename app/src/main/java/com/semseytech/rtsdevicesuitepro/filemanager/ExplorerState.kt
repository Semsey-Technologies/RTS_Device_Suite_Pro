package com.semseytech.rtsdevicesuitepro.filemanager

data class PaneState(
    val currentPath: String = "/storage/emulated/0",
    val items: List<ExplorerFileItem> = emptyList(),
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val history: List<String> = listOf("/storage/emulated/0"),
    val historyIndex: Int = 0
)

data class FileExplorerState(
    val leftPane: PaneState = PaneState(),
    val rightPane: PaneState = PaneState(
        currentPath = "/storage/emulated/0",
        history = listOf("/storage/emulated/0")
    ),
    val isSplitScreen: Boolean = false,
    val isSystemAccessEnabled: Boolean = false,
    val isScanningNetwork: Boolean = false,
    val networkNodes: List<NetworkNode> = emptyList()
)

data class NetworkNode(
    val name: String,
    val ip: String,
    val type: String
)

data class ExplorerFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
)

data class ClipboardItem(
    val item: ExplorerFileItem,
    val isMove: Boolean
)
