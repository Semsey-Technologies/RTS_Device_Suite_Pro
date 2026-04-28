# colors.xml

## What it does
Defines the primitive color palette for the Android application.

## Why it does it
Centralizing color definitions ensures visual consistency across the app and simplifies large-scale UI changes. While the app uses a dynamic Compose-based theme engine, these XML colors serve as the baseline for legacy components, the splash screen, and system-level UI elements.

## How it does it
- **Hexadecimal Values**: Colors are defined using standard ARGB hex codes (e.g., `#FF000000`).
- **Semantic Naming**: Uses both literal names (e.g., `black`, `white`) and legacy Material Design palette names (e.g., `purple_500`, `teal_200`).
- **Compose Bridge**: Although defined in XML, these colors can be accessed via `colorResource(id = R.color.name)` if needed, providing a bridge between legacy XML and modern Jetpack Compose.

## Overall role
Provides the basic color building blocks. In the RTS Device Suite Pro, it primarily supports the system's underlying Material theme and provides fallback values for the custom Neon/Cyber theme engine.

## Version History
| Version | Change Description | Author |
| :--- | :--- | :--- |
| 1.0 | Initial Documentation | AI |
