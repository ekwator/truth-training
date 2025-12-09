# Contract: Flag Fields Rules for Edit Event Screen

**Feature**: 016-full-desktop-ui  
**Date**: 2025-12-09  
**Contract Type**: Field Editability and Auto-Set Rules

## Contract Definition

### Edit Event Screen - Flag Fields

#### detected Field
- **Field**: `detected`
- **Editability**: Editable (user can toggle)
- **Default Value**: Existing event value or `false` if null
- **Component**: Checkbox or FilterChip with `onClick` handler
- **Validation**: N/A (boolean field)

#### corrected Field
- **Field**: `corrected`
- **Editability**: Read-only display (set automatically)
- **Default Value**: Calculated based on timestamp_end changes
- **Component**: Read-only display (FilterChip with `enabled={false}` or text display)
- **Auto-Set Logic**:
  1. Track initial `timestamp_end` value when Edit modal opens
  2. If initial `timestamp_end` was null: keep existing `corrected` value (don't change)
  3. If initial `timestamp_end` was set and changed: automatically set `corrected = true`
- **Display**: Show current value but disable editing

## Auto-Set Algorithm

### Input
- `event.corrected`: Current corrected value from event
- `initialTimestampEnd`: Initial timestamp_end value when modal opened (can be null)
- `timestampEnd`: Current timestamp_end value (can be null)

### Output
- `corrected`: Calculated corrected value (boolean)

### Algorithm

```typescript
function calculateCorrected(
  eventCorrected: boolean,
  initialTimestampEnd: number | null,
  timestampEnd: number | null
): boolean {
  if (initialTimestampEnd === null) {
    // If End Timestamp was initially empty, Corrected is not set
    return eventCorrected;
  } else {
    // If End Timestamp was set and changed, Corrected is automatically set
    if (timestampEnd !== null && timestampEnd !== initialTimestampEnd) {
      return true;
    } else {
      return eventCorrected;
    }
  }
}
```

### Implementation Notes

1. **Track Initial Value**: Store `initialTimestampEnd` when Edit modal opens:
   ```typescript
   const [initialTimestampEnd, setInitialTimestampEnd] = useState<number | null>(null);
   
   useEffect(() => {
     if (editingEvent) {
       setInitialTimestampEnd(editingEvent.timestamp_end ?? null);
     }
   }, [editingEvent]);
   ```

2. **Calculate Corrected**: Use `useMemo` to recalculate when `timestampEnd` changes:
   ```typescript
   const corrected = useMemo(() => {
     return calculateCorrected(
       editingEvent?.corrected ?? false,
       initialTimestampEnd,
       editTimestampEnd
     );
   }, [editingEvent?.corrected, initialTimestampEnd, editTimestampEnd]);
   ```

3. **Include in Save**: Only send `corrected` in update request if it changed:
   ```typescript
   const correctedToSave = corrected !== editingEvent.corrected ? corrected : undefined;
   ```

## Test Cases

### detected Field

1. **detected Toggle**:
   - Given: Edit Event modal is open, event.detected = false
   - When: User toggles detected checkbox
   - Then: detected value changes to true

2. **detected Default**:
   - Given: Event has detected = null
   - When: Edit Event modal opens
   - Then: detected checkbox shows false (default)

### corrected Field

1. **corrected Read-Only Display**:
   - Given: Edit Event modal is open
   - When: User views corrected field
   - Then: Field is displayed as read-only (not editable)

2. **corrected Not Set When Initial timestamp_end Was Null**:
   - Given: Event has timestamp_end = null, corrected = false
   - When: User sets timestamp_end to current date
   - Then: corrected remains false (not auto-set)

3. **corrected Auto-Set When timestamp_end Changed**:
   - Given: Event has timestamp_end = 2024-01-01, corrected = false
   - When: User changes timestamp_end to 2024-01-02
   - Then: corrected is automatically set to true

4. **corrected Not Changed When timestamp_end Unchanged**:
   - Given: Event has timestamp_end = 2024-01-01, corrected = false
   - When: User opens Edit modal and doesn't change timestamp_end
   - Then: corrected remains false

5. **corrected Preserved When Initial Was Null**:
   - Given: Event has timestamp_end = null, corrected = true
   - When: User sets timestamp_end to current date
   - Then: corrected remains true (preserved, not reset)

## Save Logic

### Save Enabled When

Save button is enabled when at least one of the following conditions is true:
1. `detected` changed from original value
2. `corrected` changed from original value (auto-set)
3. `timestamp_end` changed from original value AND validation passes

### Save Request

Only include fields in update request if they changed:
- `detected`: Include if `detected !== event.detected`
- `corrected`: Include if `corrected !== event.corrected`
- `timestamp_end`: Include if `timestamp_end !== initialTimestampEnd && validation passes`

## References

- Android EventEditScreen: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt` (lines 94, 116-131, 333-343)

