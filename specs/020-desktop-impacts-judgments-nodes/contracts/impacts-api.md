# Impacts API Contract

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Component**: Impacts API  
**Date**: 2025-01-XX

## Overview

Contract specification for Impacts API in Desktop UI, defining request/response formats, validation rules, and error handling.

## Tauri Command: `add_impact`

### Request

```typescript
interface AddImpactRequest {
  event_id: string | number;
  impact_level: number;  // 1-5
  notes?: string;
}
```

### Response

**Success** (200):
```typescript
interface Impact {
  id: string;
  event_id: string;
  impact_level: number;  // 1-5
  notes?: string;
  created_at: string;     // ISO 8601 timestamp
}
```

**Error** (400):
```typescript
{
  error: string;  // e.g., "impact_level must be between 1 and 5"
}
```

### Validation Rules

1. `impact_level`: Must be integer in range [1, 5]
2. `event_id`: Must be valid event ID (exists in database)
3. `notes`: Optional, max length 1000 characters

### Backend Implementation

**Command**: `add_impact` in `ui/desktop/src-tauri/src/commands/impacts.rs`

**Mapping Logic**:
- `impact_level` (1-5) → `type_id` (1-5)
- `impact_level > 3` → `value = true` (positive)
- `impact_level <= 3` → `value = false` (negative)

**Storage**:
- Calls `truth_storage::add_impact()` with mapped values
- Returns created impact with `created_at` timestamp

### Error Cases

1. **Invalid impact_level**: `impact_level < 1 || impact_level > 5`
   - Error: "impact_level must be between 1 and 5"

2. **Invalid event_id**: Event does not exist
   - Error: "Invalid event_id: {event_id}"

3. **Storage error**: Database operation fails
   - Error: "Failed to add impact: {error_message}"

### Frontend Usage

```typescript
// ApiService.addImpact()
const impact = await ApiService.addImpact({
  event_id: eventId,
  impact_level: 4,  // 1-5
  notes: "Optional notes"
});
```

---

## Get Impacts for Event (TODO)

### Request

```typescript
interface GetImpactsRequest {
  event_id: number;
}
```

### Response

**Success** (200):
```typescript
interface GetImpactsResponse {
  data: Impact[];
  total: number;
}
```

### Implementation Status

**Current**: Not implemented in Tauri backend  
**Alternative**: Filter all impacts by event_id in frontend after loading all impacts  
**Future**: Add `get_impacts_for_event` Tauri command

---

## Test Scenarios

### TC-001: Add Impact with Valid Data

**Given**: Valid event_id and impact_level 4  
**When**: Call `add_impact`  
**Then**: Impact is created and returned with id and created_at

### TC-002: Add Impact with Invalid Level

**Given**: impact_level = 6  
**When**: Call `add_impact`  
**Then**: Error returned: "impact_level must be between 1 and 5"

### TC-003: Add Impact with Notes

**Given**: Valid data with notes field  
**When**: Call `add_impact`  
**Then**: Impact is created with notes stored

### TC-004: Add Impact without Notes

**Given**: Valid data without notes field  
**When**: Call `add_impact`  
**Then**: Impact is created with notes = null

---

## References

- Tauri Command: `ui/desktop/src-tauri/src/commands/impacts.rs`
- ApiService: `ui/desktop/src/services/api.ts`
- Android Implementation: `specs/018-android-impacts-judgments/spec.md`

