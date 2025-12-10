# Judgments API Contract

**Feature**: Desktop Impacts, Judgments, and Network Nodes UI  
**Component**: Judgments API  
**Date**: 2025-01-XX

## Overview

Contract specification for Judgments API in Desktop UI, defining request/response formats, validation rules, and error handling.

## Tauri Command: `submit_judgment_fast`

### Request

```typescript
interface SubmitJudgmentRequest {
  event_id: string;
  assessment: string;          // "true", "false", or "uncertain"
  confidence_level: number;     // 0.0 to 1.0
  reasoning?: string;
}
```

**Frontend Mapping**:
- Desktop UI uses: 'confirm' | 'reject' | 'abstain'
- Maps to backend: 'confirm' → "true", 'reject' → "false", 'abstain' → "uncertain"

### Response

**Success** (200):
```typescript
interface Judgment {
  id: string;
  event_id: string;
  assessment: string;          // "true", "false", or "uncertain"
  confidence_level: number;     // 0.0 to 1.0
  reasoning?: string;
  submitted_at: string;         // ISO 8601 timestamp
}
```

**Error** (400):
```typescript
{
  error: string;  // e.g., "invalid assessment" or "confidence_level must be in [0,1]"
}
```

### Validation Rules

1. `assessment`: Must be "true", "false", or "uncertain"
2. `confidence_level`: Must be float in range [0.0, 1.0]
3. `event_id`: Must be valid event ID (exists in database)
4. `reasoning`: Optional, max length 2000 characters

### Backend Implementation

**Command**: `submit_judgment_fast` in `ui/desktop/src-tauri/src/commands/judgments.rs`

**Storage**:
- Generates UUID for judgment id
- Sets `submitted_at` to current UTC timestamp
- Calls `db.insert_judgment()` to store in SQLite
- Returns created judgment

### Error Cases

1. **Invalid assessment**: Not "true", "false", or "uncertain"
   - Error: "invalid assessment"

2. **Invalid confidence_level**: `confidence_level < 0.0 || confidence_level > 1.0`
   - Error: "confidence_level must be in [0,1]"

3. **Storage error**: Database operation fails
   - Error: Database error message

### Frontend Usage

```typescript
// ApiService.createJudgment()
const judgment = await ApiService.createJudgment({
  event_id: eventId,
  assessment: 'confirm',  // Maps to "true"
  confidence_level: 0.8,
  reasoning: "Optional reasoning"
});
```

---

## Tauri Command: `judgments_list_fast`

### Request

```typescript
interface GetJudgmentsRequest {
  eventId?: string;      // Optional filter by event
  page: number;          // Pagination
  perPage: number;        // Pagination
}
```

### Response

**Success** (200):
```typescript
interface JudgmentListResponse {
  data: Judgment[];
  total: number;
}
```

### Frontend Usage

```typescript
// ApiService.getJudgments()
const response = await ApiService.getJudgments(eventId, page, perPage);
const judgments = response.data;
```

---

## Test Scenarios

### TC-001: Submit Judgment with Valid Data

**Given**: Valid event_id, assessment="true", confidence_level=0.8  
**When**: Call `submit_judgment_fast`  
**Then**: Judgment is created and returned with id and submitted_at

### TC-002: Submit Judgment with Invalid Assessment

**Given**: assessment="invalid"  
**When**: Call `submit_judgment_fast`  
**Then**: Error returned: "invalid assessment"

### TC-003: Submit Judgment with Invalid Confidence

**Given**: confidence_level=1.5  
**When**: Call `submit_judgment_fast`  
**Then**: Error returned: "confidence_level must be in [0,1]"

### TC-004: Submit Judgment with Reasoning

**Given**: Valid data with reasoning field  
**When**: Call `submit_judgment_fast`  
**Then**: Judgment is created with reasoning stored

---

## References

- Tauri Command: `ui/desktop/src-tauri/src/commands/judgments.rs`
- ApiService: `ui/desktop/src/services/api.ts`
- Android Implementation: `specs/018-android-impacts-judgments/spec.md`

