# Theme.kt

## What it does
Configures the Jetpack Compose `MaterialTheme` based on the application's custom theme settings.

## Why it does it
To bridge the gap between custom `ThemePreset` data and the standard Material Design components used throughout the app.

## How it does it
- **RTSDeviceSuiteProTheme**: A wrapper composable that:
    1. Retrieves the current theme from `ThemeManager`.
    2. Overrides the `MaterialTheme` color scheme and typography.
    3. Provides the custom theme data via `CompositionLocalProvider`.
- **Dynamic Styling**: Ensures that standard components (like `Buttons`, `Cards`, and `TopAppBars`) automatically adopt the active theme's colors and font settings.

## Overall role
It is the integration point that applies the user's chosen theme to the Jetpack Compose rendering pipeline.
