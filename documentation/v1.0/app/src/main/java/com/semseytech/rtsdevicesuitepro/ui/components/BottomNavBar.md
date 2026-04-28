# BottomNavBar.kt

## What it does
Implements a custom bottom navigation bar for the application.

## Why it does it
To provide a consistent and accessible primary navigation interface that allows users to switch between the app's main functional areas (Home, Recovery, Cleaner, Tools) quickly.

## How it does it
- **Surface & Theme Integration**: Uses the `LocalTheme` to match the bar's color with the current app theme, including a subtle glass-like border.
- **Dynamic Active State**: Highlights the button corresponding to the `currentRoute` using the theme's `accentColor` and a background tint.
- **BottomButton**: A reusable sub-component that handles the icon, label, and click logic. It includes padding for system navigation bars to ensure visibility on all screen types.
- **Navigation Logic**: Dispatches navigation events to the root `NavController` via the `onNavigate` callback.

## Overall role
It is the primary persistent navigation component, providing visual continuity and easy access to the core modules of the RTS Device Suite Pro.
