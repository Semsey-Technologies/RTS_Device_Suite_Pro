# ic_launcher (Mipmap Family)

## What it does
Provides the legacy and fallback icon assets for the application across different screen densities (hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi).

## Why it does it
While modern Android versions (8.0+) use Adaptive Icons defined in `drawable/`, mipmap resources are still required for:
1. Compatibility with older Android versions.
2. Ensuring the launcher has access to pre-scaled bitmaps for optimal performance and memory usage.
3. Providing the specific icon file requested by the manifest (`android:icon="@mipmap/ic_launcher"`).

## How it does it
- **Density Scaling**: The project contains multiple versions of the same icon in different folders (e.g., `mipmap-xxxhdpi`) to ensure sharpness on high-resolution displays.
- **WebP Format**: Uses the WebP format for high compression with minimal quality loss compared to PNG.
- **Manifest Integration**: Referenced in `AndroidManifest.xml` as the primary entry point for the app's visual identity on the home screen.

## Overall role
Acts as the fallback and legacy identifier for the application. It ensures the app has a recognizable icon on every device, regardless of OS version or screen density.

## Version History
| Version | Change Description | Author |
| :--- | :--- | :--- |
| 1.0 | Initial Documentation | AI |
| 1.0.1 | Migrated from PNG to WebP for better optimization | AI |
