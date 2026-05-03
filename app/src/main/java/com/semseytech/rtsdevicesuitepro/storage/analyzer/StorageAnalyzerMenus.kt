package com.semseytech.rtsdevicesuitepro.storage.analyzer

import androidx.compose.runtime.Composable
import com.semseytech.rtsdevicesuitepro.ui.components.FileDisplaySettings
import com.semseytech.rtsdevicesuitepro.ui.components.SharedFileMenus

@Composable
fun StorageAnalyzerMenus(
    showSortMenu: Boolean, onSortMenuToggle: (Boolean) -> Unit,
    showViewMenu: Boolean, onViewMenuToggle: (Boolean) -> Unit,
    showGroupMenu: Boolean, onGroupMenuToggle: (Boolean) -> Unit,
    displaySettings: FileDisplaySettings,
    onSettingsChanged: (FileDisplaySettings) -> Unit
) {
    SharedFileMenus(
        displaySettings = displaySettings,
        showSortMenu = showSortMenu,
        onSortMenuToggle = onSortMenuToggle,
        showViewMenu = showViewMenu,
        onViewMenuToggle = onViewMenuToggle,
        showGroupMenu = showGroupMenu,
        onGroupMenuToggle = onGroupMenuToggle,
        onSettingsChanged = onSettingsChanged
    )
}
