# NavGraph.kt

## What it does
`NavGraph.kt` defines the navigation structure of the application. it sets up the `NavHost` and maps routes to their respective Composable screens.

## Why it does it
It acts as the central wiring for the app's UI. It determines which screen is shown when a specific route is requested and handles dependencies (like ViewModels and Repositories) for those screens.

## How it does it
1.  **NavHost**: It uses the `NavHost` component from Jetpack Compose Navigation, starting at the `Dashboard`.
2.  **Route Mapping**: It uses `composable(route = ...)` to define how each screen from `Screen.kt` is rendered.
3.  **Dependency Injection**: For complex screens like `SmartOrganizer` and `StorageAnalyzer`, it manually creates and injects ViewModels using `ViewModelProvider.Factory`, ensuring repositories and databases are properly initialized.
4.  **ViewModel Scoping**: For the `StorageAnalyzer`, it scopes the `StorageAnalyzerViewModel` to the `Dashboard` backstack entry. This allows state to be shared between the main analyzer screen and the category viewer screen without using a singleton.
5.  **Dynamic Routing**: It handles parameterized routes, such as the `CategoryViewer`, extracting arguments from the `backStackEntry`.

## What part it plays overall
It is the "Router" of the application. It connects the navigation events triggered by the user (via `MainActivity`'s bottom bar or buttons on screens) to the actual UI components and their logic.
