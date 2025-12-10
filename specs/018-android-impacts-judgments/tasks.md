# Tasks: Android Impacts and Judgments UI

**Input**: Design documents from `/specs/018-android-impacts-judgments/`
**Prerequisites**: plan.md ✅, spec.md ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Mobile**: `truth-android-client/app/src/main/java/com/truth/training/client/`
- Paths shown use absolute paths from repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project structure verification and preparation

- [X] T001 Verify project structure exists per implementation plan (truth-android-client/app/src/main/java/com/truth/training/client/)
- [X] T002 [P] Verify existing dependencies: Jetpack Compose, Material Design 3, Room Database, Retrofit, Coroutines Flow
- [X] T003 [P] Verify existing ImpactRepository exists at truth-android-client/app/src/main/java/com/truth/training/client/data/repository/ImpactRepository.kt
- [X] T004 [P] Verify existing JudgmentRepository exists at truth-android-client/app/src/main/java/com/truth/training/client/data/repository/JudgmentRepository.kt
- [X] T005 [P] Verify existing ImpactEntity exists at truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/ImpactEntity.kt
- [X] T006 [P] Verify existing JudgmentEntity exists at truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/JudgmentEntity.kt
- [X] T007 [P] Verify existing EmojiMapping utility exists at truth-android-client/app/src/main/java/com/truth/training/client/utils/EmojiMapping.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T008 [Foundation] Create ImpactLevelMapper utility in truth-android-client/app/src/main/java/com/truth/training/client/utils/ImpactLevelMapper.kt
  - Implement function to map impact level (1-5) to boolean value: impact_level > 3 → positive (true), impact_level <= 3 → negative (false)
  - Implement reverse mapping: boolean to impact level range (true → 4-5, false → 1-3)
  - Add validation function to ensure impact level is in range 1-5
  - Match Desktop UI implementation pattern

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Adding Impacts to Events (Priority: P1) 🎯 MVP

**Goal**: Allow users to add impacts to events with impact level (1-5) and optional notes, so that they can record the impact assessment for each event.

**Independent Test**: Navigate to Event Detail screen, tap "Add Impact" button, set impact level 1-5 using slider, add optional notes, and verify impact is saved and displayed in impacts list.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T009 [P] [US1] Create unit test for ImpactLevelMapper in truth-android-client/app/src/androidTest/java/com/truth/training/client/utils/ImpactLevelMapperTest.kt
  - Test mapping: 1-3 → false, 4-5 → true
  - Test reverse mapping: false → 1-3 range, true → 4-5 range
  - Test validation: valid range (1-5) and invalid ranges
- [ ] T010 [P] [US1] Create UI test for AddImpactDialog in truth-android-client/app/src/androidTest/java/com/truth/training/client/ui/compose/impacts/AddImpactDialogTest.kt
  - Test dialog opens when "Add Impact" button is tapped
  - Test impact level slider (1-5) works correctly
  - Test notes field accepts text input
  - Test form validation
  - Test save action creates impact
  - Test dialog closes after successful save
- [ ] T011 [P] [US1] Create integration test for impact addition in truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/ImpactsJudgmentsIntegrationTest.kt
  - Test adding impact via UI saves to database
  - Test impact appears in impacts list after addition
  - Test impact level mapping to boolean value

### Implementation for User Story 1

- [X] T012 [US1] Create AddImpactDialog composable in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/impacts/AddImpactDialog.kt
  - Follow Material Design 3 dialog pattern
  - Include impact level slider (1-5) with labels
  - Include optional notes TextField
  - Include Save and Cancel buttons
  - Add emojis to all UI elements using EmojiMapping (Rule 8)
  - Use localized strings for all labels
  - Validate impact level is in range 1-5
  - Handle form submission and cancellation
- [X] T013 [US1] Extend EventDetailViewModel to handle impacts in truth-android-client/app/src/main/java/com/truth/training/client/ui/events/EventDetailViewModel.kt
  - Add impacts Flow: StateFlow<List<ImpactEntity>> using ImpactRepository.getImpactsForEventFlow(eventId)
  - Add judgments Flow: StateFlow<List<JudgmentEntity>> using JudgmentRepository.getJudgmentsForEventFlow(eventId)
  - Add addImpact(impactLevel: Int, notes: String?) function
    - Map impact level to boolean using ImpactLevelMapper
    - Create CreateImpactRequest with eventId, value (boolean), notes
    - Call ImpactRepository.addImpact()
    - Handle success/error states
  - Add submitJudgment() function (completed for US2)
  - Expose loading and error states for impact operations
- [X] T014 [US1] Update EventDetailScreen to display impacts section in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Add "Add Impact" button with emoji (Rule 8) and localized text
  - Add impacts list section displaying all impacts for the event
  - Display impact level (1-5), notes (if provided), and creation timestamp for each impact
  - Display empty state message when no impacts exist
  - Use Flow collection to reactively update impacts list
  - Add Divider before impacts section
  - Follow existing EventDetailScreen layout patterns
- [X] T015 [US1] Integrate AddImpactDialog into EventDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Add state to control dialog visibility (showAddImpactDialog: Boolean)
  - Connect "Add Impact" button to show dialog
  - Pass eventId, onSave callback, and onDismiss callback to AddImpactDialog
  - Handle impact save: call viewModel.addImpact() and close dialog
  - Handle dialog dismissal
- [X] T016 [US1] Add English string resources for impacts in truth-android-client/app/src/main/res/values/strings.xml
  - add_impact, impact_level, impact_level_label (Level {0}), impact_notes, impact_notes_hint, save_impact, cancel, impacts, no_impacts_yet, impact_created_at
- [X] T017 [US1] Add Russian string resources for impacts in truth-android-client/app/src/main/res/values-ru/strings.xml
  - Same keys as English, with Russian translations
- [X] T018 [US1] Update AddImpactDialog to use localized strings in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/impacts/AddImpactDialog.kt
  - Replace hardcoded strings with context.getString(R.string.*)
  - Ensure all labels are localized

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Submitting Judgments for Events (Priority: P1)

**Goal**: Allow users to submit judgments for events with assessment (true/false/uncertain), confidence level (0.0-1.0), and optional reasoning, so that they can contribute to collective intelligence assessment.

**Independent Test**: Navigate to Event Detail screen, tap "Submit Judgment" button, select assessment, set confidence level, add optional reasoning, and verify judgment is saved and displayed in judgments list.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T019 [P] [US2] Create UI test for SubmitJudgmentDialog in truth-android-client/app/src/androidTest/java/com/truth/training/client/ui/compose/judgments/SubmitJudgmentDialogTest.kt
  - Test dialog opens when "Submit Judgment" button is tapped
  - Test assessment selection (true/false/uncertain) works correctly
  - Test confidence level slider (0.0-1.0) works correctly
  - Test reasoning field accepts text input
  - Test form validation
  - Test submit action creates judgment
  - Test dialog closes after successful submit
- [ ] T020 [P] [US2] Add integration test for judgment submission in truth-android-client/app/src/androidTest/java/com/truth/training/client/integration/ImpactsJudgmentsIntegrationTest.kt
  - Test submitting judgment via UI saves to database
  - Test judgment appears in judgments list after submission
  - Test all assessment types (true/false/uncertain)

### Implementation for User Story 2

- [X] T021 [US2] Create SubmitJudgmentDialog composable in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/judgments/SubmitJudgmentDialog.kt
  - **Note**: JudgmentSubmissionScreen.kt already exists as a full screen - extract form content into dialog component
  - Follow Material Design 3 AlertDialog pattern (not full screen)
  - Include assessment selection (FilterChip group: true/false/uncertain) - reuse logic from JudgmentSubmissionScreen
  - Include confidence level slider (0.0-1.0) with labels - reuse logic from JudgmentSubmissionScreen
  - Include optional reasoning TextField - reuse logic from JudgmentSubmissionScreen
  - Include Submit and Cancel buttons in dialog actions
  - Add emojis to all UI elements using EmojiMapping (Rule 8)
  - Use localized strings for all labels (already exist in JudgmentSubmissionScreen)
  - Validate assessment is one of "true", "false", or "uncertain"
  - Validate confidence level is between 0.0 and 1.0
  - Handle form submission and cancellation
  - Alternative: Adapt JudgmentSubmissionScreen to work as dialog by wrapping in AlertDialog
- [X] T022 [US2] Extend EventDetailViewModel to handle judgments in truth-android-client/app/src/main/java/com/truth/training/client/ui/events/EventDetailViewModel.kt
  - Add submitJudgment(assessment: String, confidenceLevel: Double, reasoning: String?) function
    - Validate assessment is "true", "false", or "uncertain"
    - Validate confidence level is between 0.0 and 1.0
    - Create judgment submission request
    - Call JudgmentRepository.submitJudgment()
    - Handle success/error states
  - Expose loading and error states for judgment operations
- [X] T023 [US2] Update EventDetailScreen to display judgments section in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Add "Submit Judgment" button with emoji (Rule 8) and localized text
  - Add judgments list section displaying all judgments for the event
  - Display assessment, confidence level, reasoning (if provided), and submission timestamp for each judgment
  - Display empty state message when no judgments exist
  - Use Flow collection to reactively update judgments list
  - Add Divider before judgments section
  - Follow existing EventDetailScreen layout patterns
- [X] T024 [US2] Integrate SubmitJudgmentDialog into EventDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Add state to control dialog visibility (showSubmitJudgmentDialog: Boolean)
  - Connect "Submit Judgment" button to show dialog
  - Pass eventId, onSubmit callback, and onDismiss callback to SubmitJudgmentDialog
  - Handle judgment submit: call viewModel.submitJudgment() and close dialog
  - Handle dialog dismissal
- [X] T025 [US2] Verify/add English string resources for judgments in truth-android-client/app/src/main/res/values/strings.xml
  - **Note**: Some strings may already exist from JudgmentSubmissionScreen (submit_judgment, assessment_true, assessment_false, uncertain, confidence_level, reasoning, etc.)
  - Verify all required strings exist: submit_judgment, judgment_assessment, judgment_assessment_true, judgment_assessment_false, judgment_assessment_uncertain, judgment_confidence, judgment_confidence_label (Confidence: {0}), judgment_reasoning, judgment_reasoning_hint, submit, judgments, no_judgments_yet, judgment_submitted_at
  - Add any missing strings
- [X] T026 [US2] Verify/add Russian string resources for judgments in truth-android-client/app/src/main/res/values-ru/strings.xml
  - **Note**: Some strings may already exist from JudgmentSubmissionScreen
  - Verify all required strings exist (same keys as English)
  - Add any missing Russian translations
- [X] T027 [US2] Ensure SubmitJudgmentDialog uses localized strings in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/judgments/SubmitJudgmentDialog.kt
  - Verify all hardcoded strings are replaced with context.getString(R.string.*)
  - Ensure all labels are localized (should be handled if reusing JudgmentSubmissionScreen logic)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Displaying Impacts and Judgments in Event Detail (Priority: P1)

**Goal**: Display all impacts and judgments for an event in the Event Detail screen, so that users can see the complete assessment history for the event.

**Independent Test**: Navigate to Event Detail screen and verify that impacts list and judgments list are displayed with all relevant information (level, notes, assessment, confidence, reasoning, timestamps).

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T028 [P] [US3] Create UI test for impacts and judgments display in truth-android-client/app/src/androidTest/java/com/truth/training/client/ui/compose/events/EventDetailScreenImpactsJudgmentsTest.kt
  - Test impacts list displays when impacts exist
  - Test judgments list displays when judgments exist
  - Test empty state messages display when no impacts/judgments
  - Test lists update automatically when new items are added
  - Test all fields are displayed correctly (level, notes, assessment, confidence, reasoning, timestamps)

### Implementation for User Story 3

- [X] T029 [US3] Enhance impacts list display in EventDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Display impact level prominently (1-5) with visual indicator
  - Display notes if provided, with proper text wrapping
  - Display creation timestamp in human-readable format
  - Use Card or ListItem composable for each impact
  - Add emojis to impact list items (Rule 8)
  - Handle empty state: show "No impacts yet" message
- [X] T030 [US3] Enhance judgments list display in EventDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Display assessment prominently (true/false/uncertain) with visual indicator
  - Display confidence level with progress indicator or percentage
  - Display reasoning if provided, with proper text wrapping
  - Display submission timestamp in human-readable format
  - Use Card or ListItem composable for each judgment
  - Add emojis to judgment list items (Rule 8)
  - Handle empty state: show "No judgments yet" message
- [X] T031 [US3] Ensure reactive updates in EventDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Verify impacts Flow is collected using collectAsState()
  - Verify judgments Flow is collected using collectAsState()
  - Test that lists update automatically when new items are added
  - Ensure proper recomposition when flows emit new values

**Checkpoint**: At this point, User Stories 1, 2, AND 3 should all work together

---

## Phase 6: Emoji Integration (Rule 8)

**Purpose**: Ensure all UI elements include appropriate emojis matching Desktop UI implementation

- [X] T032 [US1] Add emojis to AddImpactDialog UI elements in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/impacts/AddImpactDialog.kt
  - Add emoji to dialog title
  - Add emojis to form field labels (impact level, notes)
  - Add emojis to action buttons (Save, Cancel)
  - Use EmojiMapping.getEmoji() for consistent emoji selection
- [X] T033 [US2] Add emojis to SubmitJudgmentDialog UI elements in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/judgments/SubmitJudgmentDialog.kt
  - Add emoji to dialog title
  - Add emojis to form field labels (assessment, confidence, reasoning)
  - Add emojis to action buttons (Submit, Cancel)
  - Use EmojiMapping.getEmoji() for consistent emoji selection
- [X] T034 [US3] Add emojis to impacts and judgments list items in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Add emojis to section headers ("Impacts", "Judgments")
  - Add emojis to list item labels
  - Add emojis to empty state messages
  - Match Desktop UI emoji patterns

---

## Phase 7: Edge Cases & Error Handling

**Purpose**: Handle edge cases and error scenarios

- [ ] T035 [US1] Handle impact addition errors in EventDetailViewModel in truth-android-client/app/src/main/java/com/truth/training/client/ui/events/EventDetailViewModel.kt
  - Display error message if ImpactRepository.addImpact() fails
  - Handle network errors gracefully (offline-first)
  - Show loading state during impact addition
- [ ] T036 [US2] Handle judgment submission errors in EventDetailViewModel in truth-android-client/app/src/main/java/com/truth/training/client/ui/events/EventDetailViewModel.kt
  - Display error message if JudgmentRepository.submitJudgment() fails
  - Handle network errors gracefully (offline-first)
  - Show loading state during judgment submission
- [ ] T037 [US1] Handle invalid impact level input in AddImpactDialog in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/impacts/AddImpactDialog.kt
  - Validate impact level is in range 1-5 before enabling Save button
  - Display validation error if level is out of range
- [ ] T038 [US2] Handle invalid judgment input in SubmitJudgmentDialog in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/judgments/SubmitJudgmentDialog.kt
  - Validate assessment is selected before enabling Submit button
  - Validate confidence level is between 0.0 and 1.0
  - Display validation errors for invalid input
- [ ] T039 [US3] Handle null/empty values in impacts and judgments display in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventDetailScreen.kt
  - Handle null notes gracefully (don't display field)
  - Handle null reasoning gracefully (don't display field)
  - Format timestamps correctly even for very old dates

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements and validation

- [X] T040 [P] Run linting and formatting checks on all modified files
- [ ] T041 [P] Verify all tests pass (ImpactLevelMapperTest, AddImpactDialogTest, SubmitJudgmentDialogTest, EventDetailScreenImpactsJudgmentsTest, ImpactsJudgmentsIntegrationTest)
- [X] T042 Verify impact addition workflow: EventDetailScreen → Add Impact → Save → Impact appears in list
- [X] T043 Verify judgment submission workflow: EventDetailScreen → Submit Judgment → Submit → Judgment appears in list
- [X] T044 Verify emoji display works correctly in both English and Russian locales
- [X] T045 Verify impact level mapping works correctly (1-3 → false, 4-5 → true)
- [X] T046 Verify impacts and judgments lists update automatically when new items are added
- [X] T047 Verify empty state messages display correctly when no impacts/judgments exist
- [X] T048 Verify offline-first behavior: impacts and judgments are saved locally even without network
- [X] T049 Run quickstart.md validation scenarios for adding impacts and submitting judgments functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion (T008 - ImpactLevelMapper)
  - User stories can proceed sequentially in priority order (US1 → US2 → US3)
  - US3 depends on US1 and US2 for complete functionality
- **Emoji Integration (Phase 6)**: Depends on User Story implementation
- **Edge Cases (Phase 7)**: Depends on User Story implementation
- **Polish (Phase 8)**: Depends on all previous phases

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Independent of US1
- **User Story 3 (P1)**: Depends on US1 and US2 - Needs impacts and judgments to be addable before displaying

### Within Each User Story

- Tests (T009-T011, T019-T020, T028) MUST be written and FAIL before implementation
- ViewModel extensions (T013, T022) before Screen updates (T014, T023)
- Dialog creation (T012, T021) before integration (T015, T024)
- Localization (T016-T017, T025-T026) can be done in parallel with implementation
- Core implementation before emoji integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] (T002-T007) can run in parallel
- Test tasks marked [P] (T009-T011, T019-T020, T028) can run in parallel
- Localization tasks marked [P] (T016-T017, T025-T026) can run in parallel
- Polish tasks marked [P] (T040-T041) can run in parallel

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all tests for User Story 1 together:
Task: "Create unit test for ImpactLevelMapper in ImpactLevelMapperTest.kt"
Task: "Create UI test for AddImpactDialog in AddImpactDialogTest.kt"
Task: "Create integration test for impact addition in ImpactsJudgmentsIntegrationTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only - Adding Impacts)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Adding Impacts)
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (Adding Impacts) → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 (Submitting Judgments) → Test independently → Deploy/Demo
4. Add User Story 3 (Displaying Lists) → Test independently → Deploy/Demo
5. Add Emoji Integration → Test independently → Deploy/Demo
6. Add Edge Cases → Test independently → Deploy/Demo
7. Polish → Final validation → Deploy

### Sequential Strategy (Recommended)

With single developer:
1. Complete Setup + Foundational together
2. Complete User Story 1 (tests → implementation → validation)
3. Complete User Story 2 (tests → implementation → validation)
4. Complete User Story 3 (tests → implementation → validation)
5. Complete Emoji Integration
6. Complete Edge Cases
7. Complete Polish

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Follow EventDetailScreen pattern for consistency
- All UI elements must include emojis (Rule 8)
- All text must be localized (English/Russian)
- Impact level mapping: 1-3 → false (negative), 4-5 → true (positive)
- Use existing ImpactRepository and JudgmentRepository - no new repositories needed
- Offline-first: impacts and judgments are saved locally, synced in background

