# OrganizerEntities.kt

## What it does
This file defines the Room database entity `OrganizerRuleEntity` and the necessary TypeConverters for storing complex objects related to file organization rules.

## Why it does it
To persist user-defined file organization rules (source/target paths, triggers, and options) in a local Room database. Since Room doesn't natively support complex types like `RuleTrigger` or `List<String>`, converters are needed to serialize these to JSON strings for storage.

## How it does it
- **OrganizerRuleEntity**: An `@Entity` class mapping to the `organizer_rules` table. It stores basic fields (ID, name, paths, enabled status) and JSON strings for file types, triggers, and options.
- **Converters**: Uses `Gson` with a custom `RuleTriggerAdapter` to convert `List<String>`, `RuleTrigger`, and `OrganizerOptions` to and from JSON strings, allowing Room to handle these complex types.

## Overall role
It serves as the data schema for the Smart Organizer's persistent storage, defining how automation rules are represented in the database.
