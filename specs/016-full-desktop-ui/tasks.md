# Tasks: Timestamp and Flag Fields Rules Correction

**Input**: Design documents from `/specs/016-full-desktop-ui/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Organization**: Tasks are organized by implementation phase to enable systematic correction of timestamp and flag field rules.

## Format: `[ID] [P?] [Area] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Area]**: Which area this task belongs to (EDIT, NEW, API, BACKEND)
- Include exact file paths in descriptions

## Path Conventions

- **Desktop application**: `ui/desktop/src/` for frontend, `ui/desktop/src-tauri/src/` for backend
- All paths are relative to repository root

---

## Phase 1: Setup & Verification

**Purpose**: Verify current implementation and identify required changes

- [x] T001 Verify current Edit Event modal implementation in `ui/desktop/src/pages/Events.tsx`:
  - Check if Start Timestamp is read-only
  - Check if corrected field exists
  - Check if initial timestamp_end is tracked
  - Document current state vs required state

- [x] T002 Verify current New Event screen implementation in `ui/desktop/src/pages/NewEvent.tsx`:
  - Check timestamp field defaults and editability
  - Verify validation rules
  - Document current state vs required state

- [x] T003 Verify backend update command in `ui/desktop/src-tauri/src/commands/events.rs`:
  - Check if corrected field is supported in UpdateEventRequest
  - Check if corrected is included in UPDATE query
  - Document current state vs required state

- [x] T004 Verify API service in `ui/desktop/src/services/api.ts`:
  - Check if updateEvent method supports corrected field
  - Document current state vs required state

**Checkpoint**: Current implementation state documented, required changes identified

---

## Phase 2: Tests for Timestamp Fields Rules

**Purpose**: Write integration tests for timestamp fields rules before implementation

- [x] T010 [P] [TEST] Integration test for Edit Event Start Timestamp read-only in `ui/desktop/tests/integration/timestamp-fields-edit.test.ts`:
  - Test that Start Timestamp is displayed as read-only
  - Test that Start Timestamp shows existing event value
  - Reference: contracts/timestamp-fields-rules.md test case 1

- [x] T011 [P] [TEST] Integration test for Edit Event End Timestamp default in `ui/desktop/tests/integration/timestamp-fields-edit.test.ts`:
  - Test that End Timestamp defaults to current date if null
  - Test that End Timestamp is editable
  - Reference: contracts/timestamp-fields-rules.md test case 2

- [x] T012 [P] [TEST] Integration test for Edit Event End Timestamp validation in `ui/desktop/tests/integration/timestamp-fields-edit.test.ts`:
  - Test that End Timestamp cannot be less than Start Timestamp
  - Test validation error message
  - Reference: contracts/timestamp-fields-rules.md test case 3

- [x] T013 [P] [TEST] Integration test for New Event timestamp fields in `ui/desktop/tests/integration/timestamp-fields-new.test.ts`:
  - Test Start Timestamp defaults to current date
  - Test Start Timestamp is editable and required
  - Test End Timestamp is optional and can be null
  - Test End Timestamp validation
  - Reference: contracts/timestamp-fields-rules.md test cases 1-4

**Checkpoint**: All timestamp field tests written and failing (TDD approach)

---

## Phase 3: Tests for Flag Fields Rules

**Purpose**: Write integration tests for flag fields rules before implementation

- [x] T020 [P] [TEST] Integration test for detected field in `ui/desktop/tests/integration/flag-fields-edit.test.ts`:
  - Test that detected field is editable
  - Test that detected defaults to existing value or false
  - Reference: contracts/flag-fields-rules.md test cases 1-2

- [x] T021 [P] [TEST] Integration test for corrected field auto-set logic in `ui/desktop/tests/integration/flag-fields-edit.test.ts`:
  - Test corrected field is read-only display
  - Test corrected not set when initial timestamp_end was null
  - Test corrected auto-set when timestamp_end changed
  - Test corrected not changed when timestamp_end unchanged
  - Test corrected preserved when initial was null
  - Reference: contracts/flag-fields-rules.md test cases 1-5

**Checkpoint**: All flag field tests written and failing (TDD approach)

---

## Phase 4: Backend Implementation - corrected Field Support

**Purpose**: Add corrected field support to backend update command

- [x] T030 [BACKEND] Update UpdateEventRequest struct in `ui/desktop/src-tauri/src/commands/events.rs`:
  - Add `corrected: Option<bool>` field to UpdateEventRequest
  - Reference: data-model.md corrected field definition

- [x] T031 [BACKEND] Update update_event_fast command in `ui/desktop/src-tauri/src/commands/events.rs`:
  - Add UPDATE query for corrected field
  - Only update corrected if provided in request
  - Reference: contracts/flag-fields-rules.md save request logic

- [x] T032 [BACKEND] Verify update_event_fast returns corrected field in response:
  - Ensure corrected field is included in returned Event struct
  - Test that corrected value is correctly updated

**Checkpoint**: Backend supports corrected field in update operations

---

## Phase 5: API Service Implementation - corrected Field Support

**Purpose**: Add corrected field support to frontend API service

- [x] T040 [API] Update UpdateEventRequest type in `ui/desktop/src/types/events.ts`:
  - Add `corrected?: boolean` field to UpdateEventRequest interface
  - Reference: data-model.md corrected field definition

- [x] T041 [API] Update updateEvent method in `ui/desktop/src/services/api.ts`:
  - Add corrected parameter to updateEvent method signature
  - Pass corrected to Tauri command if provided
  - Reference: contracts/flag-fields-rules.md save request logic

**Checkpoint**: API service supports corrected field in update operations

---

## Phase 6: Edit Event Screen - Timestamp Fields Correction

**Purpose**: Ensure Start Timestamp is read-only and End Timestamp has correct default

- [x] T050 [EDIT] Verify Start Timestamp is read-only in `ui/desktop/src/pages/Events.tsx`:
  - Ensure Start Timestamp is displayed as read-only text (not DatePickerField)
  - Verify Start Timestamp shows existing event value
  - Reference: contracts/timestamp-fields-rules.md Start Timestamp rules

- [x] T051 [EDIT] Verify End Timestamp default value in `ui/desktop/src/pages/Events.tsx`:
  - Ensure End Timestamp defaults to current date if event.timestamp_end is null
  - Verify End Timestamp is editable
  - Reference: contracts/timestamp-fields-rules.md End Timestamp rules

- [x] T052 [EDIT] Verify End Timestamp validation in `ui/desktop/src/pages/Events.tsx`:
  - Ensure validation uses normalized dates (start of day)
  - Verify error message: "End timestamp cannot be less than start timestamp"
  - Reference: data-model.md date normalization

- [x] T053 [EDIT] Verify End Timestamp can be cleared in `ui/desktop/src/pages/Events.tsx`:
  - Ensure DatePickerField has allowClear={true}
  - Verify timestamp_end can be set to null
  - Reference: contracts/timestamp-fields-rules.md End Timestamp rules

**Checkpoint**: Edit Event timestamp fields match Android behavior

---

## Phase 7: Edit Event Screen - Flag Fields Implementation

**Purpose**: Add corrected field with auto-set logic to Edit Event modal

- [x] T060 [EDIT] Add initial timestamp_end tracking in `ui/desktop/src/pages/Events.tsx`:
  - Add state for initialTimestampEnd
  - Set initialTimestampEnd when Edit modal opens
  - Reference: contracts/flag-fields-rules.md implementation notes

- [x] T061 [EDIT] Implement corrected auto-set calculation in `ui/desktop/src/pages/Events.tsx`:
  - Add useMemo hook to calculate corrected value
  - Implement calculateCorrected function logic
  - Reference: contracts/flag-fields-rules.md auto-set algorithm

- [x] T062 [EDIT] Add corrected field display in `ui/desktop/src/pages/Events.tsx`:
  - Add read-only display of corrected field
  - Show corrected value but disable editing
  - Include emoji for accessibility (Rule 8)
  - Reference: contracts/flag-fields-rules.md corrected field display

- [x] T063 [EDIT] Update save logic in `ui/desktop/src/pages/Events.tsx`:
  - Include corrected in save request if it changed
  - Update canSave condition to include corrected changes
  - Only send changed fields in update request
  - Reference: contracts/flag-fields-rules.md save logic

- [x] T064 [EDIT] Update handleSaveEvent to include corrected in `ui/desktop/src/pages/Events.tsx`:
  - Pass corrected to ApiService.updateEvent if changed
  - Reference: contracts/flag-fields-rules.md save request

**Checkpoint**: Edit Event flag fields match Android behavior with auto-set logic

---

## Phase 8: New Event Screen - Timestamp Fields Verification

**Purpose**: Verify New Event screen timestamp fields match Android rules

- [x] T070 [NEW] Verify Start Timestamp default in `ui/desktop/src/pages/NewEvent.tsx`:
  - Ensure Start Timestamp defaults to current date
  - Verify Start Timestamp is editable
  - Reference: contracts/timestamp-fields-rules.md Start Timestamp rules

- [x] T071 [NEW] Verify Start Timestamp required validation in `ui/desktop/src/pages/NewEvent.tsx`:
  - Ensure Start Timestamp cannot be null
  - Verify validation error is displayed if empty
  - Reference: contracts/timestamp-fields-rules.md Start Timestamp validation

- [x] T072 [NEW] Verify End Timestamp optional in `ui/desktop/src/pages/NewEvent.tsx`:
  - Ensure End Timestamp can be null
  - Verify End Timestamp has allowClear={true}
  - Reference: contracts/timestamp-fields-rules.md End Timestamp rules

- [x] T073 [NEW] Verify End Timestamp validation in `ui/desktop/src/pages/NewEvent.tsx`:
  - Ensure End Timestamp validation uses normalized dates
  - Verify error message if End < Start
  - Reference: data-model.md date normalization

**Checkpoint**: New Event timestamp fields match Android behavior

---

## Phase 9: Integration & Validation

**Purpose**: Verify all changes work together and match Android behavior

- [x] T080 [P] Run quickstart.md validation scenarios:
  - Scenario 1: Edit Event - Start Timestamp Read-Only
  - Scenario 2: Edit Event - End Timestamp Default
  - Scenario 3: Edit Event - End Timestamp Validation
  - Scenario 4: Edit Event - detected Toggle
  - Scenario 5: Edit Event - corrected Auto-Set (Initial timestamp_end Was Set)
  - Scenario 6: Edit Event - corrected Not Set (Initial timestamp_end Was Null)
  - Scenario 7: New Event - Start Timestamp Default
  - Scenario 8: New Event - End Timestamp Optional
  - Reference: quickstart.md testing scenarios

- [x] T081 [P] Verify all screens match Android behavior:
  - Compare Edit Event screen with Android EventEditScreen
  - Compare New Event screen with Android EventCreateScreen
  - Verify timestamp field editability matches exactly
  - Verify flag field behavior matches exactly

- [x] T082 [P] Code cleanup and documentation:
  - Add comments explaining corrected auto-set logic
  - Document date normalization function
  - Update component documentation with field rules
  - Ensure all emojis present (constitutional requirement Rule 8)

- [x] T083 Final integration testing:
  - Test complete flow: Edit Event → Change timestamp_end → corrected auto-set → Save
  - Test complete flow: Edit Event → Change detected → Save
  - Test complete flow: New Event → Create with timestamps → Save
  - Test error handling for all validation rules
  - Verify no regressions in existing functionality

**Checkpoint**: All timestamp and flag field rules match Android behavior, all tests pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - can start immediately
- **Phase 2-3 (Tests)**: Depends on Phase 1 - can start after verification
- **Phase 4 (Backend)**: Depends on Phase 1 - can start in parallel with Phase 2-3
- **Phase 5 (API)**: Depends on Phase 4 - must wait for backend corrected support
- **Phase 6 (Edit Timestamp)**: Depends on Phase 1 - can start in parallel
- **Phase 7 (Edit Flags)**: Depends on Phase 5 - must wait for API corrected support
- **Phase 8 (New Event)**: Depends on Phase 1 - can start in parallel
- **Phase 9 (Integration)**: Depends on all previous phases - must wait for all implementation

### Within Each Phase

- Tests (if included) MUST be written and FAIL before implementation (TDD)
- Backend implementation before frontend integration
- Core implementation before UI integration
- Phase complete before moving to next phase

### Parallel Opportunities

- All test tasks marked [P] can run in parallel
- Phase 2, 3, 4, 6, 8 can run in parallel (different files, no dependencies)
- Phase 5 depends on Phase 4 (sequential)
- Phase 7 depends on Phase 5 (sequential)
- Phase 9 depends on all phases (sequential)

---

## Parallel Example: Phase 2-4

```bash
# Launch all tests in parallel:
Task: "Integration test for Edit Event Start Timestamp read-only"
Task: "Integration test for Edit Event End Timestamp default"
Task: "Integration test for Edit Event End Timestamp validation"
Task: "Integration test for New Event timestamp fields"
Task: "Integration test for detected field"
Task: "Integration test for corrected field auto-set logic"

# Launch backend implementation in parallel with tests:
Task: "Update UpdateEventRequest struct in events.rs"
```

---

## Implementation Strategy

### Incremental Delivery

1. Complete Phase 1: Setup verification → Document current state
2. Complete Phase 2-3: Write tests → Ensure tests fail (TDD)
3. Complete Phase 4: Backend corrected support → Test independently
4. Complete Phase 5: API corrected support → Test independently
5. Complete Phase 6: Edit Event timestamp fields → Test independently
6. Complete Phase 7: Edit Event flag fields → Test independently
7. Complete Phase 8: New Event verification → Test independently
8. Complete Phase 9: Integration and validation

### Parallel Team Strategy

With multiple developers:

1. Team completes Phase 1 together
2. Once Phase 1 is done:
   - Developer A: Phase 2-3 (Tests)
   - Developer B: Phase 4 (Backend)
   - Developer C: Phase 6 (Edit Timestamp)
   - Developer D: Phase 8 (New Event)
3. Phase 5 (API) waits for Phase 4
4. Phase 7 (Edit Flags) waits for Phase 5
5. Phase 9 (Integration) requires all phases complete

---

## Notes

- [P] tasks = different files, no dependencies
- [Area] label maps task to specific feature area for traceability
- Each phase should be independently testable
- Verify tests fail before implementing (TDD)
- Reference Android implementation code for exact behavior matching
- All UI elements must include emojis (constitutional requirement Rule 8)
- Field editability rules must match Android exactly (not approximate)
- Date normalization must use start of day for comparison (matches Android)
