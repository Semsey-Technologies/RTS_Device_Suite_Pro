package com.semseytech.rtsdevicesuitepro.recovery

import android.net.Uri
import com.semseytech.rtsdevicesuitepro.storage.analyzer.FileCategory

data class RecoverableItem(
    val name: String,
    val path: String,
    val size: Long,
    val category: FileCategory,
    val lastModified: Long = 0,
    val dateDeleted: Long? = null,
    val mimeType: String? = null,
    val uri: Uri? = null,
    val isSelected: Boolean = false,
    val sourceApp: String? = null,
    val recoverabilityScore: Float = 1.0f // 0.0 to 1.0
)

data class RecoveryCategory(
    val category: FileCategory,
    val items: List<RecoverableItem> = emptyList(),
    val isExpanded: Boolean = false
) {
    val totalSize: Long get() = items.sumOf { it.size }
    val count: Int get() = items.size
}

enum class RecoveryViewMode {
    LIST, GRID, TIMELINE, GROUPED
}

enum class RecoverySortOption {
    NAME, SIZE, DATE_CREATED, DATE_DELETED, FILE_TYPE, APP_SOURCE
}

data class RecoveryState(
    val categories: List<RecoveryCategory> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val itemsFound: Int = 0,
    val viewMode: RecoveryViewMode = RecoveryViewMode.LIST,
    val sortOption: RecoverySortOption = RecoverySortOption.DATE_DELETED,
    val isDescending: Boolean = true,
    val searchQuery: String = ""
)
