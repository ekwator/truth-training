# Tasks: Desktop UI Synchronization Based on Android Client Implementation

**Input**: Design documents from `/specs/015-request-desktop-ui/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Desktop UI**: `ui/desktop/src/` for frontend, `ui/desktop/src-tauri/src/` for backend
- **Components**: `ui/desktop/src/components/`
- **Pages**: `ui/desktop/src/pages/`
- **Stores**: `ui/desktop/src/stores/`
- **Services**: `ui/desktop/src/services/`
- **Backend**: `ui/desktop/src-tauri/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure verification

- [x] T001 Verify project structure matches implementation plan in `ui/desktop/`
- [x] T002 [P] Verify TypeScript, React, and Tauri dependencies are up to date
- [x] T003 [P] Verify linting and formatting tools are configured (ESLint, Prettier)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create navigation store in `ui/desktop/src/stores/navigation.ts` with flags (selectTemplateForEvent, viewJudgments)
- [x] T005 [P] Create template context store in `ui/desktop/src/stores/templateContext.ts` for template context passing
- [x] T006 [P] Create entity name resolution utility in `ui/desktop/src/utils/entityResolution.ts` with getEntityNameById function
- [x] T007 Create date normalization utility in `ui/desktop/src/utils/dateNormalization.ts` with normalizeToStartOfDay function
- [x] T008 Create validation utilities in `ui/desktop/src/utils/validation.ts` for event and template validation
- [x] T009 Verify database schema supports all required tables (truth_events, impact, progress_metrics, knowledge base tables)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Visual UI Parity with Android (Priority: P1) 🎯 MVP

**Goal**: Desktop UI screens match Android UI visual structure, navigation flow, and component behavior

**Independent Test**: Compare Desktop UI screens side-by-side with Android screens and verify visual structure, navigation patterns, and component behaviors match

### Implementation for User Story 1

- [x] T010 [P] [US1] Update Dashboard screen in `ui/desktop/src/pages/Dashboard.tsx` to match Android Dashboard structure (sync status card, quick stats, action buttons)
- [x] T011 [P] [US1] Update Event List screen in `ui/desktop/src/pages/Events.tsx` to match Android Event List structure
- [x] T012 [P] [US1] Update Event Detail view in `ui/desktop/src/pages/EventSummary.tsx` to match Android Event Detail structure with context field display (EventSummary.tsx contains both Detail and Edit modes via isEditing state)
- [x] T013 [P] [US1] Update Event Edit view in `ui/desktop/src/pages/EventSummary.tsx` to match Android Event Edit structure with read-only fields (EventSummary.tsx contains both Detail and Edit modes via isEditing state)
- [x] T014 [P] [US1] Update Context Templates screen in `ui/desktop/src/pages/ContextEditor.tsx` to match Android Context Templates structure
- [x] T015 [P] [US1] Update New Template screen in `ui/desktop/src/pages/ContextEditor.tsx` to match Android New Template structure
- [x] T016 [P] [US1] Update Judgments screen in `ui/desktop/src/pages/Judgments.tsx` to match Android Judgments structure
- [x] T017 [P] [US1] Update Judgment Submission screen in `ui/desktop/src/pages/Judgments.tsx` to match Android Judgment Submission structure
- [x] T018 [P] [US1] Update Overall Summary screen in `ui/desktop/src/pages/OverallSummary.tsx` to match Android Overall Summary structure
- [x] T019 [P] [US1] Update Training Results screen in `ui/desktop/src/pages/TrainingResults.tsx` to match Android Training Results structure
- [x] T020 [P] [US1] Update Settings screen in `ui/desktop/src/pages/Settings.tsx` to match Android Settings structure
- [x] T020a [P] [US1] Update NodesPanel component in `ui/desktop/src/components/NodesPanel.tsx` to match Android Nodes screen structure (Nodes functionality is embedded in Dashboard via NodesPanel component, not a separate screen)
- [x] T021 [P] [US1] Implement context field display algorithm in Event Detail view using entity name resolution with fallback to ID (completed in T012)
- [x] T022 [P] [US1] Implement context field display algorithm in Event Edit view using entity name resolution with fallback to ID (completed in T013)
- [x] T023 [P] [US1] Implement context field display algorithm in Context Templates screen using entity name resolution with fallback to ID

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Component Parity (Priority: P1)

**Goal**: Desktop UI components behave identically to their Android equivalents

**Independent Test**: Use each component on Desktop and verify it matches Android component behavior, validation, and error handling

### Implementation for User Story 2

- [x] T024 [US2] Update ContextPicker component in `ui/desktop/src/components/context/ContextPicker.tsx` to match Android ContextPicker behavior (validation, error handling, entity name resolution)
- [x] T025 [US2] Create DatePickerField component in `ui/desktop/src/components/DatePickerField.tsx` matching Android DatePickerField (date normalization, validation rules, clear capability)
- [x] T026 [US2] Implement template selection UI in NewEvent screen in `ui/desktop/src/pages/NewEvent.tsx` with flag-based navigation and form pre-filling
- [x] T027 [US2] Update Event Edit view in EventSummary.tsx to implement read-only fields display and editable fields with validation matching Android
- [x] T028 [US2] Implement corrected flag auto-calculation algorithm in Event Edit view (tracks initial End Timestamp, auto-sets when changed)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Localization System Fix (Priority: P1)

**Goal**: Switch between English and Russian languages on Desktop with UI updates and event data preservation

**Independent Test**: Switch language in Settings, verify UI updates, check that event data is preserved, and confirm context fields display correctly with localized names

### Implementation for User Story 3

- [x] T029 [US3] Audit all UI components for hardcoded strings and create localization key mapping document (completed during Phase 3-4 implementation)
- [x] T030 [P] [US3] Replace hardcoded strings in Dashboard screen with i18n keys (completed in T010)
- [x] T031 [P] [US3] Replace hardcoded strings in NewEvent screen with i18n keys (completed in T026)
- [x] T032 [P] [US3] Replace hardcoded strings in Event List screen with i18n keys (completed in T011)
- [x] T033 [P] [US3] Replace hardcoded strings in Event Detail screen with i18n keys (completed in T012)
- [x] T034 [P] [US3] Replace hardcoded strings in Event Edit screen with i18n keys (completed in T013)
- [x] T035 [P] [US3] Replace hardcoded strings in Context Templates screen with i18n keys (completed in T014)
- [x] T036 [P] [US3] Replace hardcoded strings in New Template screen with i18n keys (completed in T015)
- [x] T037 [P] [US3] Replace hardcoded strings in Judgments screen with i18n keys (completed in T016)
- [x] T038 [P] [US3] Replace hardcoded strings in Judgment Submission screen with i18n keys (completed in T017)
- [x] T039 [P] [US3] Replace hardcoded strings in Overall Summary screen with i18n keys (completed in T018)
- [x] T040 [P] [US3] Replace hardcoded strings in Training Results screen with i18n keys (completed in T019)
- [x] T041 [P] [US3] Replace hardcoded strings in Settings screen with i18n keys (completed in T020)
- [x] T042 [US3] Update Russian translations in `ui/desktop/src/i18n/ru.ts` to match Android `values-ru/strings.xml` structure
- [x] T043 [US3] Update English translations in `ui/desktop/src/i18n/index.ts` to match Android `values/strings.xml` structure
- [x] T044 [US3] Implement temporary tables solution for database re-seeding in `ui/desktop/src-tauri/src/commands/config.rs` (create temp tables, copy data, clear knowledge base, seed new locale, restore data, drop temp tables). Handle edge case: If database re-seeding fails, transaction must rollback and previous language must be restored
- [x] T045 [US3] Update reseed_knowledge_base Tauri command to use temporary tables solution with transaction safety and rollback mechanism on failure
- [x] T046 [US3] Implement clear_context_templates Tauri command in `ui/desktop/src-tauri/src/commands/contexts.rs`
- [x] T047 [US3] Update Settings screen language change handler to call clear_context_templates and reseed_knowledge_base, with form state preservation warning if user is editing a form
- [x] T048 [US3] Ensure context field display updates reactively after language change using useMemo with proper dependencies (completed in T012-T013, EventSummary uses useMemo with proper dependencies)
- [x] T048a [US3] Handle edge case: If knowledge base entities are not loaded yet, context fields should fallback to displaying ID (covered by T006 entity name resolution utility)
- [x] T048b [US3] Handle edge case: If template selection fails, display error message and handle navigation gracefully (covered by T026, T049 template selection implementation)

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work independently

---

## Phase 6: User Story 4 - Navigation Synchronization (Priority: P2)

**Goal**: Desktop navigation follows the same patterns as Android (template selection flow, view judgments flow, etc.)

**Independent Test**: Navigate through all screens and verify that navigation flows match Android patterns, including flag-based conditional routing

### Implementation for User Story 4

- [x] T049 [US4] Implement flag-based navigation for template selection flow (NewEvent → Context Templates → NewEvent with fields filled)
- [x] T050 [US4] Implement flag-based navigation for view judgments flow (Dashboard → Events List → Judgments screen)
- [x] T051 [US4] Update navigation logic in `ui/desktop/src/App.tsx` to support flag-based conditional routing
- [x] T052 [US4] Ensure back navigation follows same back stack behavior as Android (implemented with navigation stack and Escape key)
- [x] T053 [US4] Preserve Desktop keyboard shortcuts (Alt+1 through Alt+8) while implementing new navigation patterns

**Checkpoint**: At this point, User Stories 1, 2, 3, AND 4 should all work independently

---

## Phase 7: User Story 5 - Validation Rules Parity (Priority: P2)

**Goal**: Desktop form validation matches Android validation rules exactly

**Independent Test**: Submit forms with invalid data and verify that validation errors match Android validation behavior

### Implementation for User Story 5

- [x] T054 [US5] Implement event validation rules in `ui/desktop/src/utils/validation.ts` (name required, description required, all context fields required, date validation)
- [x] T055 [US5] Implement template validation rules in `ui/desktop/src/utils/validation.ts` (name required, all context fields required, duplicate detection based on context fields only)
- [x] T056 [US5] Update NewEvent screen to use validation utilities and display inline error messages matching Android
- [x] T057 [US5] Update ContextEditor screen to use validation utilities and display inline error messages matching Android
- [x] T058 [US5] Update DatePickerField component to validate date range (End >= Start, can be equal) with normalized dates, handling timezone and DST edge cases by normalizing to start of day consistently (DatePickerField already uses normalizeToStartOfDay)
- [x] T059 [US5] Implement duplicate template detection in Tauri backend command matching Android behavior (compare non-NULL fields only)
- [x] T060 [US5] Ensure validation error messages match Android error messages exactly

**Checkpoint**: All user stories should now be independently functional

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T061 [P] Update documentation in `docs/UI_Desktop.md` to reflect synchronized UI
- [x] T062 [P] Add unit tests for entity name resolution utility in `ui/desktop/src/utils/__tests__/entityResolution.test.ts`
- [x] T063 [P] Add unit tests for date normalization utility in `ui/desktop/src/utils/__tests__/dateNormalization.test.ts`
- [x] T064 [P] Add unit tests for validation utilities in `ui/desktop/src/utils/__tests__/validation.test.ts`
- [x] T065 [P] Add integration tests for template selection flow in `ui/desktop/src/pages/__tests__/templateSelection.integration.test.tsx`
- [x] T066 [P] Add integration tests for language change flow in `ui/desktop/src/pages/__tests__/languageChange.integration.test.tsx`
- [x] T067 [P] Add E2E tests for context field display after language change in `ui/desktop/tests/e2e/contextFieldDisplay.e2e.test.ts`
- [x] T068 Performance optimization: Ensure language switching completes in under 5 seconds (measure from language selection click to UI update completion, including database re-seeding; add performance logging and timing metrics to T044-T045)
- [x] T069 Code cleanup: Remove any unused code or commented-out sections
- [x] T070 Run quickstart.md validation scenarios to verify all integration scenarios work
- [x] T071 Verify all Desktop-specific functional features remain intact (command-line tools, developer tools, Desktop-only workflows). Explicitly verify FR-013 compliance: no Desktop-only interaction flows or underlying Desktop logic layer are modified; only UI structure, rendering, and navigation behavior are changed (verified: keyboard shortcuts preserved, Desktop-specific features intact, only UI layer modified)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Depends on US1 for screen structure
- **User Story 3 (P1)**: Can start after Foundational (Phase 2) - Can work in parallel with US1/US2
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) - Depends on US1 for navigation structure
- **User Story 5 (P2)**: Can start after Foundational (Phase 2) - Depends on US2 for component structure

### Within Each User Story

- Models/utilities before components
- Components before screens
- Screens before integration
- Core implementation before polish
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, user stories can start in parallel (if team capacity allows)
- All screen updates within US1 marked [P] can run in parallel
- All string replacement tasks within US3 marked [P] can run in parallel
- All polish tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all screen updates for User Story 1 together:
Task: "Update Dashboard screen in ui/desktop/src/pages/Dashboard.tsx"
Task: "Update Event List screen in ui/desktop/src/pages/Events.tsx"
Task: "Update Event Detail view in ui/desktop/src/pages/EventSummary.tsx"
Task: "Update Event Edit view in ui/desktop/src/pages/EventSummary.tsx"
Task: "Update Context Templates screen in ui/desktop/src/pages/ContextEditor.tsx"
Task: "Update NodesPanel component in ui/desktop/src/components/NodesPanel.tsx"
# ... etc
```

## Parallel Example: User Story 3

```bash
# Launch all string replacement tasks for User Story 3 together:
Task: "Replace hardcoded strings in Dashboard screen with i18n keys"
Task: "Replace hardcoded strings in NewEvent screen with i18n keys"
Task: "Replace hardcoded strings in Event List screen with i18n keys"
# ... etc
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 3 Only - All P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Visual UI Parity)
4. Complete Phase 4: User Story 2 (Component Parity)
5. Complete Phase 5: User Story 3 (Localization System Fix)
6. **STOP and VALIDATE**: Test all P1 stories independently
7. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (Visual Parity MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (Component Parity!)
4. Add User Story 3 → Test independently → Deploy/Demo (Localization Fix!)
5. Add User Story 4 → Test independently → Deploy/Demo (Navigation Sync!)
6. Add User Story 5 → Test independently → Deploy/Demo (Validation Parity!)
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Visual UI Parity)
   - Developer B: User Story 2 (Component Parity) - after US1 screens are ready
   - Developer C: User Story 3 (Localization) - can work in parallel
3. After P1 stories complete:
   - Developer A: User Story 4 (Navigation)
   - Developer B: User Story 5 (Validation)
4. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify all changes preserve Desktop-specific functional features
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Reference Android UI specification (`docs/ANDROID_UI_SPECIFICATION.md`) for exact behavior
- Use component contracts (`contracts/component-contracts.md`) for component specifications
- Use API contracts (`contracts/api-contracts.md`) for backend command specifications

---

**Total Tasks**: 75 (including edge case handling tasks)  
**P1 Tasks**: 50 (T001-T048, T048a-T048b)  
**P2 Tasks**: 10 (T049-T060)  
**Polish Tasks**: 13 (T061-T071, T020a)  
**Parallel Tasks**: 37+ (marked with [P])

