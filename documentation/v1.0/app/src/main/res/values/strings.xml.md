# strings.xml

## What it does
Central repository for all user-facing text strings in the application.

## Why it does it
To support localization (i18n) and maintain a single source of truth for text. By externalizing strings, the app can easily be translated into different languages by adding additional `values-xx/strings.xml` files without changing the code.

## How it does it
- **Key-Value Pairs**: Defines strings using the `<string>` tag with a unique `name` attribute.
- **Accessibility**: Allows the use of string resources in both XML layouts (`@string/app_name`) and Kotlin code (`getString(R.string.app_name)`).

## Overall role
It is the linguistic foundation of the app. Currently, it holds the primary `app_name`, but it is designed to scale as the app adds more complex dialogue, instructions, and labels.

## Version History
| Version | Change Description | Author |
| :--- | :--- | :--- |
| 1.0 | Initial Documentation | AI |
