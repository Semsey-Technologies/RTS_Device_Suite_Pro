# StorageAnalyzerContent.kt

## What it does
Manages the scrollable content area of the Storage Analyzer, switching between the dashboard summary and detailed file lists.

## Why it does it
To provide a structured layout for storage information, handling different display modes (List, Grid) and collapsible sections.

## How it does it
- **LazyColumn**: Used as the primary scroll container for the dashboard.
- **Sectioned Layout**:
    - **Summary Section**: Shows the `StorageSummaryCard`.
    - **Categories Section**: Shows the `CategoryGrid`, which can be collapsed/expanded.
    - **Largest Files Section**: Displays a list of files, optionally grouped by headers (e.g., grouped by folder or date).
- **Sticky Headers**: Implements `stickyHeader` for group labels (like "A - E" or "July 2023") when grouping is enabled.
- **View Mode Switching**: Delegates to `StorageAnalyzerListContent` or `StorageAnalyzerGridContent` based on the user's view mode preference.

## Overall role
It is the layout manager for the analyzer's main data view, responsible for orchestrating the display of categories and file lists.
