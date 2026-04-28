package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.runtime.Composable

@Composable
fun StorageAnalyzerDialogWrapper(
    dialogState: StorageAnalyzerDialogState,
    viewModel: StorageAnalyzerViewModel
) {
    StorageAnalyzerDialogContainers(
        showFileOptions = dialogState.showFileOptions,
        showRenameDialog = dialogState.showRenameDialog,
        showDetailsDialog = dialogState.showDetailsDialog,
        showMoveCopyDialog = dialogState.showMoveCopyDialog,
        showMultiMoveCopyDialog = dialogState.showMultiMoveCopyDialog,
        onDismissFileOptions = { dialogState.showFileOptions = null },
        onDismissRename = { dialogState.showRenameDialog = null },
        onDismissDetails = { dialogState.showDetailsDialog = null },
        onDismissMoveCopy = { dialogState.showMoveCopyDialog = null },
        onDismissMultiMoveCopy = { dialogState.showMultiMoveCopyDialog = null },
        onFileAction = { file, action ->
            when (action) {
                "open" -> viewModel.openFile(file)
                "share" -> viewModel.shareFile(file)
                "delete" -> viewModel.deleteFile(file)
                "rename" -> dialogState.showRenameDialog = file
                "details" -> dialogState.showDetailsDialog = file
                "move" -> dialogState.showMoveCopyDialog = file to true
                "copy" -> dialogState.showMoveCopyDialog = file to false
            }
        },
        onRenameConfirm = { file, newName -> viewModel.renameFile(file, newName) },
        onMoveCopyConfirm = { file, dest, isMove ->
            if (isMove) viewModel.moveFile(file, dest)
            else viewModel.copyFile(file, dest)
        },
        onMultiMoveCopyConfirm = { dest, isMove ->
            if (isMove) viewModel.moveSelectedFiles(dest)
            else viewModel.copySelectedFiles(dest)
        }
    )
}
