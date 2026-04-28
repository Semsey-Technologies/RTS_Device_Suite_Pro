# ArchiveExplanations.kt

## What it does
`ArchiveExplanations` is a simple utility object that provides human-readable descriptions for technical archiving terms and settings used within the `ArchiveDialog`.

## Why it does it
Archiving often involves jargon (LZMA2, AES-256, Solid Blocks, Dictionary Size) that might be confusing to casual users. This file centralizes documentation strings to:
- **Educate Users:** Help users make informed decisions about compression and security.
- **Maintain Consistency:** Ensure the same explanation is used throughout the app.
- **Clean UI Code:** Keep the UI code focused on layout rather than containing large blocks of static text.

## How it does it
- **Key-Value Mapping:** It uses a `getExplanation(key: String)` function with a `when` expression to map technical labels to their corresponding descriptions.
- **Fallback Mechanism:** Provides a default "No explanation available" response for unknown keys.

## Overall role
It acts as a **knowledge base** for the Archive module's UI, serving as the content provider for help dialogs and tooltips.
