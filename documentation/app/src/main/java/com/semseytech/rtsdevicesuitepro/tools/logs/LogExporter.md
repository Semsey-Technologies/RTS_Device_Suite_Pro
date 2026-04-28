# LogExporter.kt

## What it does
Provides utility functions to capture system logs (logcat) and device metadata, formatting them into a professional Markdown report.

## Why it does it
To facilitate debugging and bug reporting. By centralizing system information and logs into a single shareable file, it allows users to provide technical details to developers efficiently.

## How it does it
- **Logcat Capture**: Executes a system command (`logcat -d`) to grab the latest logs from the device buffer.
- **Markdown Generation**: Constructs a structured `.md` file containing:
    - Device hardware and OS details.
    - Application version.
    - A large snippet of the system log.
- **Sharing Integration**:
    - **shareLog**: Uses `FileProvider` to share the generated Markdown file with other apps (like Drive, Dropbox, or messaging apps).
    - **emailDeveloper**: Specialized sharing intent prepopulated with the developer's email address and a bug report template.

## Overall role
A diagnostic utility that automates the collection of technical support data, packaged as a user-friendly log exporter.
