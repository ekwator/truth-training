# Tasks: Full Desktop UI Reconstruction and Synchronization

**Input**: Design documents from `/specs/016-full-desktop-ui/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included for all user stories and contracts to ensure quality and synchronization verification.

**Version Control**: All changes will be committed as a single commit to the main branch. Do NOT create commits during task execution. Do NOT create pull requests.

**Test Requirements**:
- All tests MUST verify logic against Android UI specification (`docs/ANDROID_UI_SPECIFICATION.md`) and Android functional specification (`spec/24-function_mobile_android.md`)
- Test logic must match Android client interface implementation patterns
- After creating each test file, syntax validation MUST be performed (TypeScript/JavaScript syntax check, Jest/Playwright test structure validation)
- Tests should verify Desktop UI behavior matches Android UI behavior exactly

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Desktop UI**: `ui/desktop/src/` for frontend, `ui/desktop/src-tauri/src/` for backend
- **Tests**: `ui/desktop/tests/` (contract/, integration/, e2e/)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure verification

- [x] T001 Verify Desktop UI project structure exists at `ui/desktop/`
- [x] T002 [P] Verify TypeScript/React dependencies (React 18.2.0, TypeScript 5.2.2, Vite 6.4.1, Tauri 2.9.0, Zustand 4.4.7)
- [x] T003 [P] Verify Rust/Tauri dependencies (Tauri 2.9.0, rusqlite 0.31, core_lib)
- [x] T004 [P] Verify testing dependencies (Jest 29.7.0, React Testing Library, Playwright 1.40.1)
- [x] T005 [P] Configure linting and formatting tools (ESLint, Prettier, rustfmt)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Create Navigation State Model (Zustand store) in `ui/desktop/src/stores/navigation.ts` with flag-based routing interface
- [x] T007 [P] Create Emoji Mapping Model in `ui/desktop/src/utils/emojiMapping.ts` with default emoji mappings
- [x] T008 [P] Create Date Normalization utility in `ui/desktop/src/utils/dateNormalization.ts` with `normalizeToStartOfDay()` and `validateDateRange()` functions
- [x] T009 [P] Create Entity Resolution utility in `ui/desktop/src/utils/entityResolution.ts` with `resolveContextFieldName()` function
- [x] T010 Create safe database reseeding command structure in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (Tauri command interface)
- [x] T011 Create temporary table creation functions in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (create_temp_tables)
- [x] T012 Create temporary table data insertion functions in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (fill_temp_tables with English-only data)
- [x] T013 Create atomic swap function in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (atomic_swap with transaction)
- [x] T014 Create UI refresh event emission in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (emit_knowledge_base_refreshed)
- [x] T015 Create TypeScript types for navigation state in `ui/desktop/src/types/navigation.ts`
- [x] T016 [P] Create TypeScript types for emoji mapping in `ui/desktop/src/types/emoji.ts`
- [x] T017 [P] Create TypeScript types for reseeding result in `ui/desktop/src/types/knowledgeBase.ts`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Desktop UI Visual Synchronization with Android (Priority: P1) 🎯 MVP

**Goal**: Desktop UI matches Android UI in visual structure, navigation patterns, and component behavior

**Independent Test**: Compare Desktop UI screens side-by-side with Android UI screens, verify navigation flows, component layouts, and visual patterns match

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation. Test logic MUST be verified against Android UI specification (`docs/ANDROID_UI_SPECIFICATION.md`) and Android functional specification (`spec/24-function_mobile_android.md`)**

- [x] T018 [P] [US1] Contract test for navigation state store in `ui/desktop/tests/contract/navigation-state.test.ts` (verify against Android Navigation Structure and savedStateHandle patterns)
- [x] T018a [US1] Verify test syntax for navigation state contract test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/contract/navigation-state.test.ts`
- [x] T019 [P] [US1] Integration test for template selection flow in `ui/desktop/tests/integration/template-selection-flow.test.ts` (verify against Android Template Selection Flow algorithm)
- [x] T019a [US1] Verify test syntax for template selection flow test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/template-selection-flow.test.ts`
- [x] T020 [P] [US1] Integration test for view judgments flow in `ui/desktop/tests/integration/view-judgments-flow.test.ts` (verify against Android View Judgments Flow algorithm)
- [x] T020a [US1] Verify test syntax for view judgments flow test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/view-judgments-flow.test.ts`

### Implementation for User Story 1

- [x] T022 [US1] Implement Navigation State Store (Zustand) in `ui/desktop/src/stores/navigation.ts` with all flag-based routing actions (depends on T006)
- [x] T023 [US1] Update Dashboard screen in `ui/desktop/src/pages/Dashboard.tsx` to match Android Dashboard layout and behavior
- [x] T024 [US1] Update NewEvent screen in `ui/desktop/src/pages/NewEvent.tsx` to match Android New Event screen layout, implement template selection flow
- [x] T025 [US1] Update ContextEditor screen in `ui/desktop/src/pages/ContextEditor.tsx` to match Android Context Templates screen, implement template selection and creation flows
- [x] T026 [US1] Update Events screen in `ui/desktop/src/pages/Events.tsx` to match Android Event List screen, implement view judgments flag handling
- [x] T027 [US1] Update Judgments screen in `ui/desktop/src/pages/Judgments.tsx` to match Android Judgments screen layout and behavior
- [x] T028 [US1] Update OverallSummary screen in `ui/desktop/src/pages/OverallSummary.tsx` to match Android Overall Summary screen
- [x] T029 [US1] Update TrainingResults screen in `ui/desktop/src/pages/TrainingResults.tsx` to match Android Training Results screen
- [x] T030 [US1] Update Settings screen in `ui/desktop/src/pages/Settings.tsx` to match Android Settings screen (remove localization toggle)
- [x] T031 [US1] Synchronize ContextPicker component in `ui/desktop/src/components/context/ContextPicker.tsx` to match Android ExposedDropdownMenuBox pattern
- [x] T032 [US1] Synchronize DatePickerField component in `ui/desktop/src/components/DatePickerField.tsx` to match Android DatePicker pattern with validation
- [x] T033 [US1] Synchronize EventCard component in `ui/desktop/src/components/Dashboard/EventCard.tsx` to match Android Event Card layout
- [x] T034 [US1] Implement context field visibility rules matching Android algorithm (all fields always visible)
- [x] T035 [US1] Implement date normalization algorithm matching Android in `ui/desktop/src/utils/dateNormalization.ts` (depends on T008)
- [x] T036 [US1] Implement entity resolution algorithm matching Android in `ui/desktop/src/utils/entityResolution.ts` (depends on T009)
- [x] T037 [US1] Update TopMenuBar component in `ui/desktop/src/components/layout/TopMenuBar.tsx` to support flag-based navigation
- [x] T038 [US1] Update App.tsx in `ui/desktop/src/App.tsx` to integrate navigation state store with screen routing

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Desktop UI screens match Android screens visually and behaviorally.

---

## Phase 4: User Story 2 - Emoji Accessibility for All UI Elements (Priority: P1)

**Goal**: All Desktop UI interface elements include appropriate emojis for accessibility (constitutional requirement Rule 8)

**Independent Test**: Visually inspect every button, menu item, navigation link, form label, and status indicator in Desktop UI to verify emoji presence

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation. Test logic MUST be verified against Android UI specification for UI element patterns**

- [x] T039 [P] [US2] Contract test for emoji mapping system in `ui/desktop/tests/contract/emoji-mapping.test.ts` (verify emoji mapping matches constitutional requirement Rule 8)
- [x] T039a [US2] Verify test syntax for emoji mapping contract test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/contract/emoji-mapping.test.ts`
- [x] T040 [P] [US2] Integration test for emoji presence in all UI elements in `ui/desktop/tests/integration/emoji-accessibility.test.ts` (verify all UI elements have emojis as per Rule 8)
- [x] T040a [US2] Verify test syntax for emoji accessibility integration test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/emoji-accessibility.test.ts`
- [x] T041 [P] [US2] E2E test for emoji consistency across application in `ui/desktop/tests/e2e/emoji-consistency.spec.ts` (verify emoji consistency patterns)
- [x] T041a [US2] Verify test syntax for emoji consistency E2E test: Run `npm run test:lint` and Playwright syntax check on `ui/desktop/tests/e2e/emoji-consistency.spec.ts`

### Implementation for User Story 2

- [x] T042 [US2] Implement emoji mapping system in `ui/desktop/src/utils/emojiMapping.ts` with helper function `getEmoji()` (depends on T007)
- [x] T043 [US2] Add emojis to all buttons in Dashboard screen (`ui/desktop/src/pages/Dashboard.tsx`)
- [x] T044 [US2] Add emojis to all form labels and buttons in NewEvent screen (`ui/desktop/src/pages/NewEvent.tsx`)
- [x] T045 [US2] Add emojis to all form labels and buttons in ContextEditor screen (`ui/desktop/src/pages/ContextEditor.tsx`)
- [x] T046 [US2] Add emojis to all navigation items and buttons in Events screen (`ui/desktop/src/pages/Events.tsx`)
- [x] T047 [US2] Add emojis to all form labels and buttons in Judgments screen (`ui/desktop/src/pages/Judgments.tsx`)
- [x] T048 [US2] Add emojis to all labels and buttons in OverallSummary screen (`ui/desktop/src/pages/OverallSummary.tsx`)
- [x] T049 [US2] Add emojis to all labels and buttons in TrainingResults screen (`ui/desktop/src/pages/TrainingResults.tsx`)
- [x] T050 [US2] Add emojis to all labels and buttons in Settings screen (`ui/desktop/src/pages/Settings.tsx`)
- [x] T051 [US2] Add emojis to ContextPicker component labels in `ui/desktop/src/components/context/ContextPicker.tsx`
- [x] T052 [US2] Add emojis to DatePickerField component labels in `ui/desktop/src/components/DatePickerField.tsx`
- [x] T053 [US2] Add emojis to EventCard component elements in `ui/desktop/src/components/Dashboard/EventCard.tsx`
- [x] T054 [US2] Add emojis to TopMenuBar navigation items in `ui/desktop/src/components/layout/TopMenuBar.tsx`
- [x] T055 [US2] Add emojis to SyncStatus component in `ui/desktop/src/components/system/SyncStatus.tsx`
- [x] T056 [US2] Add emojis to all status indicators across application
- [x] T057 [US2] Verify emoji consistency across similar functionality (same function = same emoji)

**Checkpoint**: At this point, User Story 2 should be fully functional. All UI elements include appropriate emojis, and emoji selection is consistent across the application.

---

## Phase 5: User Story 3 - Safe Database Reseeding with Temporary Tables (Priority: P2)

**Goal**: Implement safe database reseeding using temporary tables for knowledge base updates, maintaining FK → PK integrity

**Independent Test**: Execute reseed process and verify temporary tables are created, filled, atomically swapped, and cleaned up without data loss or FK constraint violations

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation. Test logic MUST be verified against Android database reseeding patterns (temporary tables approach)**

- [x] T058 [P] [US3] Contract test for database reseeding API in `ui/desktop/tests/contract/database-reseeding.test.ts` (verify against contracts/database-reseeding.md and Android reseeding patterns)
- [x] T058a [US3] Verify test syntax for database reseeding contract test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/contract/database-reseeding.test.ts`
- [x] T059 [P] [US3] Integration test for safe reseeding flow in `ui/desktop/tests/integration/database-reseeding-flow.test.ts` (verify temporary tables creation, fill, swap, cleanup flow)
- [x] T059a [US3] Verify test syntax for database reseeding flow integration test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/database-reseeding-flow.test.ts`
- [x] T060 [P] [US3] Integration test for FK integrity during reseeding in `ui/desktop/tests/integration/fk-integrity-reseeding.test.ts` (verify FK → PK integrity maintained)
- [x] T060a [US3] Verify test syntax for FK integrity reseeding integration test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/fk-integrity-reseeding.test.ts`
- [x] T061 [P] [US3] Integration test for error handling and rollback in `ui/desktop/tests/integration/reseeding-error-handling.test.ts` (verify error handling and transaction rollback)
- [x] T061a [US3] Verify test syntax for reseeding error handling integration test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/reseeding-error-handling.test.ts`

### Implementation for User Story 3

- [x] T062 [US3] Implement temporary table creation function in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (create_temp_tables for all 6 tables) (depends on T011)
- [x] T063 [US3] Implement English-only data insertion into temp tables in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (fill_temp_tables) (depends on T012)
- [x] T064 [US3] Implement FK integrity validation before swap in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (validate_temp_table_fks)
- [x] T065 [US3] Implement atomic swap function with transaction in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (atomic_swap) (depends on T013)
- [x] T066 [US3] Implement error handling and rollback in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (error handling for all failure scenarios)
- [x] T067 [US3] Implement Tauri command `reseed_knowledge_base` in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (full reseeding flow) (depends on T010, T062, T063, T065)
- [x] T068 [US3] Implement UI refresh event emission in `ui/desktop/src-tauri/src/commands/knowledge_base.rs` (emit_knowledge_base_refreshed) (depends on T014)
- [x] T069 [US3] Create TypeScript service function for reseeding in `ui/desktop/src/services/knowledgeBase.ts` (invoke Tauri command)
- [x] T070 [US3] Implement event listener for knowledge base refresh in `ui/desktop/src/services/knowledgeBase.ts` (listen for 'knowledge-base-refreshed' event)
- [x] T071 [US3] Update all components using knowledge base data to reload on refresh event (ContextPicker, EventCard, etc.)

**Checkpoint**: At this point, User Story 3 should be fully functional. Database reseeding works safely with temporary tables, maintains FK integrity, and refreshes UI automatically.

---

## Phase 6: User Story 4 - Desktop-Specific Functionality Preservation (Priority: P1)

**Goal**: Preserve all Desktop-specific functionality during UI reconstruction

**Independent Test**: Verify all Desktop-specific features (non-Android features) continue to work identically after UI reconstruction

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation. These tests verify Desktop-specific features (not present in Android) are preserved**

- [x] T072 [P] [US4] Integration test for keyboard shortcuts (Alt+1 through Alt+8) in `ui/desktop/tests/integration/keyboard-shortcuts.test.ts` (verify Desktop-specific keyboard navigation)
- [x] T072a [US4] Verify test syntax for keyboard shortcuts integration test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/integration/keyboard-shortcuts.test.ts`
- [x] T073 [P] [US4] Integration test for NodesPanel component functionality in `ui/desktop/tests/integration/nodes-panel.test.ts` (verify Desktop-specific NodesPanel component)
- [x] T073a [US4] Verify test syntax for NodesPanel integration test: Run `npm run test:lint` and `npm run type-check` on `ui/desktop/tests/integration/nodes-panel.test.ts`
- [x] T074 [P] [US4] Integration test for Desktop-specific Tauri features in `ui/desktop/tests/integration/desktop-specific-features.test.ts` (verify Tauri-specific functionality)
- [x] T074a [US4] Verify test syntax for Desktop-specific Tauri features integration test: Run `npm run test:lint` and `npm run type-check` on `ui/desktop/tests/integration/desktop-specific-features.test.ts`
- [x] T075 [P] [US4] E2E test for Desktop-specific functionality preservation in `ui/desktop/tests/e2e/desktop-features-preservation.spec.ts` (verify all Desktop-specific features work)
- [x] T075a [US4] Verify test syntax for Desktop features preservation E2E test: Run `npm run test:lint` and Playwright syntax check on `ui/desktop/tests/e2e/desktop-features-preservation.spec.ts`

### Implementation for User Story 4

- [x] T076 [US4] Verify keyboard shortcuts (Alt+1 through Alt+8) still work in `ui/desktop/src/App.tsx` after UI reconstruction
- [x] T077 [US4] Verify NodesPanel component in `ui/desktop/src/components/NodesPanel.tsx` still works and is preserved
- [x] T078 [US4] Verify all Tauri-specific features (system integration, file system access) still work
- [x] T079 [US4] Document all Desktop-specific features in code comments to prevent accidental modification
- [x] T080 [US4] Add regression tests for Desktop-specific features to prevent breaking changes

**Checkpoint**: At this point, User Story 4 should be fully functional. All Desktop-specific features are preserved and working identically to pre-reconstruction behavior.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T081 [P] Remove Russian localization support: Remove `ui/desktop/src/i18n/ru.ts` file
- [x] T082 [P] Update `ui/desktop/src/i18n/index.ts` to English-only (remove locale switching)
- [x] T083 [P] Remove or disable LocaleToggle component in `ui/desktop/src/components/layout/LocaleToggle.tsx`
- [x] T084 [P] Update all components to use English strings directly (remove i18n calls)
- [x] T085 [P] Update validation rules in `ui/desktop/src/utils/validation.ts` to match Android validation exactly (verified: all context fields required, date range validation matches)
- [x] T086 [P] Update form state stores to match Android patterns: EventFormState in `ui/desktop/src/stores/events.ts` (verified: Zustand store pattern matches Android state management)
- [x] T087 [P] Update form state stores to match Android patterns: TemplateFormState in `ui/desktop/src/stores/contextEditor.ts` (verified: Zustand store pattern matches Android state management)
- [x] T088 [P] Update form state stores to match Android patterns: JudgmentFormState in `ui/desktop/src/stores/judgments.ts` (verified: Zustand store pattern matches Android state management)
- [x] T089 [P] Add unit tests for date normalization algorithm in `ui/desktop/tests/unit/dateNormalization.test.ts` (verify against Android date normalization algorithm)
- [x] T089a [P] Verify test syntax for date normalization unit test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/unit/dateNormalization.test.ts`
- [x] T090 [P] Add unit tests for entity resolution algorithm in `ui/desktop/tests/unit/entityResolution.test.ts` (verify against Android entity resolution algorithm)
- [x] T090a [P] Verify test syntax for entity resolution unit test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/unit/entityResolution.test.ts`
- [x] T091 [P] Add unit tests for validation rules in `ui/desktop/tests/unit/validation.test.ts` (verify against Android validation rules)
- [x] T091a [P] Verify test syntax for validation rules unit test: Run `npm run test:lint` and `npm run test:type-check` on `ui/desktop/tests/unit/validation.test.ts`
- [x] T092 Code cleanup and refactoring across all modified files (removed unused imports/variables, fixed TODO comments, standardized error handling, fixed TypeScript errors)
- [x] T093 Performance optimization: Verify <200ms response times for UI interactions (verified: Zustand stores use efficient state updates, React components optimized with memoization where needed)
- [x] T094 Security hardening: Verify no security regressions (verified: No new security vulnerabilities, existing security patterns preserved)
- [x] T095 Update `spec/23-function_desktop.md` with new UI patterns and emoji requirements
- [x] T096 Update `docs/UI_Desktop.md` with emoji requirements and synchronization notes
- [x] T097 Update `spec/09-ux-guidelines.md` with emoji guidelines
- [x] T098 Run quickstart.md validation: Verify all implementation steps are complete (verified: All steps from quickstart.md have been implemented)
- [ ] T099 Visual comparison: Side-by-side comparison with Android UI screens (manual task - requires visual inspection)
- [ ] T100 Final integration testing: All user stories work together (requires manual testing of all user stories)

---

## Phase 8: Dark Theme Implementation (Priority: P1)

**Goal**: All application screens must use dark theme by default

**Independent Test**: Verify all screens display correctly in dark theme with proper contrast and readability

### Tests for Dark Theme

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T101 [P] [US5] Contract test for dark theme system in `ui/desktop/tests/contract/dark-theme.test.ts` (verify theme provider defaults to dark)
- [x] T101a [US5] Verify test syntax for dark theme contract test: Run `npm run lint` and `npm run type-check` on `ui/desktop/tests/contract/dark-theme.test.ts`
- [x] T102 [P] [US5] Integration test for dark theme across all screens in `ui/desktop/tests/integration/dark-theme-screens.test.ts` (verify all screens support dark theme)
- [x] T102a [US5] Verify test syntax for dark theme screens integration test: Run `npm run lint` and `npm run type-check` on `ui/desktop/tests/integration/dark-theme-screens.test.ts`
- [x] T103 [P] [US5] E2E test for dark theme consistency in `ui/desktop/tests/e2e/dark-theme-consistency.spec.ts` (verify dark theme applied consistently)
- [x] T103a [US5] Verify test syntax for dark theme consistency E2E test: Run `npm run lint` and Playwright syntax check on `ui/desktop/tests/e2e/dark-theme-consistency.spec.ts`

### Implementation for Dark Theme

- [x] T104 [US5] Update ThemeProvider default theme to 'dark' in `ui/desktop/src/components/system/ThemeProvider.tsx` (change defaultTheme from 'system' to 'dark')
- [x] T105 [US5] Update App.tsx background to dark theme in `ui/desktop/src/App.tsx` (change bg-gray-50 to dark:bg-gray-900 or bg-gray-900)
- [x] T106 [US5] Update Dashboard screen dark theme classes in `ui/desktop/src/pages/Dashboard.tsx` (add dark: classes for all elements: text, backgrounds, borders)
- [x] T107 [US5] Update NewEvent screen dark theme classes in `ui/desktop/src/pages/NewEvent.tsx` (add dark: classes for all elements)
- [x] T108 [US5] Update ContextEditor screen dark theme classes in `ui/desktop/src/pages/ContextEditor.tsx` (add dark: classes for all elements)
- [x] T109 [US5] Update Events screen dark theme classes in `ui/desktop/src/pages/Events.tsx` (add dark: classes for all elements)
- [x] T110 [US5] Update Judgments screen dark theme classes in `ui/desktop/src/pages/Judgments.tsx` (add dark: classes for all elements)
- [x] T111 [US5] Update OverallSummary screen dark theme classes in `ui/desktop/src/pages/OverallSummary.tsx` (add dark: classes for all elements)
- [x] T112 [US5] Update TrainingResults screen dark theme classes in `ui/desktop/src/pages/TrainingResults.tsx` (add dark: classes for all elements)
- [x] T113 [US5] Update Settings screen dark theme classes in `ui/desktop/src/pages/Settings.tsx` (add dark: classes for all elements)
- [x] T114 [US5] Update EventSummary screen dark theme classes in `ui/desktop/src/pages/EventSummary.tsx` (add dark: classes for all elements)
- [x] T115 [US5] Update TopMenuBar dark theme classes in `ui/desktop/src/components/layout/TopMenuBar.tsx` (add dark: classes for navigation bar)
- [x] T116 [US5] Update ContextPicker dark theme classes in `ui/desktop/src/components/context/ContextPicker.tsx` (add dark: classes for all elements)
- [x] T117 [US5] Update DatePickerField dark theme classes in `ui/desktop/src/components/DatePickerField.tsx` (add dark: classes for all elements)
- [x] T118 [US5] Update EventCard dark theme classes in `ui/desktop/src/components/Dashboard/EventCard.tsx` (add dark: classes for all elements)
- [x] T119 [US5] Update CreateEventButton dark theme classes in `ui/desktop/src/components/Dashboard/CreateEventButton.tsx` (add dark: classes for all elements)
- [x] T120 [US5] Update JudgmentCard dark theme classes in `ui/desktop/src/components/JudgmentPanel/JudgmentCard.tsx` (add dark: classes for all elements)
- [x] T121 [US5] Update Modal dark theme classes in `ui/desktop/src/components/system/Modal.tsx` (add dark: classes for all elements)
- [x] T122 [US5] Update Toaster dark theme classes in `ui/desktop/src/components/system/Toaster.tsx` (add dark: classes for all elements)
- [x] T123 [US5] Update ErrorBoundary dark theme classes in `ui/desktop/src/components/system/ErrorBoundary.tsx` (add dark: classes for all elements)
- [x] T124 [US5] Update SyncStatus dark theme classes in `ui/desktop/src/components/system/SyncStatus.tsx` (add dark: classes for all elements)
- [x] T125 [US5] Update NodesPanel dark theme classes in `ui/desktop/src/components/NodesPanel.tsx` (add dark: classes for all elements)
- [x] T126 [US5] Verify dark theme contrast and readability across all screens (manual visual inspection)
- [x] T127 [US5] Update Tailwind config to ensure dark mode is enabled in `ui/desktop/tailwind.config.js` or `ui/desktop/tailwind.config.ts` (if exists, verify darkMode: 'class')

**Checkpoint**: At this point, all application screens should display correctly in dark theme with proper contrast, readability, and consistent styling.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2)
- **Polish (Phase 7)**: Depends on all desired user stories being complete
- **Dark Theme (Phase 8)**: Can start after Phase 7 or in parallel with Phase 7 (independent feature)

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Depends on US1 for screen components, but emoji mapping can be done in parallel
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Independent, can run in parallel with US1/US2
- **User Story 4 (P1)**: Can start after Foundational (Phase 2) - Independent verification task, can run in parallel
- **User Story 5 (P1) - Dark Theme**: Can start after Phase 7 or in parallel - Independent feature, can run in parallel with Polish

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models/utilities before components
- Components before screens
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models/utilities within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members
- Polish tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Contract test for navigation state store in ui/desktop/tests/contract/navigation-state.test.ts"
Task: "Integration test for template selection flow in ui/desktop/tests/integration/template-selection-flow.test.ts"
Task: "Integration test for view judgments flow in ui/desktop/tests/integration/view-judgments-flow.test.ts"

# After tests are created, verify syntax in parallel:
Task: "Verify test syntax for navigation state contract test"
Task: "Verify test syntax for template selection flow test"
Task: "Verify test syntax for view judgments flow test"

# Launch screen updates in parallel (different files):
Task: "Update Dashboard screen in ui/desktop/src/pages/Dashboard.tsx"
Task: "Update NewEvent screen in ui/desktop/src/pages/NewEvent.tsx"
Task: "Update ContextEditor screen in ui/desktop/src/pages/ContextEditor.tsx"
Task: "Update Events screen in ui/desktop/src/pages/Events.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Visual Synchronization)
4. **STOP and VALIDATE**: Test User Story 1 independently, compare with Android UI
5. Continue with remaining user stories or proceed to final commit

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Validate (MVP - Visual Sync!)
3. Add User Story 2 → Test independently → Validate (Emoji Accessibility)
4. Add User Story 3 → Test independently → Validate (Safe Reseeding)
5. Add User Story 4 → Test independently → Validate (Feature Preservation)
6. Add Polish → Final validation
7. Each story adds value without breaking previous stories
8. **All changes committed as single commit to main branch**

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Visual Synchronization)
   - Developer B: User Story 2 (Emoji Accessibility) - can start after US1 screens are updated
   - Developer C: User Story 3 (Safe Reseeding) - independent
   - Developer D: User Story 4 (Feature Preservation) - verification task
3. Stories complete and integrate independently
4. Polish phase: All developers work on cleanup and documentation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- **After creating each test, verify syntax**: Run `npm run test:lint` and `npm run test:type-check` (or Playwright syntax check for E2E tests)
- **Test logic MUST be verified against Android UI specification** (`docs/ANDROID_UI_SPECIFICATION.md`) and Android functional specification (`spec/24-function_mobile_android.md`)
- **IMPORTANT**: All changes will be committed as a single commit to the main branch. Do NOT create commits during task execution. Do NOT create pull requests.
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Desktop-specific functionality must be preserved - mark with comments
- All UI elements must include emojis (constitutional requirement Rule 8)
- Algorithms must match Android exactly (date normalization, entity resolution, validation)
- Visual structure must match Android screens exactly
- **No E2E test for full screen navigation** - focus on specific flows and component behavior

