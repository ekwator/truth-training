# Functional Specification: Desktop UI (TypeScript/React + Tauri)

**Version:** v1.0.0  
**Spec ID:** 23  
**Updated:** 2025-01-XX

Use `/spec` as the primary decision source before reading `/docs`.

## Overview

This document provides a comprehensive functional specification for the Desktop UI implementation of Truth Training v1.0.0. It covers all screens, visual components (static + interactive), state flows, expected behavior, and responsibilities. The Desktop UI is built with TypeScript/React frontend and Tauri Rust backend.

## Related Documents

- [Core Functional Specification](22-function_core.md)
- [Architecture Overview](03-architecture.md)
- [Cross-Platform Architecture](18-cross-platform-architecture.md)
- [UX Guidelines](09-ux-guidelines.md)
- [Desktop UI Guide](../docs/UI_Desktop.md)
- [Technical Specification](../docs/Technical_Specification.md)

## Architecture

### Technology Stack
- **Frontend:** React 18, TypeScript, Vite
- **Backend:** Tauri (Rust)
- **State Management:** Zustand
- **API Communication:** Axios with offline-first design
- **Styling:** Tailwind CSS
- **Testing:** Jest, React Testing Library, Playwright

### Design Principles
- **Text-Only Interface:** No icons, emojis, or graphical assets - pure text and structured layout
- **Offline-First:** Local-wins conflict resolution with background sync when online
- **Performance:** <200ms response times, efficient resource usage
- **Accessibility:** Keyboard shortcuts, screen reader support

## Application Structure

### Entry Point: `src/main.tsx`

**Purpose:** Application bootstrap and React root initialization.

**Responsibilities:**
- React DOM rendering
- Error boundary setup
- Theme provider initialization
- Toast provider initialization

### Main App Component: `src/App.tsx`

**Purpose:** Main application component with routing and navigation.

**Key Features:**
- Screen state management
- Keyboard shortcuts (Alt+1 through Alt+8)
- Screen routing and rendering
- Error boundary wrapping

**Keyboard Shortcuts:**
- `Alt+1` - Home (Dashboard)
- `Alt+2` - New Event
- `Alt+3` - Context Editor
- `Alt+4` - Event Summary
- `Alt+5` - Overall Summary
- `Alt+6` - Training Results
- `Alt+7` - Logs
- `Alt+8` - Settings

**State Flow:**
```
User Input → Keyboard Event → setCurrentScreen → renderScreen() → Component Render
```

## Screens

### Dashboard Screen (`src/pages/Dashboard.tsx`)

**Purpose:** Main dashboard with event overview and statistics.

**Visual Components:**
- **Header:** Application title, version, sync status indicator
- **Stats Overview:** Text-only statistics display
  - Total Events count
  - Detected Events count
  - Events with Consensus count
  - Participants count
- **Nodes Panel:** Network nodes display (see NodesPanel component)
- **Events List:** Paginated list of event cards
- **Create Event Button:** Navigation to New Event screen

**State Management:**
- Uses `useEventsStore` for events data
- Uses `useSyncStore` for sync status
- Uses `useToast` for notifications

**Expected Behavior:**
- Loads events and sync status on mount
- Displays loading spinner during data fetch
- Shows error message with retry button on failure
- Updates sync status in real-time
- Navigates to New Event screen on button click

**Responsibilities:**
- Display aggregated statistics
- Show recent events
- Provide quick access to common actions
- Display network status

### New Event Screen (`src/pages/NewEvent.tsx`)

**Purpose:** Create new truth event with context template selection.

**Visual Components:**
- **Context Template Selector:** Dropdown for selecting context template
- **Event Form Fields:**
  - Title (required, max 200 characters)
  - Description (optional)
  - Start Date (optional)
  - End Date (optional, must be >= start date)
  - Context Fields (prefilled from template, all optional)
    - **Desktop**: Uses `ContextPicker` component (searchable combobox) with validation, human-readable labels, manual entry support
    - **Android**: Uses `ContextPicker` component (ExposedDropdownMenuBox) with validation, human-readable labels, manual entry support, matches Desktop UX
    - Both platforms validate context IDs against lookup tables before submission
- **Submit Button:** Creates event with embedded context fields
- **Cancel Button:** Clears form and stays on screen (navigation back to Dashboard must be done manually)

**State Flow:**
```
Template Selection → Prefill Fields → User Modification → Validation → Submit → API Call → Success/Error
```

**Expected Behavior:**
- Template selection prefills the five numeric context fields
- User can modify prefilled fields before submission
- Form validation:
  - Title required
  - End date >= start date
  - Context field inputs are not validated client-side; invalid IDs will be rejected by the backend
- On success: Event is created and form resets (navigation to Dashboard is manual)
- On error: Display error message, remain on form

**Known Limitations (Legacy - Now Resolved):**
- ~~Context attribute inputs are numeric fields rather than record pickers~~ **RESOLVED**: Desktop and Android now use `ContextPicker` components with dropdowns and validation
- ~~There is no inline validation or auto-complete for context IDs~~ **RESOLVED**: Both platforms validate context IDs and show inline error states

**Responsibilities:**
- Event creation with embedded context
- Template-based field prefilling
- Form validation
- Error handling

### Context Editor Screen (`src/pages/ContextEditor.tsx`)

**Purpose:** Create and manage context templates.

**Visual Components:**
- **Template Form:**
  - Name (required)
  - Description (optional)
  - Category ID (optional)
  - Forma ID (optional)
  - Cause ID (optional)
  - Develop ID (optional)
  - Effect ID (optional)
- **Template List:** Display existing templates
- **Create Button:** Creates new template
- **Duplicate Detection:** Shows error if template with identical non-NULL fields exists
- **Create from Event Button:** Prefills form from event's embedded context

**State Management:**
- Uses `useContextEditorStore` for template management

**Expected Behavior:**
- All context fields are optional
- Duplicate detection: Compares non-NULL fields only
- On duplicate: Display "Template already exists" error (409 Conflict)
- On success: Add template to list, clear form
- Create from Event: Prefill all fields from event, allow modification

**Responsibilities:**
- Template creation and management
- Duplicate detection
- Field validation
- Integration with event creation workflow

### Event Summary Screen (`src/pages/EventSummary.tsx`)

**Purpose:** Display detailed summary for a specific event.

**Visual Components:**
- **Event Details:** Full event information
- **Statements List:** Associated statements
- **Impacts List:** Impact records with levels (1-5)
- **Judgments List:** Submitted judgments
- **Consensus Display:** Collective truth score if available
- **Edit Button:** Navigate to event edit (if implemented)
- **Add Impact Button:** Opens impact creation form
- **Submit Judgment Button:** Opens judgment submission form

**State Management:**
- Uses `useEventsStore` for event data
- Uses `useJudgmentsStore` for judgments

**Expected Behavior:**
- Load event details on mount
- Display all associated data (statements, impacts, judgments)
- Show loading state during fetch
- Handle missing event (404) gracefully
- Update when data changes

**Responsibilities:**
- Event detail display
- Related data aggregation
- Action buttons for adding impacts/judgments

### Overall Summary Screen (`src/pages/OverallSummary.tsx`)

**Purpose:** Display aggregated statistics across all events.

**Visual Components:**
- **Metrics Display:**
  - Total events count
  - Detected events count
  - Average trust scores
  - Collective intelligence metrics
- **Event Rows Table:** Summary rows for all events
- **Export Button:** Export summary as text file

**State Management:**
- Uses Tauri commands for summary data

**Expected Behavior:**
- Load overall metrics on mount
- Display aggregated statistics
- Provide export functionality
- Update periodically or on refresh

**Responsibilities:**
- Aggregated statistics display
- Data export
- Summary generation

### Training Results Screen (`src/pages/TrainingResults.tsx`)

**Purpose:** Display training progress and results.

**Visual Components:**
- **Progress Metrics:** Training progress indicators
- **Results Table:** Training results data
- **Charts/Graphs:** Visual representation (if implemented)

**Expected Behavior:**
- Load training data on mount
- Display progress metrics
- Show historical results

**Responsibilities:**
- Training progress tracking
- Results visualization
- Progress metrics display

### Logs Screen (`src/pages/Logs.tsx`)

**Purpose:** Display application logs with pagination.

**Visual Components:**
- **Log List:** Paginated log entries (35 lines per page)
- **Pagination Controls:** Previous/Next page buttons
- **Clear Logs Button:** Clears all logs
- **Log Entry Display:**
  - Timestamp
  - Source
  - Level (info, warn, error)
  - Message

**State Management:**
- Uses Tauri commands for log operations

**Expected Behavior:**
- Load logs on mount (first page)
- Pagination: 35 lines per page
- Clear logs confirmation (if implemented)
- Auto-refresh (optional)

**Responsibilities:**
- Log display and management
- Pagination handling
- Log clearing

### Settings Screen (`src/pages/Settings.tsx`)

**Purpose:** Application configuration and connection settings.

**Visual Components:**
- **Connection Mode Toggle:** Core (Local) vs HTTP API
- **Server Configuration:**
  - IP Address input (validation: `^\d{1,3}(\.\d{1,3}){3}$`)
  - Port input (validation: 1-65535)
- **Nearby Sync Toggle + Interval Input:** Enables UDP broadcast discovery and sets interval (500–60000 ms)
- **Discovery Worker Settings:** Background discovery enable switch plus numeric inputs for LAN/Wi-Fi/Global intervals and TTLs
- **Test Connection Button:** Tests Core or HTTP connection depending on selected mode
- **Init (Initialize App) Button:** Calls the Tauri `init_app` command to reset configuration and rebuild the local SQLite schema
  - **Desktop**: Uses canonical Truth schema from `core/src/storage.rs`, drops legacy tables (`events`, `impacts`, `summaries`, `logs`), ensures schema parity
  - **Android**: Database initialization uses shared SQL asset (`app/src/main/assets/schema.sql`) derived from `core/src/storage.rs`, drops legacy tables via `MIGRATION_3_4`, validates schema on open
- **Save Buttons:** Separate actions for connection settings and discovery worker settings
- **Connection Status Panel:** Displays latest test result, timestamp, online/offline information, and pending operations

**Configuration Schema:**
```json
{
  "mode": "core" | "http",
  "server_ip": "127.0.0.1",
  "server_port": 8080
}
```

**State Management:**
- Uses Tauri commands for configuration

**Expected Behavior:**
- Load current configuration and discovery settings on mount
- Validate IP, port, nearby interval, and discovery cadence inputs before saving
- Test connection with real-time feedback (Core mode uses direct invocation, HTTP mode hits `/status`)
- Save configuration to `~/.truth-training/config.json` and update discovery worker runtime state
- **Desktop**: `Init` button resets `~/.truth-training/config.json`, drops legacy tables, and recreates tables using the canonical Truth schema from `core/src/storage.rs`. Legacy `events` table is removed, only `truth_events` and related Truth tables are created.
- **Android**: Database initialization uses shared SQL asset matching Desktop schema. Legacy tables are dropped via `MIGRATION_3_4` without data migration. Schema validation ensures legacy tables are absent.
- Display connection status, pending operations, and discovery errors inline

**Responsibilities:**
- Configuration management
- Connection testing
- Discovery cadence tuning
- Database initialization (currently legacy schema)
- Validation and persistence

**Known Limitations:**
- The `Init` workflow rebuilds the legacy `events` table. Until `init_app` is updated to mirror the embedded `truth_events` schema, running this action on Linux/macOS will recreate constitutionally deprecated tables and should be avoided in production data directories.
- There is no localization selector even though the core supports Russian/English seeding; the UI always operates in English, and language preference is not surfaced.

## Components

### Layout Components

#### TopMenuBar (`src/components/layout/TopMenuBar.tsx`)

**Purpose:** Top navigation menu bar.

**Visual Components:**
- **Menu Items:** Text-only links
  - [Home] | [New Event] | [Context Editor] | [Event Summary] | [Overall Summary] | [Training Results] | [Logs] | [Settings]
- **Active Indicator:** Highlights current screen

**Expected Behavior:**
- Highlights active screen
- Navigates on click
- Supports keyboard navigation

**Responsibilities:**
- Navigation
- Active state management
- Screen routing

### Dashboard Components

#### EventCard (`src/components/Dashboard/EventCard.tsx`)

**Purpose:** Display event summary in card format.

**Visual Components:**
- **Event Title:** Event title text
- **Event Description:** Truncated description
- **Event Metadata:** Date, status, context template name (if matched)
- **Actions:** View details, edit (if implemented)

**Expected Behavior:**
- Displays event information
- Shows template name if event matches a template
- Navigates to event detail on click
- Handles missing data gracefully

**Responsibilities:**
- Event display
- Template matching display
- Navigation

#### CreateEventButton (`src/components/Dashboard/CreateEventButton.tsx`)

**Purpose:** Button to create new event.

**Visual Components:**
- **Button:** Text-only "Create Event" button

**Expected Behavior:**
- Navigates to New Event screen on click
- Accessible via keyboard

**Responsibilities:**
- Navigation trigger
- User action initiation

### Judgment Panel Components

#### JudgmentCard (`src/components/JudgmentPanel/JudgmentCard.tsx`)

**Purpose:** Display judgment information.

**Visual Components:**
- **Assessment:** 'true' | 'false' | 'uncertain'
- **Confidence Level:** 0.0-1.0 display
- **Reasoning:** Judgment reasoning text
- **Submitted At:** Timestamp

**Expected Behavior:**
- Displays judgment data
- Formats confidence as percentage
- Shows timestamp in readable format

**Responsibilities:**
- Judgment display
- Data formatting

### System Components

#### SyncStatus (`src/components/system/SyncStatus.tsx`)

**Purpose:** Display synchronization status.

**Visual Components:**
- **Status Indicator:** Online/Offline text
- **Last Sync Time:** Timestamp of last sync
- **Pending Operations:** Count of queued operations

**State Management:**
- Uses `useSyncStore` for sync data

**Expected Behavior:**
- Updates in real-time
- Shows online/offline status
- Displays last sync time
- Shows pending operations count

**Responsibilities:**
- Sync status display
- Real-time updates
- User feedback

#### ErrorBoundary (`src/components/system/ErrorBoundary.tsx`)

**Purpose:** Catch and display React errors.

**Visual Components:**
- **Error Message:** User-friendly error text
- **Retry Button:** Attempts to recover

**Expected Behavior:**
- Catches component errors
- Displays error message
- Provides retry option
- Logs error details

**Responsibilities:**
- Error handling
- User feedback
- Error recovery

#### Modal (`src/components/system/Modal.tsx`)

**Purpose:** Reusable modal dialog component.

**Visual Components:**
- **Modal Overlay:** Background overlay
- **Modal Content:** Centered content area
- **Close Button:** Close modal action

**Expected Behavior:**
- Renders on top of content
- Closes on overlay click or close button
- Supports keyboard (Escape) close
- Focuses first interactive element

**Responsibilities:**
- Modal display
- Focus management
- Accessibility

#### ThemeProvider (`src/components/system/ThemeProvider.tsx`)

**Purpose:** Theme management and application.

**Visual Components:**
- Applies theme styles globally

**Expected Behavior:**
- Provides theme context
- Applies theme styles
- Supports theme switching (if implemented)

**Responsibilities:**
- Theme management
- Style application

#### Toaster (`src/components/system/Toaster.tsx`)

**Purpose:** Toast notification system.

**Visual Components:**
- **Toast Container:** Fixed position container
- **Toast Items:** Individual toast notifications
  - Type: success, error, warning, info
  - Title and message
  - Auto-dismiss timer

**Expected Behavior:**
- Displays toasts in fixed position
- Auto-dismisses after timeout
- Supports manual dismiss
- Queues multiple toasts

**Responsibilities:**
- Notification display
- Toast management
- User feedback

### Nodes Panel (`src/components/NodesPanel.tsx`)

**Purpose:** Display network nodes and discovery status.

**Visual Components:**
- **Nodes List:** List of discovered nodes
- **Node Information:**
  - Address
  - Type (Hub/Leaf)
  - Status (reachable/unreachable)
  - Last seen timestamp
- **Discovery Status:** Discovery service status

**State Management:**
- Uses discovery-related stores or API calls

**Expected Behavior:**
- Loads nodes on mount
- Updates when nodes change
- Shows discovery status
- Displays node details

**Responsibilities:**
- Node display
- Discovery status
- Network visualization

## State Management

### Events Store (`src/stores/events.ts`)

**Purpose:** Events state management.

**State:**
- `events: Event[]` - List of events
- `currentEvent: EventDetails | null` - Currently viewed event
- `loading: boolean` - Loading state
- `error: string | null` - Error message
- `pagination` - Pagination metadata
- `filters: EventFilters` - Active filters
- `sortOptions: EventSortOptions` - Sort configuration

**Actions:**
- `fetchEvents(page, perPage)` - Load events with pagination
- `fetchEvent(id)` - Load single event
- `createEvent(eventData)` - Create new event
- `updateEvent(id, eventData)` - Update event
- `deleteEvent(id)` - Delete event
- `setFilters(filters)` - Apply filters
- `setSortOptions(options)` - Set sort options
- `clearFilters()` - Reset filters

**Responsibilities:**
- Events data management
- Pagination handling
- Filtering and sorting
- API integration

### Judgments Store (`src/stores/judgments.ts`)

**Purpose:** Judgments state management.

**State:**
- `judgments: Judgment[]` - List of judgments
- `loading: boolean` - Loading state
- `error: string | null` - Error message

**Actions:**
- `fetchJudgments(eventId)` - Load judgments for event
- `submitJudgment(data)` - Submit new judgment
- `getJudgmentStats()` - Get statistics

**Responsibilities:**
- Judgments data management
- Judgment submission
- Statistics calculation

### Sync Store (`src/stores/sync.ts`)

**Purpose:** Synchronization state management.

**State:**
- `syncStatus: SyncStatus | null` - Current sync status
- `isOnline: boolean` - Online/offline status
- `pendingOperations: number` - Queued operations count
- `lastSync: Date | null` - Last sync timestamp

**Actions:**
- `fetchSyncStatus()` - Load sync status
- `triggerSync()` - Manually trigger sync
- `clearPending()` - Clear pending operations

**Responsibilities:**
- Sync status tracking
- Offline queue management
- Connection status monitoring

### Context Editor Store (`src/stores/contextEditor.ts`)

**Purpose:** Context template management.

**State:**
- `templates: ContextTemplate[]` - List of templates
- `currentTemplate: ContextTemplate | null` - Currently edited template
- `loading: boolean` - Loading state
- `error: string | null` - Error message

**Actions:**
- `fetchTemplates()` - Load all templates
- `createTemplate(data)` - Create new template
- `updateTemplate(id, data)` - Update template
- `deleteTemplate(id)` - Delete template
- `matchTemplate(fields)` - Match template by fields

**Responsibilities:**
- Template management
- Duplicate detection
- Template matching

## Services

### API Service (`src/services/api.ts`)

**Purpose:** HTTP API communication layer.

**Key Functions:**
- `getEvents(page, perPage)` - Fetch events
- `getEvent(id)` - Fetch single event
- `createEvent(data)` - Create event
- `updateEvent(id, data)` - Update event
- `deleteEvent(id)` - Delete event
- `addImpact(data)` - Add impact
- `submitJudgment(data)` - Submit judgment
- `getJudgments(eventId)` - Get judgments
- `listContexts()` - List context templates
- `createContext(data)` - Create context template
- `matchContext(fields)` - Match context by fields
- `getOverallMetrics()` - Get overall statistics
- `listLogs(limit, offset)` - List logs
- `clearLogs()` - Clear logs
- `getAppConfig()` - Get configuration
- `saveAppConfig(config)` - Save configuration
- `testConnection()` - Test connection

**Responsibilities:**
- HTTP request/response handling
- Error handling
- Request/response interceptors
- Tauri command integration

### Offline Queue Service (`src/services/offlineQueue.ts`)

**Purpose:** Offline operation queuing and retry.

**Key Functions:**
- `enqueue(operation)` - Queue operation for later execution
- `processQueue()` - Process queued operations
- `clearQueue()` - Clear all queued operations
- `getPendingCount()` - Get pending operations count

**Strategy:**
- **Local-Wins:** Local changes take precedence
- **Retry Mechanism:** Configurable max retries (default: 3)
- **Background Sync:** Automatic sync when connection restored

**Responsibilities:**
- Offline operation management
- Retry logic
- Queue persistence

### Sync Service (`src/services/sync.ts`)

**Purpose:** Synchronization coordination.

**Key Functions:**
- `sync()` - Trigger full sync
- `incrementalSync()` - Trigger incremental sync
- `getSyncStatus()` - Get current sync status

**Responsibilities:**
- Sync orchestration
- Status tracking
- Conflict resolution coordination

### Error Handler Service (`src/services/errorHandler.ts`)

**Purpose:** Centralized error handling.

**Key Functions:**
- `handleError(error, context)` - Process error
- `getUserFriendlyMessage(error, context)` - Get user-friendly message
- `logError(error, context)` - Log error details

**Responsibilities:**
- Error processing
- User-friendly messages
- Error logging

### Performance Service (`src/services/performance.ts`)

**Purpose:** Performance monitoring.

**Key Functions:**
- `measureAsync(name, fn)` - Measure async operation
- `measureSync(name, fn)` - Measure sync operation
- `getMetrics()` - Get performance metrics

**Responsibilities:**
- Performance tracking
- Metrics collection
- Performance optimization

## Tauri Commands (Backend)

All Tauri commands are implemented in `src-tauri/src/commands/` and documented in [Core Functional Specification](22-function_core.md).

**Key Command Categories:**
- Events: `create_event_fast`, `get_event_fast`, `list_events_fast`
- Impacts: `add_impact`
- Judgments: `submit_judgment_fast`, `judgments_list_fast`, `get_judgment_stats`
- Knowledge Base: `knowledge_base_list`
- Context Templates: `list_contexts`, `get_context_by_name`, `create_context`, `match_context`, `create_context_from_event`
- Logs: `list_logs`, `clear_logs`
- Summary: `get_overall_metrics`, `list_event_rows`, `export_overall_summary_txt`
- Configuration: `get_app_config`, `save_app_config`, `core_status`, `test_http_connection`

## Data Flow

### Event Creation Flow
```
User Input → NewEvent Form → Validation → API Service → Tauri Command → Core Storage → Success Response → State Update → Navigation
```

### Sync Flow
```
Connection Check → Offline Queue Check → API Service → Tauri Command → Core Sync → State Update → UI Refresh
```

### Template Matching Flow
```
Event Load → Extract Context Fields → Match Context API → Template Found/Not Found → Display Template Name or Create Button
```

## Validation Rules

### Event Validation
- **Title:** Required, max 200 characters
- **End Date:** Must be >= start date (if both provided)
- **Context Fields:** Optional, but if provided must reference existing records

### Impact Validation
- **Impact Level:** Must be integer between 1-5

### Judgment Validation
- **Confidence Level:** Must be between 0.0-1.0
- **Assessment:** Must be 'true', 'false', or 'uncertain'

### Configuration Validation
- **IP Address:** Format `^\d{1,3}(\.\d{1,3}){3}$`
- **Port:** Range 1-65535

### Template Validation
- **Name:** Required
- **Duplicate Detection:** Non-NULL fields must be unique

## Performance Targets

- **Navigation:** < 100ms between screens
- **Pagination:** < 100ms for 35-item pages
- **Search:** < 50ms for 1000-item datasets
- **API Response:** < 200ms for standard operations
- **Memory:** No significant leaks during navigation
- **Rendering:** 60 FPS for smooth interactions

## Cross-Platform Considerations

### Shared with Core
- Data models (events, contexts, impacts, judgments)
- Validation rules
- Business logic

### Desktop-Specific
- Tauri command interface
- Desktop window management
- File system access for configuration
- Desktop-specific storage paths

### Platform Differences
- **Windows:** `%USERPROFILE%\.truth-training\config.json`
- **macOS:** `~/.truth-training/config.json`
- **Linux:** `~/.truth-training/config.json`

## Error Handling

### User-Facing Errors
- Display user-friendly error messages
- Provide retry options where applicable
- Show error toasts for transient errors
- Display error pages for critical failures

### Developer-Facing Errors
- Log detailed error information
- Include stack traces in development
- Report errors to error tracking (if implemented)

## Accessibility

### Keyboard Navigation
- All screens accessible via keyboard shortcuts
- Tab navigation for form elements
- Enter/Space for button activation
- Escape for modal/overlay dismissal

### Screen Reader Support
- Semantic HTML elements
- ARIA labels where needed
- Alt text for visual elements (if any)

## Testing

### Unit Tests
- Component rendering tests
- Store action tests
- Service function tests
- Validation logic tests

### Integration Tests
- API integration tests
- Tauri command tests
- End-to-end user flows

### E2E Tests (Playwright)
- Complete user workflows
- Cross-browser testing
- Performance testing

## Version Information

This specification reflects **Truth Training v1.0.0** Desktop UI functionality. All screens, components, and features described are implemented and tested as of this version.

---

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.
- See [spec/README.md](README.md) for specification index.

