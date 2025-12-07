# Implementation Plan: Android UI Registration and Launch Configuration

**Branch**: `013-goal-objective-properly` | **Date**: 2025-12-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-goal-objective-properly/spec.md`

## Summary

Fix Android application launch issue where the app displays a blank/black screen by properly configuring the navigation graph to use DashboardScreen as the initial screen. The current implementation has MainActivity correctly configured in AndroidManifest.xml, but the navigation graph's start destination ("events") is empty, causing the blank screen. This plan addresses: (1) Register DashboardScreen in navigation graph, (2) Set DashboardScreen as start destination, (3) Create ViewModelFactory for DashboardViewModel, (4) Ensure proper initialization flow.

## Technical Context

**Language/Version**: Kotlin (Android), Jetpack Compose  
**Primary Dependencies**: 
- androidx.navigation:navigation-compose (Navigation)
- androidx.lifecycle:lifecycle-viewmodel-compose (ViewModel)
- androidx.compose.material3 (Material Design 3)
- Room database (local storage)

**Storage**: Room (SQLite) database via TruthDatabase  
**Testing**: 
- JUnit for unit tests
- Android Instrumentation tests (Espresso)
- Physical device testing required

**Target Platform**: Android (API 24+, targeting Android 12+)  
**Project Type**: Mobile (Android)  
**Performance Goals**: 
- App launch within 2 seconds (SC-001)
- Screen display without blank/black screen (SC-005)
- 30+ seconds stability without premature closure (SC-002)

**Constraints**: 
- CLI part must remain untouched (FR-011)
- Android-specific only (no Desktop/Server changes)
- Must work on physical devices (minimum 2 devices for testing)

**Scale/Scope**: 
- Single Activity (MainActivity) modification
- Navigation graph update (1 screen registration)
- ViewModelFactory creation (1 factory)
- ~3-5 files to modify

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

✅ **Rule 1 - Cross-Platform Scope & Parity**: Android-only feature, no cross-platform impact. CLI exclusion explicitly maintained.

✅ **Rule 2 - Source Documents as Authority**: Changes align with CONTRIBUTING.md (Android testing requirements) and SECURITY.md (no security impact).

✅ **Rule 3 - Releases, Installation & Automation**: No release automation changes required.

✅ **Rule 4 - Dependency, Vulnerability & Platform Safeguards**: No new dependencies added, existing Android dependencies used.

✅ **Rule 5 - Database & Schema Integrity**: No database schema changes.

✅ **Rule 6 - CI, Tooling & Automation Discipline**: Spec-Kit workflow followed, plan artifacts generated.

✅ **Rule 7 - Security & Privacy Enforcement**: No security or privacy impact, UI-only changes.

**Constitution Status**: ✅ PASSED - All gates satisfied.

## Project Structure

### Documentation (this feature)

```text
specs/013-goal-objective-properly/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
truth-android-client/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/truth/training/client/
│   │   │   │   ├── MainActivity.kt                    # MODIFY: Ensure NavigationHost initialization
│   │   │   │   └── ui/
│   │   │   │       └── DashboardViewModel.kt          # EXISTS: Use for DashboardScreen
│   │   │   │   └── ui/compose/
│   │   │   │       ├── MainNavigation.kt              # MODIFY: Register DashboardScreen, change start destination
│   │   │   │       ├── DashboardScreen.kt              # EXISTS: Use as initial screen
│   │   │   │       └── ViewModelFactory.kt             # CREATE: Factory for ViewModels
│   │   │   └── AndroidManifest.xml                    # VERIFY: Already correct, no changes needed
│   │   └── androidTest/
│   │       └── java/com/truth/training/client/
│   │           └── integration/
│   │               └── MainActivityLaunchTest.kt       # MODIFY/EXTEND: Add DashboardScreen display tests
```

**Structure Decision**: Android mobile project structure. Changes limited to UI layer (MainActivity, Navigation, ViewModels). No changes to Core, Server, or CLI components.

## Complexity Tracking

> **No constitution violations - complexity tracking not required**

## Progress Tracking

- [x] Phase 0: Research and discovery ✅
  - [x] research.md generated
  - [x] Root cause identified (empty "events" route)
  - [x] Current state analyzed
- [x] Phase 1: Data model and contracts ✅
  - [x] data-model.md generated
  - [x] quickstart.md generated
  - [x] contracts/ directory created
    - [x] navigation-contract.md
    - [x] viewmodel-factory-contract.md
    - [x] activity-contract.md
- [ ] Phase 2: Task decomposition (handled by `/speckit.tasks`)

---

_Version: v1.0.0_
