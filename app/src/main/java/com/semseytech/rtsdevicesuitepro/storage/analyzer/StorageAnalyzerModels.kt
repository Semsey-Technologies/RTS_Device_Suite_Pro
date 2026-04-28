package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.net.Uri

data class StorageStats(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0,
    val categoryStats: Map<FileCategory, CategoryInfo> = emptyMap(),
    val largestFiles: List<FileInfo> = emptyList(),
    val isScanning: Boolean = false,
    val scanProgress: String = ""
)

data class CategoryInfo(
    val count: Int = 0,
    val totalSize: Long = 0,
    val files: List<FileInfo> = emptyList()
)

data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val category: FileCategory,
    val lastModified: Long = 0,
    val mimeType: String? = null,
    val uri: Uri? = null,
    val isSelected: Boolean = false
)

enum class FileCategory {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APKS, OTHERS
}

enum class SortOption {
    NAME, SIZE, DATE_MODIFIED, DATE_CREATED, TYPE, AUTHORS, CATEGORIES, TAGS, TITLE
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

enum class GroupByOption {
    NONE, NAME, DATE_MODIFIED, DATE_CREATED, FOLDER, TYPE, AUTHOR, TAG, CATEGORY, SIZE
}

enum class ViewMode {
    LIST, GRID_SMALL, GRID_MEDIUM, GRID_LARGE
}

data class DisplaySettings(
    val sortOption: SortOption = SortOption.SIZE,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val viewMode: ViewMode = ViewMode.LIST,
    val groupBy: GroupByOption = GroupByOption.NONE
)
