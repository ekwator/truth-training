# Implementation Plan: Android Node Details View

**Branch**: `019-android-node-details` | **Date**: 2025-12-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/019-android-node-details/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

**Primary Requirement**: Implement node detail view functionality in Android, allowing users to tap on a node card in NodesScreen to view detailed information about that node, including address, type (Hub/Leaf and technical), status, timestamps, and all other node attributes.

**Technical Approach**: 
- Create NodeDetailScreen composable following EventDetailScreen pattern
- Add navigation route "node/{nodeId}" to MainNavigation
- Make NodeCard clickable to navigate to NodeDetailScreen
- Implement NodeTypeMapper utility to map technical types (LAN/WIFI/GLOBAL/RELAY/CLIENT) to user-friendly types (Hub/Leaf)
- Create NodeDetailViewModel for managing node detail state
- Display all NodeEntity fields with calculated fields (expires_in, age)
- Ensure all UI elements include emojis (Rule 8) and support bilingual localization (English/Russian)

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin 1.9+ / Android API 24+ (Android 7.0+)  
**Primary Dependencies**: Jetpack Compose, Material Design 3, AndroidX Navigation Compose, Room Database, Coroutines Flow  
**Storage**: Room Database (SQLite) via DiscoveryRepository, existing NodeEntity  
**Testing**: JUnit4, AndroidJUnit4, Compose UI Testing, MockK  
**Target Platform**: Android 7.0+ (API 24+)  
**Project Type**: mobile (Android application)  
**Performance Goals**: Smooth 60fps UI, instant navigation, <100ms screen load time  
**Constraints**: Offline-capable, bilingual localization (EN/RU), emoji support for all UI elements (Rule 8), Material Design 3 compliance  
**Scale/Scope**: Single new screen (NodeDetailScreen), navigation enhancement, utility function (NodeTypeMapper), integration with existing NodesScreen

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Rule 8 (Emoji Requirement)**: ✅ PASS - All UI elements (screen title, labels, buttons, status indicators) must include semantically meaningful emojis matching Desktop UI implementation. Emojis are language-independent and combined with localized text.

**Bilingual Localization**: ✅ PASS - All text labels must be localized using Android string resources (`values/strings.xml` for English, `values-ru/strings.xml` for Russian). Emojis remain constant across languages.

**Offline-First Strategy**: ✅ PASS - Uses existing DiscoveryRepository and NodeEntity which implement offline-first pattern with Room Database.

**Desktop UI Parity**: ✅ PASS - Implementation must match Desktop UI patterns (separate detail screen accessible by tapping on item) for consistency across platforms, following EventDetailScreen pattern.

**No Additional Gates**: No other constitutional requirements identified that would block this feature.

## Project Structure

### Documentation (this feature)

```text
specs/019-android-node-details/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
truth-android-client/
├── app/src/main/java/com/truth/training/client/
│   ├── ui/compose/nodes/
│   │   ├── NodesScreen.kt                    # MODIFY: Make NodeCard clickable, add navigation
│   │   ├── NodeDetailScreen.kt                # NEW: Detail screen for node information
│   │   └── NodesViewModel.kt                  # EXISTING: No changes needed
│   ├── ui/nodes/
│   │   └── NodeDetailViewModel.kt              # NEW: ViewModel for NodeDetailScreen
│   ├── utils/
│   │   ├── EmojiMapping.kt                    # EXISTING: Use for emoji support
│   │   └── NodeTypeMapper.kt                  # NEW: Map technical types to Hub/Leaf
│   ├── ui/compose/
│   │   └── MainNavigation.kt                  # MODIFY: Add "node/{nodeId}" route
│   ├── data/repository/
│   │   └── DiscoveryRepository.kt             # EXISTING: Use for fetching node by ID
│   └── data/database/entities/
│       └── NodeEntity.kt                       # EXISTING: Node data model
├── app/src/main/res/
│   ├── values/strings.xml                     # MODIFY: Add EN strings for node details
│   └── values-ru/strings.xml                  # MODIFY: Add RU strings for node details
└── app/src/androidTest/java/com/truth/training/client/
    ├── ui/compose/nodes/
    │   └── NodeDetailScreenTest.kt            # NEW: UI tests for NodeDetailScreen
    └── utils/
        └── NodeTypeMapperTest.kt                # NEW: Unit tests for NodeTypeMapper
```

**Structure Decision**: Mobile application structure (Option 3). The feature extends existing NodesScreen with navigation to a new NodeDetailScreen, following the EventDetailScreen pattern. No new repositories or entities needed - uses existing DiscoveryRepository and NodeEntity.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations identified. All constitutional requirements are satisfied:
- Rule 8 (Emoji): Using existing EmojiMapping utility
- Bilingual Localization: Using existing Android string resources pattern
- Offline-First: Using existing repository pattern
- Desktop UI Parity: Following existing EventDetailScreen pattern

**No complexity violations to justify.**
