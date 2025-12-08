# API Contracts: Desktop UI Synchronization

**Feature**: 015-request-desktop-ui  
**Date**: 2025-01-XX  
**Status**: Design Complete

## Overview

This document defines the Tauri command contracts for Desktop UI synchronization. Commands must match Android functionality while preserving Desktop-specific features.

## Tauri Commands

### Event Commands

#### create_event_fast

**Purpose**: Create a new event with embedded context fields.

**Request**:
```typescript
interface CreateEventRequest {
  description: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  vector: boolean;
  timestampStart: number; // UNIX timestamp
  timestampEnd: number | null; // UNIX timestamp, optional
}
```

**Response**:
```typescript
interface Event {
  id: number;
  description: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  vector: boolean;
  detected: boolean | null;
  corrected: boolean;
  timestampStart: number;
  timestampEnd: number | null;
  code: number;
  collectiveScore: number | null;
  // Display helpers
  categoryName: string | null;
  formaName: string | null;
  causeName: string | null;
  developName: string | null;
  effectName: string | null;
}
```

**Validation**:
- All context fields must exist in respective tables (FK validation)
- Timestamp validation (End >= Start, normalized)
- Description must be non-empty

**Errors**:
- 400 Bad Request: Invalid FK or validation error
- 500 Internal Server Error: Database error

#### get_event_fast

**Purpose**: Retrieve a single event by ID.

**Request**:
```typescript
interface GetEventRequest {
  eventId: number;
}
```

**Response**: `Event` (same as create_event_fast)

**Errors**:
- 404 Not Found: Event not found
- 500 Internal Server Error: Database error

#### list_events_fast

**Purpose**: List all events with pagination.

**Request**:
```typescript
interface ListEventsRequest {
  page?: number;
  pageSize?: number;
}
```

**Response**:
```typescript
interface ListEventsResponse {
  data: Event[];
  total: number;
}
```

**Errors**:
- 500 Internal Server Error: Database error

### Context Template Commands

#### list_contexts

**Purpose**: List all context templates.

**Request**: None

**Response**:
```typescript
interface ContextTemplate {
  id: number;
  name: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  description: string | null;
}
```

**Errors**:
- 500 Internal Server Error: Database error

#### create_context_template

**Purpose**: Create a new context template.

**Request**:
```typescript
interface CreateContextTemplateRequest {
  name: string;
  categoryId: number | null;
  formaId: number | null;
  causeId: number | null;
  developId: number | null;
  effectId: number | null;
  description: string | null;
}
```

**Response**: `ContextTemplate`

**Validation**:
- Name must be non-empty
- All context fields must exist in respective tables (FK validation)
- Duplicate detection: compare non-NULL fields only

**Errors**:
- 400 Bad Request: Invalid FK or validation error
- 409 Conflict: Duplicate template (non-NULL fields match existing template)
- 500 Internal Server Error: Database error

#### clear_context_templates

**Purpose**: Clear all context templates (used during language change).

**Request**: None

**Response**: `{ success: boolean }`

**Errors**:
- 500 Internal Server Error: Database error

### Knowledge Base Commands

#### knowledge_base_list

**Purpose**: List all knowledge base entities.

**Request**:
```typescript
interface KnowledgeBaseListRequest {
  entityType: 'category' | 'forma' | 'cause' | 'develop' | 'effect' | 'impact_type';
}
```

**Response**:
```typescript
interface KnowledgeBaseEntity {
  id: number;
  name: string;
  description: string | null;
  quality?: boolean; // For forma, cause, develop, effect
}
```

**Errors**:
- 400 Bad Request: Invalid entity type
- 500 Internal Server Error: Database error

#### reseed_knowledge_base

**Purpose**: Re-seed knowledge base with new locale using temporary tables solution.

**Request**:
```typescript
interface ReseedKnowledgeBaseRequest {
  locale: 'en' | 'ru';
  forceReseed: boolean; // If true, clear and re-seed
}
```

**Response**: `{ success: boolean }`

**Process** (in single transaction):
1. Create temporary tables: `temp_truth_events`, `temp_impact`, `temp_progress_metrics`
2. Copy data from main tables to temporary tables
3. Clear knowledge base tables
4. Insert new knowledge base records (same IDs, different names)
5. Restore data from temporary tables
6. Drop temporary tables

**Errors**:
- 400 Bad Request: Invalid locale
- 500 Internal Server Error: Database error or transaction failure

### Configuration Commands

#### get_app_config

**Purpose**: Get application configuration.

**Request**: None

**Response**:
```typescript
interface AppConfig {
  mode: 'core' | 'http';
  serverIp: string;
  serverPort: number;
  locale: 'en' | 'ru';
  nearbySync: boolean;
  nearbyIntervalMs: number;
}
```

**Errors**:
- 500 Internal Server Error: Config file read error

#### save_app_config

**Purpose**: Save application configuration.

**Request**: `AppConfig`

**Response**: `{ success: boolean }`

**Errors**:
- 400 Bad Request: Invalid config values
- 500 Internal Server Error: Config file write error

### Impact Commands

#### add_impact

**Purpose**: Add impact record to event.

**Request**:
```typescript
interface AddImpactRequest {
  eventId: number;
  typeId: number;
  value: boolean;
  notes: string | null;
}
```

**Response**:
```typescript
interface Impact {
  id: number;
  eventId: number;
  typeId: number;
  value: boolean;
  notes: string | null;
}
```

**Validation**:
- Event ID must exist
- Type ID must exist
- Value must be boolean

**Errors**:
- 400 Bad Request: Invalid FK or validation error
- 500 Internal Server Error: Database error

### Judgment Commands

#### submit_judgment_fast

**Purpose**: Submit judgment for event.

**Request**:
```typescript
interface SubmitJudgmentRequest {
  eventId: number;
  assessment: 'true' | 'false' | 'uncertain';
  confidenceLevel: number; // 0.0-1.0
  reasoning: string | null;
}
```

**Response**:
```typescript
interface Judgment {
  id: number;
  eventId: number;
  assessment: 'true' | 'false' | 'uncertain';
  confidenceLevel: number;
  reasoning: string | null;
  submittedAt: number; // UNIX timestamp
}
```

**Validation**:
- Event ID must exist
- Assessment must be one of: 'true', 'false', 'uncertain'
- Confidence level must be between 0.0 and 1.0

**Errors**:
- 400 Bad Request: Invalid FK or validation error
- 500 Internal Server Error: Database error

#### judgments_list_fast

**Purpose**: List judgments for event.

**Request**:
```typescript
interface JudgmentsListRequest {
  eventId: number;
}
```

**Response**:
```typescript
interface JudgmentsListResponse {
  data: Judgment[];
  total: number;
}
```

**Errors**:
- 500 Internal Server Error: Database error

#### get_judgment_stats

**Purpose**: Get judgment statistics for event.

**Request**:
```typescript
interface GetJudgmentStatsRequest {
  eventId: number;
}
```

**Response**:
```typescript
interface JudgmentStats {
  total: number;
  trueCount: number;
  falseCount: number;
  uncertainCount: number;
  averageConfidence: number;
  consensusPercentage: number;
}
```

**Errors**:
- 500 Internal Server Error: Database error

### Summary Commands

#### get_overall_metrics

**Purpose**: Get overall summary metrics.

**Request**: None

**Response**:
```typescript
interface OverallMetrics {
  totalEvents: number;
  detectedEvents: number;
  eventsWithConsensus: number;
  averageCollectiveScore: number;
  lastUpdated: number; // UNIX timestamp
}
```

**Errors**:
- 500 Internal Server Error: Database error

#### list_event_rows

**Purpose**: List event rows for summary table.

**Request**:
```typescript
interface ListEventRowsRequest {
  page?: number;
  pageSize?: number;
}
```

**Response**:
```typescript
interface EventRow {
  id: number;
  description: string;
  detected: boolean | null;
  collectiveScore: number | null;
  timestampStart: number;
  timestampEnd: number | null;
}
```

**Errors**:
- 500 Internal Server Error: Database error

#### export_overall_summary_txt

**Purpose**: Export overall summary as text file.

**Request**: None

**Response**: `{ filePath: string }`

**Errors**:
- 500 Internal Server Error: File write error

## Error Response Format

All commands return errors in consistent format:

```typescript
interface ErrorResponse {
  error: string;
  code: number; // HTTP status code equivalent
  message: string;
}
```

## Transaction Safety

### Database Operations

**Single Transaction**: All multi-step operations (e.g., knowledge base re-seeding) must be in a single transaction.

**Rollback**: On error, transaction must rollback completely.

**Atomicity**: Either all operations succeed or all fail.

### Temporary Tables Solution

**Process**:
1. Begin transaction
2. Create temporary tables
3. Copy data
4. Clear knowledge base
5. Seed knowledge base
6. Restore data
7. Drop temporary tables
8. Commit transaction

**Error Handling**: On any error, rollback transaction and cleanup temporary tables.

## Performance Requirements

### Response Times

- Simple queries: <100ms
- Complex queries: <500ms
- Knowledge base re-seeding: <5 seconds
- File operations: <1 second

### Data Size

- Support up to 10,000 events
- Support up to 1,000 context templates
- Support up to 100,000 impacts
- Support up to 50,000 judgments

## Validation Rules

### Event Validation

- Name: required, non-empty string
- Description: required, non-empty string
- All context fields: required, non-null (FK validation)
- Start Timestamp: required, valid UNIX timestamp
- End Timestamp: optional, but if provided:
  - Must be >= Start Timestamp (normalized to start of day)
  - Can be equal to Start Timestamp

### Template Validation

- Name: required, non-empty string
- All context fields: required, non-null (FK validation)
- Duplicate detection: compare non-NULL fields only

### Judgment Validation

- Assessment: required, must be 'true', 'false', or 'uncertain'
- Confidence Level: required, must be between 0.0 and 1.0

## References

- [Android UI Specification](../../../docs/ANDROID_UI_SPECIFICATION.md)
- [Data Model](data-model.md)

---

**Status**: Design complete, ready for implementation

