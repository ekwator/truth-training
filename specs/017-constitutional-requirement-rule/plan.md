# Implementation Plan: Android UI Emoji Accessibility Implementation (Constitutional Requirement Rule 8)

**Branch**: `017-constitutional-requirement-rule` | **Date**: 2025-12-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/017-constitutional-requirement-rule/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement emoji accessibility for all Android UI elements matching Desktop UI emoji mapping to comply with constitutional requirement Rule 8. This includes creating a centralized emoji mapping utility in Kotlin matching Desktop `emojiMapping.ts` structure, and adding emojis to all screens, action buttons, form field labels, status indicators, and navigation elements across the Android application. The implementation ensures 100% visual parity with Desktop UI emoji assignments while preserving Material Design 3 layout patterns. Emoji mapping is language-independent (same emoji regardless of English/Russian language setting), while UI components combine emojis with localized text strings from Android string resources.

## Technical Context

**Language/Version**: Kotlin (compatible with Android API 24+), Jetpack Compose  
**Primary Dependencies**: Jetpack Compose, Material Design 3, AndroidX Navigation Compose, AndroidX Resources (for localization)  
**Storage**: N/A (emoji mapping is compile-time constants, no persistence needed)  
**Testing**: JUnit 4, Espresso (for UI tests), Compose UI testing framework  
**Target Platform**: Android (API 24+, Android 7.0+)  
**Project Type**: Mobile (Android)  
**Performance Goals**: Emoji rendering should not impact UI performance (<16ms frame time), emoji lookup O(1)  
**Constraints**: Unicode emoji support (Unicode 12.0+), graceful degradation if emoji rendering fails, Material Design 3 layout preservation, accessibility (TalkBack) compatibility, bilingual localization support (English/Russian) with language-independent emojis  
**Scale/Scope**: ~15 Android screens, ~50+ UI elements requiring emojis, centralized utility class, full parity with Desktop implementation, support for English and Russian languages

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Rule 1 — Cross-Platform Scope & Parity
✅ **PASS** – Feature explicitly enforces emoji parity between Desktop and Android UI, ensuring cross-platform consistency for accessibility.

### Rule 2 — Source Documents as Authority
✅ **PASS** – Implementation follows Desktop UI emoji mapping from `ui/desktop/src/utils/emojiMapping.ts` as authoritative source.

### Rule 3 — Releases, Installation & Automation
✅ **PASS** – No impact on release automation; emoji implementation is UI enhancement only.

### Rule 4 — Dependency, Vulnerability & Platform Safeguards
✅ **PASS** – Uses Unicode emoji characters (no additional dependencies), graceful degradation handles platform-specific issues.

### Rule 5 — Database & Schema Integrity
✅ **PASS** – No database schema changes required; emoji mapping is compile-time constants.

### Rule 6 — CI, Tooling & Automation Discipline
✅ **PASS** – Emoji validation can be integrated into existing Android UI tests and CI/CD workflows.

### Rule 7 — Security & Privacy Enforcement
✅ **PASS** – Emojis are UI-only enhancements, no impact on security or privacy model.

### Rule 8 — UI Desktop Emoji Accessibility Requirement
✅ **PASS** – This feature directly implements Rule 8 for Android platform, ensuring all UI elements include appropriate emojis matching Desktop implementation. Emojis provide universal visual cues that transcend language barriers (English/Russian), supporting accessibility regardless of language proficiency.

## Project Structure

### Documentation (this feature)

```text
specs/017-constitutional-requirement-rule/
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
├── app/src/main/java/com/truth/training/client/
│   ├── utils/
│   │   └── EmojiMapping.kt          # Centralized emoji mapping utility (NEW, language-independent)
│   └── ui/compose/
│       ├── DashboardScreen.kt        # Add emojis to screen title, buttons, status
│       ├── events/
│       │   ├── EventCreateScreen.kt  # Add emojis to screen title, form labels, buttons
│       │   ├── EventEditScreen.kt    # Add emojis to screen title, form labels, buttons
│       │   ├── EventDetailScreen.kt  # Add emojis to screen title, buttons
│       │   └── EventListScreen.kt    # Add emojis to screen title, navigation
│       ├── contexts/
│       │   ├── ContextTemplateEditorScreen.kt  # Add emojis to screen title, form labels, buttons
│       │   └── ContextTemplateListScreen.kt    # Add emojis to screen title, buttons
│       ├── judgments/
│       │   ├── JudgmentListScreen.kt           # Add emojis to screen title, buttons
│       │   └── JudgmentSubmissionScreen.kt     # Add emojis to screen title, form labels, buttons
│       ├── summary/
│       │   └── OverallSummaryScreen.kt         # Add emojis to screen title, status
│       ├── training/
│       │   └── TrainingResultsScreen.kt        # Add emojis to screen title, status
│       ├── settings/
│       │   └── SettingsScreen.kt               # Add emojis to screen title, form labels, buttons
│       ├── nodes/
│       │   └── NodesScreen.kt                  # Add emojis to screen title, buttons, status
│       ├── components/
│       │   ├── ContextPicker.kt                # Add emojis to field labels
│       │   └── DatePickerField.kt              # Add emojis to field labels
│       └── MainNavigation.kt                   # Add emojis to navigation items
│
├── app/src/main/res/
│   ├── values/
│   │   └── strings.xml                         # English strings (existing, may need new keys)
│   └── values-ru/
│       └── strings.xml                         # Russian strings (existing, may need new keys)
│
└── app/src/androidTest/java/com/truth/training/client/
    └── ui/
        └── EmojiMappingTest.kt                 # Unit tests for emoji mapping utility (NEW)
        └── integration/
            └── EmojiCoverageTest.kt            # Integration tests for emoji presence (NEW)
            └── EmojiLocalizationTest.kt        # Tests for emoji display in both languages (NEW)
```

**Structure Decision**: Mobile Android project structure. Emoji mapping utility resides in `utils/` package matching Desktop structure pattern. All Compose screens require emoji additions to titles, buttons, labels, and status indicators. UI components combine language-independent emojis from `EmojiMapping` utility with localized text from Android string resources (`values/strings.xml` for English, `values-ru/strings.xml` for Russian).

## Complexity Tracking

> **No constitutional violations** - Feature aligns with Rule 8 requirement and maintains platform parity.

## Progress Tracking

### Phase 0: Outline & Research
**Status**: ✅ COMPLETE

1. ✅ Extract unknowns from Technical Context
2. ✅ Research Kotlin emoji handling best practices
3. ✅ Research Compose text rendering with emojis
4. ✅ Analyze Desktop emoji mapping structure
5. ✅ Research accessibility (TalkBack) compatibility
6. ✅ Research bilingual localization integration (English/Russian)
7. ✅ Generate research.md

**Output**: research.md with all technical decisions including localization strategy
**Status**: ✅ COMPLETE → `/home/ekwator/Code/truth-training/specs/017-constitutional-requirement-rule/research.md`

### Phase 1: Design & Contracts
**Status**: ✅ COMPLETE

1. ✅ Extract emoji mapping structure → data-model.md
2. ✅ Generate contract tests for emoji utility
3. ✅ Generate contract documentation for emoji coverage
4. ✅ Extract test scenarios from user stories → quickstart.md
5. ⚠️ Update agent context file (skipped - cursor-agent not recognized, manual update may be needed)

**Output**: data-model.md, contracts/, quickstart.md
**Status**: ✅ COMPLETE → 
- data-model.md at `/home/ekwator/Code/truth-training/specs/017-constitutional-requirement-rule/data-model.md`
- contracts at `/home/ekwator/Code/truth-training/specs/017-constitutional-requirement-rule/contracts/*.md`
- quickstart.md at `/home/ekwator/Code/truth-training/specs/017-constitutional-requirement-rule/quickstart.md`

### Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each screen → emoji implementation task [P]
- Centralized utility → implementation task
- Each user story → integration test task
- Implementation tasks to make tests pass
- Localization testing tasks → verify emoji display in both English and Russian

**Ordering Strategy**:
- TDD order: Tests before implementation
- Dependency order: Centralized utility before screen implementations
- Screen-by-screen: Implement emojis screen by screen for manageable scope
- Localization verification: Test emoji + localized text combination for both languages
