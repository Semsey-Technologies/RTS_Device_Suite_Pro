# ArchiveDialog.kt

## What it does
`ArchiveDialog` is a complex configuration overlay used when creating a new archive. It allows users to name their archive and fine-tune dozens of technical parameters related to compression and encryption.

## Why it does it
Archive creation is not a "one size fits all" process. Advanced users require control over:
- **Efficiency:** Choosing compression levels (e.g., Ultra vs. Store).
- **Format:** Selecting between ZIP, 7z, etc.
- **Security:** Implementing AES-256 encryption and hiding filenames.
- **Resource Usage:** Limiting CPU threads or dictionary sizes for low-memory devices.
- **Distribution:** Splitting large archives into smaller volumes.

## How it does it
- **Stateful UI:** Uses local Compose state to track user selections before they are "committed" to the ViewModel.
- **Input Controls:** Uses `OutlinedTextField` for text inputs (name, password) and custom `SelectableOption` / `CheckboxOption` components for settings.
- **Educational Tooltips:** Integrates with `ArchiveExplanations` to show info dialogs explaining what each technical setting (like "Solid Block Size") actually does.
- **Validation:** Ensures the "OK" button is only enabled when a valid name is provided and passwords match.
- **Scrollable Layout:** Uses a `verticalScroll` modifier to handle the large number of configuration options on smaller screens.

## Overall role
It serves as the **configuration interface** for the archiving engine. It abstracts the complexity of the `ArchiveOptions` data model into a user-friendly (and educational) wizard.
