package com.semseytech.rtsdevicesuitepro.ui.components

enum class FileSortOption {
    NAME, SIZE, TYPE, DATE, DATE_MODIFIED, DATE_CREATED, AUTHORS, CATEGORIES, TAGS, TITLE
}

enum class FileSortOrder {
    ASCENDING, DESCENDING
}

enum class FileGroupByOption {
    NONE, NAME, DATE, DATE_MODIFIED, DATE_CREATED, FOLDER, TYPE, AUTHOR, TAG, CATEGORY, SIZE, SIZE_RANGE
}

enum class FileViewMode {
    LIST, DETAILS, GRID_SMALL, GRID_MEDIUM, GRID_LARGE
}

data class FileDisplaySettings(
    val sortOption: FileSortOption = FileSortOption.SIZE,
    val sortOrder: FileSortOrder = FileSortOrder.DESCENDING,
    val viewMode: FileViewMode = FileViewMode.LIST,
    val groupBy: FileGroupByOption = FileGroupByOption.NONE
)
