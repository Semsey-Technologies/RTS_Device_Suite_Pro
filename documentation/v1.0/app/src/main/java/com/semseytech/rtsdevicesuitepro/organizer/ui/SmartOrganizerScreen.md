# SmartOrganizerScreen.kt

## What it does
Provides the primary user interface for managing file organization rules.

## Why it does it
To allow users to view, create, toggle, and delete automation rules in a clean, modern interface. It provides visual feedback on which rules are active and allows for manual execution of the organization logic.

## How it does it
- **Scaffold/TopAppBar**: Implements the standard Material Design layout with a "SMART ORGANIZER" title and a manual "Run All Now" button.
- **LazyColumn**: Efficiently displays the list of `OrganizerRule` cards.
- **RuleCard**: A custom component for each rule that displays the name, path transformation (Source -> Target), and an enable/disable switch. It also includes a dropdown menu for deletion.
- **EmptyState**: A placeholder UI shown when no rules have been created yet, guiding the user to use the Floating Action Button (FAB).
- **AddRuleDialog**: (Referenced) A dialog that handles the complex input required for creating a new rule.

## Overall role
It is the user-facing entry point for the Smart Organizer module, providing an intuitive dashboard for managing automation tasks.
