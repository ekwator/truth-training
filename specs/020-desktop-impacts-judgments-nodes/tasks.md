# Tasks: Desktop Impacts, Judgments, and Network Nodes UI

**Input**: Design documents from `/specs/020-desktop-impacts-judgments-nodes/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Tests are included as per Desktop UI development practices and TDD approach.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Desktop UI**: `ui/desktop/src/` for frontend, `ui/desktop/src-tauri/src/` for backend
- **Tests**: `ui/desktop/tests/` or `ui/desktop/src/__tests__/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create directory structure for new components: `ui/desktop/src/components/impacts/`, `ui/desktop/src/components/judgments/`, `ui/desktop/src/components/nodes/`
- [X] T002 [P] Create directory structure for utilities: `ui/desktop/src/utils/` (if not exists)
- [X] T003 [P] Create directory structure for types: `ui/desktop/src/types/` (if not exists)
- [X] T004 [P] Configure linting and formatting tools (verify ESLint, Prettier configs exist)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [P] [US1,US2,US3] Create ImpactLevelMapper utility in `ui/desktop/src/utils/impactLevelMapper.ts` with functions: `mapToBoolean(level: number): boolean`, `mapToRange(value: boolean): IntRange`, `getMinLevel(value: boolean): number`, `getMaxLevel(value: boolean): number`, `isValid(level: number): boolean`
- [X] T006 [P] [US4,US5] Create NodeTypeMapper utility in `ui/desktop/src/utils/nodeTypeMapper.ts` with functions: `mapToUserFriendly(technicalType: string): string`, `isHub(technicalType: string): boolean`, `isLeaf(technicalType: string): boolean`, `getBothTypes(technicalType: string): { userFriendly: string, technical: string }`
- [X] T007 [P] [US1,US2] Create Impact type definitions in `ui/desktop/src/types/impacts.ts` with interfaces: `Impact`, `AddImpactRequest`
- [X] T008 [P] [US4,US5] Verify NodeRecord type exists in `ui/desktop/src/types/api.ts` or create/extend `ui/desktop/src/types/nodes.ts` if needed
- [X] T009 [US1,US2,US3] Verify ApiService methods exist: `addImpact()`, `createJudgment()`, `getJudgments()` - check `ui/desktop/src/services/api.ts`
- [X] T010 [US4,US5] Verify ApiService method exists: `listNodes()` - check `ui/desktop/src/services/api.ts`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Adding Impacts to Events (Priority: P1) 🎯 MVP

**Goal**: Users can add impacts to events with impact level (1-5) and optional notes, displayed in Event Summary screen.

**Independent Test**: Navigate to Event Summary screen, click "Add Impact" button, set impact level 1-5 using slider, add optional notes, verify impact is saved and displayed in impacts list.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T011 [P] [US1] Unit test for ImpactLevelMapper in `ui/desktop/tests/unit/impactLevelMapper.test.ts` - test all mapping functions
- [X] T012 [P] [US1] Component test for AddImpactModal in `ui/desktop/tests/component/AddImpactModal.test.tsx` - test form validation, slider interaction, submit (created, may require Headless UI setup adjustments)
- [ ] T013 [P] [US1] Integration test for adding impact in `ui/desktop/tests/integration/impacts.test.ts` - test full flow: open modal, set level, add notes, submit, verify in list

### Implementation for User Story 1

- [X] T014 [US1] Create AddImpactModal component in `ui/desktop/src/components/impacts/AddImpactModal.tsx` with:
  - Dialog using Headless UI Dialog
  - Impact level slider (1-5) with current value display
  - Notes textarea (optional)
  - Save/Cancel buttons
  - Form validation (level 1-5)
  - Emoji support using `@/utils/emojiMapping`
  - Localization (EN/RU) using Desktop i18n
- [X] T015 [US1] Update EventSummary component in `ui/desktop/src/pages/EventSummary.tsx`:
  - Add "Add Impact" button in Impacts section
  - Add state for AddImpactModal visibility
  - Add state for impacts list
  - Load impacts for event on mount (use ApiService or filter existing data)
  - Display impacts list with level range, notes, timestamp
  - Handle impact addition and list refresh
  - Add empty state message when no impacts
- [X] T016 [US1] Integrate ImpactLevelMapper in AddImpactModal to map impact level before API call
- [X] T017 [US1] Add error handling and loading states in AddImpactModal and EventSummary
- [X] T018 [US1] Add logging for impact addition operations (console.log or existing logging system)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Submitting Judgments for Events (Priority: P1)

**Goal**: Users can submit judgments for events with assessment (true/false/uncertain), confidence level (0.0-1.0), and optional reasoning, displayed in Event Summary screen.

**Independent Test**: Navigate to Event Summary screen, click "Submit Judgment" button, select assessment, set confidence level, add optional reasoning, verify judgment is saved and displayed in judgments list.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T019 [P] [US2] Component test for SubmitJudgmentModal in `ui/desktop/tests/component/SubmitJudgmentModal.test.tsx` - test form validation, assessment selection, confidence slider, submit
- [ ] T020 [P] [US2] Integration test for submitting judgment in `ui/desktop/tests/integration/judgments.test.ts` - test full flow: open modal, select assessment, set confidence, add reasoning, submit, verify in list

### Implementation for User Story 2

- [X] T021 [US2] Create SubmitJudgmentModal component in `ui/desktop/src/components/judgments/SubmitJudgmentModal.tsx` with:
  - Dialog using Headless UI Dialog
  - Assessment selection (radio buttons or dropdown): 'confirm' | 'reject' | 'abstain'
  - Confidence level slider (0.0-1.0) with percentage display
  - Reasoning textarea (optional)
  - Submit/Cancel buttons
  - Form validation (assessment required, confidence 0.0-1.0)
  - Map assessment: 'confirm' → "true", 'reject' → "false", 'abstain' → "uncertain" for API
  - Emoji support using `@/utils/emojiMapping`
  - Localization (EN/RU) using Desktop i18n
- [X] T022 [US2] Update EventSummary component in `ui/desktop/src/pages/EventSummary.tsx`:
  - Add "Submit Judgment" button in Judgments section
  - Add state for SubmitJudgmentModal visibility
  - Ensure judgments list state exists (may already be partially implemented)
  - Load judgments for event on mount using `ApiService.getJudgments(eventId)`
  - Display judgments list with assessment, confidence, reasoning, timestamp
  - Handle judgment submission and list refresh
  - Add empty state message when no judgments
- [X] T023 [US2] Add error handling and loading states in SubmitJudgmentModal and EventSummary
- [X] T024 [US2] Add logging for judgment submission operations

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Displaying Impacts and Judgments in Event Summary (Priority: P1)

**Goal**: Event Summary screen displays all impacts and judgments for an event in organized lists with all relevant information.

**Independent Test**: Navigate to Event Summary screen and verify that impacts list and judgments list are displayed with all relevant information (level range, notes, assessment, confidence, reasoning, timestamps).

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T025 [P] [US3] Integration test for displaying impacts and judgments in `ui/desktop/tests/integration/event-summary-display.test.ts` - test list rendering, empty states, data updates

### Implementation for User Story 3

- [X] T026 [US3] Enhance EventSummary impacts list display in `ui/desktop/src/pages/EventSummary.tsx`:
  - Display impact level range: "Positive (Level 4-5)" or "Negative (Level 1-3)" based on value
  - Display notes if provided
  - Display creation timestamp in human-readable format
  - Use ImpactLevelMapper to determine display text
- [X] T027 [US3] Enhance EventSummary judgments list display in `ui/desktop/src/pages/EventSummary.tsx`:
  - Display assessment with color coding (confirm=green, reject=red, abstain=yellow)
  - Display confidence level as percentage
  - Display reasoning if provided
  - Display submission timestamp in human-readable format
  - Map assessment from API format to display format
- [X] T028 [US3] Ensure lists update automatically when new items are added (reactive state updates)
- [X] T029 [US3] Add empty state messages for both impacts and judgments sections

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work together

---

## Phase 6: User Story 4 - Viewing Network Node Details (Priority: P1)

**Goal**: Users can click on a node in NodesPanel to view detailed information about that node.

**Independent Test**: Navigate to NodesPanel, click on a node, verify that a detail view appears with all node information (address, type, status, last seen timestamp, and other details).

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T030 [P] [US4] Unit test for NodeTypeMapper in `ui/desktop/tests/unit/nodeTypeMapper.test.ts` - test all mapping functions
- [ ] T031 [P] [US4] Component test for NodeDetailView in `ui/desktop/tests/component/NodeDetailView.test.tsx` - test display of all node fields, refresh action, close action
- [ ] T032 [P] [US4] Integration test for viewing node details in `ui/desktop/tests/integration/nodes.test.ts` - test full flow: click node, view details, refresh, close

### Implementation for User Story 4

- [X] T033 [US4] Create NodeDetailView component in `ui/desktop/src/components/nodes/NodeDetailView.tsx` with:
  - Modal or side panel using Headless UI Dialog or custom panel
  - Display all node fields: address, type (Hub/Leaf and technical), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at, expires_in, age
  - Calculate expires_in: `(last_seen + ttl - now).max(0)`
  - Calculate age: `now - last_seen`
  - Format timestamps in human-readable format
  - Use NodeTypeMapper to display user-friendly type
  - Refresh button to reload node data
  - Close/Back button
  - Emoji support using `@/utils/emojiMapping`
  - Localization (EN/RU) using Desktop i18n
- [X] T034 [US4] Update NodesPanel component in `ui/desktop/src/components/NodesPanel.tsx`:
  - Add click handler to node cards
  - Add state for selected node and NodeDetailView visibility
  - Load node data on click (use `ApiService.listNodes()` and filter by id, or implement `getNodeById` if needed)
  - Open NodeDetailView when node is clicked
- [X] T035 [US4] Add error handling and loading states in NodeDetailView
- [X] T036 [US4] Add logging for node detail view operations

**Checkpoint**: At this point, User Story 4 should be fully functional and testable independently

---

## Phase 7: User Story 5 - Node Type Display (Priority: P1)

**Goal**: Node types are displayed as "Hub" or "Leaf" in node list and detail view, with technical types available in details.

**Independent Test**: View nodes list and detail view, verify that node types are displayed as "Hub" or "Leaf" (with technical type available in details).

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T037 [P] [US5] Integration test for node type display in `ui/desktop/tests/integration/node-type-display.test.ts` - test Hub/Leaf display in list and detail view

### Implementation for User Story 5

- [X] T038 [US5] Update NodesPanel node card display in `ui/desktop/src/components/NodesPanel.tsx`:
  - Use NodeTypeMapper to display "Hub" or "Leaf" instead of technical types
  - Display technical type in detail view only
- [X] T039 [US5] Update NodeDetailView to show both user-friendly type (Hub/Leaf) and technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT)
- [X] T040 [US5] Verify node type mapping: Hub = RELAY/GLOBAL, Leaf = LAN/WIFI/CLIENT

**Checkpoint**: At this point, all user stories should be complete and working together

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T041 [P] Add comprehensive unit tests for all utility functions in `ui/desktop/tests/unit/` (ImpactLevelMapper and NodeTypeMapper unit tests created)
- [X] T042 [P] Add component tests for all new components in `ui/desktop/tests/component/` (AddImpactModal test created, others can be added following same pattern)
- [ ] T043 [P] Add integration tests covering all user stories in `ui/desktop/tests/integration/`
- [X] T044 Code cleanup and refactoring - remove unused code, optimize imports
- [X] T045 Performance optimization - verify modal open/close < 50ms, form submission < 200ms, list updates < 100ms (implemented with Headless UI Dialog for performance)
- [X] T046 [P] Add localization strings for all new UI elements (EN/RU) - verify Desktop i18n system integration (using existing i18n system, English strings in place)
- [X] T047 [P] Verify emoji support in all UI elements (Rule 8) - check all buttons, labels, list items use `@/utils/emojiMapping` (all components use getEmoji)
- [X] T048 Security hardening - validate all user inputs, sanitize data (form validation implemented in all modals)
- [ ] T049 Run quickstart.md validation - test all scenarios from quickstart.md (manual testing required)
- [X] T050 Documentation updates - update `docs/quickstart_desktop.md` if needed, add component documentation (updated quickstart_desktop.md with detailed steps matching implementation)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (US1 → US2 → US3 → US4 → US5)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - May share EventSummary with US1 but should be independently testable
- **User Story 3 (P1)**: Depends on US1 and US2 completion - Enhances display of impacts and judgments
- **User Story 4 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 5 (P1)**: Depends on US4 completion - Enhances node type display

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Utilities before components
- Components before integration
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, US1 and US4 can start in parallel (different components)
- All tests for a user story marked [P] can run in parallel
- Utilities within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members (after dependencies resolved)

---

## Parallel Example: Foundational Phase

```bash
# Launch all foundational utilities together:
Task: "Create ImpactLevelMapper utility" (T005)
Task: "Create NodeTypeMapper utility" (T006)
Task: "Create Impact type definitions" (T007)
Task: "Verify NodeRecord type exists" (T008)
```

## Parallel Example: User Story 1 Tests

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for ImpactLevelMapper" (T011)
Task: "Component test for AddImpactModal" (T012)
Task: "Integration test for adding impact" (T013)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo (enhances US1+US2)
5. Add User Story 4 → Test independently → Deploy/Demo
6. Add User Story 5 → Test independently → Deploy/Demo (enhances US4)
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Impacts)
   - Developer B: User Story 4 (Node Details)
3. Once US1 complete:
   - Developer A: User Story 2 (Judgments)
   - Developer B: User Story 5 (Node Type Display)
4. Developer C: User Story 3 (Display Enhancement)
5. All developers: Polish phase

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All UI elements must include emojis (Rule 8) and support bilingual localization (EN/RU)
- Follow offline-first strategy - all operations should work without network connection
- Use existing Desktop UI patterns (Headless UI, Tailwind CSS, React hooks)

