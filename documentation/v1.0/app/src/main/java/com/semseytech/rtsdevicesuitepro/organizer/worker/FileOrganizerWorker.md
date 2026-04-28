# FileOrganizerWorker.kt

## What it does
The core execution engine of the Smart Organizer, responsible for actually moving and processing files based on user-defined rules.

## Why it does it
To perform potentially long-running file operations in the background without blocking the UI, utilizing Android's `WorkManager` for reliable execution.

## How it does it
- **CoroutineWorker**: Inherits from `CoroutineWorker` to support asynchronous file operations.
- **doWork()**: Fetches all enabled rules from the database and processes them sequentially.
- **processRule()**: Scans the source directory and evaluates each file against the rule's criteria (file extensions or categories like "audio", "video").
- **shouldMove()**: Logic to determine if a file/folder matches the rule, including options to ignore subfolders or move entire directory trees.
- **moveItem()**: Handles the transfer of files, including a fallback from `renameTo()` to manual `copyAndDelete()` if moving across file systems. It also triggers the archive extraction logic if enabled.

## Overall role
It is the background processor that implements the automation logic, transforming the user's rules into actual file system changes.
