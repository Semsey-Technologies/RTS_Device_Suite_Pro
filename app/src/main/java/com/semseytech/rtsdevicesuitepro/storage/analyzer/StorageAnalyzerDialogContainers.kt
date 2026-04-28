package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.runtime.Composable

@Composable
fun StorageAnalyzerDialogContainers(
    showFileOptions: FileInfo?,
    showRenameDialog: FileInfo?,
    showDetailsDialog: FileInfo?,
    showMoveCopyDialog: Pair<FileInfo, Boolean>?,
    showMultiMoveCopyDialog: Boolean?,
    onDismissFileOptions: () -> Unit,
    onDismissRename: () -> Unit,
    onDismissDetails: () -> Unit,
    onDismissMoveCopy: () -> Unit,
    onDismissMultiMoveCopy: () -> Unit,
    onFileAction: (FileInfo, String) -> Unit,
    onRenameConfirm: (FileInfo, String) -> Unit,
    onMoveCopyConfirm: (FileInfo, String, Boolean) -> Unit,
    onMultiMoveCopyConfirm: (String, Boolean) -> Unit
) {
    if (showFileOptions != null) {
        FileOptionsBottomSheet(
            file = showFileOptions,
            onDismiss = onDismissFileOptions,
            onAction = { action ->
                onFileAction(showFileOptions, action)
                onDismissFileOptions()
            }
        )
    }

    if (showMoveCopyDialog != null) {
        val (file, isMove) = showMoveCopyDialog
        DestinationDialog(
            title = if (isMove) "MOVE TO" else "COPY TO",
            onDismiss = onDismissMoveCopy,
            onConfirm = { dest ->
                onMoveCopyConfirm(file, dest, isMove)
                onDismissMoveCopy()
            }
        )
    }

    if (showMultiMoveCopyDialog != null) {
        val isMove = showMultiMoveCopyDialog
        DestinationDialog(
            title = if (isMove) "MOVE SELECTED TO" else "COPY SELECTED TO",
            onDismiss = onDismissMultiMoveCopy,
            onConfirm = { dest ->
                onMultiMoveCopyConfirm(dest, isMove)
                onDismissMultiMoveCopy()
            }
        )
    }

    if (showRenameDialog != null) {
        RenameDialog(
            file = showRenameDialog,
            onDismiss = onDismissRename,
            onConfirm = { newName ->
                onRenameConfirm(showRenameDialog, newName)
                onDismissRename()
            }
        )
    }

    if (showDetailsDialog != null) {
        DetailsDialog(
            file = showDetailsDialog,
            onDismiss = onDismissDetails
        )
    }
}
