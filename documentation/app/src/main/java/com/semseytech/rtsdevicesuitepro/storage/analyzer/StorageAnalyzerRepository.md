# StorageAnalyzerRepository.kt

## What it does
Handles the low-level logic for scanning device storage, managing files, and gathering installed application data.

## Why it does it
To provide a unified interface for accessing storage data from multiple sources: the Android `MediaStore`, the direct File System (`java.io.File`), and the `PackageManager` (for installed apps). It also centralizes file manipulation operations (delete, move, copy, rename) with consistent logging.

## How it does it
- **getStorageStats()**: Orchestrates a three-part scan:
    1. **MediaStore Scan**: Quickly retrieves media files (Images, Videos, Audio) indexed by the system.
    2. **Recursive File Scan**: Performs a deep crawl of the file system (if `MANAGE_EXTERNAL_STORAGE` is granted) to find non-media files.
    3. **App Scan**: Uses `PackageManager` to measure the size of installed user applications.
- **File Operations**: Implements `deleteFile`, `moveFile`, `copyFile`, and `renameFile`. It handles complex scenarios, such as falling back to manual copy/delete if `renameTo()` fails across storage partitions.
- **Proof Logging**: Includes detailed `Log.d("RTS PROOF", ...)` statements for critical operations, documenting exactly which API was used and whether the operation succeeded.
- **Intents**: Provides `openFile` and `shareFile` using `FileProvider` to safely share file URIs with other apps.

## Overall role
It is the data engine of the Storage Analyzer, responsible for high-performance scanning and robust file management while adhering to Android's modern storage permissions.
