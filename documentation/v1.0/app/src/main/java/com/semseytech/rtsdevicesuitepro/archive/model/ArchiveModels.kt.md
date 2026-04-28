# ArchiveModels.kt

## What it does
`ArchiveModels.kt` defines the data structures and enums used throughout the Archive Engine module to represent archive configurations and file information.

## Why it does it
It provides a strongly-typed domain model for the archive feature. By defining enums for formats, compression levels, and methods, it ensures consistency across the UI, ViewModel, and the `ArchiveManager` logic, preventing errors from invalid configurations.

## How it does it
1.  **Enums**: Defines `ArchiveFormat`, `CompressionLevel`, `CompressionMethod`, `PathMode`, and `EncryptionMethod` to cover various technical aspects of archiving.
2.  **ArchiveOptions**: A `data class` that aggregates all possible settings for creating an archive, such as format, password, and dictionary size. It uses default values for a standard configuration.
3.  **FileItem**: A lightweight wrapper around the standard `java.io.File` that exposes common properties like size and name as individual fields, making them easier to use in Compose UI lists.

## What part it plays overall
It serves as the "Common Language" for the Archive module. Every component in the module (UI, Logic, ViewModel) refers to these models to communicate state and intent regarding file archiving tasks.
