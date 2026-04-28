# ArchiveManager.kt

## What it does
`ArchiveManager` is a singleton object that provides high-level functionality for creating, extracting, and testing various archive formats (ZIP, 7z, TAR, GZIP, BZIP2, XZ).

## Why it does it
It encapsulates the complexity of using the Apache Commons Compress library. It provides a clean, unified API for the rest of the application to handle file compression and extraction tasks without needing to deal with the nuances of different stream types and configurations.

## How it does it
1.  **Format Dispatching**: It uses a `when` expression to delegate creation and extraction tasks to format-specific private methods.
2.  **Apache Commons Compress**: It leverages specialized stream classes from Apache Commons Compress (e.g., `ZipArchiveOutputStream`, `SevenZOutputFile`, `TarArchiveInputStream`).
3.  **Recursive Processing**: For formats that support multiple files (ZIP, 7z, TAR), it recursively traverses directories to add all contents to the archive.
4.  **Compression Configuration**: It maps custom `ArchiveOptions` (like compression level and method) to the library-specific constants.
5.  **Password Support**: It includes basic support for password-protected 7z archives and detects encrypted ZIP files.
6.  **Stream Management**: It makes extensive use of Kotlin's `.use { ... }` extension function to ensure all file and archive streams are correctly closed, preventing resource leaks.

## What part it plays overall
It is the "Core Logic Engine" for the Archive feature. It performs the actual heavy lifting of file manipulation, while `ArchiveViewModel` manages the UI state and user interactions.
