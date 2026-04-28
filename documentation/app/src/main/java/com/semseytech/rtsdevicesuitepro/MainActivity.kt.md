# MainActivity.kt

## What it does
`MainActivity` is the primary entry point of the RTS Device Suite Pro application. It handles the initial setup of the app's UI using Jetpack Compose and manages runtime permissions.

## Why it does it
As the main activity, it serves as the host for the entire application's navigation and provides the necessary context for the app to function. It ensures that the app has the required permissions (SMS, Contacts, Call Log, and Storage) before the user interacts with features that depend on them.

## How it does it
1.  **Permission Management**: It uses `ActivityResultContracts.RequestMultiplePermissions` to request a comprehensive set of permissions required for device maintenance tasks.
2.  **UI Setup**: It uses `setContent` to set up the Compose-based UI, wrapping everything in `RTSDeviceSuiteProTheme`.
3.  **Navigation**: It initializes a `NavController` and sets up a `Scaffold` with a `BottomNavBar`. It manages navigation logic, ensuring efficient switching between the Dashboard and other screens.
4.  **Edge-to-Edge**: It calls `enableEdgeToEdge()` to provide a modern, immersive UI experience.
5.  **Special Permissions**: For Android 11+, it explicitly checks and requests `MANAGE_EXTERNAL_STORAGE` to allow deep file system analysis and cleaning.

## What part it plays overall
It acts as the "Orchestrator" of the application. It ties together the theme, navigation graph, and the persistent bottom navigation bar, while ensuring the app operates within the Android security model by managing permissions.
