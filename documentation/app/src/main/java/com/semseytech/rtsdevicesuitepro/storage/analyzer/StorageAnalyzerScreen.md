# StorageAnalyzerScreen.kt

## What it does
The high-level Compose screen component for the Storage Analyzer module.

## Why it does it
To provide a cohesive and interactive dashboard where users can visualize storage usage, browse large files, and manage data across different categories. It orchestrates sub-components like the top bar, main content area, and various dialogs.

## How it does it
- **State Collection**: Collects state from the `StorageAnalyzerViewModel`, including `uiState`, `isSelectionMode`, and `displaySettingsMap`.
- **Sorting/Grouping Logic**: Uses `remember` and `LaunchedEffect` to efficiently compute sorted and grouped lists of "Largest Files" whenever the underlying data or settings change.
- **Scaffold Integration**:
    - **Top Bar**: Displays the `StorageAnalyzerTopBar` which contains menus for sorting, grouping, and view modes.
    - **Floating Action Button**: Shows a specialized "Delete" FAB only when files are selected.
- **Content Delegation**: Passes processed data and event callbacks (like `onFileClick`) down to `StorageAnalyzerContent`.
- **Dialog Management**: Integrates the `StorageAnalyzerDialogWrapper` to handle context menus, file details, and confirmation prompts.

## Overall role
It serves as the main entry point and coordinator for the Storage Analyzer's user interface, managing the layout and high-level interaction flow.
