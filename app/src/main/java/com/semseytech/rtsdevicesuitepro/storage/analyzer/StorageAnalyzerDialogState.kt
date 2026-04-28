package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.runtime.*

class StorageAnalyzerDialogState {
    var showFileOptions by mutableStateOf<FileInfo?>(null)
    var showRenameDialog by mutableStateOf<FileInfo?>(null)
    var showDetailsDialog by mutableStateOf<FileInfo?>(null)
    var showMoveCopyDialog by mutableStateOf<Pair<FileInfo, Boolean>?>(null)
    var showMultiMoveCopyDialog by mutableStateOf<Boolean?>(null)

    fun dismissAll() {
        showFileOptions = null
        showRenameDialog = null
        showDetailsDialog = null
        showMoveCopyDialog = null
        showMultiMoveCopyDialog = null
    }
}

@Composable
fun rememberStorageAnalyzerDialogState() = remember { StorageAnalyzerDialogState() }
