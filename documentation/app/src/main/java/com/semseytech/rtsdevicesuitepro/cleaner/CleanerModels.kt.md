# CleanerModels.kt

## What it does
`CleanerModels.kt` defines the data structures and state enumerations specific to the Cleaner & Maintenance module. It captures the concepts of junk categories, individual file items, app-specific cache information, and the overall progress of a cleanup operation.

## Why it does it
A cleanup tool needs to handle diverse data types (files vs. apps) and track complex multi-step processes. This file provides:
- **Consistency:** Ensures the UI and ViewModel use the same definitions for "Cleanup Progress" and "Cleanup Results".
- **Categorization:** Allows the app to group different types of "junk" (duplicates, logs, temp files) with their own metadata (icons, descriptions).
- **Safety Metadata:** Stores warning reasons and app type definitions to help the user understand the risks of cleaning.
- **State Management:** Uses the `CleanerState` enum to strictly control the transitions between scanning, cleaning, and completion.

## How it does it
- **`CleanupCategory`:** A container for grouped junk items, holding UI metadata (icon, name) and state (isSelected, isExpanded).
- **`AppCacheInfo`:** Specifically designed for the Guided Cache feature, storing the package name, app name, and specialized safety warnings.
- **`CleanerState` (Enum):** Defines the lifecycle of a cleanup session:
    - `IDLE`: Initial state.
    - `SCANNING`: Currently looking for junk.
    - `READY_TO_CLEAN`: Scan finished, waiting for user confirmation.
    - `CLEANING`: Deleting files.
    - `GUIDED_CACHE`: Leading user through system settings for app caches.
    - `COMPLETED`: Showing summary.
- **`CleanupResult` / `CleanupProgress`:** Simple data classes for reporting metrics (bytes cleaned, items processed) to the user.

## Overall role
It defines the **Domain and State Language** for the Cleaner module, enabling the ViewModel to accurately report its progress and results to the UI.
