# OrganizerRepository.kt

## What it does
Acts as a mediator between the data sources (Room DAO) and the UI/Domain layer (ViewModel and Worker).

## Why it does it
To decouple the business logic from the specific data storage implementation. It also handles the conversion between database entities (`OrganizerRuleEntity`) and domain models (`OrganizerRule`).

## How it does it
- **allRules**: Exposes a `Flow` of domain models by mapping the DAO's entity flow using GSON for deserialization.
- **Mapping Logic**: Contains private extension functions (`toDomain` and `toEntity`) to bridge the gap between the storage format (JSON strings in DB) and the functional format (Rich objects in code).
- **CRUD Wrapper**: Provides suspended methods for inserting, deleting, and updating rules, delegating to the DAO.

## Overall role
It provides a clean API for the rest of the application to interact with organizer rules, managing the complexity of data transformation and persistence.
