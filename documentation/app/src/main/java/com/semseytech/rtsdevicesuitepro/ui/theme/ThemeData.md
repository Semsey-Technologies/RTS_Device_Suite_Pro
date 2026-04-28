# ThemeData.kt

## What it does
Defines the structure and presets for the application's visual themes.

## Why it does it
To support a highly customizable and aesthetically diverse UI, ranging from "Cyber/Tech" to "Minimal/Professional" styles. It allows the app to change its entire color palette, textures, and animations dynamically.

## How it does it
- **ThemePreset**: A data class that stores color values (`startColor`, `endColor`, `accentColor`), text colors, and visual effects like `hasGlassTexture` or `hasGridOverlay`.
- **ThemeCategory**: Groups presets into logical collections (e.g., "Cyber / Tech Core", "Creative / Experimental").
- **ThemePresets**: A hardcoded list containing dozens of predefined themes, such as "Neon Dark Cyber", "Terminal Green", "Aurora Pulse", and "Analyzer Mode".

## Overall role
It acts as the configuration manifest for the application's look and feel, providing the data needed by the theme engine to render the UI.
