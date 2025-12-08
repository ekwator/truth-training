# Data Model: Desktop UI Synchronization

**Feature**: 015-request-desktop-ui  
**Date**: 2025-01-XX  
**Status**: Design Complete

## Overview

This document describes the data model, entity relationships, and data flow for the Desktop UI synchronization feature. The data model aligns with the Android implementation while preserving Desktop-specific functional features.

## Database Schema

### Core Tables

The Desktop UI uses the same database schema as Android, defined in `core/src/storage.rs` and documented in `docs/Data_Schema.md`.

#### Knowledge Base Tables

**category** (INTEGER PK, name TEXT, description TEXT)
- Lookup table for event categories
- Seeded with locale-specific names
- ID values must be identical across all languages

**forma** (INTEGER PK, name TEXT, quality BOOLEAN, description TEXT)
- Lookup table for form of logic
- Seeded with locale-specific names
- ID values must be identical across all languages

**cause** (INTEGER PK, name TEXT, quality BOOLEAN, description TEXT)
- Lookup table for causes
- Seeded with locale-specific names
- ID values must be identical across all languages

**develop** (INTEGER PK, name TEXT, quality BOOLEAN, description TEXT)
- Lookup table for manifestations
- Seeded with locale-specific names
- ID values must be identical across all languages

**effect** (INTEGER PK, name TEXT, quality BOOLEAN, description TEXT)
- Lookup table for consequences
- Seeded with locale-specific names
- ID values must be identical across all languages

**impact_type** (INTEGER PK, name TEXT, description TEXT)
- Lookup table for impact types
- Seeded with locale-specific names
- ID values must be identical across all languages

**context** (INTEGER PK, name TEXT, category_id FK, forma_id FK, cause_id FK, develop_id FK, effect_id FK, description TEXT)
- Template table for reusable context combinations
- All FK fields are nullable
- Duplicate detection based on non-NULL fields only

#### Event Tables

**truth_events** (INTEGER PK, description TEXT, category_id FK, forma_id FK, cause_id FK, develop_id FK, effect_id FK, vector BOOLEAN, detected BOOLEAN, corrected BOOLEAN, timestamp_start INTEGER, timestamp_end INTEGER, code INTEGER, collective_score REAL)
- Main event table
- Embedded context fields (category_id, forma_id, cause_id, develop_id, effect_id)
- All context fields are nullable
- FK relationships preserved during knowledge base re-seeding

**impact** (INTEGER PK, event_id FK, type_id FK, value BOOLEAN, notes TEXT)
- Impact records for events
- FK to truth_events.id (preserved during re-seeding)

**progress_metrics** (INTEGER PK, timestamp INTEGER, total_events INTEGER, total_events_group INTEGER, total_positive_impact REAL, total_positive_impact_group REAL, total_negative_impact REAL, total_negative_impact_group REAL, trend REAL, trend_group REAL)
- Aggregated progress metrics
- Generated from truth_events and impact data

## Entity Relationships

### Event Context Relationships

```
truth_events
├── category_id → category.id (nullable)
├── forma_id → forma.id (nullable)
├── cause_id → cause.id (nullable)
├── develop_id → develop.id (nullable)
└── effect_id → effect.id (nullable)
```

**Key Constraint**: All FK relationships must be preserved during knowledge base re-seeding. ID values in knowledge base tables remain constant across languages.

### Template Relationships

```
context
├── category_id → category.id (nullable)
├── forma_id → forma.id (nullable)
├── cause_id → cause.id (nullable)
├── develop_id → develop.id (nullable)
└── effect_id → effect.id (nullable)
```

**Duplicate Detection**: Templates are considered duplicates if all non-NULL context fields match exactly.

### Impact Relationships

```
impact
├── event_id → truth_events.id (required)
└── type_id → impact_type.id (required)
```

## Data Flow

### Event Creation Flow

```
User Input (NewEvent.tsx)
    ↓
Form Validation (client-side)
    ↓
Zustand Store (events.ts)
    ↓
Tauri Command (create_event_fast)
    ↓
Database Insert (storage.rs)
    ↓
Success Response
    ↓
UI Update (reactive state)
```

**Data Transformation**:
- Frontend: `EventFormData` → Tauri: `CreateEventRequest` → Database: `truth_events` row

### Template Selection Flow

```
NewEvent Screen
    ↓
User clicks "Select Template"
    ↓
Navigation flag set (selectTemplateForEvent = true)
    ↓
Navigate to Context Templates
    ↓
User selects template
    ↓
Template context stored in Zustand
    ↓
Navigate back to NewEvent
    ↓
Form fields updated from template
```

**Data Structure**:
```typescript
interface TemplateContext {
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
}
```

### Context Field Display Flow

```
Event Entity (with context IDs)
    ↓
Knowledge Base Entities (categories, formas, causes, develops, effects)
    ↓
Entity Name Resolution (getEntityNameById)
    ↓
Display Value (name or ID fallback)
```

**Algorithm**:
1. Lookup entity by ID in knowledge base list
2. Extract name field
3. Fallback to ID string if name not found
4. Update reactively when knowledge base changes

### Language Change Flow

```
Settings Screen
    ↓
User selects language
    ↓
Confirmation dialog
    ↓
Tauri Command (reseed_knowledge_base)
    ↓
Temporary Tables Solution:
  1. Create temp_truth_events, temp_impact, temp_progress_metrics
  2. Copy data to temporary tables
  3. Clear knowledge base tables
  4. Seed knowledge base with new locale
  5. Restore data from temporary tables
  6. Drop temporary tables
    ↓
Clear context templates
    ↓
Update UI locale
    ↓
UI re-renders with new language
```

**Data Preservation**:
- Event data (truth_events) preserved via temporary tables
- Impact data preserved via temporary tables
- Progress metrics preserved via temporary tables
- FK relationships maintained (IDs unchanged)
- Context templates cleared (user-created data)

## State Management

### Frontend State (Zustand)

**Navigation State**:
```typescript
interface NavigationState {
  selectTemplateForEvent: boolean;
  viewJudgments: boolean;
  setSelectTemplateForEvent: (value: boolean) => void;
  setViewJudgments: (value: boolean) => void;
}
```

**Event State**:
```typescript
interface EventsState {
  events: Event[];
  selectedEvent: Event | null;
  loading: boolean;
  error: string | null;
  // ... actions
}
```

**Template Context State**:
```typescript
interface TemplateContextState {
  selectedTemplateContext: TemplateContext | null;
  setSelectedTemplateContext: (context: TemplateContext | null) => void;
}
```

### Backend State (Tauri)

**Database Connection**:
- Managed via `Db` struct with `Mutex<Connection>`
- Single connection per application instance
- WAL mode enabled for concurrent access

**Configuration State**:
- Stored in `~/.truth-training/config.json`
- Includes: mode, server_ip, server_port, locale, nearby_sync, nearby_interval_ms

## Data Validation

### Event Validation

**Client-Side** (TypeScript):
- Name: required, non-empty string
- Description: required, non-empty string
- All context fields: required, non-null (categoryId, formaId, causeId, developId, effectId)
- Start Timestamp: required, defaults to current date
- End Timestamp: optional, but if provided:
  - Must be >= Start Timestamp (normalized to start of day)
  - Can be equal to Start Timestamp

**Server-Side** (Rust):
- FK validation: all context IDs must exist in respective tables
- Timestamp validation: normalized comparison
- Vector: boolean validation

### Template Validation

**Client-Side** (TypeScript):
- Name: required, non-empty string
- All context fields: required, non-null
- Duplicate detection: compare non-NULL fields only

**Server-Side** (Rust):
- FK validation: all context IDs must exist
- Duplicate detection: SQL query for matching non-NULL fields

## Knowledge Base Re-seeding

### Temporary Tables Solution

**Purpose**: Preserve event data during language change while updating knowledge base names.

**Process** (in single transaction):

1. **Create Temporary Tables**:
```sql
CREATE TABLE temp_truth_events AS SELECT * FROM truth_events;
CREATE TABLE temp_impact AS SELECT * FROM impact;
CREATE TABLE temp_progress_metrics AS SELECT * FROM progress_metrics;
```

2. **Clear Knowledge Base**:
```sql
DELETE FROM context;
DELETE FROM impact_type;
DELETE FROM effect;
DELETE FROM develop;
DELETE FROM cause;
DELETE FROM forma;
DELETE FROM category;
```

3. **Seed Knowledge Base** (with new locale):
- Insert category records (same IDs, new names)
- Insert forma records (same IDs, new names)
- Insert cause records (same IDs, new names)
- Insert develop records (same IDs, new names)
- Insert effect records (same IDs, new names)
- Insert impact_type records (same IDs, new names)

4. **Restore Event Data**:
```sql
INSERT INTO truth_events SELECT * FROM temp_truth_events;
INSERT INTO impact SELECT * FROM temp_impact;
INSERT INTO progress_metrics SELECT * FROM temp_progress_metrics;
```

5. **Drop Temporary Tables**:
```sql
DROP TABLE temp_truth_events;
DROP TABLE temp_impact;
DROP TABLE temp_progress_metrics;
```

**Key Constraint**: All operations must be in a single transaction to ensure atomicity.

## Context Field Display

### Entity Name Resolution

**Algorithm**:
```typescript
function getEntityNameById<T>(
  id: number | null,
  entities: T[],
  getId: (entity: T) => number,
  getName: (entity: T) => string
): string | null {
  if (id === null) return null;
  const entity = entities.find(e => getId(e) === id);
  return entity ? getName(entity) : null;
}
```

**Usage**:
```typescript
const categoryDisplay = useMemo(() => {
  if (!event.categoryId) return null;
  const name = getEntityNameById(
    event.categoryId,
    categories,
    (c) => c.id,
    (c) => c.name
  );
  return name || event.categoryId.toString();
}, [event.categoryId, categories]);
```

**Reactive Updates**:
- `useMemo` with dependencies: [fieldId, entities]
- Updates automatically when knowledge base changes
- Falls back to ID if name not found

## Data Synchronization

### Desktop-Specific Features

**Offline Queue**:
- Events created offline stored in Zustand
- Synced when connection available
- Tauri commands handle offline/online state

**Local-First Architecture**:
- All data stored locally in SQLite
- Tauri backend provides direct database access
- No external API required for core functionality

### Android Parity

**Shared Patterns**:
- Same database schema
- Same validation rules
- Same entity relationships
- Same knowledge base re-seeding approach

**Differences**:
- Desktop uses Tauri backend (Rust) vs Android Room (Kotlin)
- Desktop uses Zustand (TypeScript) vs Android StateFlow (Kotlin)
- Desktop uses React components vs Android Compose

## Performance Considerations

### Database Operations

**Indexes**:
- FK fields indexed for join performance
- Timestamp fields indexed for range queries

**Query Optimization**:
- Use prepared statements
- Batch operations where possible
- Transaction-based updates

### UI Updates

**Reactive State**:
- Zustand provides efficient state updates
- React memoization prevents unnecessary re-renders
- Entity name resolution cached via useMemo

**Large Datasets**:
- Pagination for event lists
- Lazy loading for knowledge base entities
- Virtual scrolling for long lists (if needed)

## Error Handling

### Data Validation Errors

**Client-Side**:
- Inline error messages for form fields
- Disabled save button when validation fails
- Clear error states on field change

**Server-Side**:
- FK constraint violations return 400 Bad Request
- Duplicate template returns 409 Conflict
- Database errors return 500 Internal Server Error

### Data Integrity Errors

**Recovery**:
- Transaction rollback on failure
- Temporary tables cleanup on error
- Event data restoration from backup (if available)

## Migration Notes

### Schema Compatibility

**No Schema Changes Required**: Desktop UI synchronization does not require database schema changes. Existing schema supports all required functionality.

### Data Migration

**Language Change**:
- Uses temporary tables solution (no data loss)
- FK relationships preserved
- Event data intact

**Context Templates**:
- Cleared on language change (user-created data)
- Can be recreated after language change

## References

- [Data Schema Documentation](../../docs/Data_Schema.md)
- [Android UI Specification](../../docs/ANDROID_UI_SPECIFICATION.md)
- [Localization Implementation](../014-android-localization/LOCALIZATION_IMPLEMENTATION.md)

---

**Status**: Design complete, ready for implementation

