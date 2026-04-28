# MainActivity.kt

## What it does
The single entry-point activity for the application, responsible for initialization, permission management, and hosting the main UI shell.

## Why it does it
Following the modern "Single Activity Architecture," this file manages the lifecycle of the app, handles the complex permission requests required for device-wide analysis (SMS, Contacts, Storage), and sets up the root navigation structure.

## How it does it
- **Permission Launcher**: Uses `registerForActivityResult` with `RequestMultiplePermissions` to handle a wide range of system permissions in a modern, asynchronous way.
- **checkAndRequestPermissions()**: A comprehensive method that adaptively requests permissions based on the Android API level (e.g., handling granular media permissions in Android 13+ and `MANAGE_EXTERNAL_STORAGE` in Android 11+).
- **Jetpack Compose Integration**: Sets the content using the `RTSDeviceSuiteProTheme` and initializes the `NavController`.
- **UI Shell**: Defines a global `Scaffold` that hosts the `BottomNavBar` and the `NavGraph`. It includes logic for intelligent backstack management to prevent redundant navigation.
- **Edge-to-Edge**: Calls `enableEdgeToEdge()` to ensure the UI draws behind system bars for a modern, immersive look.

## Overall role
It is the foundation of the app, coordinating system-level requirements (permissions, intent handling) and the root UI container.
