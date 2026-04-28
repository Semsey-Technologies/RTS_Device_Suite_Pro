# StorageAnalyzerTopBar.kt

## What it does
A specialized top app bar for the Storage Analyzer module, containing navigation, actions, and filtering menus.

## Why it does it
To provide quick access to view controls (Sort, Group, View Mode) and context-aware actions (Select All, Delete) in a standardized header.

## How it does it
- **Context-Aware UI**: Switches between a standard "Dashboard" header and a "Selection Mode" header (showing the count of selected files).
- **Dropdown Menus**: Houses the `StorageAnalyzerMenus` for configuring:
    - **Sorting**: By Size, Name, Date, etc.
    - **Grouping**: By Folder, Type, Month, etc.
    - **View Mode**: List view vs. different Grid sizes.
- **Actions**: Includes a refresh button to re-trigger the storage scan and a "Select All" toggle when in selection mode.

## Overall role
It provides the primary navigation and filtering controls for the analyzer, allowing users to customize how they browse their storage.
