# BackupViewModel.kt

## What it does
`BackupViewModel` is the engine behind the Backup module. it is responsible for discovering, fetching, and archiving various types of system and user data, including SMS/MMS, Call Logs, Contacts, APKs, and Files. It also handles the orchestration of the backup process, including creating ZIP or TAR archives.

## Why it does it
A comprehensive backup solution requires deep integration with Android's content providers and file system. This ViewModel:
- **Centralizes Data Collection:** Fetches data from disparate sources (Telephony provider, Contacts provider, Package Manager, MediaStore).
- **Manages Permissions:** Checks and handles necessary permissions for data access.
- **Handles Background Processing:** Executes resource-intensive data fetching and archive creation on background threads to keep the UI responsive.
- **Provides State:** Exposes a unified `BackupUiState` for the UI to observe.
- **Supports Multiple Formats:** Allows users to choose between ZIP, TAR, and GZ compressed TAR formats.

## How it does it
- **Data Fetching:**
    - Uses `ContentResolver` to query system databases for SMS (`Telephony.Sms.CONTENT_URI`), MMS, Call Logs (`CallLog.Calls.CONTENT_URI`), and Contacts.
    - Uses `PackageManager` to identify user-installed APKs and their source paths.
    - Scans common external storage directories (Downloads, Documents, etc.) recursively to find user files.
- **Special Handling:**
    - **MMS:** Extracts binary parts (attachments) and text parts from the complex MMS provider structure.
    - **Default SMS App:** Manages the logic for checking and requesting the app to become the default SMS handler (required for full message access on some Android versions).
- **Archiving Logic:**
    - Defines an `ArchiveHelper` interface with implementations for `ZipOutputStream` and Apache Commons Compress `TarArchiveOutputStream`.
    - Generates a `manifest.json` file during backup to index all included items, making restoration easier.
    - Progress Tracking: Updates `backupStatus` and `backupProgress` in the UI state as it iterates through files and data categories.
- **State Management:** Uses `MutableStateFlow` to manage selection states (per-item, per-category, or "Master" select-all).

## Overall role
It acts as the **Controller and Data Manager** for the Backup module. It sits between the Android system (Content Providers/File System) and the user interface, ensuring data is correctly collected, prepared, and packaged into a portable backup file.
