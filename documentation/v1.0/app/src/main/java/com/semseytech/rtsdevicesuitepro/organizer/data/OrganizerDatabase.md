# OrganizerDatabase.kt

## What it does
Defines the Room database configuration for the Smart Organizer module.

## Why it does it
To provide a centralized, thread-safe access point to the local SQLite database that stores the user's file organization rules.

## How it does it
- **@Database**: Annotates the class with the list of entities (`OrganizerRuleEntity`) and the version number.
- **@TypeConverters**: Registers the `Converters` class to handle JSON serialization for complex objects.
- **Singleton Pattern**: Uses a `Companion Object` with a `synchronized` block to ensure only one instance of the database is created (Singleton pattern), which is a best practice for Room.

## Overall role
It is the main entry point for the database layer, providing the DAO to the repository.
