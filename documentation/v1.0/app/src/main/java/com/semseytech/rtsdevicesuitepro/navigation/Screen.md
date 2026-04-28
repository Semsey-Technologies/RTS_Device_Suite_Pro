# Screen.kt

## What it does
Defines the destination routes and metadata for all screens in the application using a sealed class.

## Why it does it
To provide a type-safe and centralized way to manage navigation paths. This prevents hardcoding string routes throughout the UI and makes it easy to update screen titles or route structures in one place.

## How it does it
- **Sealed Class**: The `Screen` class is sealed to ensure a fixed set of navigation targets. Each destination is an `object` (or a `class` if it takes arguments).
- **Properties**: Each screen has a `route` (the string ID used by the NavController) and a `title` (often used in TopAppBars).
- **Dynamic Routes**: The `CategoryViewer` screen includes a `createRoute` function to handle URL parameters (e.g., `category_viewer/IMAGES`), demonstrating how to pass arguments between screens.

## Overall role
It acts as the navigation "map" for the application, defining every possible destination the user can visit.
