# ExtractionUtils.kt

## What it does
Provides utility functions for extracting compressed archive files (ZIP, TAR, etc.) into a target directory.

## Why it does it
To support the "Auto-Extract" feature of the Smart Organizer, allowing the application to decompress incoming archives and organize their contents automatically.

## How it does it
- **Apache Commons Compress**: Uses the `ArchiveStreamFactory` to automatically detect the archive format (ZIP, TAR, GZIP, etc.) and create the appropriate input stream.
- **extract()**: Iterates through each entry in the archive, creates the necessary subdirectories, and writes the entry's content to the file system at the destination path.

## Overall role
A specialized utility for file decompression, integrated into the Organizer's workflow to handle complex file types seamlessly.
