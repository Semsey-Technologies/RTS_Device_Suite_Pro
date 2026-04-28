# FolderMonitorService.kt

## What it does
A foreground service that monitors specific folders for real-time changes (file creation or movement).

## Why it does it
To support the "On Folder Modified" trigger in the Smart Organizer. Unlike interval-based polling, this service provides immediate reaction to file system events.

## How it does it
- **Foreground Service**: Runs with a notification to prevent being killed by the system, ensuring persistent monitoring.
- **FileObserver**: Utilizes the Android `FileObserver` API to watch for `CREATE` and `MOVED_TO` events in the configured source paths.
- **refreshObservers()**: Dynamically updates the list of active observers whenever rules are added, removed, or toggled in the UI.
- **triggerWorker()**: When a file event is detected, it enqueues an expedited `FileOrganizerWorker` task to process the rules immediately.

## Overall role
It provides the "event-driven" component of the Smart Organizer, ensuring that automation rules can react instantly to new files arriving in watched directories.
