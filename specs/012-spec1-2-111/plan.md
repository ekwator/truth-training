# Implementation Plan: Android Parity with Desktop UI & Startup Fix

**Branch**: `012-spec1-2-111` | **Date**: 2025-12-03 | **Spec**: `/home/ekwator/Code/truth-training/specs/012-spec1-2-111/spec.md`  
**Input**: Feature specification from `/specs/012-spec1-2-111/spec.md`

## Summary

Android app must achieve functional and behavioral parity with Desktop UI while fixing the critical startup bug where the app disappears immediately after launch. This requires: (1) fixing AndroidManifest.xml to make MainActivity the launcher with proper intent filters, (2) ensuring TruthDatabase initialization uses canonical schema from shared SQL assets matching `core/src/storage.rs`, (3) removing legacy `events` tables without data migration, (4) replacing numeric context inputs with dropdowns populated from embedded Room database, (5) implementing context validation matching Desktop behavior, (6) documenting or implementing RU/EN localization parity, and (7) updating all relevant documentation to reflect Android behavior alongside Desktop.

## Technical Context

**Language/Version**: Kotlin (Android SDK 33+), Jetpack Compose, Room 2.x, Gradle 8.x  
**Primary Dependencies**: AndroidX Room, Navigation Compose, Material3, Kotlin Coroutines, WorkManager  
**Storage**: SQLite via Room database (`TruthDatabase`), matching Desktop SQLite schema structure  
**Testing**: Android instrumented tests (androidTest), unit tests (test), UI tests (Compose Test), manual testing on emulator/device  
**Target Platform**: Android (API 24+), offline-capable, embedded database  
**Project Type**: Mobile Android app (Kotlin + Compose) with shared schema via SQL assets  
**Performance Goals**: App launch <2s, database initialization <1s on clean install, context dropdown load <200ms for ≤100 options, navigation transitions <16ms  
**Constraints**: Must match Desktop schema exactly (Rule 5), remove all legacy tables, validate context IDs against embedded lookup data, maintain offline-first UX, optional telemetry (only if infrastructure exists), RU/EN parity or clear documentation of EN-only status  
**Scale/Scope**: Single-tenant Android DB (<10k events), ~100 context rows, 5+ documentation files to update, navigation graph with 5+ screens

## Constitution Check

- ✅ **Rule 1 — Cross-Platform Scope & Parity**: Android must match Desktop schema, context UX, and localization behavior; shared SQL assets ensure schema parity across platforms.
- ✅ **Rule 5 — Database & Schema Integrity**: Using canonical schema from shared SQL assets derived from `core/src/storage.rs` enforces Truth tables, FK constraints, and migration tracking; legacy table removal prevents schema drift.
- ✅ **Rule 6 — CI, Tooling & Automation**: Spec-Kit artifacts (research, data-model, quickstart, contracts, tasks) generated here; plan mandates automated tests (instrumented/unit) for schema validation.
- ✅ **Rule 7 — Security & Privacy Enforcement**: Context validation prevents data corruption; database initialization avoids half-applied migrations; no secrets stored in locale preferences.

## Project Structure

### Documentation (this feature)

```text
specs/012-spec1-2-111/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── android-startup.md
│   ├── android-db-init.md
│   ├── android-context-dropdown.md
│   └── android-localization.md
└── tasks.md
```

### Source Code (repository root)

```text
core/
└── src/storage.rs              # Canonical schema (source for shared SQL asset)

truth-android-client/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Fix launcher Activity declaration
│   │   ├── java/com/truth/training/client/
│   │   │   ├── MainActivity.kt         # Fix Compose UI initialization
│   │   │   ├── TruthTrainingApplication.kt  # DB initialization
│   │   │   ├── data/
│   │   │   │   ├── database/
│   │   │   │   │   ├── TruthDatabase.kt           # Update schema, remove legacy entities
│   │   │   │   │   ├── TruthDatabaseMigrations.kt # Add migration to drop legacy tables
│   │   │   │   │   └── entities/                  # Update entities to match canonical schema
│   │   │   │   └── repository/
│   │   │   │       └── EventRepository.kt        # Context validation logic
│   │   │   └── ui/compose/
│   │   │       ├── events/
│   │   │       │   └── EventCreateScreen.kt       # Replace numeric inputs with dropdowns
│   │   │       └── components/                    # New context picker component (to add)
│   │   └── assets/                                # Shared SQL schema file (to add)
│   └── build.gradle.kts
└── README.md / CHANGELOG.md

docs/
├── quickstart_android.md
├── UI_Desktop.md
├── spec/23-function_desktop.md
└── spec/09-ux-guidelines.md
```

**Structure Decision**: Retain existing Android monorepo layout (`truth-android-client/`), add shared SQL asset file in `app/src/main/assets/` for schema parity, create reusable Compose context picker component, and update documentation files under `docs/` and `spec/`.

## Complexity Tracking

No constitutional violations or extra subsystems beyond existing Android app structure; no additional justification required.

## Progress Tracking

| Phase | Output | Status | Notes |
|-------|--------|--------|-------|
| Phase 0 | `research.md` | ✅ Complete | Analyzed current Android state, startup issue, schema gaps, and UX differences. |
| Phase 1 | `data-model.md`, `contracts/`, `quickstart.md` | ✅ Complete | Defined canonical schema usage, Android-specific contracts, and validation steps. |
| Phase 2 | `tasks.md` | ⏳ Pending | Task list grouped by user story with file-level guidance (created via `/tasks` command). |
