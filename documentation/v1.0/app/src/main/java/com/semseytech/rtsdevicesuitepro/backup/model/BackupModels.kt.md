# BackupModels.kt

## What it does
`BackupModels.kt` defines the data structures used throughout the Backup module. It includes a sealed class hierarchy for different types of backupable items, state classes for categories, and manifest models for identifying the contents of a backup archive.

## Why it does it
A backup system needs to represent many different types of data (a contact is different from an APK, which is different from an SMS) in a way that the UI can display consistently and the backup engine can process uniformly. This file provides:
- **Type Safety:** Using sealed classes ensures that all possible backup items are accounted for.
- **Structured Data:** Organizes raw system data into domain-specific objects (e.g., `MessageDetail` with attachment support).
- **UI State Definition:** Defines how categories and items are grouped and their selection/expansion states.
- **Portability:** Defines the `BackupManifest` structure which is written to the archive, allowing the app to understand what a backup contains without scanning every file.

## How it does it
- **`BackupItem` (Sealed Class):** The root of the hierarchy. Each subclass (`SmsMessage`, `CallLogEntry`, `Contact`, `Apk`, `UserFile`, etc.) contains properties specific to that data type while sharing common properties like `id`, `displayName`, and `isSelected`.
- **`BackupCategory`:** Groups `BackupItem` objects (e.g., "SMS", "Contacts") and tracks the state of the group (expanded/collapsed, all-selected).
- **Manifest Classes:** `BackupManifest` and `ManifestEntry` are simple data classes designed for JSON serialization, storing metadata about the backup (timestamp, device name) and its contents.
- **Nested Detail Classes:** Classes like `MessageDetail` and `MmsAttachment` handle the complexity of multi-part data like MMS messages.

## Overall role
It defines the **Domain Model** and **State Model** for the Backup module, providing the "vocabulary" used by the ViewModel and the UI to talk about data being backed up.
