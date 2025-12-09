# Quickstart: Timestamp and Flag Fields Rules

**Feature**: 016-full-desktop-ui  
**Date**: 2025-12-09

## Overview

This quickstart guide covers the rules for editing and filling timestamp fields for New Event and Edit Event screens, as well as detected and corrected fields for Edit Event screen, according to Android client application rules.

## Key Rules

### Edit Event Screen

#### Timestamp Fields

1. **Start Timestamp**:
   - **Rule**: Read-only (not editable)
   - **Display**: Show existing value as formatted date
   - **Implementation**: Use read-only text display or DatePickerField with `disabled={true}`

2. **End Timestamp**:
   - **Rule**: Always editable
   - **Default**: Current date if field is null
   - **Validation**: Must be >= Start Timestamp (normalized to start of day)
   - **Clear**: Can be cleared (set to null)
   - **Implementation**: Use DatePickerField with `enabled={true}`, `allowClear={true}`

#### Flag Fields

1. **detected**:
   - **Rule**: Editable (user can toggle)
   - **Default**: Existing event value or `false` if null
   - **Implementation**: Use checkbox or toggle with `onChange` handler

2. **corrected**:
   - **Rule**: Read-only display, set automatically
   - **Auto-Set Logic**:
     - If initial `timestamp_end` was null: keep existing `corrected` value
     - If initial `timestamp_end` was set and changed: automatically set `corrected = true`
   - **Implementation**: Use read-only display (text or disabled FilterChip)

### New Event Screen

#### Timestamp Fields

1. **Start Timestamp**:
   - **Rule**: Editable, defaults to current date, required
   - **Validation**: Cannot be empty
   - **Implementation**: Use DatePickerField with `enabled={true}`, default to `Date.now() / 1000`

2. **End Timestamp**:
   - **Rule**: Editable, optional (can be null)
   - **Validation**: If set, must be >= Start Timestamp
   - **Clear**: Can be cleared (set to null)
   - **Implementation**: Use DatePickerField with `enabled={true}`, `allowClear={true}`

## Testing Scenarios

### Scenario 1: Edit Event - Start Timestamp Read-Only

1. Navigate to Events screen
2. Click Edit on an event
3. Verify Start Timestamp is displayed as read-only (not editable)
4. Verify Start Timestamp shows existing event value

### Scenario 2: Edit Event - End Timestamp Default

1. Navigate to Events screen
2. Click Edit on an event with null timestamp_end
3. Verify End Timestamp field shows current date as default
4. Verify End Timestamp is editable

### Scenario 3: Edit Event - End Timestamp Validation

1. Navigate to Events screen
2. Click Edit on an event
3. Set End Timestamp to a date before Start Timestamp
4. Verify validation error is displayed: "End timestamp cannot be less than start timestamp"
5. Verify save is disabled

### Scenario 4: Edit Event - detected Toggle

1. Navigate to Events screen
2. Click Edit on an event
3. Toggle detected checkbox
4. Verify detected value changes
5. Save event
6. Verify detected value is saved

### Scenario 5: Edit Event - corrected Auto-Set (Initial timestamp_end Was Set)

1. Navigate to Events screen
2. Click Edit on an event with timestamp_end = 2024-01-01, corrected = false
3. Change End Timestamp to 2024-01-02
4. Verify corrected is automatically set to true (display only, not editable)
5. Save event
6. Verify corrected value is saved as true

### Scenario 6: Edit Event - corrected Not Set (Initial timestamp_end Was Null)

1. Navigate to Events screen
2. Click Edit on an event with timestamp_end = null, corrected = false
3. Set End Timestamp to current date
4. Verify corrected remains false (not auto-set)
5. Save event
6. Verify corrected value remains false

### Scenario 7: New Event - Start Timestamp Default

1. Navigate to New Event screen
2. Verify Start Timestamp shows current date
3. Verify Start Timestamp is editable
4. Verify Start Timestamp is required

### Scenario 8: New Event - End Timestamp Optional

1. Navigate to New Event screen
2. Fill required fields
3. Leave End Timestamp empty
4. Save event
5. Verify event is saved with timestamp_end = null

## Implementation Checklist

### Edit Event Screen

- [ ] Start Timestamp displayed as read-only
- [ ] End Timestamp defaults to current date if null
- [ ] End Timestamp is editable
- [ ] End Timestamp validation (>= Start Timestamp)
- [ ] End Timestamp can be cleared
- [ ] detected field is editable (checkbox/toggle)
- [ ] corrected field is displayed (read-only)
- [ ] corrected auto-set logic implemented
- [ ] Save enabled when fields change
- [ ] Only changed fields included in save request

### New Event Screen

- [ ] Start Timestamp defaults to current date
- [ ] Start Timestamp is editable
- [ ] Start Timestamp is required
- [ ] End Timestamp is editable
- [ ] End Timestamp is optional (can be null)
- [ ] End Timestamp validation (>= Start Timestamp)
- [ ] End Timestamp can be cleared

## References

- `research.md` - Detailed research findings
- `data-model.md` - Entity models and validation rules
- `contracts/` - API contracts for field validation
- Android source: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/`
