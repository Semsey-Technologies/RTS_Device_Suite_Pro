# StorageAnalyzerDialogWrapper.kt

## What it does
A container component that manages all dialog-based UI interactions for the Storage Analyzer.

## Why it does it
To keep the main screen logic clean by centralizing all "pop-up" interactions (File Options, Rename, Delete Confirmation) into a single wrapper.

## How it does it
- **Dialog Orchestration**: Based on the state of `StorageAnalyzerDialogState`, it conditionally displays:
    - **File Options Dialog**: Shows actions like Open, Share, Rename, Move, and Delete for a selected file.
    - **Rename Dialog**: A text input prompt for renaming a file.
    - **Confirmation Dialogs**: For dangerous operations like mass deletion.
- **ViewModel Integration**: Directly calls `viewModel` methods (e.g., `renameFile`, `deleteFile`) when a dialog action is confirmed.

## Overall role
It serves as the manager for modal interactions within the analyzer, ensuring that secondary workflows like file management are handled consistently.
