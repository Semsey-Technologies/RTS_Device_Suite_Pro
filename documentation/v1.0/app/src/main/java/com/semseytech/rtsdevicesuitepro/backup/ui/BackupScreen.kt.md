# BackupScreen.kt

## What it does
`BackupScreen` is the user interface for the Backup module. It presents a categorized list of everything that can be backed up, allows the user to select specific items or categories, and triggers the backup process. It also handles permission requests and interaction with the Android system's file picker to save the final backup file.

## Why it does it
The backup process is complex, so the UI needs to make it approachable:
- **Categorization:** Breaks down hundreds of items into manageable sections (SMS, Calls, APKs).
- **Transparency:** Provides a "Master" switch for convenience but allows granular control over every individual item.
- **Permission Management:** Transparently handles the multiple Android permissions needed for such deep system access.
- **Process Feedback:** Shows progress bars and status messages during the intensive backup operation.
- **System Integration:** Uses modern Android "Storage Access Framework" (SAF) to let users choose exactly where to save their backup (e.g., to SD card or Google Drive).

## How it does it
- **Permission Handling:** Uses `rememberLauncherForActivityResult` with `RequestMultiplePermissions` to request all needed access (SMS, Contacts, Storage) in one go.
- **Hierarchical List:** Uses a `LazyColumn` to render:
    - **`MasterBackupButton`:** A top-level toggle to select everything.
    - **`CategoryCard`:** Expandable headers for each data type.
    - **`BackupItemRow`:** Individual items with a checkbox and a secondary "preview" toggle.
- **Dynamic Previews:** When an item (like an SMS thread) is clicked, it expands to show a small snippet of the actual data, helping the user verify what they are backing up.
- **Role Management:** Includes a button to request becoming the "Default SMS App" if needed, using the `RoleManager` API (Android 10+).
- **Saving Logic:** Once the ViewModel creates a temporary internal backup file, the screen uses `CreateDocument` activity result to prompt the user to "Save As" the file to a permanent location, then copies the data.

## Overall role
It acts as the **View layer** for the Backup module, providing a rich, interactive, and transparent interface for the complex data-gathering operations managed by the `BackupViewModel`.
