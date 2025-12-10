# Feature Specification: Android Node Details View

**Feature Branch**: `019-android-node-details`  
**Created**: 2025-12-10  
**Status**: Draft  
**Input**: User description: "@docs/quickstart_android.md:389-398 Этот функционал и соответствующие экраны не реализованы в приложении. Необходимо реализовать в соответствии со спецификацией проекта."

## Clarifications

### Session 2025-12-10

- Q: Should node detail view be a separate screen or a bottom sheet/dialog? → A: According to spec/24-function_mobile_android.md and Desktop UI patterns, node details should be displayed in a separate detail screen accessible by tapping on a node card, matching EventDetailScreen pattern.
- Q: Should node type display show "Hub/Leaf" or technical types (LAN/WIFI/GLOBAL/RELAY/CLIENT)? → A: According to quickstart_android.md, node details should show "Type (Hub/Leaf)". However, NodeEntity stores technical types. We should display both: user-friendly "Hub/Leaf" mapping for primary display, with technical type available in details. Hub = RELAY/GLOBAL, Leaf = LAN/WIFI/CLIENT.
- Q: What information should be displayed in node detail view? → A: All node information from NodeEntity: address, type (both Hub/Leaf and technical), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at. Also display calculated fields: expires_in, age.
- Q: Should node detail screen have actions? → A: Yes, should include actions like: refresh node, health check, remove node (if applicable), similar to Desktop NodesPanel detail view.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Viewing Node Details (Priority: P1)

As an Android user, I want to tap on a node in the nodes list to view detailed information about that node, so that I can see all available information about the network node.

**Why this priority**: This is a core feature described in quickstart_android.md and required by spec/24-function_mobile_android.md. Without this functionality, users cannot view complete node information.

**Independent Test**: Can be fully tested by navigating to Nodes screen, tapping on a node card, and verifying that a detail screen appears with all node information (address, type, status, last seen timestamp, and other details).

**Acceptance Scenarios**:

1. **Given** I am viewing the Nodes screen with discovered nodes, **When** I tap on a node card, **Then** a NodeDetailScreen appears showing all node information
2. **Given** I am viewing the NodeDetailScreen, **When** I examine the screen, **Then** I see address, type (Hub/Leaf and technical type), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at, expires_in, and age
3. **Given** I am viewing the NodeDetailScreen, **When** I tap the back button, **Then** I return to the Nodes screen
4. **Given** I am viewing the NodeDetailScreen, **When** I examine the type field, **Then** I see both user-friendly type (Hub/Leaf) and technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT)

---

### User Story 2 - Node Type Display (Priority: P1)

As an Android user, I want to see node type displayed as "Hub" or "Leaf" in the node list and detail view, so that I can quickly understand the node's role in the network.

**Why this priority**: This is specified in quickstart_android.md as "Type (Hub/Leaf)". Users need to understand node roles at a glance.

**Independent Test**: Can be fully tested by viewing nodes list and detail screen, and verifying that node types are displayed as "Hub" or "Leaf" (with technical type available in details).

**Acceptance Scenarios**:

1. **Given** I am viewing the Nodes screen, **When** I examine node cards, **Then** each node displays type as "Hub" or "Leaf" (not technical types like LAN/WIFI)
2. **Given** I am viewing the NodeDetailScreen, **When** I examine the type field, **Then** I see both "Hub/Leaf" display and technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT) in details
3. **Given** a node has type RELAY or GLOBAL, **When** I view the node, **Then** it displays as "Hub"
4. **Given** a node has type LAN, WIFI, or CLIENT, **When** I view the node, **Then** it displays as "Leaf"

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: NodeCard in NodesScreen MUST be clickable and navigate to NodeDetailScreen when tapped.
- **FR-002**: NodeDetailScreen MUST display all node information: address, type (Hub/Leaf and technical), status (reachable/unreachable), last seen timestamp, TTL, source, node_id, created_at, updated_at, expires_in, age.
- **FR-003**: Node type MUST be displayed as "Hub" or "Leaf" in node list cards, with technical type (LAN/WIFI/GLOBAL/RELAY/CLIENT) available in detail view.
- **FR-004**: Node type mapping MUST be: Hub = RELAY or GLOBAL, Leaf = LAN, WIFI, or CLIENT.
- **FR-005**: NodeDetailScreen MUST include a TopAppBar with node address as title and back navigation.
- **FR-006**: NodeDetailScreen MUST display status (reachable/unreachable) with visual indicators (colors/badges).
- **FR-007**: NodeDetailScreen MUST display timestamps in human-readable format (e.g., "2025-12-10 14:30:25").
- **FR-008**: NodeDetailScreen MUST display calculated fields: expires_in (time until TTL expires) and age (time since last_seen).
- **FR-009**: NodeDetailScreen MUST support refresh action to update node information.
- **FR-010**: All UI elements MUST include appropriate emojis matching Desktop UI implementation (constitutional requirement Rule 8).
- **FR-011**: All text labels MUST be localized (English/Russian) using Android string resources.
- **FR-012**: NodeDetailScreen MUST use Navigation Compose for navigation from NodesScreen.

### Key Entities *(include if feature involves data)*

- **NodeEntity**: Existing entity representing a network node. Key attributes: id, address, type (LAN/WIFI/GLOBAL/RELAY/CLIENT), reachable (0/1), lastSeen, ttl, source, nodeId, createdAt, updatedAt.
- **NodeDetailScreen**: New Compose screen for displaying detailed node information.
- **NodeTypeMapper**: Utility function to map technical node types (LAN/WIFI/GLOBAL/RELAY/CLIENT) to user-friendly types (Hub/Leaf).
- **NodeDetailViewModel**: ViewModel for NodeDetailScreen managing node data and actions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can successfully tap on a node card in NodesScreen and navigate to NodeDetailScreen.
- **SC-002**: NodeDetailScreen displays all required node information in a clear, organized layout.
- **SC-003**: Node types are correctly displayed as "Hub" or "Leaf" in node list cards, with technical types available in detail view.
- **SC-004**: Node type mapping (Hub = RELAY/GLOBAL, Leaf = LAN/WIFI/CLIENT) is correctly implemented.
- **SC-005**: All timestamps are displayed in human-readable format.
- **SC-006**: Calculated fields (expires_in, age) are correctly computed and displayed.
- **SC-007**: Navigation between NodesScreen and NodeDetailScreen works correctly using Navigation Compose.
- **SC-008**: All UI elements include appropriate emojis (Rule 8) and support bilingual localization (English/Russian).
- **SC-009**: NodeDetailScreen matches Desktop UI patterns for node detail display.
- **SC-010**: Refresh action updates node information correctly.

## Edge Cases

- **EC-001**: If node type is unknown or invalid, display "Unknown" and show technical type in details.
- **EC-002**: If node has expired TTL (expires_in <= 0), display "Expired" status clearly.
- **EC-003**: If node_id is null, display "N/A" or hide the field.
- **EC-004**: If source is null, display "Unknown" or hide the field.
- **EC-005**: Handle very old timestamps (e.g., years ago) with appropriate formatting.
- **EC-006**: Handle very large TTL values with appropriate formatting (e.g., days, hours, minutes).

## Dependencies

- **Existing Components**: NodesScreen, NodesViewModel, NodeEntity, DiscoveryRepository
- **Navigation**: AndroidX Navigation Compose
- **UI Framework**: Jetpack Compose, Material Design 3
- **Localization**: Android string resources (values/strings.xml, values-ru/strings.xml)
- **Emoji Support**: EmojiMapping utility

## References

- `spec/24-function_mobile_android.md` - Android functional specification
- `docs/quickstart_android.md:389-398` - User documentation for node viewing
- `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/nodes/NodesScreen.kt` - Existing NodesScreen implementation
- `truth-android-client/app/src/main/java/com/truth/training/client/data/database/entities/NodeEntity.kt` - NodeEntity definition
- Desktop UI: `ui/desktop/src/components/NodesPanel.tsx` - Desktop node detail implementation

