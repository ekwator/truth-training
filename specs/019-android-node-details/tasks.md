# Tasks: Android Node Details View

**Input**: Design documents from `/specs/019-android-node-details/`
**Prerequisites**: plan.md ✅, spec.md ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Mobile**: `truth-android-client/app/src/main/java/com/truth/training/client/`
- Paths shown use absolute paths from repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project structure verification and preparation

- [X] T001 Verify project structure exists per implementation plan (truth-android-client/app/src/main/java/com/truth/training/client/)
- [X] T002 [P] Verify existing dependencies: Jetpack Compose, Material Design 3, Navigation Compose, Room Database
- [X] T003 [P] Verify existing EmojiMapping utility exists at truth-android-client/app/src/main/java/com/truth/training/client/utils/EmojiMapping.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 [Foundation] Create NodeTypeMapper utility in truth-android-client/app/src/main/java/com/truth/training/client/utils/NodeTypeMapper.kt
  - Implement function to map technical types (LAN/WIFI/GLOBAL/RELAY/CLIENT) to user-friendly types (Hub/Leaf)
  - Mapping: Hub = RELAY or GLOBAL, Leaf = LAN, WIFI, or CLIENT
  - Handle unknown/invalid types by returning "Unknown"
  - Add function to get both user-friendly and technical type display

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Viewing Node Details (Priority: P1) 🎯 MVP

**Goal**: Allow users to tap on a node card in NodesScreen to view detailed information about that node in a separate NodeDetailScreen.

**Independent Test**: Navigate to Nodes screen, tap on a node card, verify that a detail screen appears with all node information (address, type, status, last seen timestamp, and other details).

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T005 [P] [US1] Create unit test for NodeTypeMapper in truth-android-client/app/src/androidTest/java/com/truth/training/client/utils/NodeTypeMapperTest.kt
  - Test Hub mapping (RELAY → Hub, GLOBAL → Hub)
  - Test Leaf mapping (LAN → Leaf, WIFI → Leaf, CLIENT → Leaf)
  - Test unknown type handling
- [ ] T006 [P] [US1] Create UI test for NodeDetailScreen in truth-android-client/app/src/androidTest/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreenTest.kt
  - Test screen displays all node information
  - Test navigation from NodesScreen
  - Test back navigation
  - Test refresh action

### Implementation for User Story 1

- [X] T007 [US1] Create NodeDetailViewModel in truth-android-client/app/src/main/java/com/truth/training/client/ui/nodes/NodeDetailViewModel.kt
  - Extend AndroidViewModel
  - Accept nodeId as parameter
  - Use DiscoveryRepository to fetch node by ID
  - Expose StateFlow for node data
  - Implement refresh action
  - Handle loading and error states
- [X] T008 [US1] Create NodeDetailScreen composable in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Follow EventDetailScreen pattern
  - Display all NodeEntity fields: address, type (Hub/Leaf and technical), status, last seen, TTL, source, node_id, created_at, updated_at
  - Display calculated fields: expires_in, age
  - Use TopAppBar with node address as title and back navigation
  - Display status with visual indicators (colors/badges)
  - Format timestamps in human-readable format
  - Include refresh action button
  - Add emojis to all UI elements using EmojiMapping (Rule 8)
- [X] T009 [US1] Add navigation route "node/{nodeId}" to MainNavigation.kt in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/MainNavigation.kt
  - Add navArgument for nodeId (Long type)
  - Create composable route for NodeDetailScreen
  - Pass nodeId to NodeDetailViewModel
  - Handle navigation callbacks
- [X] T010 [US1] Make NodeCard clickable in NodesScreen.kt in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodesScreen.kt
  - Add onClick parameter to NodeCard composable
  - Pass navigation callback from NodesScreen to NodeCard
  - Navigate to "node/{nodeId}" route when card is tapped
  - Update NodeCard signature to accept onNodeClick: (Long) -> Unit

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Node Type Display (Priority: P1)

**Goal**: Display node types as "Hub" or "Leaf" in node list cards and detail view, with technical types available in details.

**Independent Test**: View nodes list and detail screen, verify that node types are displayed as "Hub" or "Leaf" (with technical type available in details).

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US2] Add UI test for node type display in NodesScreen in truth-android-client/app/src/androidTest/java/com/truth/training/client/ui/compose/nodes/NodesScreenTest.kt
  - Test node cards display "Hub" or "Leaf" instead of technical types
  - Test detail screen shows both Hub/Leaf and technical type

### Implementation for User Story 2

- [X] T012 [US2] Update NodeCard in NodesScreen.kt to use NodeTypeMapper in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodesScreen.kt
  - Import NodeTypeMapper
  - Replace technical type display with user-friendly type (Hub/Leaf) in node card
  - Keep technical type available for detail view
  - Update AssistChip label to show "Hub" or "Leaf"
- [X] T013 [US2] Update NodeDetailScreen to display both Hub/Leaf and technical type in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Display user-friendly type (Hub/Leaf) prominently
  - Display technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT) in details section
  - Use NodeTypeMapper for consistent mapping

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: Localization & Emoji Integration

**Purpose**: Ensure all UI elements are localized and include emojis (Rule 8)

- [X] T014 [P] [US1] Add English string resources for node details in truth-android-client/app/src/main/res/values/strings.xml
  - node_detail_title, node_address, node_type, node_type_hub, node_type_leaf, node_status, node_status_reachable, node_status_unreachable, node_last_seen, node_ttl, node_expires_in, node_age, node_source, node_id, node_created_at, node_updated_at, node_refresh, node_unknown_type, node_expired
- [X] T015 [P] [US1] Add Russian string resources for node details in truth-android-client/app/src/main/res/values-ru/strings.xml
  - Same keys as English, with Russian translations
- [X] T016 [US1] Update NodeDetailScreen to use localized strings in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Replace hardcoded strings with context.getString(R.string.*)
  - Ensure all labels are localized
- [X] T017 [US1] Add emojis to NodeDetailScreen UI elements using EmojiMapping in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Add emoji to screen title
  - Add emojis to all section headers
  - Add emojis to action buttons
  - Add emojis to status indicators
  - Follow existing EmojiMapping patterns from EventDetailScreen
- [X] T018 [US2] Update NodeCard to use localized strings for type display in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodesScreen.kt
  - Use context.getString(R.string.node_type_hub) or context.getString(R.string.node_type_leaf)
  - Ensure emojis are included in type display

---

## Phase 6: Edge Cases & Error Handling

**Purpose**: Handle edge cases and error scenarios

- [X] T019 [US1] Handle unknown/invalid node types in NodeDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Display "Unknown" for invalid types
  - Show technical type in details even for unknown types
- [X] T020 [US1] Handle expired TTL in NodeDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Display "Expired" status clearly when expires_in <= 0
  - Use error color scheme for expired status
- [X] T021 [US1] Handle null node_id and source fields in NodeDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Display "N/A" or hide field if node_id is null
  - Display "Unknown" or hide field if source is null
- [X] T022 [US1] Handle timestamp formatting edge cases in NodeDetailScreen in truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodeDetailScreen.kt
  - Format very old timestamps appropriately (e.g., years ago)
  - Format very large TTL values appropriately (e.g., days, hours, minutes)
  - Use SimpleDateFormat or similar for consistent formatting

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements and validation

- [X] T023 [P] Run linting and formatting checks on all modified files
- [ ] T024 [P] Verify all tests pass (NodeTypeMapperTest, NodeDetailScreenTest, NodesScreenTest)
- [X] T025 Verify navigation works correctly: NodesScreen → NodeDetailScreen → back to NodesScreen
- [X] T026 Verify emoji display works correctly in both English and Russian locales
- [X] T027 Verify node type mapping works correctly for all node types (LAN, WIFI, GLOBAL, RELAY, CLIENT)
- [X] T028 Verify calculated fields (expires_in, age) are computed and displayed correctly
- [X] T029 Verify refresh action updates node information correctly
- [X] T030 Run quickstart.md validation scenarios for node viewing functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-4)**: All depend on Foundational phase completion (T004 - NodeTypeMapper)
  - User stories can proceed sequentially in priority order (US1 → US2)
- **Localization (Phase 5)**: Depends on User Story implementation
- **Edge Cases (Phase 6)**: Depends on User Story implementation
- **Polish (Phase 7)**: Depends on all previous phases

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Depends on NodeTypeMapper (T004) and integrates with US1 NodeDetailScreen

### Within Each User Story

- Tests (T005, T006, T011) MUST be written and FAIL before implementation
- ViewModel (T007) before Screen (T008)
- Screen (T008) before Navigation (T009)
- Navigation (T009) before making cards clickable (T010)
- Core implementation before localization
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] (T002, T003) can run in parallel
- Test tasks marked [P] (T005, T006, T011) can run in parallel
- Localization tasks marked [P] (T014, T015) can run in parallel
- Polish tasks marked [P] (T023, T024) can run in parallel

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all tests for User Story 1 together:
Task: "Create unit test for NodeTypeMapper in NodeTypeMapperTest.kt"
Task: "Create UI test for NodeDetailScreen in NodeDetailScreenTest.kt"
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
4. Add Localization → Test independently → Deploy/Demo
5. Add Edge Cases → Test independently → Deploy/Demo
6. Polish → Final validation → Deploy

### Sequential Strategy (Recommended)

With single developer:
1. Complete Setup + Foundational together
2. Complete User Story 1 (tests → implementation → validation)
3. Complete User Story 2 (tests → implementation → validation)
4. Complete Localization
5. Complete Edge Cases
6. Complete Polish

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

