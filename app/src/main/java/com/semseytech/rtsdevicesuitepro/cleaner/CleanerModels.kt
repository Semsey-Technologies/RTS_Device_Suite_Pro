package com.semseytech.rtsdevicesuitepro.cleaner

import androidx.compose.ui.graphics.vector.ImageVector
import com.semseytech.rtsdevicesuitepro.ui.components.*

data class CleanupCategory(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    var isSelected: Boolean = true,
    var count: Int = 0,
    var sizeBytes: Long = 0,
    val isAppCache: Boolean = false,
    val items: List<CleanupItem> = emptyList(),
    var isExpanded: Boolean = false
)

data class CleanupItem(
    val id: String,
    val path: String,
    val name: String,
    val sizeBytes: Long = 0,
    var isSelected: Boolean = true,
    val extraInfo: String? = null
)

data class AppTypeInfo(
    val typeName: String,
    val description: String,
    val consequences: String
)

data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val cacheSize: Long,
    val iconDrawable: android.graphics.drawable.Drawable?,
    val warningReason: String? = null,
    var isSelected: Boolean = false, // Default to false
    val appType: String? = null
)

data class CleanupResult(
    val cleanedSizeBytes: Long = 0,
    val duplicatesRemoved: Int = 0,
    val foldersCleared: Int = 0,
    val appsProcessed: Int = 0,
    val failedActions: List<String> = emptyList()
)

data class CleanupProgress(
    val currentItemName: String = "",
    val progress: Float = 0f,
    val itemsProcessed: Int = 0,
    val totalItems: Int = 0
)

enum class CleanerState {
    IDLE, SCANNING, READY_TO_CLEAN, CLEANING, GUIDED_CACHE, COMPLETED
}
