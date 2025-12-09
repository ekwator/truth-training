# Data Model: Timestamp and Flag Fields Rules

**Feature**: 016-full-desktop-ui  
**Date**: 2025-12-09  
**Phase**: 1 - Design

## Entity: Event

### Fields

#### Timestamp Fields

1. **timestamp_start** (number, required)
   - **Type**: UNIX timestamp (seconds since epoch)
   - **Edit Event Screen**: Read-only (not editable)
   - **New Event Screen**: Editable, defaults to current date, required
   - **Validation**: Must be valid timestamp

2. **timestamp_end** (number | null, optional)
   - **Type**: UNIX timestamp (seconds since epoch) or null
   - **Edit Event Screen**: Editable, defaults to current date if null
   - **New Event Screen**: Editable, can be null/empty
   - **Validation**: 
     - If set, must be >= timestamp_start (normalized to start of day for comparison)
     - Can be cleared (set to null)

#### Flag Fields

1. **detected** (boolean | null, optional)
   - **Type**: Boolean or null
   - **Edit Event Screen**: Editable (user can toggle)
   - **New Event Screen**: Not applicable (not part of create flow)
   - **Default**: false if null

2. **corrected** (boolean, required)
   - **Type**: Boolean
   - **Edit Event Screen**: Read-only display, set automatically
   - **New Event Screen**: Not applicable (not part of create flow)
   - **Auto-Set Logic**:
     - Track initial `timestamp_end` value when Edit modal opens
     - If initial `timestamp_end` was null: keep existing `corrected` value (don't change)
     - If initial `timestamp_end` was set and changed: automatically set `corrected = true`
   - **Display**: Show current value but disable editing

## Field Editability Rules

### Edit Event Screen

| Field | Editability | Default Value | Notes |
|-------|-------------|---------------|-------|
| description | Read-only | Existing value | Display only |
| timestamp_start | Read-only | Existing value | Display only |
| timestamp_end | Editable | Current date if null | Can be changed |
| detected | Editable | Existing value or false | User can toggle |
| corrected | Auto-set | Calculated | Display only, not editable |
| Context fields | Read-only | Existing values | Display only |

### New Event Screen

| Field | Editability | Default Value | Notes |
|-------|-------------|---------------|-------|
| description | Editable | Empty | Required |
| timestamp_start | Editable | Current date | Required, cannot be empty |
| timestamp_end | Editable | null | Optional, can be cleared |
| detected | N/A | N/A | Not part of create flow |
| corrected | N/A | N/A | Not part of create flow |
| Context fields | Editable | null | All optional |

## Validation Rules

### Edit Event Screen

1. **timestamp_end validation**:
   - If set, must be >= timestamp_start (normalized to start of day)
   - Can be null (cleared)
   - Error message: "End timestamp cannot be less than start timestamp"

2. **Save enabled when**:
   - `detected` changed OR
   - `corrected` changed (auto-set) OR
   - `timestamp_end` changed (and validation passes)

### New Event Screen

1. **timestamp_start validation**:
   - Required (cannot be null)
   - Must be valid timestamp

2. **timestamp_end validation**:
   - If set, must be >= timestamp_start
   - Can be null (optional)

3. **Save enabled when**:
   - `description` is not empty AND
   - `timestamp_start` is set AND
   - All context fields are filled (if required) AND
   - `timestamp_end` validation passes (if set)

## Auto-Set Logic for `corrected` Field

### Algorithm

```typescript
// Track initial timestamp_end when modal opens
const initialTimestampEnd = event.timestamp_end;

// Calculate corrected value based on changes
const corrected = useMemo(() => {
  if (initialTimestampEnd === null) {
    // If End Timestamp was initially empty, Corrected is not set
    return event.corrected;
  } else {
    // If End Timestamp was set and changed, Corrected is automatically set
    if (timestampEnd !== null && timestampEnd !== initialTimestampEnd) {
      return true;
    } else {
      return event.corrected;
    }
  }
}, [timestampEnd, initialTimestampEnd, event.corrected]);
```

### Implementation Notes

1. Store `initialTimestampEnd` when Edit modal opens (use `useState` with `useEffect` or `useMemo`)
2. Recalculate `corrected` whenever `timestampEnd` changes
3. Include `corrected` in save request if it changed from original value
4. Display `corrected` value but disable editing (read-only display)

## Date Normalization

For timestamp comparison, normalize both timestamps to start of day (00:00:00) to compare dates without time:

```typescript
function normalizeToStartOfDay(timestamp: number): number {
  const date = new Date(timestamp * 1000);
  date.setHours(0, 0, 0, 0);
  return Math.floor(date.getTime() / 1000);
}
```

Use normalized values for comparison: `normalizeToStartOfDay(timestampEnd) >= normalizeToStartOfDay(timestampStart)`
