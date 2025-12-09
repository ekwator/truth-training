# Research: Timestamp and Flag Fields Rules for New Event and Edit Event Screens

**Feature**: 016-full-desktop-ui  
**Date**: 2025-12-09  
**Phase**: 0 - Research

## Research Objective

Verify and correct the rules for editing and filling timestamp fields for New Event and Edit Event screens, as well as detected and corrected fields for Edit Event screen, according to Android client application rules.

## Android Implementation Analysis

### EventEditScreen (Android)

**Source**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt`

#### Timestamp Fields Rules

1. **Start Timestamp**:
   - **Editability**: NOT editable (read-only)
   - **Implementation**: `enabled = false` in DatePickerField
   - **Value**: Always uses existing event value (`event.timestampStart`)
   - **Code Reference**: Lines 100-101, 275-281

2. **End Timestamp**:
   - **Editability**: Always available for editing
   - **Default Value**: If not filled, defaults to current date (`System.currentTimeMillis()`)
   - **Implementation**: `enabled = true` in DatePickerField
   - **Validation**: Cannot be less than Start Timestamp (normalized to start of day for comparison)
   - **Code Reference**: Lines 106-111, 283-307

#### Flag Fields Rules

1. **detected**:
   - **Editability**: Editable (user can toggle)
   - **Implementation**: FilterChip with `onClick = { detected = !detected }`
   - **Default Value**: Uses existing event value or `false` if null
   - **Code Reference**: Lines 94, 333-337

2. **corrected**:
   - **Editability**: NOT editable (set automatically)
   - **Implementation**: FilterChip with `enabled = false`, `onClick = { }`
   - **Auto-Set Logic**:
     - If End Timestamp was initially empty (`initialTimestampEnd == null`): Corrected is NOT set (keeps existing value)
     - If End Timestamp was set and changed (`timestampEnd != null && timestampEnd != initialTimestampEnd`): Corrected is automatically set to `true`
   - **Code Reference**: Lines 116-131, 338-343

#### Save Logic

- Save is enabled when:
  - `detected` changed OR
  - `corrected` changed OR
  - `timestampEnd` changed (and validation passes)
- Code Reference: Lines 133-135

### EventCreateScreen (Android)

**Source**: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt`

#### Timestamp Fields Rules

1. **Start Timestamp**:
   - **Editability**: Editable
   - **Default Value**: Current date (`System.currentTimeMillis()`)
   - **Required**: Cannot be empty
   - **Code Reference**: Lines 69-72

2. **End Timestamp**:
   - **Editability**: Editable
   - **Default Value**: `null` (can be empty)
   - **Clear Capability**: Can be cleared (allows empty value)
   - **Validation**: Cannot be less than Start Timestamp
   - **Code Reference**: Lines 73-74

## Current Desktop Implementation Analysis

### Edit Event Screen (`ui/desktop/src/pages/Events.tsx`)

**Current Implementation Issues**:

1. **End Timestamp Default**: ✅ Correctly defaults to current date if not filled (line 64)
2. **detected Field**: ✅ Correctly editable (line 41-42)
3. **corrected Field**: ❌ **MISSING** - Not implemented
4. **Start Timestamp**: ❌ **NOT VERIFIED** - Need to check if it's read-only

### New Event Screen (`ui/desktop/src/pages/NewEvent.tsx`)

**Current Implementation**:

1. **Start Timestamp**: ✅ Defaults to current date (line 44)
2. **End Timestamp**: ✅ Can be null/empty (line 45)
3. **Validation**: ✅ Has validation for `timestamp_end >= timestamp_start`

## Required Corrections

### Edit Event Screen Corrections

1. **Start Timestamp**: Must be read-only (not editable)
2. **corrected Field**: Must be added with automatic setting logic:
   - Track initial `timestamp_end` value
   - If initial `timestamp_end` was null: don't change `corrected`
   - If initial `timestamp_end` was set and changed: automatically set `corrected = true`
3. **corrected Field Display**: Must be displayed but not editable (read-only display)

### New Event Screen Verification

1. **Start Timestamp**: Verify it defaults to current date and is required
2. **End Timestamp**: Verify it can be empty/null

## Technical Decisions

1. **corrected Auto-Set Logic**: Implement using `useEffect` or `useMemo` to track changes to `timestampEnd` compared to initial value
2. **Start Timestamp Read-Only**: Use `disabled` prop on DatePickerField component
3. **corrected Display**: Add read-only display of `corrected` field in Edit Event modal

## References

- Android EventEditScreen: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventEditScreen.kt`
- Android EventCreateScreen: `truth-android-client/app/src/main/java/com/truth/training/client/ui/compose/events/EventCreateScreen.kt`
- Desktop Events.tsx: `ui/desktop/src/pages/Events.tsx`
- Desktop NewEvent.tsx: `ui/desktop/src/pages/NewEvent.tsx`
