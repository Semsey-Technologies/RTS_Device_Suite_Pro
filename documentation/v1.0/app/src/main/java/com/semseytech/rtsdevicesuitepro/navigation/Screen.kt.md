# Screen.kt

## What it does
`Screen` is a sealed class that defines the navigation routes and display titles for all screens within the application.

## Why it does it
It provides a type-safe way to manage navigation routes throughout the app. By centralizing route definitions, it prevents hardcoded string errors and makes it easy to update or add new screens.

## How it does it
It uses a `sealed class` where each screen is represented as an `object` (or a `class` if it takes arguments). Each entry contains:
- `route`: The internal string used by the Navigation component to identify the screen.
- `title`: A human-readable title for the screen, often used in top bars or labels.
- `CategoryViewer`: A special case that includes a `createRoute` helper function to handle dynamic navigation arguments (category names).

## What part it plays overall
It serves as the "Navigation Registry" for the app. It is used by the `NavGraph` to set up routes and by the `BottomNavBar` or other UI elements to trigger navigation actions.
