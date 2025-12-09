# Implementation Plan: Full Desktop UI Reconstruction and Synchronization

**Branch**: `016-full-desktop-ui` | **Date**: 2025-12-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-full-desktop-ui/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Full reconstruction and synchronization of Desktop UI visual layer and UI behavior with Android client implementation. The Desktop UI must match Android UI in visual structure, navigation patterns, component behavior, and algorithms while preserving all Desktop-specific functionality. All UI elements must include appropriate emojis for accessibility (constitutional requirement Rule 8). The interface language is English-only (localization removed), but safe database reseeding using temporary tables is required for knowledge base updates.

## Technical Context

**Language/Version**: TypeScript 5.2.2, React 18.2.0, Rust 1.75+ (Tauri backend)
**Primary Dependencies**: 
- Frontend: React 18.2.0, TypeScript 5.2.2, Vite 6.4.1, Tauri 2.9.0, Zustand 4.4.7, Tailwind CSS, Headless UI
- Backend: Tauri (Rust), SQLite (rusqlite 0.31), core_lib (Rust)
- Testing: Jest 29.7.0, React Testing Library, Playwright 1.40.1

**Storage**: SQLite (rusqlite 0.31) via Tauri backend, local database with offline-first design
**Testing**: Jest, React Testing Library, Playwright for E2E, contract tests
**Target Platform**: Desktop (Linux/Windows/macOS) via Tauri
**Project Type**: Desktop application (Tauri + React frontend)
**Performance Goals**: <200ms response times for UI interactions, efficient resource usage
**Constraints**: 
- Must preserve all Desktop-specific functionality (non-Android features)
- Must match Android UI patterns exactly (visual structure, navigation, component behavior)
- All UI elements must include emojis (constitutional requirement Rule 8)
- English-only interface (localization removed)
- Safe database reseeding required (temporary tables approach)
- Offline-first architecture with local-wins conflict resolution
**Scale/Scope**: 
- 8 Desktop screens total (7 synchronized with Android + 1 Desktop-specific: EventSummary)
- All UI components (ContextPicker, DatePickerField, etc.) must match Android patterns
- Flag-based navigation system (template selection, view judgments)
- Safe database reseeding for knowledge base tables (category, forma, cause, develop, effect, context)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Rule 1 — Cross-Platform Scope & Parity
✅ **PASS**: Desktop UI reconstruction maintains platform parity by synchronizing with Android UI implementation. All 7 Desktop screens synchronized with Android will match Android screens in visual structure, navigation, and behavior. EventSummary screen is Desktop-specific and preserved.

### Rule 2 — Source Documents as Authority
✅ **PASS**: Implementation follows Android UI Specification (`docs/ANDROID_UI_SPECIFICATION.md`), Android Implementation Report (`docs/ANDROID_UI_IMPLEMENTATION_REPORT.md`), and functional specifications (`spec/23-function_desktop.md`, `spec/24-function_mobile_android.md`).

### Rule 3 — Releases, Installation & Automation
✅ **PASS**: Desktop UI remains installable for Linux/Windows/macOS. No changes to release automation required for this feature.

### Rule 4 — Dependency, Vulnerability & Platform Safeguards
✅ **PASS**: No new dependencies introduced. Existing dependencies (React, TypeScript, Tauri, etc.) remain unchanged. Security scans continue as per existing process.

### Rule 5 — Database & Schema Integrity
✅ **PASS**: Safe database reseeding using temporary tables maintains FK → PK integrity. Schema changes follow documented migration process. All knowledge base tables maintain proper foreign key relationships.

### Rule 6 — CI, Tooling & Automation Discipline
✅ **PASS**: Spec-Kit artifacts (spec, plan, research, data-model, contracts, quickstart) are created and versioned. All templates remain aligned.

### Rule 7 — Security & Privacy Enforcement
✅ **PASS**: No changes to security model. Privacy rules (no persistent identity, no telemetry, no plaintext secrets) remain enforced.

### Rule 8 — UI Desktop Emoji Accessibility Requirement
✅ **PASS**: All Desktop UI interface elements MUST include appropriate emojis. This is a core requirement of this feature (FR-007, FR-008, FR-009). Emojis must be semantically meaningful, consistent across similar functionality, and validated during UI development, code review, and release automation.

**Constitution Check Result**: ✅ **ALL RULES PASS**

## Project Structure

### Documentation (this feature)

```text
specs/016-full-desktop-ui/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── database-reseeding.md
│   └── navigation-state.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
ui/desktop/
├── src/
│   ├── pages/               # 8 Desktop screens (Dashboard, NewEvent, ContextEditor, EventSummary, Events, Judgments, OverallSummary, TrainingResults, Settings)
│   │   ├── Dashboard.tsx
│   │   ├── NewEvent.tsx
│   │   ├── ContextEditor.tsx
│   │   ├── EventSummary.tsx (Desktop-specific, preserved)
│   │   ├── Events.tsx (includes EventDetail/EventEdit as modals/inline forms)
│   │   ├── Judgments.tsx
│   │   ├── OverallSummary.tsx
│   │   ├── TrainingResults.tsx
│   │   └── Settings.tsx
│   ├── components/          # React components
│   │   ├── context/
│   │   │   └── ContextPicker.tsx
│   │   ├── DatePickerField.tsx
│   │   ├── layout/
│   │   │   ├── TopMenuBar.tsx
│   │   │   └── LocaleToggle.tsx (to be removed/disabled for English-only)
│   │   ├── Dashboard/
│   │   │   ├── CreateEventButton.tsx
│   │   │   └── EventCard.tsx
│   │   ├── JudgmentPanel/
│   │   │   └── JudgmentCard.tsx
│   │   ├── NodesPanel.tsx
│   │   └── system/
│   │       ├── ErrorBoundary.tsx
│   │       ├── Modal.tsx
│   │       ├── SyncStatus.tsx
│   │       ├── ThemeProvider.tsx
│   │       └── Toaster.tsx
│   ├── services/            # API, offline queue, validation
│   │   ├── api.ts
│   │   ├── offline.ts
│   │   ├── offlineQueue.ts
│   │   ├── validation.ts
│   │   └── sync.ts
│   ├── stores/              # Zustand state management
│   │   ├── events.ts
│   │   ├── contextEditor.ts
│   │   ├── judgments.ts
│   │   ├── navigation.ts (flag-based routing)
│   │   ├── sync.ts
│   │   └── templateContext.ts
│   ├── utils/               # Utility functions
│   │   ├── dateNormalization.ts (must match Android algorithm)
│   │   ├── entityResolution.ts (context field name resolution)
│   │   ├── emojiMapping.ts (emoji mapping system)
│   │   └── validation.ts (must match Android validation rules)
│   ├── types/               # TypeScript types
│   │   ├── api.ts
│   │   ├── contexts.ts
│   │   ├── events.ts
│   │   ├── judgments.ts
│   │   ├── navigation.ts
│   │   ├── emoji.ts
│   │   └── knowledgeBase.ts
│   ├── i18n/                # Localization (to be removed/disabled for English-only)
│   │   ├── index.ts
│   │   └── ru.ts (to be removed)
│   ├── App.tsx               # Main app component with routing
│   └── main.tsx              # Entry point
├── src-tauri/
│   ├── src/
│   │   ├── commands/         # Tauri commands
│   │   │   ├── events.rs
│   │   │   ├── contexts.rs
│   │   │   ├── judgments.rs
│   │   │   ├── knowledge_base.rs (safe reseeding with temp tables)
│   │   │   ├── summary.rs
│   │   │   └── config.rs
│   │   ├── storage.rs        # SQLite wrapper with safe reseeding
│   │   └── lib.rs
│   └── Cargo.toml
└── tests/
    ├── contract/             # Contract tests
    ├── integration/          # Integration tests
    └── e2e/                  # E2E tests with Playwright
```

**Structure Decision**: Desktop UI follows existing Tauri + React structure. All screens and components are in `ui/desktop/src/`. Tauri backend commands are in `ui/desktop/src-tauri/src/commands/`. Safe database reseeding is implemented in `storage.rs` and `knowledge_base.rs` command. Flag-based navigation state is managed in `stores/navigation.ts` (Zustand store equivalent to Android's `savedStateHandle`). EventDetail and EventEdit functionality (Android screens 4-5) are implemented within Events screen using modal dialogs or inline forms, not as separate screens. EventSummary is Desktop-specific and preserved unchanged.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations - all constitution rules pass.

---

## Implementation Tasks & Strategy

**Reference**: Complete task breakdown is available in [tasks.md](tasks.md). This section provides an overview and execution strategy.

### Task Overview

**Total Tasks**: 108 tasks organized across 7 phases:
- **Phase 1: Setup** (5 tasks) - Project initialization and structure verification
- **Phase 2: Foundational** (12 tasks) - Core infrastructure (BLOCKS all user stories)
- **Phase 3: User Story 1** (21 tasks) - Visual Synchronization (P1, MVP)
- **Phase 4: User Story 2** (19 tasks) - Emoji Accessibility (P1)
- **Phase 5: User Story 3** (14 tasks) - Safe Database Reseeding (P2)
- **Phase 6: User Story 4** (9 tasks) - Desktop-Specific Preservation (P1)
- **Phase 7: Polish** (28 tasks) - Cross-cutting concerns

### Execution Dependencies

**Critical Path**:
1. **Phase 1 (Setup)** → No dependencies, can start immediately
2. **Phase 2 (Foundational)** → Depends on Setup, **BLOCKS all user stories**
3. **User Stories (Phase 3-6)** → All depend on Foundational completion
   - Can proceed in parallel after Foundational (if team capacity allows)
   - Or sequentially in priority order (P1 → P2)
4. **Phase 7 (Polish)** → Depends on all desired user stories being complete

**User Story Dependencies**:
- **US1 (P1)**: Independent after Foundational
- **US2 (P1)**: Can start after Foundational, depends on US1 for screen components (emoji mapping can be parallel)
- **US3 (P2)**: Independent after Foundational, can run parallel with US1/US2
- **US4 (P1)**: Independent verification task, can run parallel

### Implementation Strategies

#### Strategy 1: MVP First (User Story 1 Only)
1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Visual Synchronization)
4. **STOP and VALIDATE**: Test User Story 1 independently, compare with Android UI
5. Deploy/demo if ready

#### Strategy 2: Incremental Delivery
1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP - Visual Sync!)
3. Add User Story 2 → Test independently → Deploy/Demo (Emoji Accessibility)
4. Add User Story 3 → Test independently → Deploy/Demo (Safe Reseeding)
5. Add User Story 4 → Test independently → Deploy/Demo (Feature Preservation)
6. Add Polish → Final release
7. Each story adds value without breaking previous stories

#### Strategy 3: Parallel Team Strategy
With multiple developers:
1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Visual Synchronization)
   - Developer B: User Story 2 (Emoji Accessibility) - can start after US1 screens are updated
   - Developer C: User Story 3 (Safe Reseeding) - independent
   - Developer D: User Story 4 (Feature Preservation) - verification task
3. Stories complete and integrate independently
4. Polish phase: All developers work on cleanup and documentation

### Test Requirements

**Critical Test Requirements** (from tasks.md):
- All tests MUST verify logic against Android UI specification (`docs/ANDROID_UI_SPECIFICATION.md`) and Android functional specification (`spec/24-function_mobile_android.md`)
- Test logic must match Android client interface implementation patterns
- After creating each test file, syntax validation MUST be performed:
  - TypeScript/JavaScript: `npm run test:lint` and `npm run test:type-check`
  - Playwright E2E: Playwright syntax check
- Tests should verify Desktop UI behavior matches Android UI behavior exactly
- Write tests FIRST, ensure they FAIL before implementation

### Key Implementation Notes

- **[P] tasks**: Can run in parallel (different files, no dependencies)
- **[Story] label**: Maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Desktop-specific functionality must be preserved - mark with comments
- All UI elements must include emojis (constitutional requirement Rule 8)
- Algorithms must match Android exactly (date normalization, entity resolution, validation)
- Visual structure must match Android screens exactly
- No E2E test for full screen navigation - focus on specific flows and component behavior

### Task Execution Order

**Within Each User Story**:
1. Tests (if included) MUST be written and FAIL before implementation
2. Models/utilities before components
3. Components before screens
4. Core implementation before integration
5. Story complete before moving to next priority

**Parallel Opportunities**:
- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models/utilities within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members
- Polish tasks marked [P] can run in parallel

---

## Progress Tracking

### Phase 0: Research & Technology Decisions
- [x] Analyze Android UI Specification and Implementation Report
- [x] Review Desktop UI current structure and components
- [x] Research flag-based navigation patterns in React/TypeScript
- [x] Research safe database reseeding with temporary tables
- [x] Research emoji implementation patterns for accessibility
- [x] Document Android UI algorithms (context field visibility, date normalization)
- [x] Identify Desktop-specific functionality to preserve
- [x] Create research.md

**Status**: ✅ **COMPLETE**

### Phase 1: Design & Architecture
- [x] Design flag-based navigation system (React equivalent to Android savedStateHandle)
- [x] Design safe database reseeding algorithm with temporary tables
- [x] Design emoji mapping system for UI elements
- [x] Design component synchronization patterns (ContextPicker, DatePickerField)
- [x] Design screen layout synchronization (8 Desktop screens: 7 synchronized + 1 Desktop-specific)
- [x] Design state management patterns (Zustand stores for navigation flags)
- [x] Create data-model.md
- [x] Create contracts/ directory with API contracts
- [x] Create quickstart.md

**Status**: ✅ **COMPLETE**

### Phase 2: Task Breakdown
- [x] Generate tasks.md via /speckit.tasks command
- [x] Organize tasks by user story priority
- [x] Define dependencies and execution order
- [x] Integrate tasks.md with plan.md

**Status**: ✅ **COMPLETE**

---

## Execution Flow

### Phase 0: Research (COMPLETE)

Research document created at `specs/016-full-desktop-ui/research.md` covering:
- Android UI Specification analysis
- Desktop UI current structure review
- Flag-based navigation patterns research
- Safe database reseeding research
- Emoji implementation patterns
- Algorithm synchronization requirements
- Desktop-specific functionality identification

**Key Decisions**:
- EventDetail and EventEdit (Android screens 4-5) implemented within Events screen using modals/inline forms
- EventSummary preserved as Desktop-specific screen (not synchronized with Android)
- Android UI Specification and implementation code are sources of truth for algorithms
- Performance metrics deferred to implementation phase

### Phase 1: Design (COMPLETE)

Design artifacts created:
1. **data-model.md**: ✅ Data models for navigation flags, emoji elements, temporary tables, component state, algorithms
2. **contracts/**: ✅ API contracts for safe reseeding (`database-reseeding.md`), navigation state management (`navigation-state.md`)
3. **quickstart.md**: ✅ Quick start guide for Desktop UI reconstruction with step-by-step implementation instructions

**Key Design Decisions**:
- Navigation state managed via Zustand store (equivalent to Android's `savedStateHandle`)
- Emoji mapping system with helper function `getEmoji()` for consistent emoji assignment
- Temporary tables approach for safe database reseeding with atomic swap
- Context field visibility: all fields always visible (matching Android algorithm)
- Date normalization and entity resolution algorithms match Android exactly

### Phase 2: Task Breakdown (COMPLETE)

Tasks document created at `specs/016-full-desktop-ui/tasks.md` with:
- 108 tasks organized by user story priority (US1, US2, US3, US4)
- 7 phases: Setup, Foundational, 4 User Stories, Polish
- Parallel execution opportunities marked with [P]
- Dependencies and execution order defined
- Test tasks for all user stories and contracts
- Implementation strategy (MVP First, Incremental Delivery, Parallel Team)

**Integration**: Tasks.md integrated with plan.md - see "Implementation Tasks & Strategy" section above.

**Next Steps**: Proceed with implementation following tasks.md execution order and strategies outlined in plan.md.
