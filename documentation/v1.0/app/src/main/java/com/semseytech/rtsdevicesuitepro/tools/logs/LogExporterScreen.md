# LogExporterScreen.kt

## What it does
Provides the user interface for the log export utility.

## Why it does it
To give users a simple "one-click" way to generate, view, and share technical diagnostic reports.

## How it does it
- **Visual Feedback**: Shows a professional summary of what information will be collected (Device ID, System Logs, Error Traces).
- **Interactive Actions**:
    - **Generate/Share**: Calls `LogExporter.generateLogMarkdown` and immediately triggers the Android share sheet.
    - **Email Support**: Directly launches an email client with the log file attached.
- **Safety Info**: Includes text explaining that logs are used for debugging purposes only and are stored locally in the app's cache before sharing.

## Overall role
The user-facing component of the diagnostic module, providing a clean interface for a technically complex data collection process.
