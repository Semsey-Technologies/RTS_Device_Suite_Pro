# OrganizerViewModel.kt

## What it does
Manages the UI state and background processing for the Smart Organizer module.

## Why it does it
It serves as the bridge between the UI (Compose screens) and the business logic (Repository, WorkManager, and Foreground Service). It ensures that rule changes are persisted and that background automation is updated accordingly.

## How it does it
- **rules StateFlow**: Exposes the list of rules from the repository, converting it to a `StateFlow` that the UI can observe.
- **addRule/deleteRule/toggleRule**: Dispatches data operations to the repository and triggers maintenance tasks like rescheduling work.
- **scheduleWork()**: Uses `WorkManager` to trigger an immediate, expedited run of the `FileOrganizerWorker`.
- **refreshMonitorService()**: Sends a "REFRESH" intent to the `FolderMonitorService` to ensure that any active folder watches are updated to reflect the latest rule configurations.
- **runRulesNow()**: Provides a manual override to trigger the organization logic immediately.

## Overall role
It coordinates the lifecycle of organization rules, ensuring that user interactions in the UI result in both persistent data changes and active background automation updates.
