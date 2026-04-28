# CleanerViewModel.kt

## What it does
`CleanerViewModel` is the business logic controller for the Cleaner module. It handles the identification of junk files (duplicates, empty folders, residual data) and orchestrates a "Guided Cache Clearing" process for third-party applications.

## Why it does it
Android restricts apps from directly clearing the cache of other applications for security reasons. To overcome this while maintaining safety, this ViewModel:
- **Identifies Junk Categories:** Scans for various types of non-essential data that occupy storage.
- **Educates on Risks:** Provides detailed warnings about the consequences of clearing cache for specific app types (e.g., banking vs. social media).
- **Automates the "Manual" Process:** Provides a "Guided Cache" workflow that leads the user through system settings for each selected app, minimizing the number of clicks required.
- **Ensures Safety:** Defaults to not selecting sensitive apps and provides clear "Safety Guidance" to prevent accidental data loss.

## How it does it
- **Scanning Logic:**
    - Uses `PackageManager` to list installed non-system apps and estimates their cache impact.
    - Simulates/Implements scanning for file system junk like duplicates and empty folders.
- **Guided Workflow:**
    - Uses a state machine (`CleanerState`) to move from `IDLE` -> `CLEANING` -> `GUIDED_CACHE` -> `COMPLETED`.
    - Uses `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` to jump directly to the system settings page for a specific app.
- **Safety Heuristics:** `getWarningReason(packageName)` uses keyword matching (e.g., "bank", "auth", "game") to flag apps where clearing cache might be disruptive.
- **Progress Tracking:** Updates a `CleanupProgress` model to provide real-time feedback to the UI during the file deletion phase.

## Overall role
It acts as a **Storage Optimization Coordinator**. It manages the technical complexity of identifying junk and provides a structured, safe, and semi-automated way for users to reclaim storage space that would otherwise be inaccessible.
