# ThemeManager.kt

## What it does
A singleton object that manages the active theme, UI scaling, and typography settings for the entire application.

## Why it does it
To provide a centralized, reactive way to update the application's appearance. It allows users to change themes, adjust text sizes, and switch fonts in real-time across all screens.

## How it does it
- **Mutable State**: Uses Compose's `mutableStateOf` to track the `currentTheme`, `uiScale`, and font preferences. Any change to these values triggers a recomposition of the entire UI.
- **CompositionLocal**: Defines `LocalTheme`, which allows Composable functions deep in the UI tree to access the current theme without explicit parameter passing.
- **Typography Scaling**: Provides methods to scale titles, subtitles, and body text independently, supporting accessibility and user preference.
- **AppFont (Enum)**: Maps friendly names to system `FontFamily` options.

## Overall role
It is the "engine" behind the app's dynamic styling, coordinating theme transitions and UI scaling to ensure a consistent and personalized user experience.
