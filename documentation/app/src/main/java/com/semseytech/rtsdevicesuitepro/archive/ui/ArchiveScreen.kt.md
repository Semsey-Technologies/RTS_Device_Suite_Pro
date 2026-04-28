# ArchiveScreen.kt

## What it does
`ArchiveScreen` is the primary user interface for the Archive Manager module. It provides a file explorer-like interface where users can navigate the device's storage, select files, and perform various archive-related operations such as creating archives, extracting them, and managing file systems (copy, move, delete).

## Why it does it
The Archive Manager needs a visual way for users to interact with their files. Instead of just being a backend utility, this screen provides:
- **Navigation:** A way to browse through directories and select targets for archiving or extraction.
- **Action Hub:** A centralized toolbar for all supported operations (Add, Extract, Test, Copy, Move, Delete, Info).
- **Status Feedback:** Real-time feedback via Snackbars for errors and operations.
- **Dynamic Configuration:** Access to sorting and view options to tailor the file browsing experience.

## How it does it
- **State Management:** It observes state from `ArchiveViewModel`, including the current directory, file list, selection state, and UI triggers (dialog visibility).
- **Scaffold & Components:**
    - **TopAppBar:** Displays the current module name and provides "Back", "Up", and "Refresh" navigation controls.
    - **BottomAppBar (Toolbar):** A custom toolbar containing `ToolbarAction` components for core features.
    - **LazyColumn:** Efficiently renders the list of files in the current directory using `FileRow` items.
- **Dialog Integration:** Orchestrates various overlays for complex tasks:
    - `ArchiveDialog` for creating new archives.
    - `ArchiveInfoDialog` for viewing file metadata.
    - `CopyMoveDialog` for file system operations.
    - `PasswordDialog` for handling encrypted archives during extraction.
- **Interaction Logic:** Handles simple clicks (navigation or selection toggle) and long clicks (selection toggle) on file items.

## Overall role
It acts as the **View layer** in the MVVM pattern for the Archive module. It translates user interactions into commands for the `ArchiveViewModel` and renders the current state of the file system and archive operations to the user.
