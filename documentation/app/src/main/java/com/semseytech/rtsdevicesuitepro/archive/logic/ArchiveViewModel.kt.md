# ArchiveViewModel.kt

## What it does
`ArchiveViewModel` manages the state and business logic for the Archive Engine screen. It handles file browsing, sorting, selection, and triggers archive operations (creation, extraction, deletion).

## Why it does it
It separates the UI concerns from the archive logic. It provides a stable, observable state for the `ArchiveScreen` to react to, ensuring that long-running operations (like creating a ZIP) don't block the UI thread and that the file list remains synchronized with the file system.

## How it does it
1.  **State Management**: Uses Compose `mutableStateOf` and `mutableStateListOf` to track the current directory, file list, selected files, and UI dialog states.
2.  **Concurrency**: Uses `viewModelScope.launch(Dispatchers.IO)` to perform file system operations and archive tasks off the main thread.
3.  **File Navigation**: Functions like `navigateTo` and `navigateUp` update the `currentDirectory` and trigger a refresh.
4.  **Sorting and Grouping**: Implements logic to sort files by name, type, size, or date, and ensures directories are consistently grouped.
5.  **Archive Orchestration**: Delegates the actual archive creation and extraction to the `ArchiveManager` singleton, while managing the surrounding UI states (loading indicators, error messages, password prompts).
6.  **File Operations**: Provides wrapper methods for standard file operations like recursive copy, move, and delete for the selected files.

## What part it plays overall
It acts as the "State Holder" for the Archive module. It mediates between the `ArchiveScreen` (the view) and `ArchiveManager` (the model/service), holding all the temporary UI state needed to perform complex file management tasks.
