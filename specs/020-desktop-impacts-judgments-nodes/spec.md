# Feature Specification: Desktop Impacts, Judgments, and Network Nodes UI

**Feature Branch**: `020-desktop-impacts-judgments-nodes`  
**Created**: 2025-01-XX  
**Status**: Draft  
**Input**: User description: "Используя информацию о реализации для android-client необходимо реализавать эту-же функциональность для UI Desktop но использую его собстенную структуру экранов. @docs/quickstart_desktop.md:306-332"

## Clarifications

### Session 2025-01-XX

- Q: Should impacts and judgments be added inline in EventSummary screen or as separate modals? → A: According to Desktop UI patterns and Android implementation, impacts and judgments should be added inline in EventSummary screen using modals/dialogs, matching the existing Desktop UI structure.
- Q: Should impact level use 1-5 slider as in Android? → A: Yes, impact level must be 1-5 as specified in quickstart_desktop.md. The Desktop API uses `value: Boolean` but we need to map impact_level (1-5) to the boolean value. For now, impact_level > 3 maps to positive (true), <= 3 maps to negative (false), matching Android implementation pattern.
- Q: Should Network Nodes be a separate screen or integrated into existing NodesPanel? → A: According to Desktop UI structure, NodesPanel already exists. We need to add node detail view functionality, allowing users to click on a node to view detailed information, similar to Android NodeDetailScreen.
- Q: How should node types be displayed? → A: Node types should be displayed as "Hub" or "Leaf" in the node list, with technical types (LAN/WIFI/GLOBAL/RELAY/CLIENT) available in detail view, matching Android implementation.
- Q: Should we follow Android implementation patterns? → A: Yes, we should match Android implementation patterns for consistency, but adapt to Desktop UI structure (React/TypeScript with Tauri, using existing components and navigation).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Adding Impacts to Events (Priority: P1)

As a Desktop user, I want to add impacts to events with impact level (1-5) and optional notes, so that I can record the impact assessment for each event.

**Why this priority**: This is a core feature described in quickstart_desktop.md and required for feature parity with Android implementation. Without this functionality, users cannot complete the impact assessment workflow.

**Independent Test**: Can be fully tested by navigating to Event Summary screen, clicking "Add Impact" button, setting impact level 1-5 using slider, adding optional notes, and verifying impact is saved and displayed in impacts list.

**Acceptance Scenarios**:

1. **Given** I am viewing an event summary screen, **When** I click "Add Impact" button, **Then** an impact form appears (modal/dialog) with impact level slider (1-5) and notes field
2. **Given** I am filling the impact form, **When** I set impact level using slider (1-5) and add optional notes, **Then** the form validates the input and enables Save button
3. **Given** I submit the impact form, **When** the impact is saved, **Then** the impact appears in the impacts list on Event Summary screen and the form closes
4. **Given** I view the impacts list, **When** I examine the list, **Then** each impact displays impact level range (Positive 4-5 or Negative 1-3), notes (if provided), and creation timestamp

---

### User Story 2 - Submitting Judgments for Events (Priority: P1)

As a Desktop user, I want to submit judgments for events with assessment (true/false/uncertain), confidence level (0.0-1.0), and optional reasoning, so that I can contribute to collective intelligence assessment.

**Why this priority**: This is a core feature described in quickstart_desktop.md and required for feature parity with Android implementation. Without this functionality, users cannot complete the judgment submission workflow.

**Independent Test**: Can be fully tested by navigating to Event Summary screen, clicking "Submit Judgment" button, selecting assessment, setting confidence level, adding optional reasoning, and verifying judgment is saved and displayed in judgments list.

**Acceptance Scenarios**:

1. **Given** I am viewing an event summary screen, **When** I click "Submit Judgment" button, **Then** a judgment form appears (modal/dialog) with assessment selection (true/false/uncertain), confidence level slider (0.0-1.0), and reasoning field
2. **Given** I am filling the judgment form, **When** I select assessment and set confidence level, **Then** the form validates the input and enables Submit button
3. **Given** I submit the judgment form, **When** the judgment is saved, **Then** the judgment appears in the judgments list on Event Summary screen and the form closes
4. **Given** I view the judgments list, **When** I examine the list, **Then** each judgment displays assessment, confidence level, reasoning (if provided), and submission timestamp

---

### User Story 3 - Displaying Impacts and Judgments in Event Summary (Priority: P1)

As a Desktop user, I want to view all impacts and judgments for an event in the Event Summary screen, so that I can see the complete assessment history for the event.

**Why this priority**: This is required for feature parity with Android implementation. Event Summary Screen must display impacts list and judgments list. Without this, users cannot see existing assessments.

**Independent Test**: Can be fully tested by navigating to Event Summary screen and verifying that impacts list and judgments list are displayed with all relevant information (level range, notes, assessment, confidence, reasoning, timestamps).

**Acceptance Scenarios**:

1. **Given** an event has impacts, **When** I view the Event Summary screen, **Then** the impacts list section displays all impacts with level range, notes, and timestamp
2. **Given** an event has judgments, **When** I view the Event Summary screen, **Then** the judgments list section displays all judgments with assessment, confidence, reasoning, and timestamp
3. **Given** an event has no impacts or judgments, **When** I view the Event Summary screen, **Then** empty state messages are displayed for impacts and judgments sections
4. **Given** impacts or judgments are added, **When** I view the Event Summary screen, **Then** the lists update automatically to show new items

---

### User Story 4 - Viewing Network Node Details (Priority: P1)

As a Desktop user, I want to click on a node in the NodesPanel to view detailed information about that node, so that I can see all available information about the network node.

**Why this priority**: This is a core feature described in quickstart_desktop.md and required for feature parity with Android implementation. Without this functionality, users cannot view complete node information.

**Independent Test**: Can be fully tested by navigating to NodesPanel (if accessible from Settings or Dashboard), clicking on a node, and verifying that a detail view appears with all node information (address, type, status, last seen timestamp, and other details).

**Acceptance Scenarios**:

1. **Given** I am viewing the NodesPanel with discovered nodes, **When** I click on a node, **Then** a node detail view appears showing all node information
2. **Given** I am viewing the node detail view, **When** I examine the view, **Then** I see address, type (Hub/Leaf and technical type), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at, expires_in, and age
3. **Given** I am viewing the node detail view, **When** I click the close/back button, **Then** I return to the NodesPanel
4. **Given** I am viewing the node detail view, **When** I examine the type field, **Then** I see both user-friendly type (Hub/Leaf) and technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT)

---

### User Story 5 - Node Type Display (Priority: P1)

As a Desktop user, I want to see node type displayed as "Hub" or "Leaf" in the node list and detail view, so that I can quickly understand the node's role in the network.

**Why this priority**: This is specified in quickstart_desktop.md as "Type (Hub/Leaf)". Users need to understand node roles at a glance, matching Android implementation.

**Independent Test**: Can be fully tested by viewing nodes list and detail view, and verifying that node types are displayed as "Hub" or "Leaf" (with technical type available in details).

**Acceptance Scenarios**:

1. **Given** I am viewing the NodesPanel, **When** I examine node cards, **Then** each node displays type as "Hub" or "Leaf" (not technical types like LAN/WIFI)
2. **Given** I am viewing the node detail view, **When** I examine the type field, **Then** I see both "Hub/Leaf" display and technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT) in details
3. **Given** a node has type RELAY or GLOBAL, **When** I view the node, **Then** it displays as "Hub"
4. **Given** a node has type LAN, WIFI, or CLIENT, **When** I view the node, **Then** it displays as "Leaf"

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: EventSummary screen MUST display "Add Impact" button that opens impact form (modal/dialog).
- **FR-002**: Impact form MUST include impact level slider (1-5) and optional notes field.
- **FR-003**: Impact level MUST be validated to be in range 1-5.
- **FR-004**: Impact form MUST map impact_level (1-5) to boolean value: impact_level > 3 → positive (true), impact_level <= 3 → negative (false), matching Android implementation.
- **FR-005**: EventSummary screen MUST display "Submit Judgment" button that opens judgment form (modal/dialog).
- **FR-006**: Judgment form MUST include assessment selection (true/false/uncertain), confidence level slider (0.0-1.0), and optional reasoning field.
- **FR-007**: Assessment MUST be validated to be "true", "false", or "uncertain".
- **FR-008**: Confidence level MUST be validated to be between 0.0 and 1.0.
- **FR-009**: EventSummary screen MUST display impacts list section showing all impacts for the event.
- **FR-010**: EventSummary screen MUST display judgments list section showing all judgments for the event.
- **FR-011**: Impacts list MUST display impact level range (Positive 4-5 or Negative 1-3), notes (if provided), and creation timestamp.
- **FR-012**: Judgments list MUST display assessment, confidence level, reasoning (if provided), and submission timestamp.
- **FR-013**: EventSummary screen MUST display empty state messages when no impacts or judgments exist.
- **FR-014**: Impacts and judgments lists MUST update automatically when new items are added.
- **FR-015**: NodesPanel MUST support clicking on a node to view detailed information.
- **FR-016**: Node detail view MUST display all node information: address, type (Hub/Leaf and technical), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at, expires_in, age.
- **FR-017**: Node type MUST be displayed as "Hub" or "Leaf" in node list, with technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT) available in detail view.
- **FR-018**: Node type mapping MUST be: Hub = RELAY or GLOBAL, Leaf = LAN, WIFI, or CLIENT.
- **FR-019**: All UI elements MUST include appropriate emojis matching Desktop UI implementation (constitutional requirement Rule 8).
- **FR-020**: All text labels MUST be localized (English/Russian) using Desktop i18n system.
- **FR-021**: Impact addition MUST use ApiService.addImpact() for offline-first storage.
- **FR-022**: Judgment submission MUST use ApiService.submitJudgment() for offline-first storage.
- **FR-023**: EventSummary MUST observe impacts and judgments data for reactive updates.

### Key Entities *(include if feature involves data)*

- **Impact**: Represents a recorded impact for an event. Key attributes: id, eventId, typeId, value (Boolean), notes, createdAt. Impact level (1-5) is mapped to typeId and value.
- **Judgment**: Represents a user's assessment of an event. Key attributes: id, eventId, assessment ("true"/"false"/"uncertain"), confidenceLevel (0.0-1.0), reasoning, submittedAt.
- **NodeRecord**: Existing entity representing a network node. Key attributes: id, address, node_type (LAN/WIFI/GLOBAL/RELAY/CLIENT), reachable (boolean), last_seen, ttl, source, node_id, expires_in.
- **ApiService**: Service for API communication. Key methods: addImpact(), submitJudgment(), getImpactsForEvent(), getJudgmentsForEvent(), listNodes(), getNodeById().
- **ImpactLevelMapper**: Utility function to map impact levels (1-5) to boolean values (true/false) and vice-versa.
- **NodeTypeMapper**: Utility function to map technical node types (LAN/WIFI/GLOBAL/RELAY/CLIENT) to user-friendly types (Hub/Leaf).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can successfully click "Add Impact" button in EventSummary and add impacts with level 1-5.
- **SC-002**: Users can successfully click "Submit Judgment" button in EventSummary and submit judgments with assessment and confidence.
- **SC-003**: EventSummary displays all impacts and judgments for an event in organized lists.
- **SC-004**: Impact level mapping (1-3 → false, 4-5 → true) is correctly implemented.
- **SC-005**: Users can successfully click on a node in NodesPanel and view detailed information.
- **SC-006**: Node detail view displays all required node information in a clear, organized layout.
- **SC-007**: Node types are correctly displayed as "Hub" or "Leaf" in node list, with technical types available in detail view.
- **SC-008**: Node type mapping (Hub = RELAY/GLOBAL, Leaf = LAN/WIFI/CLIENT) is correctly implemented.
- **SC-009**: All timestamps are displayed in human-readable format.
- **SC-010**: Calculated fields (expires_in, age) are correctly computed and displayed.
- **SC-011**: All UI elements include appropriate emojis (Rule 8) and support bilingual localization (English/Russian).
- **SC-012**: Desktop UI matches Android UI patterns for impacts, judgments, and node details display.
- **SC-013**: Forms validate input correctly and provide clear error messages.
- **SC-014**: Lists update automatically when new items are added.

## Edge Cases

- **EC-001**: If impact level is out of range, display validation error and disable Save button.
- **EC-002**: If assessment is not selected, display validation error and disable Submit button.
- **EC-003**: If confidence level is out of range (0.0-1.0), display validation error and disable Submit button.
- **EC-004**: Handle null/empty notes and reasoning gracefully (don't display field if empty).
- **EC-005**: If node type is unknown or invalid, display "Unknown" and show technical type in details.
- **EC-006**: If node has expired TTL (expires_in <= 0), display "Expired" status clearly.
- **EC-007**: If node_id is null, display "N/A" or hide the field.
- **EC-008**: If source is null, display "Unknown" or hide the field.
- **EC-009**: Handle very old timestamps (e.g., years ago) with appropriate formatting.
- **EC-010**: Handle very large TTL values with appropriate formatting (e.g., days, hours, minutes).
- **EC-011**: Handle network errors gracefully (offline-first strategy).
- **EC-012**: Handle loading states during impact addition and judgment submission.

## Technical Notes

- Desktop UI uses React/TypeScript with Tauri backend
- State management uses Zustand stores
- API communication through ApiService (Tauri commands)
- UI components use Tailwind CSS with dark mode support
- Emoji mapping uses `@/utils/emojiMapping` utility
- Localization uses Desktop i18n system (similar to Android string resources)
- Forms should use Headless UI Dialog components for modals
- Sliders should use HTML5 range input or custom slider component
- Node detail view can be implemented as a modal/dialog or separate panel

## Dependencies

- Existing EventSummary.tsx component
- Existing NodesPanel.tsx component
- Existing ApiService for API communication
- Existing emojiMapping utility
- Existing i18n localization system
- Tauri commands for backend communication

## References

- Android implementation: `specs/018-android-impacts-judgments/spec.md`
- Android node details: `specs/019-android-node-details/spec.md`
- Desktop quickstart: `docs/quickstart_desktop.md:306-332`
- Desktop UI structure: `ui/desktop/src/pages/EventSummary.tsx`, `ui/desktop/src/components/NodesPanel.tsx`

