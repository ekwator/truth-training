# Feature Specification: Truth Training Desktop UI — Text-Only Interface

**Feature Branch**: `003-truth-training-desktop`  
**Created**: 2025-10-28  
**Status**: Draft  
**Input**: User description: "Truth Training Desktop UI — Text-Only Interface Specification"

## User Scenarios & Testing (mandatory)

### Primary User Story
As an operator of Truth Training, I need a minimal, text-only desktop interface to create and review training events, view summaries and analytical results, and inspect logs, so that I can manage the system efficiently without any graphical assets or icons.

### Acceptance Scenarios
1. Given the app is open, When I use the top menu to select Home, Then I see system versions, sync status, active clients list, last data update, and actions to Create New Event and Refresh Data.
2. Given I am on New Event, When I fill Event Name, Description, select Context from the knowledge base list, choose Start/End dates, and click Save Event, Then the event is created and available in Event Summary.
3. Given events exist, When I open Event Summary and select an event, Then I can read description, results (impact), notes, recommendations and use Add Impact, Edit Summary, Save Changes, Back to Event List.
4. Given historical data exists, When I open Overall Summary and click Refresh Data, Then I see text metrics (Total Events, Average Impact Level, Last Updated) and a text table with Event, Summary, Impact, Date.
5. Given I need trends, When I open Training Results, set filters (Date Range, Context) and click Update, Then I see ASCII-style progress bar for Impact Progress and the Average Score value; Reset Filters restores defaults.
6. Given I am troubleshooting, When I open Logs, Then I see a scrollable text log and can Clear Log, Save Log to File, and Refresh.
7. Given I prefer keyboard, When I press Alt+1..Alt+6, Then the app navigates to the corresponding screens instantly.

### Edge Cases
- Empty knowledge base: Context dropdown shows [NEEDS CLARIFICATION: behavior when Data_Schema.md is empty or missing].
- No events: Event Summary lists a clear text state indicating no events exist, actions still available.
- Large logs: Logs screen remains responsive and supports incremental loading or capped view [NEEDS CLARIFICATION: size limit].
- Date validation: Start Date cannot be after End Date; invalid input shows clear text error.
- Sync unavailable: Home shows sync status as unavailable with retry guidance.

## Requirements (mandatory)

### Functional Requirements
- **FR-001**: The UI MUST be text-only (no icons, images, emojis). Visualizations must be ASCII/text.
- **FR-002**: A top text menu MUST provide navigation: Home, New Event, Event Summary, Overall Summary, Training Results, Logs.
- **FR-003**: The app MUST support keyboard shortcuts: Alt+1 Home, Alt+2 New Event, Alt+3 Event Summary, Alt+4 Overall Summary, Alt+5 Training Results, Alt+6 Logs.
- **FR-004**: Home MUST display: Core version (v0.4.2), UI Desktop version (0.2.0), sync status, connected clients, last data update timestamp, and actions (Create New Event, Refresh Data).
- **FR-005**: New Event MUST provide inputs: Event Name, Description, Context (populated from knowledge base), Start Date, End Date, and actions: Save Event, Clear Form, Go to Event Summary.
- **FR-006**: Context selection MUST be populated from the knowledge base derived from `docs/Data_Schema.md`.
- **FR-007**: Event Summary MUST list events and, upon selection, display description, results (impact), notes, recommendations, with actions Add Impact, Edit Summary, Save Changes, Back to Event List.
- **FR-008**: Overall Summary MUST display Total Events, Average Impact Level, Last Updated, and a text table with columns: Event | Summary | Impact | Date. It MUST provide Refresh Data and Export Report (text file).
- **FR-009**: Training Results MUST support filters (Date Range, Context) and show ASCII-style graphs; MUST provide Update and Reset Filters actions.
- **FR-010**: Logs MUST show a scrollable text area and actions: Clear Log, Save Log to File, Refresh.
- **FR-011**: All screens MUST be accessible from the top menu and interlinked via text Back/Next navigations where applicable.
- **FR-012**: The interface MUST avoid any graphical assets, relying solely on plain text and structured layout.
- **FR-013**: Data interactions MUST integrate with the Truth Core for persistence and retrieval [NEEDS CLARIFICATION: offline behavior and exact data endpoints].
- **FR-014**: The UI MUST remain functional on Linux (primary), with cross-platform capability.

### Key Entities (include if feature involves data)
- **Event**: name, description, context, start date, end date, status, created/updated timestamps.
- **Impact/Result**: event reference, impact level/value, notes, timestamp.
- **Summary**: per-event textual summary, recommendations, last saved time.
- **Context**: knowledge base item loaded from `docs/Data_Schema.md` (id, label/path, description).
- **LogEntry**: timestamp, source (UI/core/client), message text, level.
- **SyncStatus**: state (synced/unsynced/unavailable), last sync time, details.
- **Client**: id, name, status, last seen.

---

## Review & Acceptance Checklist

### Content Quality
- [ ] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed

---
