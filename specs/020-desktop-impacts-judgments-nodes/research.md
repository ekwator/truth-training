# Research: Desktop Impacts, Judgments, and Network Nodes UI

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Date**: 2025-01-XX  
**Status**: Complete

## Research Objectives

1. Analyze Android implementation patterns for impacts, judgments, and node details
2. Review existing Desktop UI structure and components
3. Identify API methods and Tauri commands available
4. Determine component architecture and state management approach
5. Document technical decisions and constraints

## Android Implementation Analysis

### Impacts Implementation (specs/018-android-impacts-judgments)

**Key Components:**
- `ImpactLevelMapper.kt`: Maps impact levels (1-5) to boolean values
  - Levels 1-3 → false (negative)
  - Levels 4-5 → true (positive)
- `AddImpactDialog.kt`: Modal dialog with slider (1-5) and notes field
- `EventDetailViewModel.kt`: Manages impact state and operations
- `EventDetailScreen.kt`: Displays impacts list and "Add Impact" button

**Data Flow:**
1. User sets impact level (1-5) in dialog
2. ImpactLevelMapper maps to boolean value
3. CreateImpactRequest sent to ImpactRepository
4. ImpactEntity stored in local database
5. UI updates reactively via Flow

### Judgments Implementation (specs/018-android-impacts-judgments)

**Key Components:**
- `SubmitJudgmentDialog.kt`: Modal dialog with assessment selection, confidence slider, reasoning field
- `EventDetailViewModel.kt`: Manages judgment state and operations
- `EventDetailScreen.kt`: Displays judgments list and "Submit Judgment" button

**Data Flow:**
1. User selects assessment (true/false/uncertain)
2. User sets confidence level (0.0-1.0)
3. Optional reasoning added
4. CreateJudgmentRequest sent to JudgmentRepository
5. JudgmentEntity stored in local database
6. UI updates reactively via Flow

### Node Details Implementation (specs/019-android-node-details)

**Key Components:**
- `NodeTypeMapper.kt`: Maps technical types to Hub/Leaf
  - Hub = RELAY, GLOBAL
  - Leaf = LAN, WIFI, CLIENT
- `NodeDetailViewModel.kt`: Manages node data and refresh
- `NodeDetailScreen.kt`: Displays all node information
- `NodeCard`: Clickable card that navigates to detail screen

**Data Flow:**
1. User clicks node card in NodesScreen
2. Navigation to NodeDetailScreen with nodeId
3. NodeDetailViewModel loads node from DiscoveryRepository
4. NodeDetailScreen displays all node fields
5. Refresh action reloads node data

## Desktop UI Structure Analysis

### Existing Components

**EventSummary.tsx:**
- Currently displays judgments list (partial implementation)
- Displays impacts list (partial implementation, reserved for future)
- Uses ApiService for data fetching
- React hooks for state management
- Tailwind CSS for styling

**NodesPanel.tsx:**
- Displays list of nodes with filters
- Supports refresh, discover, cleanup, health check actions
- Uses ApiService.listNodes() for data
- No detail view currently implemented

**ApiService:**
- `addImpact(impactData: AddImpactRequest)`: ✅ Exists
- `createJudgment(judgmentData: CreateJudgmentRequest)`: ✅ Exists
- `getJudgments(eventId?: number)`: ✅ Exists
- `getImpactsForEvent(eventId: number)`: ❌ Needs implementation
- `listNodes(nodeType?, reachable?)`: ✅ Exists
- `getNodeById(nodeId: number)`: ❌ Needs implementation

### Tauri Commands Analysis

**Existing Commands:**
- `add_impact`: ✅ Exists in `commands/impacts.rs`
- `submit_judgment_fast`: ✅ Exists in `commands/judgments.rs`
- `judgments_list_fast`: ✅ Exists
- `list_nodes`: ✅ Exists in `discovery.rs`

**Missing Commands:**
- `get_impacts_for_event`: ❌ Needs implementation
- `get_node_by_id`: ❌ Needs implementation (or use existing list_nodes with filter)

### State Management

**Current Approach:**
- React hooks (useState, useEffect) for component state
- Zustand stores for global state (sync, navigation)
- ApiService for API communication
- No dedicated store for impacts/judgments/nodes

**Recommended Approach:**
- Use React hooks for local component state (modals, forms)
- Use ApiService directly for data fetching
- Consider Zustand store if shared state needed across components

## Technical Decisions

### 1. Impact Level Mapping

**Decision**: Use ImpactLevelMapper utility matching Android implementation
- Levels 1-3 → false (negative impact)
- Levels 4-5 → true (positive impact)
- Store boolean value in database
- Display as "Positive (Level 4-5)" or "Negative (Level 1-3)" in UI

**Rationale**: Matches Android implementation for consistency and matches Desktop API which uses boolean value.

### 2. Modal vs Separate Screen

**Decision**: Use Headless UI Dialog components for modals
- AddImpactModal: Modal dialog in EventSummary
- SubmitJudgmentModal: Modal dialog in EventSummary
- NodeDetailView: Modal or side panel in NodesPanel

**Rationale**: Matches Desktop UI patterns (modals for forms) and Android implementation (dialogs).

### 3. Node Type Display

**Decision**: Use NodeTypeMapper utility matching Android implementation
- Display "Hub" or "Leaf" in node list
- Show technical type in detail view
- Mapping: Hub = RELAY/GLOBAL, Leaf = LAN/WIFI/CLIENT

**Rationale**: Matches Android implementation and user-friendly display requirements.

### 4. Data Fetching Strategy

**Decision**: Use ApiService methods with React hooks
- useEffect for initial load
- useState for data storage
- Manual refresh on user action
- No automatic polling (unlike Android Flow)

**Rationale**: Desktop UI uses request-response pattern, not reactive streams like Android Flow.

### 5. Form Validation

**Decision**: Client-side validation before API call
- Impact level: 1-5 range check
- Assessment: "true"/"false"/"uncertain" check
- Confidence: 0.0-1.0 range check
- Disable submit button if invalid

**Rationale**: Provides immediate feedback and matches Android implementation.

### 6. Error Handling

**Decision**: Display errors in modals and use toast notifications
- Form validation errors: Inline in modal
- API errors: Toast notification + error state
- Network errors: Offline queue (existing OfflineQueueService)

**Rationale**: Matches Desktop UI patterns and existing error handling.

## Constraints

### API Constraints

1. **Impact API**: Uses `impact_level` (1-5) in request, but backend stores boolean `value`
   - Solution: Map in frontend using ImpactLevelMapper before API call
   - Backend TODO comment indicates future mapping consideration

2. **Judgment API**: Uses `assessment` as string ("true"/"false"/"uncertain")
   - Desktop types use `JudgmentAssessment` type ('confirm'/'reject'/'abstain')
   - Need to map between formats

3. **Node API**: `list_nodes` exists, but no `get_node_by_id`
   - Solution: Filter `list_nodes` by address or add new command

### UI Constraints

1. **EventSummary**: Already has partial impacts/judgments display
   - Need to enhance with "Add Impact" and "Submit Judgment" buttons
   - Need to load impacts data (currently empty array)

2. **NodesPanel**: No detail view currently
   - Need to add click handler and detail view component

3. **Localization**: Desktop uses i18n system
   - Need to add strings for new UI elements
   - Match Android string keys for consistency

### Performance Constraints

1. **Modal Performance**: < 50ms open/close
2. **Form Submission**: < 200ms (local, offline-first)
3. **List Updates**: < 100ms after data fetch
4. **60fps UI**: Smooth interactions, no jank

## Dependencies

### Existing Dependencies (No Changes Required)

- React 18.2+
- TypeScript 5.2+
- Tailwind CSS
- Headless UI (Dialog components)
- Zustand (if needed for shared state)
- Tauri 2.0
- core_lib (backend storage)

### New Dependencies (None Required)

- All required functionality available in existing dependencies
- ImpactLevelMapper and NodeTypeMapper: Simple utility functions, no dependencies

## Implementation Approach

### Phase 1: Utilities

1. Create `utils/impactLevelMapper.ts`
2. Create `utils/nodeTypeMapper.ts`
3. Write unit tests for both utilities

### Phase 2: API Methods

1. Add `getImpactsForEvent()` to ApiService (if backend command exists)
2. Verify `addImpact()` works correctly
3. Verify `createJudgment()` works correctly
4. Add `getNodeById()` or use filtered `listNodes()`

### Phase 3: Components

1. Create `AddImpactModal.tsx`
2. Create `SubmitJudgmentModal.tsx`
3. Create `NodeDetailView.tsx`
4. Update `EventSummary.tsx` with buttons and lists
5. Update `NodesPanel.tsx` with click handler

### Phase 4: Integration

1. Integrate modals into EventSummary
2. Integrate NodeDetailView into NodesPanel
3. Add emoji support (Rule 8)
4. Add localization (EN/RU)
5. Test offline-first behavior

## References

- Android Impacts/Judgments Spec: `specs/018-android-impacts-judgments/spec.md`
- Android Node Details Spec: `specs/019-android-node-details/spec.md`
- Android Implementation: `truth-android-client/app/src/main/java/com/truth/training/client/`
- Desktop ApiService: `ui/desktop/src/services/api.ts`
- Desktop Tauri Commands: `ui/desktop/src-tauri/src/commands/`
- Desktop EventSummary: `ui/desktop/src/pages/EventSummary.tsx`
- Desktop NodesPanel: `ui/desktop/src/components/NodesPanel.tsx`

