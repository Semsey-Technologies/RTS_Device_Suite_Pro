# StorageAnalyzerModels.kt

## What it does
Defines the data structures used for storage analysis, including statistics, file information, categories, and UI display settings.

## Why it does it
To provide a consistent, type-safe model for representing the state of the device's storage. It categorizes files (Images, Videos, etc.) and allows for complex UI configurations like sorting, grouping, and different view modes (Grid vs List).

## How it does it
- **StorageStats**: The root data class containing global metrics (Total/Used/Free space) and a map of category-specific statistics.
- **FileInfo**: Represents a single file or installed app, including metadata like size, path, and MIME type.
- **FileCategory (Enum)**: Groups files into logical types (IMAGES, VIDEOS, AUDIO, etc.).
- **DisplaySettings**: Stores user preferences for the analyzer view, such as `SortOption` (Name, Size, Date), `SortOrder`, and `ViewMode`.
- **GroupByOption (Enum)**: Allows the UI to bucket files by attributes like "Folder", "Type", or "Size Range".

## Overall role
It acts as the domain model for the Storage Analyzer module, bridging the data retrieved from the system and the state displayed in the Compose UI.
