package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.net.Uri
import com.semseytech.rtsdevicesuitepro.ui.components.*

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
    val isSelected: Boolean = false,
    val author: String? = null,
    val tags: List<String> = emptyList()
)

enum class FileCategory {
    IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APKS, OTHERS
}

data class DisplaySettings(
    val sortOption: FileSortOption = FileSortOption.SIZE,
    val sortOrder: FileSortOrder = FileSortOrder.DESCENDING,
    val viewMode: FileViewMode = FileViewMode.LIST,
    val groupBy: FileGroupByOption = FileGroupByOption.NONE
)
