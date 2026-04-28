# OrganizerDao.kt

## What it does
Provides the Data Access Object (DAO) interface for the Smart Organizer's database operations.

## Why it does it
It abstracts the low-level SQL queries into a clean Kotlin interface, allowing the application to perform CRUD (Create, Read, Update, Delete) operations on the file organization rules.

## How it does it
- **getAllRules()**: Returns a `Flow<List<OrganizerRuleEntity>>` to provide real-time updates when rules change.
- **getEnabledRules()**: A suspend function used by the background worker to fetch only active rules for processing.
- **CRUD Operations**: Includes standard `@Insert`, `@Delete`, and `@Update` methods to manage rule entries.

## Overall role
It acts as the interface between the application's repository and the Room database, handling the persistence logic for automation rules.
