# Contract: Timestamp Fields Rules for Event Screens

**Feature**: 016-full-desktop-ui  
**Date**: 2025-12-09  
**Contract Type**: Field Editability Rules

## Contract Definition

### Edit Event Screen - Timestamp Fields

#### Start Timestamp
- **Field**: `timestamp_start`
- **Editability**: Read-only (not editable)
- **Display**: Show existing value as formatted date
- **Component**: DatePickerField with `disabled={true}` or read-only text display
- **Validation**: N/A (not editable)

#### End Timestamp
- **Field**: `timestamp_end`
- **Editability**: Always editable
- **Default Value**: Current date (`Date.now() / 1000`) if field is null
- **Component**: DatePickerField with `enabled={true}`
- **Validation**: 
  - If set, must be >= timestamp_start (normalized to start of day)
  - Can be cleared (set to null)
- **Error Message**: "End timestamp cannot be less than start timestamp"

### New Event Screen - Timestamp Fields

#### Start Timestamp
- **Field**: `timestamp_start`
- **Editability**: Editable
- **Default Value**: Current date (`Date.now() / 1000`)
- **Required**: Yes (cannot be empty)
- **Component**: DatePickerField with `enabled={true}`
- **Validation**: Must be valid timestamp, cannot be null

#### End Timestamp
- **Field**: `timestamp_end`
- **Editability**: Editable
- **Default Value**: null (can be empty)
- **Required**: No (optional)
- **Component**: DatePickerField with `enabled={true}`, `allowClear={true}`
- **Validation**: 
  - If set, must be >= timestamp_start
  - Can be null (optional)

## Test Cases

### Edit Event Screen

1. **Start Timestamp Read-Only**:
   - Given: Edit Event modal is open
   - When: User views Start Timestamp field
   - Then: Field is displayed as read-only (not editable)

2. **End Timestamp Default Value**:
   - Given: Event has null timestamp_end
   - When: Edit Event modal opens
   - Then: End Timestamp field shows current date as default

3. **End Timestamp Validation**:
   - Given: Event has timestamp_start = 2024-01-01
   - When: User sets timestamp_end = 2023-12-31
   - Then: Validation error is displayed: "End timestamp cannot be less than start timestamp"

4. **End Timestamp Can Be Cleared**:
   - Given: Event has timestamp_end set
   - When: User clears End Timestamp field
   - Then: timestamp_end is set to null

### New Event Screen

1. **Start Timestamp Default**:
   - Given: New Event screen is open
   - When: Screen loads
   - Then: Start Timestamp field shows current date

2. **Start Timestamp Required**:
   - Given: New Event screen is open
   - When: User tries to save without Start Timestamp
   - Then: Validation error is displayed

3. **End Timestamp Optional**:
   - Given: New Event screen is open
   - When: User saves event without End Timestamp
   - Then: Event is saved with timestamp_end = null

4. **End Timestamp Validation**:
   - Given: Start Timestamp = 2024-01-01
   - When: User sets End Timestamp = 2023-12-31
   - Then: Validation error is displayed

## References

- Android EventEditScreen: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt` (lines 100-111, 275-307)
- Android EventCreateScreen: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt` (lines 69-74)

