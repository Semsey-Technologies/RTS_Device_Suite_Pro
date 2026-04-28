# OrganizerRule.kt

## What it does
Defines the domain models for the Smart Organizer, including rules, triggers, and processing options.

## Why it does it
To provide a strongly-typed structure for representing automation logic. It defines when a rule should run (Triggers), what it should look for (File Types), where it should move things (Paths), and how it should handle complex scenarios like archives (Options).

## How it does it
- **OrganizerRule**: The primary data class containing all information about a specific automation task.
- **RuleTrigger (Sealed Class)**: Defines various event-based or time-based triggers:
    - `Interval`: Every X minutes.
    - `Daily/Weekly`: Scheduled times.
    - `OnIdle/OnPowerConnected`: Device state changes.
    - `OnFolderModified`: File system events.
- **RuleTriggerAdapter**: A custom GSON `JsonSerializer/Deserializer` to handle the polymorphic nature of the `RuleTrigger` sealed class when saving to the database.
- **OrganizerOptions**: Configuration for edge cases, such as whether to ignore subfolders or how to handle compressed files (`ArchiveOptions`).

## Overall role
It is the "brain" of the Organizer module's data structure, defining the vocabulary and logic used by both the UI for configuration and the background worker for execution.
