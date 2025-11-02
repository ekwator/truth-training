# Research: Align Android Client with Desktop v1.0.0

**Date**: 2025-11-02  
**Feature**: Align Truth Training Android Client with Desktop v1.0.0 Features  
**Branch**: 007-title-align-truth

---

## Resolved Clarifications

### CLAR-001: Android Screen Coverage
**Question**: Should Android support all 8 screens from Desktop, or a subset optimized for mobile?

**Decision**: Android will implement all core functional screens but adapt UI/UX for mobile constraints:
- **Full Feature Parity**: All 8 screens from Desktop (Dashboard, New Event, Context Editor, Event Summary, Overall Summary, Training Results, Logs, Settings)
- **Mobile Optimization**: Stack navigation, bottom tabs, or drawer navigation instead of desktop menu bar
- **Rationale**: Functional parity required for cross-platform data consistency; UI patterns adapt to platform norms

**Alternatives Considered**:
- Subset of screens: Rejected - breaks functional parity requirement (FR-025, FR-026)
- WebView wrapper: Rejected - violates Constitution (native implementation required)

---

### CLAR-002: Backward Compatibility
**Question**: Should Android maintain backward compatibility with older server versions, or require v1.0.0+?

**Decision**: Require v1.0.0+ server for new Android v1.0.0 client:
- **Breaking Change**: Android v1.0.0 requires Core/Server v1.0.0 (embedded fields, Context Templates API)
- **Migration Path**: Legacy Android v0.3.0 clients remain compatible with v0.4.0 servers
- **Version Check**: Android app validates server version on connect; shows clear error if incompatible
- **Rationale**: Aligns with Desktop v1.0.0 baseline; simplifies implementation by removing legacy context_id support

**Alternatives Considered**:
- Dual-mode support: Rejected - adds complexity, violates YAGNI principle (Constitution V)
- Feature flags: Rejected - temporary solution, creates maintenance burden

---

### CLAR-003: Mobile Performance Targets
**Question**: What are acceptable response time thresholds for mobile?

**Decision**: Mobile-optimized performance targets:
- **UI Response**: < 200ms for user actions (tap, swipe, navigation)
- **Data Loading**: < 500ms for initial screen load (first paint)
- **Sync Operations**: Background priority, < 2s for small batches (< 10 items)
- **Offline Queue**: Non-blocking, processed in background
- **Rationale**: Mobile devices have different performance characteristics than desktop; targets balance responsiveness with battery efficiency

**Alternatives Considered**:
- Desktop-equivalent targets (< 100ms): Rejected - unrealistic for mobile devices, battery impact
- Relaxed targets (< 5s): Rejected - poor user experience

---

## Technology Decisions

### Decision: Room Database (Android)
**What**: Use Room persistence library for local SQLite storage

**Rationale**:
- **Constitution Compliance**: SQLite requirement (Constitution V), Android-native solution
- **Type Safety**: Compile-time SQL validation, Kotlin type mapping
- **Observability**: LiveData/Flow integration for reactive UI updates
- **Migration Support**: Built-in schema migration handling
- **Reference Model**: Desktop uses rusqlite; Room provides equivalent abstraction on Android

**Alternatives Considered**:
- Direct SQLite (Android.database.sqlite): Rejected - verbose, error-prone, no compile-time validation
- Realm: Rejected - external dependency, adds complexity
- SharedPreferences only: Rejected - insufficient for relational data (events, templates, judgments)

---

### Decision: Migration from SharedPreferences to Room
**What**: Migrate from SharedPreferences (tokens only) to Room (full data model)

**Rationale**:
- **Functional Requirement**: FR-015 requires local storage for all data (events, templates, judgments)
- **Offline Support**: FR-016 requires offline queue; Room enables transaction-based operations
- **Data Consistency**: FR-027 requires referential integrity; Room enforces foreign keys

**Migration Strategy**:
1. Create Room database schema matching Desktop SQLite schema
2. Implement DAOs for all entities (Event, ContextTemplate, Judgment, Impact, Summary)
3. Migrate token storage: SharedPreferences → Room (or keep SharedPreferences for tokens only)
4. Add offline queue table for pending sync operations
5. Implement sync service using WorkManager for background processing

**Alternatives Considered**:
- Keep SharedPreferences: Rejected - cannot store relational data
- Hybrid approach: Considered - keep tokens in SharedPreferences, data in Room (acceptable)

---

### Decision: API Client Architecture
**What**: Retrofit-based API client with offline queue integration

**Rationale**:
- **Existing Infrastructure**: Android already uses Retrofit 2.11.0
- **Type Safety**: Retrofit interfaces provide compile-time API contract validation
- **Offline Queue**: OkHttp interceptors can queue requests when offline
- **Concurrency**: Coroutines + Flow for reactive data streams

**Implementation Pattern**:
```
Repository (TruthRepository)
  ├── Room Database (local source of truth)
  ├── Retrofit API (remote sync)
  └── Offline Queue (WorkManager + Room table)
```

**Alternatives Considered**:
- Ktor: Rejected - requires significant refactoring of existing code
- Volley: Rejected - deprecated, no coroutines support

---

### Decision: Context Templates API Integration
**What**: Implement Context Templates endpoints matching Desktop v1.0.0

**Rationale**:
- **API Compatibility**: FR-021 requires all Context Templates API endpoints
- **Functional Parity**: FR-006-FR-010 require template creation, selection, matching
- **Reference**: Desktop implements: `GET /contexts`, `POST /contexts`, `POST /contexts/match`, `POST /contexts/from-event`

**Endpoints to Implement**:
- `GET /api/v1/contexts` - List all templates
- `POST /api/v1/contexts` - Create template (with duplicate detection)
- `POST /api/v1/contexts/match` - Match event fields to templates
- `GET /api/v1/contexts/{id}` - Get template by ID
- Embedded fields in events: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` (replacing `context_id`)

**Alternatives Considered**:
- Template-only mode (no API): Rejected - breaks sync between Desktop and Android
- Simplified API subset: Rejected - breaks FR-021 requirement

---

### Decision: Event Data Model Migration
**What**: Migrate from `context_id` (legacy) to embedded context fields (v1.0.0)

**Rationale**:
- **API Compatibility**: FR-022 requires embedded fields matching Desktop v1.0.0
- **Breaking Change**: VERSION_REGISTRY.md documents v1.0.0 breaking change (context_id removed)
- **Reference Model**: Desktop uses embedded fields: `category_id`, `forma_id`, `cause_id`, `develop_id`, `effect_id` (all optional)

**Migration Strategy**:
1. Update Event DTO to remove `context_id`, add embedded fields
2. Update Room Entity to match new schema
3. Update API requests/responses to use embedded fields
4. Handle legacy data: if migration needed, convert `context_id` → embedded fields (requires knowledge base lookup)

**Alternatives Considered**:
- Support both formats: Rejected - breaks FR-022, adds complexity
- Gradual migration: Rejected - Android v1.0.0 targets v1.0.0 API baseline only

---

### Decision: Offline-First Architecture
**What**: Local-wins conflict resolution with background sync (matching Desktop)

**Rationale**:
- **Functional Requirement**: FR-018 requires same conflict strategy as Desktop
- **User Experience**: FR-019 requires no data loss during sync
- **Reference**: Desktop uses local-wins with background sync queue

**Implementation**:
- Room database as source of truth (local)
- Background sync via WorkManager (periodic sync when online)
- Conflict resolution: Local timestamp > remote timestamp = keep local
- Sync queue table tracks pending operations (create, update, delete)
- Sync status indicator shows online/offline state and pending count

**Alternatives Considered**:
- Remote-wins: Rejected - violates FR-018 (must match Desktop strategy)
- Last-write-wins (server timestamp): Rejected - violates FR-018

---

### Decision: P2P Discovery Extension
**What**: Maintain NSD discovery, extend for event propagation

**Rationale**:
- **Existing Infrastructure**: Android already implements NSD (`_truthnode._tcp.`)
- **Constitution Compliance**: Constitution IX requires cross-platform collective intelligence
- **Functional Enhancement**: Extend P2P for event sync (not just discovery)

**Implementation**:
- Keep NSD for peer discovery
- Add event propagation via HTTP P2P endpoints (matching Desktop)
- Use Ed25519 signatures for P2P message verification (already implemented)

**Alternatives Considered**:
- Remove P2P: Rejected - violates Constitution IX (cross-platform collective intelligence)
- Full mesh sync: Rejected - complexity, battery impact (deferred to future)

---

### Decision: Testing Strategy
**What**: Expand testing coverage to match Desktop standards

**Rationale**:
- **Quality Gates**: Constitution requires integration tests across layers
- **Functional Parity**: Ensure Android behavior matches Desktop

**Test Coverage**:
- **Unit Tests**: Room DAOs, ViewModels, API clients, validation logic
- **Integration Tests**: End-to-end workflows (create event → sync → view on Desktop)
- **Contract Tests**: API endpoint validation (match Desktop contracts)
- **UI Tests**: Espresso tests for critical user flows

**Alternatives Considered**:
- Manual testing only: Rejected - violates Constitution IV (integration testing requirement)
- Full E2E automation: Considered but deferred - sufficient coverage with unit + integration

---

## Dependencies Analysis

### Required Android Libraries
- **Room**: `androidx.room:room-runtime:2.6.1` (SQLite abstraction)
- **Room KTX**: `androidx.room:room-ktx:2.6.1` (Coroutines support)
- **Room Compiler**: `androidx.room:room-compiler:2.6.1` (kapt)
- **WorkManager**: `androidx.work:work-runtime-ktx:2.9.0` (background sync)
- **Existing**: Retrofit 2.11.0, OkHttp 4.12.0, Coroutines 1.9.0, BouncyCastle 1.78

### Core Library Integration
- **JNI Library**: `libtruth_core.so` (arm64-v8a, x86_64) - already integrated
- **Features**: Use `mobile` feature flag (Constitution compliance)
- **Version**: Target Core v1.0.0 (matching Desktop)

### CI/CD Updates
- **Workflow**: `.github/workflows/android-build.yml` - update version to 1.0.0
- **Artifacts**: APK generation, version tagging
- **Testing**: Add Room migration tests, API contract tests

---

## Risk Assessment

### High Risk
- **Data Migration**: Converting from SharedPreferences-only to Room requires careful schema design
- **API Breaking Changes**: Embedded fields require complete DTO refactoring
- **Sync Conflicts**: Local-wins strategy must match Desktop exactly

### Medium Risk
- **Performance**: Room queries + Retrofit + background sync may impact battery
- **Offline Queue**: Complex state management for pending operations

### Low Risk
- **P2P Extension**: NSD already works; extending for events is incremental
- **UI Adaptation**: Mobile navigation patterns are well-established

---

## Success Criteria

1. ✅ All NEEDS CLARIFICATION markers resolved
2. ✅ Technology stack selected (Room, WorkManager, Retrofit)
3. ✅ Migration strategy defined (SharedPreferences → Room)
4. ✅ API contracts identified (v1.0.0 endpoints)
5. ✅ Performance targets established (mobile-optimized)
6. ✅ Constitution compliance verified

---

## Next Steps

- **Phase 1**: Design data model (Room entities, DAOs)
- **Phase 1**: Design API contracts (Retrofit interfaces)
- **Phase 1**: Design sync architecture (offline queue, WorkManager)
- **Phase 2**: Generate tasks from design artifacts

