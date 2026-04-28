# DashboardViewModel.kt

## What it does
Manages the state for the main dashboard screen, providing a summary of the device's status and recent activities.

## Why it does it
To provide a reactive data source for the application's entry point, summarizing complex state (storage usage, backup history, settings) into a simplified format for display.

## How it does it
- **DashboardUiState**: A data class containing the metrics shown on the home screen, such as the `lastBackupDate` and `storageUsedPercent`.
- **StateFlow**: Uses `MutableStateFlow` to hold and expose the state, allowing the UI to react instantly to updates.

## Overall role
It coordinates the "big picture" data for the user, providing a quick glance at the device suite's current status.
