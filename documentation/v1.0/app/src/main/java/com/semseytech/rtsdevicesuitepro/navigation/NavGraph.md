# NavGraph.kt

## What it does
Implements the navigation structure of the application using Jetpack Compose Navigation.

## Why it does it
To coordinate the transitions between different screens and manage the instantiation of ViewModels with their required dependencies (repositories, databases).

## How it does it
- **NavHost**: The root container that manages the navigation stack, starting at the `Dashboard`.
- **Composable Routes**: Maps each `Screen` route to a specific Composable function (e.g., `BackupScreen`, `CleanerScreen`, `StorageAnalyzerScreen`).
- **ViewModel Injection**: Uses custom `ViewModelProvider.Factory` implementations inside `composable` blocks to manually inject repositories and application contexts into ViewModels.
- **State Sharing**: For modules like the `StorageAnalyzer`, it uses `viewModelStoreOwner = mainActivityEntry` to share a single ViewModel instance across multiple screens (Dashboard and Category Viewer), ensuring data consistency.
- **Argument Handling**: Extracts parameters from the navigation backstack (e.g., the `category` string for the `CategoryViewerScreen`) and converts them into domain types.

## Overall role
It is the "switchboard" of the app, connecting URLs/routes to their respective UI components and ensuring all necessary data and logic are correctly initialized for each screen.
