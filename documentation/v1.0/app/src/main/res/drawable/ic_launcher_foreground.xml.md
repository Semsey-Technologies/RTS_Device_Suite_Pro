# ic_launcher_foreground.xml

## What it does
Defines the foreground layer of the application's adaptive launcher icon.

## Why it does it
Android 8.0+ requires adaptive icons consisting of separate foreground and background layers to allow the system to apply different masks (circle, square, squircle) and visual effects (parallax, pulsing).

## How it does it
- **Inset Wrapper**: Uses an `<inset>` tag to provide padding (32dp) around the main icon asset.
- **Source Reference**: Points to `@drawable/icon` (which is the high-resolution brand logo).
- **Safe Zone**: The insetting ensures that the logo remains within the "safe zone" regardless of the mask shape applied by the device's launcher.

## Overall role
It provides the visual "identity" of the app on the device's home screen, ensuring the logo is centered and correctly sized for modern Android icon standards.

## Version History
| Version | Change Description | Author |
| :--- | :--- | :--- |
| 1.0 | Initial Documentation | AI |
| 1.0.1 | Adjusted insets for better logo centering | AI |
