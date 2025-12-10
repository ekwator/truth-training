# Implementation Plan: Android Impacts and Judgments UI

**Branch**: `018-android-impacts-judgments` | **Date**: 2025-12-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/018-android-impacts-judgments/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

**Primary Requirement**: Implement "Adding Impacts" and "Submitting Judgments" functionality in the Android EventDetailScreen, allowing users to record impact assessments (level 1-5 with optional notes) and submit judgments (true/false/uncertain with confidence 0.0-1.0 and optional reasoning) for events.

**Technical Approach**: 
- Integrate impact and judgment forms into EventDetailScreen using Material Design 3 dialogs/modals or inline forms, matching Desktop UI patterns
- Display impacts and judgments lists in dedicated sections within EventDetailScreen
- Use existing ImpactRepository and JudgmentRepository for offline-first data persistence
- Map impact level (1-5) to boolean value: impact_level > 3 → positive (true), impact_level <= 3 → negative (false)
- Implement reactive UI updates using Flow collections from repositories
- Ensure all UI elements include emojis (constitutional requirement Rule 8) and support bilingual localization (English/Russian)

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin 1.9+ / Android API 24+ (Android 7.0+)  
**Primary Dependencies**: Jetpack Compose, Material Design 3, AndroidX Navigation Compose, Room Database, Retrofit, Coroutines Flow  
**Storage**: Room Database (SQLite) with offline-first strategy, Retrofit for API communication  
**Testing**: JUnit4, AndroidJUnit4, Compose UI Testing, MockK  
**Target Platform**: Android 7.0+ (API 24+)  
**Project Type**: mobile (Android application)  
**Performance Goals**: Smooth 60fps UI, <200ms form submission response, offline-first with background sync  
**Constraints**: Offline-capable, bilingual localization (EN/RU), emoji support for all UI elements (Rule 8), Material Design 3 compliance  
**Scale/Scope**: Single screen enhancement (EventDetailScreen), 2 new form dialogs, 2 list displays, integration with existing repositories

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Rule 8 (Emoji Requirement)**: ✅ PASS - All UI elements (buttons, form fields, list items, labels) must include semantically meaningful emojis matching Desktop UI implementation. Emojis are language-independent and combined with localized text.

**Bilingual Localization**: ✅ PASS - All text labels must be localized using Android string resources (`values/strings.xml` for English, `values-ru/strings.xml` for Russian). Emojis remain constant across languages.

**Offline-First Strategy**: ✅ PASS - Uses existing ImpactRepository and JudgmentRepository which implement offline-first pattern with Room Database and background sync via TruthApi.

**Desktop UI Parity**: ✅ PASS - Implementation must match Desktop UI patterns (inline forms/modals within Event Summary screen) for consistency across platforms.

**No Additional Gates**: No other constitutional requirements identified that would block this feature.

## Project Structure

### Documentation (this feature)

```text
specs/018-android-impacts-judgments/
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
│   ├── ui/compose/events/
│   │   └── EventDetailScreen.kt                    # MODIFY: Add impacts/judgments sections and forms
│   ├── ui/events/
│   │   └── EventDetailViewModel.kt                 # MODIFY: Add impacts/judgments flows and actions
│   ├── ui/compose/impacts/                          # NEW: Impact form dialog/modal component
│   │   └── AddImpactDialog.kt                       # NEW: Dialog for adding impact
│   ├── ui/compose/judgments/
│   │   ├── JudgmentSubmissionScreen.kt              # EXISTING: May be reused or integrated
│   │   └── SubmitJudgmentDialog.kt                  # NEW: Dialog for submitting judgment
│   ├── data/repository/
│   │   ├── ImpactRepository.kt                      # EXISTING: Use for impact operations
│   │   └── JudgmentRepository.kt                   # EXISTING: Use for judgment operations
│   ├── data/database/entities/
│   │   ├── ImpactEntity.kt                          # EXISTING: Impact data model
│   │   └── JudgmentEntity.kt                        # EXISTING: Judgment data model
│   └── utils/
│       └── EmojiMapping.kt                          # EXISTING: Use for emoji support
├── app/src/main/res/
│   ├── values/strings.xml                           # MODIFY: Add EN strings for impacts/judgments
│   └── values-ru/strings.xml                       # MODIFY: Add RU strings for impacts/judgments
└── app/src/androidTest/java/com/truth/training/client/
    ├── ui/compose/events/
    │   └── EventDetailScreenImpactsJudgmentsTest.kt # NEW: UI tests for impacts/judgments
    └── integration/
        └── ImpactsJudgmentsIntegrationTest.kt       # NEW: Integration tests
```

**Structure Decision**: Mobile application structure (Option 3). The feature extends existing EventDetailScreen with new UI components (dialogs/modals) and integrates with existing repositories. No new screens are required - functionality is integrated inline into EventDetailScreen matching Desktop UI pattern.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations identified. All constitutional requirements are satisfied:
- Rule 8 (Emoji): Using existing EmojiMapping utility
- Bilingual Localization: Using existing Android string resources pattern
- Offline-First: Using existing repository pattern
- Desktop UI Parity: Following existing UI patterns

**No complexity violations to justify.**
