# Functional Specification: Android Mobile Client (Kotlin)

**Version:** v1.0.0  
**Spec ID:** 24  
**Updated:** 2025-01-XX

Use `/spec` as the primary decision source before reading `/docs`.

## Overview

This document provides a comprehensive functional specification for the Android mobile client of Truth Training v1.0.0. It covers all screens, UI components, behavior, interactions, and module/function responsibilities. The Android client is built with Kotlin, Jetpack Compose, Room database, and WorkManager.

## Related Documents

- [Core Functional Specification](22-function_core.md)
- [Desktop UI Functional Specification](23-function_desktop.md)
- [Architecture Overview](03-architecture.md)
- [Cross-Platform Architecture](18-cross-platform-architecture.md)
- [Android Integration Guide](../docs/integration/android/README_INTEGRATION.md)
- [Android Migration Guide](../docs/ANDROID_MIGRATION.md)

## Architecture

### Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Database:** Room (SQLite)
- **Networking:** Retrofit, OkHttp
- **Background Work:** WorkManager
- **Dependency Injection:** Manual DI (can be upgraded to Hilt/Koin)
- **State Management:** ViewModel, StateFlow
- **Navigation:** Jetpack Navigation Compose

### Design Principles
- **Offline-First:** Local database with background sync
- **Material Design 3:** Modern Material Design components
- **Performance:** Efficient database queries, lazy loading
- **Battery Efficiency:** Optimized background sync intervals (15 minutes)

### Localization Status
- **Current Status:** **English-only (EN)**
- **Language Switching:** Not implemented
- **Translation Files:** Only `app/src/main/res/values/strings.xml` exists (no `values-ru/` directory)
- **Parity Note:** Desktop UI supports RU/EN language switching with automatic knowledge base reseeding; Android localization parity is planned for a future release
- **Recommendation:** Implement localization to match Desktop UI functionality:
  - Add `values-ru/strings.xml` with Russian translations
  - Add language selector in Settings screen
  - Integrate locale-aware knowledge base seeding (Core supports `seed_knowledge_base` with locale parameter)
  - Persist locale preference in SharedPreferences
  - Trigger knowledge base reseed when locale changes

## Application Structure

### Application Class: `TruthTrainingApplication`

**Purpose:** Application-level initialization and dependency management.

**Key Responsibilities:**
- Database initialization (`TruthDatabase`)
- Network module setup
- Truth Core initialization (`TruthCore.initNode()`)
- Dependency provision

**Initialization Flow:**
```
Application Start → Database Init → Network Module → Truth Core Init → Ready
```

### Main Activity: `MainActivity`

**Purpose:** Main application entry point and navigation setup.

**Key Features:**
- Jetpack Compose UI setup
- Navigation controller initialization
- Repository initialization
- Theme application

**Responsibilities:**
- Activity lifecycle management
- Navigation graph setup
- Dependency injection
- UI initialization

## Screens

### Dashboard Screen (`ui/compose/DashboardScreen.kt`)

**Purpose:** Main dashboard with sync status and quick access.

**Visual Components:**
- **Top App Bar:** "Dashboard" title
- **Sync Status Card:**
  - Online/Offline indicator
  - Last sync time
  - Sync button
  - Pending operations count
- **Quick Stats Section:**
  - Events count card
  - Other statistics (if implemented)
- **Quick Actions Section:**
  - "View Events" button
  - "Manage Context Templates" button
  - Other action buttons

**State Management:**
- Uses `SyncStatus` flow
- Observes event count
- Handles sync trigger

**Expected Behavior:**
- Displays current sync status
- Shows event count
- Provides quick navigation to main features
- Triggers manual sync on button press
- Updates sync status in real-time

**Responsibilities:**
- Dashboard overview
- Sync status display
- Quick navigation
- Statistics display

### Event List Screen (`ui/compose/events/EventListScreen.kt`)

**Purpose:** Display paginated list of events.

**Visual Components:**
- **Top App Bar:** "Events" title, "Add" FAB
- **Event List:** LazyColumn with event items
- **Event Item:**
  - Event title/description
  - Date information
  - Status indicator
  - Context template name (if matched)
- **Pagination:** Load more on scroll
- **Empty State:** Message when no events
- **Loading State:** Progress indicator

**State Management:**
- Uses `EventRepository` for data
- Observes `getAllEventsFlow()`
- Handles pagination

**Expected Behavior:**
- Loads events on mount
- Displays events in list
- Shows loading during fetch
- Handles empty state
- Supports pull-to-refresh (if implemented)
- Navigates to event detail on item click
- Navigates to create event on FAB click

**Responsibilities:**
- Event list display
- Pagination handling
- Navigation to detail/create

### Event Detail Screen (`ui/compose/events/EventDetailScreen.kt`)

**Purpose:** Display detailed event information.

**Visual Components:**
- **Top App Bar:** Event title, back button, edit button
- **Event Information:**
  - Full description
  - Dates (start/end)
  - Context fields (category, forma, cause, develop, effect)
  - Status (detected, corrected)
  - Collective score (if available)
- **Related Data Sections:**
  - Statements list
  - Impacts list
  - Judgments list
- **Action Buttons:**
  - Edit event
  - Add impact
  - Submit judgment
  - Delete event (if implemented)

**State Management:**
- Uses `EventRepository.getEventByIdFlow()`
- Observes related data (statements, impacts, judgments)

**Expected Behavior:**
- Loads event details on mount
- Displays all event information
- Shows related data
- Handles missing event (404)
- Updates when data changes
- Navigates to edit screen on edit button

**Responsibilities:**
- Event detail display
- Related data aggregation
- Action buttons

### Event Create Screen (`ui/compose/events/EventCreateScreen.kt`)

**Purpose:** Create new truth event.

**Visual Components:**
- **Top App Bar:** "New Event" title, back button
- **Event Form:**
  - Description input (required)
  - Context template selector (dropdown)
  - Context fields (prefilled from template, all optional):
    - Category selector
    - Forma selector
    - Cause selector
    - Develop selector
    - Effect selector
  - Start date picker
  - End date picker
  - Vector toggle (outgoing/incoming)
- **Submit Button:** Creates event
- **Cancel Button:** Returns to list

**State Management:**
- Uses form state (remember)
- Uses `EventRepository.createEvent()`

**Expected Behavior:**
- Template selection prefills context fields
- User can modify prefilled fields
- Form validation:
  - Description required
  - End date >= start date
  - Context field FKs must reference existing records
- On success: Navigate to event detail, show success message
- On error: Display error message, remain on form

**Responsibilities:**
- Event creation
- Template-based field prefilling
- Form validation
- Error handling

### Event Edit Screen (`ui/compose/events/EventEditScreen.kt`)

**Purpose:** Edit existing event.

**Visual Components:**
- **Top App Bar:** "Edit Event" title, back button, save button
- **Event Form:** Same as create screen, pre-filled with existing data
- **Save Button:** Updates event
- **Cancel Button:** Discards changes

**State Management:**
- Uses form state with initial values
- Uses `EventRepository.updateEvent()`

**Expected Behavior:**
- Loads event data on mount
- Pre-fills form with existing data
- Validates on save
- On success: Navigate to detail, show success message
- On error: Display error message

**Responsibilities:**
- Event editing
- Form pre-filling
- Validation
- Update handling

### Context Template List Screen (`ui/compose/contexts/ContextTemplateListScreen.kt`)

**Purpose:** Display and manage context templates.

**Visual Components:**
- **Top App Bar:** "Context Templates" title, "Add" FAB
- **Template List:** LazyColumn with template items
- **Template Item:**
  - Template name
  - Context fields summary
  - Edit/Delete actions
- **Empty State:** Message when no templates
- **Loading State:** Progress indicator

**State Management:**
- Uses `ContextTemplateRepository`
- Observes template list flow

**Expected Behavior:**
- Loads templates on mount
- Displays templates in list
- Navigates to editor on item click
- Navigates to create on FAB click
- Supports delete (with confirmation)

**Responsibilities:**
- Template list display
- Template management
- Navigation

### Context Template Editor Screen (`ui/compose/contexts/ContextTemplateEditorScreen.kt`)

**Purpose:** Create or edit context template.

**Visual Components:**
- **Top App Bar:** "Edit Template" or "New Template" title, back button, save button
- **Template Form:**
  - Name input (required)
  - Description input (optional)
  - Context field selectors (all optional):
    - Category selector
    - Forma selector
    - Cause selector
    - Develop selector
    - Effect selector
- **Save Button:** Creates or updates template
- **Duplicate Detection:** Shows error if duplicate exists

**State Management:**
- Uses form state
- Uses `ContextTemplateRepository`

**Expected Behavior:**
- Pre-fills form if editing
- Validates name required
- Detects duplicates (non-NULL fields)
- On duplicate: Show error message
- On success: Navigate to list, show success message

**Responsibilities:**
- Template creation/editing
- Duplicate detection
- Validation

### Context Template Selection Screen (`ui/compose/contexts/ContextTemplateSelectionScreen.kt`)

**Purpose:** Select context template for event creation.

**Visual Components:**
- **Top App Bar:** "Select Template" title, back button
- **Template List:** LazyColumn with selectable templates
- **Template Item:** Template name, fields summary, select button
- **Empty State:** Message when no templates

**State Management:**
- Uses `ContextTemplateRepository`
- Returns selected template to caller

**Expected Behavior:**
- Loads templates on mount
- Displays selectable templates
- Returns selected template on selection
- Navigates back on cancel

**Responsibilities:**
- Template selection
- Template display
- Selection callback

### Judgment List Screen (`ui/compose/judgments/JudgmentListScreen.kt`)

**Purpose:** Display judgments for an event.

**Visual Components:**
- **Top App Bar:** "Judgments" title, back button
- **Judgment List:** LazyColumn with judgment items
- **Judgment Item:**
  - Assessment ('true' | 'false' | 'uncertain')
  - Confidence level (0.0-1.0)
  - Reasoning text
  - Submitted timestamp
- **Empty State:** Message when no judgments
- **FAB:** "Submit Judgment" button

**State Management:**
- Uses `JudgmentRepository`
- Observes judgments flow for event

**Expected Behavior:**
- Loads judgments on mount
- Displays judgments in list
- Navigates to submission screen on FAB click
- Updates when new judgments added

**Responsibilities:**
- Judgment list display
- Navigation to submission

### Judgment Submission Screen (`ui/compose/judgments/JudgmentSubmissionScreen.kt`)

**Purpose:** Submit judgment for an event.

**Visual Components:**
- **Top App Bar:** "Submit Judgment" title, back button
- **Judgment Form:**
  - Assessment selector ('true' | 'false' | 'uncertain')
  - Confidence level slider (0.0-1.0)
  - Reasoning text input
- **Submit Button:** Submits judgment
- **Cancel Button:** Returns to list

**State Management:**
- Uses form state
- Uses `JudgmentRepository.submitJudgment()`

**Expected Behavior:**
- Validates form:
  - Assessment required
  - Confidence between 0.0-1.0
  - Reasoning optional
- On success: Navigate to list, show success message
- On error: Display error message

**Responsibilities:**
- Judgment submission
- Form validation
- Error handling

### Nodes Screen (`ui/compose/nodes/NodesScreen.kt`)

**Purpose:** Display network nodes and discovery status.

**Visual Components:**
- **Top App Bar:** "Nodes" title
- **Discovery Status:** Online/offline, discovery service status
- **Nodes List:** LazyColumn with node items
- **Node Item:**
  - Node address
  - Node type (Hub/Leaf)
  - Status (reachable/unreachable)
  - Last seen timestamp
  - Source (LAN/Global/Manual)
- **Empty State:** Message when no nodes
- **Refresh Button:** Manual refresh

**State Management:**
- Uses `DiscoveryRepository`
- Observes nodes flow

**Expected Behavior:**
- Loads nodes on mount
- Displays nodes in list
- Shows discovery status
- Updates when nodes change
- Supports manual refresh

**Responsibilities:**
- Node display
- Discovery status
- Network visualization

## Components

### Reusable UI Components

#### StatCard
**Purpose:** Display statistic in card format.

**Visual Components:**
- Card container
- Icon
- Title text
- Value text
- Click handler (optional)

#### SyncStatusCard
**Purpose:** Display sync status information.

**Visual Components:**
- Online/Offline indicator
- Last sync time
- Sync button
- Pending operations count

#### QuickActionButton
**Purpose:** Quick action button with icon.

**Visual Components:**
- Button container
- Icon
- Text label
- Click handler

## Data Layer

### Database (Room)

#### Entities

##### EventEntity
**Fields:**
- `id: Long` (primary key)
- `description: String`
- `categoryId: Long?`
- `formaId: Long?`
- `causeId: Long?`
- `developId: Long?`
- `effectId: Long?`
- `vector: Boolean`
- `detected: Boolean?`
- `corrected: Boolean`
- `timestampStart: Long`
- `timestampEnd: Long?`
- `code: Int`
- `collectiveScore: Float?`
- `createdAt: Long`
- `updatedAt: Long`

##### ContextTemplateEntity
**Fields:**
- `id: Long` (primary key)
- `name: String`
- `description: String?`
- `categoryId: Long?`
- `formaId: Long?`
- `causeId: Long?`
- `developId: Long?`
- `effectId: Long?`
- `createdAt: Long`
- `updatedAt: Long`

##### ImpactEntity
**Fields:**
- `id: Long` (primary key)
- `eventId: Long` (foreign key)
- `impactLevel: Int` (1-5)
- `notes: String?`
- `createdAt: Long`

##### JudgmentEntity
**Fields:**
- `id: Long` (primary key)
- `eventId: Long` (foreign key)
- `assessment: String` ('true' | 'false' | 'uncertain')
- `confidenceLevel: Float` (0.0-1.0)
- `reasoning: String?`
- `submittedAt: Long`

##### NodeEntity
**Fields:**
- `id: Long` (primary key)
- `address: String`
- `nodeType: String` ('Hub' | 'Leaf')
- `reachable: Boolean`
- `lastSeen: Long`
- `ttl: Long`
- `source: String` ('LAN' | 'Global' | 'Manual')
- `nodeId: String?`
- `createdAt: Long`
- `updatedAt: Long`

#### DAOs (Data Access Objects)

##### EventDao
**Methods:**
- `getAllEventsFlow(): Flow<List<EventEntity>>`
- `getEventById(id: Long): EventEntity?`
- `getEventByIdFlow(id: Long): Flow<EventEntity?>`
- `listEvents(limit: Int, offset: Int): List<EventEntity>`
- `insertEvent(event: EventEntity): Long`
- `updateEvent(event: EventEntity)`
- `deleteEvent(event: EventEntity)`

##### ContextTemplateDao
**Methods:**
- `getAllTemplatesFlow(): Flow<List<ContextTemplateEntity>>`
- `getTemplateById(id: Long): ContextTemplateEntity?`
- `getTemplateByName(name: String): ContextTemplateEntity?`
- `insertTemplate(template: ContextTemplateEntity): Long`
- `updateTemplate(template: ContextTemplateEntity)`
- `deleteTemplate(template: ContextTemplateEntity)`
- `matchTemplate(categoryId, formaId, causeId, developId, effectId): ContextTemplateEntity?`

##### ImpactDao
**Methods:**
- `getImpactsForEvent(eventId: Long): Flow<List<ImpactEntity>>`
- `insertImpact(impact: ImpactEntity): Long`
- `updateImpact(impact: ImpactEntity)`
- `deleteImpact(impact: ImpactEntity)`

##### JudgmentDao
**Methods:**
- `getJudgmentsForEvent(eventId: Long): Flow<List<JudgmentEntity>>`
- `insertJudgment(judgment: JudgmentEntity): Long`
- `updateJudgment(judgment: JudgmentEntity)`
- `deleteJudgment(judgment: JudgmentEntity)`

##### NodeDao
**Methods:**
- `getAllNodesFlow(): Flow<List<NodeEntity>>`
- `getNodeById(id: Long): NodeEntity?`
- `getNodeByAddress(address: String): NodeEntity?`
- `insertNode(node: NodeEntity): Long`
- `updateNode(node: NodeEntity)`
- `deleteNode(node: NodeEntity)`

### Repositories

#### EventRepository
**Purpose:** Event data management with offline-first strategy.

**Key Methods:**
- `getAllEventsFlow(): Flow<List<EventEntity>>` - Observe all events
- `getEventById(id: Long): EventEntity?` - Get single event
- `getEventByIdFlow(id: Long): Flow<EventEntity?>` - Observe single event
- `listEvents(limit, offset): List<EventEntity>` - Paginated list
- `createEvent(request): Result<EventEntity>` - Create event
- `updateEvent(id, request): Result<EventEntity>` - Update event
- `deleteEvent(id): Result<Unit>` - Delete event
- `syncFromServer(): Result<Int>` - Sync events from server
- `syncToServer(): Result<Int>` - Push local events to server

**Responsibilities:**
- Local database operations
- Network synchronization
- Conflict resolution
- Offline queue management

#### ContextTemplateRepository
**Purpose:** Context template management.

**Key Methods:**
- `getAllTemplatesFlow(): Flow<List<ContextTemplateEntity>>`
- `getTemplateById(id: Long): ContextTemplateEntity?`
- `getTemplateByName(name: String): ContextTemplateEntity?`
- `createTemplate(data): Result<ContextTemplateEntity>`
- `updateTemplate(id, data): Result<ContextTemplateEntity>`
- `deleteTemplate(id): Result<Unit>`
- `matchTemplate(fields): ContextTemplateEntity?`

**Responsibilities:**
- Template CRUD operations
- Duplicate detection
- Template matching

#### JudgmentRepository
**Purpose:** Judgment management.

**Key Methods:**
- `getJudgmentsForEvent(eventId: Long): Flow<List<JudgmentEntity>>`
- `submitJudgment(data): Result<JudgmentEntity>`
- `getJudgmentStats(): Result<JudgmentStats>`

**Responsibilities:**
- Judgment submission
- Judgment retrieval
- Statistics calculation

#### DiscoveryRepository
**Purpose:** Node discovery management.

**Key Methods:**
- `getAllNodesFlow(): Flow<List<NodeEntity>>`
- `startDiscovery()` - Start discovery service
- `stopDiscovery()` - Stop discovery service
- `refreshNodes()` - Manual refresh

**Responsibilities:**
- Node discovery
- Discovery service management
- Node list updates

### Network Layer

#### TruthApi (Retrofit Interface)
**Purpose:** HTTP API communication.

**Key Endpoints:**
- `GET /api/v1/events` - List events
- `GET /api/v1/events/{id}` - Get event
- `POST /api/v1/events` - Create event
- `PUT /api/v1/events/{id}` - Update event
- `DELETE /api/v1/events/{id}` - Delete event
- `POST /api/v1/impacts` - Add impact
- `POST /api/v1/judgments` - Submit judgment
- `GET /api/v1/contexts` - List contexts
- `POST /api/v1/contexts` - Create context
- `GET /api/v1/nodes` - List nodes
- `GET /api/v1/info` - Node info
- `GET /api/v1/stats` - Statistics

**Responsibilities:**
- HTTP request/response handling
- JSON serialization/deserialization
- Error handling

#### NetworkModule
**Purpose:** Network configuration and dependency injection.

**Key Components:**
- OkHttp client setup
- Retrofit instance creation
- API interface provision
- Interceptor configuration

**Responsibilities:**
- Network configuration
- Dependency provision
- Interceptor setup

## Background Work

### SyncWorker (WorkManager)

**Purpose:** Background synchronization with server.

**Configuration:**
- **Interval:** 15 minutes (configurable)
- **Constraints:** Network required, battery not low
- **Retry Policy:** Exponential backoff

**Workflow:**
```
Trigger → Check Network → Pull from Server → Push to Server → Update Sync Status → Complete
```

**Responsibilities:**
- Periodic synchronization
- Network state checking
- Sync queue processing
- Error handling and retry

### NodeSyncWorker (WorkManager)

**Purpose:** Background node discovery and sync.

**Configuration:**
- **Interval:** 15 minutes (configurable)
- **Constraints:** Network required

**Workflow:**
```
Trigger → UDP Discovery → HTTP Reachability → Update Nodes → Complete
```

**Responsibilities:**
- Node discovery
- Reachability checks
- Node list updates

## P2P and Discovery

### P2PClient
**Purpose:** P2P communication client.

**Key Functions:**
- UDP multicast listener for LAN discovery
- HTTP sync with discovered nodes
- Message handling

**Responsibilities:**
- Peer discovery
- P2P communication
- Message processing

### LanDiscoveryClient
**Purpose:** LAN-based node discovery.

**Key Functions:**
- UDP multicast listener (`239.255.0.1:52525`)
- Node announcement
- Node discovery callback

**Responsibilities:**
- LAN discovery
- Node detection
- Discovery callbacks

### P2PDiscoveryService
**Purpose:** Background discovery service.

**Key Functions:**
- Start/stop discovery
- Periodic discovery
- Node list management

**Responsibilities:**
- Discovery service management
- Periodic discovery
- Node list updates

## Truth Core Integration

### TruthCore (JNI Bridge)
**Purpose:** Bridge to Rust core library.

**Key Functions:**
- `initNode()` - Initialize Truth Core node
- `processJsonRequest(json: String): String` - Process JSON request
- `verifySignature(message, signature, publicKey): Boolean` - Verify signature

**Responsibilities:**
- JNI integration
- Core library access
- Cryptographic operations

## State Management

### ViewModels

#### NodesViewModel (`ui/compose/nodes/NodesViewModel.kt`)
**Purpose:** Nodes screen state management.

**State:**
- `nodes: StateFlow<List<NodeEntity>>`
- `discoveryStatus: StateFlow<DiscoveryStatus>`
- `loading: StateFlow<Boolean>`
- `error: StateFlow<String?>`

**Actions:**
- `refreshNodes()`
- `startDiscovery()`
- `stopDiscovery()`

**Responsibilities:**
- Nodes state management
- Discovery coordination
- UI state updates

## Data Flow

### Event Creation Flow
```
User Input → EventCreateScreen → Form Validation → EventRepository.createEvent() → Room Insert → Success → Navigation
                                                                                      ↓
                                                                              SyncQueue (if offline)
```

### Sync Flow
```
SyncWorker Trigger → Check Network → EventRepository.syncFromServer() → TruthApi → Room Insert → Update UI
                                                                                              ↓
                                                                                    SyncQueue Processing
```

### Template Matching Flow
```
Event Load → Extract Context Fields → ContextTemplateRepository.matchTemplate() → Display Template Name or Create Button
```

## Validation Rules

### Event Validation
- **Description:** Required
- **End Date:** Must be >= start date (if both provided)
- **Context Fields:** Optional, but if provided must reference existing records
- **Impact Level:** Must be integer between 1-5

### Judgment Validation
- **Assessment:** Must be 'true', 'false', or 'uncertain'
- **Confidence Level:** Must be between 0.0-1.0

### Template Validation
- **Name:** Required
- **Duplicate Detection:** Non-NULL fields must be unique

## Offline-First Strategy

### Local-Wins Conflict Resolution
- Local changes take precedence over remote changes
- Remote changes are merged when no local conflict exists
- Conflicts are resolved by timestamp (newer wins)

### Sync Queue
- Operations queued when offline
- Processed when connection restored
- Retry with exponential backoff
- Max retries: 3 (configurable)

### Background Sync
- WorkManager triggers sync every 15 minutes
- Network constraint required
- Battery optimization considered

## Node Discovery (v1.0.0)

### Discovery Methods
1. **UDP Multicast (LAN):**
   - Listens on `239.255.0.1:52525`
   - Discovers nodes on local network
   - TTL-based cleanup

2. **Global Registry Polling:**
   - HTTP polling of global registry
   - Periodic updates
   - TTL-based cleanup

3. **HTTP Reachability Checks:**
   - Verifies node accessibility
   - Updates node status
   - Removes unreachable nodes

### Discovery UI Integration
- `NodesScreen` displays discovered nodes
- Real-time updates via Flow
- Manual refresh support
- Discovery status indicator

## Performance Considerations

### Database Optimization
- Efficient queries with indexes
- Flow-based reactive updates
- Lazy loading for large lists
- Pagination support

### Network Optimization
- Request caching where appropriate
- Batch operations when possible
- Background sync intervals
- Battery-efficient networking

### UI Performance
- LazyColumn for large lists
- Remember for expensive computations
- State hoisting for reusability
- Efficient recomposition

## Error Handling

### User-Facing Errors
- Display Snackbar with error message
- Provide retry options where applicable
- Show error states in UI
- Handle network errors gracefully

### Developer-Facing Errors
- Log detailed error information
- Include stack traces in development
- Report errors to crash reporting (if implemented)

## Accessibility

### Material Design 3
- Semantic components
- Screen reader support
- High contrast support
- Touch target sizes (48dp minimum)

### Navigation
- Back button support
- Deep linking (if implemented)
- Navigation state preservation

## Testing

### Unit Tests
- Repository tests
- ViewModel tests
- Use case tests
- Validation tests

### Integration Tests
- Database tests
- Network tests
- Sync tests

### UI Tests
- Compose UI tests
- Navigation tests
- User flow tests

## Version Information

This specification reflects **Truth Training v1.0.0** Android mobile client functionality. All screens, components, and features described are implemented and tested as of this version.

---

_Version: v1.0.0_

- See [docs/README.md](../docs/README.md) for detailed explanations.
- See [spec/README.md](README.md) for specification index.

