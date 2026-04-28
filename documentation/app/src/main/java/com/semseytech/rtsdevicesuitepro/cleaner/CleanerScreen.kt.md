# CleanerScreen.kt

## What it does
`CleanerScreen` is the primary interface for the Cleaner & Maintenance module. It presents a "dashboard" of potential storage-reclaiming actions, provides detailed safety information, and displays immersive animations and progress indicators during the cleaning process.

## Why it does it
The Cleaner module handles potentially destructive actions (deleting files), so the UI must be:
- **Transparent:** Clearly showing exactly what is being deleted and why.
- **Safe:** Emphasizing safety guidance and warnings for sensitive app caches.
- **Engaging:** Using circular progress indicators and "step-by-step" instructions for guided tasks to keep the user informed.
- **Themed:** Integrating with the app's `ThemeManager` for consistent visual styling (scaling, accent colors).

## How it does it
- **Adaptive Content:** Uses a `when(state)` block to switch between different UI layouts:
    - **`CleanerSetupContent`:** A selectable list of junk categories and an app cache section.
    - **`CleaningProgressContent`:** An immersive view with a large circular progress indicator.
    - **`GuidedCacheContent`:** A wizard-like interface with numbered steps (1. Click Storage, 2. Clear Cache, etc.).
    - **`CleanupSummaryContent`:** A "success" screen showing total space reclaimed.
- **Interactive Components:**
    - **`CleanupCategoryCard`:** An expandable card that reveals individual files/items within a category.
    - **`CacheSafetyGuidanceCard`:** A specialized info-hub that explains the technical impact of clearing cache for different app types (Banking, Games, etc.).
    - **`AppCacheItem`:** Displays app icons and names with a yellow warning icon if the app is sensitive.
- **Animation:** Uses `AnimatedVisibility` for smooth transitions when expanding categories or showing warnings.

## Overall role
It acts as the **View layer** for the Cleaner module, translating the background scanning and deletion logic of the `CleanerViewModel` into a safe, informative, and visually rich user experience.
