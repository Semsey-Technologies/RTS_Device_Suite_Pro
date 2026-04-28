# StorageAnalyzerUI.kt

## What it does
Provides UI components and layout logic for the Storage Analyzer's dashboard and category views.

## Why it does it
To provide a modular and reusable set of UI elements (like storage cards, category grids, and file lists) that are consistent across the analysis module.

## How it does it
- **StorageSummaryCard**: Displays the overall storage usage (Used/Total) with a progress bar.
- **CategoryGrid**: Shows a grid of `CategoryCard` components (Images, Videos, etc.) with their respective counts and sizes.
- **FileInfoItem**: Renders a single file entry with its icon (based on MIME type), name, size, and date. It handles long-press for selection mode.
- **EmptyState**: A visually informative view shown when a category has no files or the scan hasn't run.

## Overall role
It contains the visual "building blocks" of the storage analyzer, defining how metrics and file metadata are presented to the user.
