# StorageAnalyzerViewModel.kt

## What it does
Orchestrates the UI state, user interactions, and background operations for the Storage Analyzer module.

## Why it does it
To manage complex UI logic like multi-file selection, dynamic sorting/grouping across different scopes (e.g., Dashboard vs. Category views), and triggering background storage scans.

## How it does it
- **uiState (StateFlow)**: Exposes the `StorageStats` (progress, scan results) to the UI.
- **Selection Management**: Handles entering/exiting selection mode and tracking `selectedFiles` in a `Set<String>`.
- **Scope-based Settings**: Uses a `displaySettingsMap` to maintain independent sorting and view preferences for different views (e.g., you can sort Images by Date but Videos by Size).
- **Sorting & Grouping**: Contains the logic to sort file lists based on `SortOption` and group them into map buckets (e.g., grouping by "Month" or "Size Range").
- **Action Dispatching**: Bridges UI clicks (Delete, Move, Rename) to the `StorageAnalyzerRepository`, followed by a state refresh (`runFullStorageScan`).

## Overall role
It serves as the central logic controller for the storage analysis experience, ensuring the UI remains responsive and synchronized with the underlying file system data.
