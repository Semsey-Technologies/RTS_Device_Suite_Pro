# DashboardScreen.kt

## What it does
The primary entry-point screen of the RTS Device Suite Pro application.

## Why it does it
To provide a central hub for navigating to all major modules (Backup, Cleaner, Archive, etc.) and to give a high-level overview of system health and performance.

## How it does it
- **Bento-style Layout**: Uses a grid of cards to represent different modules, each with distinct icons, titles, and descriptions.
- **Dynamic Headers**: Features a "Tech-inspired" header with the RTS logo and system status indicators.
- **Metric Cards**: Displays quick-action tiles for storage analysis, recent backups, and one-tap optimizations.
- **Navigation Dispatching**: Communicates with the `NavGraph` via callbacks to handle screen transitions.

## Overall role
It is the "Home" of the application, designed for speed and clarity, allowing users to jump directly into the utility they need.
